package com.example.tetris.ai;

import com.example.tetris.core.state.OpMove;
import com.example.tetris.core.state.PureGame;
import com.example.tetris.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Socket-based AI that delegates planning to an external process via a simple line-delimited JSON
 * protocol.
 *
 * <h2>Protocol</h2>
 * For each {@link PureGame} snapshot, this AI:
 * <ol>
 *   <li>Opens a TCP connection to {@code host:port}.</li>
 *   <li>Sends one <em>single-line</em> compact JSON encoding of the snapshot, terminated by
 *   {@code '\n'}.</li>
 *   <li>Reads lines until the first non-blank JSON line is received and decodes it as
 *   {@link OpMove}.</li>
 * </ol>
 * The connection is short-lived per request (no pooling) to keep the implementation robust and
 * stateless.
 *
 * <h2>Resilience</h2>
 * Network errors and timeouts are retried with a small backoff until the task is interrupted or a
 * valid
 * response is received. The enclosing {@link Ai} cancels in-flight work when a new snapshot
 * arrives.
 *
 * <h2>Threading</h2>
 * Uses a single-threaded executor to perform blocking I/O. Call {@link #shutdown()} to release the
 * executor when finished.
 *
 * @implNote Connections are one-shot by design. If you later add connection pooling, ensure
 *     strict snapshot-to-request pairing and prompt interrupt handling.
 * @see Ai#submit(PureGame)
 * @see Ai#shouldStartNewPlan(PureGame, PureGame, OpMove)
 * @see Ai#getCurrentIntent(OpMove, PureGame)
 */
public class EternalAi extends Ai {

  /**
   * Default loopback host for the external AI service.
   */
  private static final String DEFAULT_HOST = "127.0.0.1";
  /**
   * Default TCP port for the external AI service.
   */
  private static final int DEFAULT_PORT = 3000;

  /**
   * Milliseconds to wait when establishing a TCP connection.
   */
  private static final int CONNECT_TIMEOUT_MS = 3000;
  /**
   * Socket read timeout for line polling; allows periodic interrupt checks.
   */
  private static final int READ_TIMEOUT_MS = 50;
  /**
   * Backoff between reconnect attempts after I/O errors.
   */
  private static final long RETRY_BACKOFF_MS = 100;

  /**
   * Compact JSON mapper for low-overhead wire format.
   */
  private static final JsonUtil COMPACT_JSON = JsonUtil.getInstance().copy()
      .configure(m -> m.disable(SerializationFeature.INDENT_OUTPUT));

  /**
   * Target host running the external AI.
   */
  private final String host;
  /**
   * Target port running the external AI.
   */
  private final int port;

  /**
   * Dedicated worker for blocking socket I/O.
   */
  private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "EternalAi-worker");
    t.setDaemon(true);
    return t;
  });

  /**
   * Creates an instance that connects to {@code 127.0.0.1:3000}.
   */
  public EternalAi() {
    this(DEFAULT_HOST, DEFAULT_PORT);
  }

  /**
   * Creates an instance that connects to the provided endpoint.
   *
   * @param host hostname or IP of the external AI server (non-null)
   * @param port TCP port of the external AI server
   *
   * @throws NullPointerException if {@code host} is null
   */
  public EternalAi(String host, int port) {
    this.host = Objects.requireNonNull(host, "host is null");
    this.port = port;
  }

  /**
   * Throws {@link CancellationException} if the current thread has been interrupted.
   *
   * <p>Use this guard at key points (before connect, before send, during read loops) to ensure
   * responsive cancellation when the enclosing {@link Ai} replaces an in-flight computation.</p>
   *
   * @throws CancellationException if the thread is interrupted
   */
  private static void checkInterrupted() {
    if (Thread.currentThread().isInterrupted()) {
      throw new CancellationException("submit interrupted/cancelled");
    }
    // cooperative checks hinge on the caller invoking cancel(true)
  }

  /**
   * Sleeps for the configured retry backoff, preserving interrupt semantics.
   *
   * @throws CancellationException if interrupted while sleeping
   */
  private static void sleepBackoff() {
    try {
      TimeUnit.MILLISECONDS.sleep(RETRY_BACKOFF_MS);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new CancellationException("Interrupted during backoff");
    }
  }

  /**
   * Replan heuristic specializing the base policy.
   *
   * <p>Replans when:</p>
   * <ul>
   *   <li>There was no previous snapshot,</li>
   *   <li>the cell raster changed (deep compare),</li>
   *   <li>the active tetromino’s {@code type} changed, or</li>
   *   <li>the active tetromino’s {@code rotation} changed.</li>
   * </ul>
   *
   * @implNote This assumes both {@code newSnapshot.tetromino()} and
   *     {@code lastSnapshot.tetromino()} are non-null. If your pipeline allows frames with no
   *     active piece, consider relaxing or guarding this to avoid NPEs.
   */
  @Override
  protected boolean shouldStartNewPlan(PureGame newSnapshot, PureGame lastSnapshot,
      OpMove existingPlan) {
    if (lastSnapshot == null) {
      return true;
    }
    if (!Arrays.deepEquals(newSnapshot.cells(), lastSnapshot.cells())) {
      return true;
    }
    if (newSnapshot.tetromino().type() != lastSnapshot.tetromino().type()) {
      return true;
    }
    return newSnapshot.tetromino().rotation() != lastSnapshot.tetromino().rotation();
  }

  /**
   * Submits a network task that sends the snapshot to the external AI and awaits an
   * {@link OpMove}.
   *
   * <p><strong>Behavior:</strong></p>
   * <ul>
   *   <li>Serializes the snapshot to a compact JSON line.</li>
   *   <li>Attempts request/response exchanges repeatedly until a valid line is received, an I/O
   *       error occurs (then retried with backoff), or the task is interrupted/cancelled.</li>
   *   <li>Propagates JSON mapping errors via {@link CompletionException}.</li>
   * </ul>
   *
   * @param snapshot the immutable game state to send (non-null)
   *
   * @return a future that completes with the decoded {@link OpMove}, or completes exceptionally
   *
   * @throws NullPointerException if {@code snapshot} is {@code null}
   */
  @Override
  protected CompletableFuture<OpMove> submit(PureGame snapshot) {
    Objects.requireNonNull(snapshot, "snapshot is null");
    return CompletableFuture.supplyAsync(() -> {
      try {
        final String json = COMPACT_JSON.toJson(snapshot);
        while (true) {
          checkInterrupted();
          try {
            return COMPACT_JSON.fromJson(requestOnce(json), OpMove.class);
          } catch (SocketTimeoutException e) {
            // Treat as transient; allow loop to retry.
            LOG.log(Level.FINE, "EternalAi: socket timeout at top-level, retrying", e);
          } catch (JsonProcessingException e) {
            // Serialization/parsing error: surface as completion failure.
            throw new CompletionException(e);
          } catch (IOException ioe) {
            // Connection issues: back off and retry.
            LOG.log(Level.FINE,
                "EternalAi: socket issue, retrying: " + ioe.getClass().getSimpleName(), ioe);
            sleepBackoff();
          }
        }
      } catch (JsonProcessingException e) {
        throw new CompletionException(e);
      }
    }, worker);
  }

  /**
   * Performs a single request/response exchange against the external AI.
   *
   * <p>Steps:</p>
   * <ol>
   *   <li>Open a fresh socket and configure timeouts.</li>
   *   <li>Send the provided JSON line with trailing newline.</li>
   *   <li>Read the first non-blank response line and return it (raw).</li>
   * </ol>
   * The caller is responsible for decoding the returned JSON string into {@link OpMove}.
   *
   * @param json a single-line JSON payload representing {@link PureGame}
   *
   * @return the first non-blank response line from the server
   *
   * @throws SocketTimeoutException if a read operation times out
   * @throws EOFException           if the server closes the connection before sending a line
   * @throws IOException            on any other I/O errors (connect/write/read)
   * @throws CancellationException  if interrupted at guarded points
   */
  private String requestOnce(String json) throws IOException, CancellationException {
    try (Socket s = new Socket()) {
      checkInterrupted();
      s.setTcpNoDelay(true);
      s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
      s.setSoTimeout(READ_TIMEOUT_MS);
      try (
          BufferedWriter w = new BufferedWriter(
              new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
          BufferedReader r = new BufferedReader(
              new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
      ) {
        send(w, json);
        return read(r);
      }
    }
  }

  /**
   * Sends a JSON line and flushes the stream.
   *
   * @param w    the writer bound to the socket's output stream
   * @param json a single-line JSON payload
   *
   * @throws IOException           if writing fails
   * @throws CancellationException if interrupted before or during the writing
   */
  private void send(BufferedWriter w, String json) throws IOException, CancellationException {
    checkInterrupted();
    w.write(json);
    w.write('\n');
    w.flush();
  }

  /**
   * Reads lines until the first non-blank is received, then returns it.
   *
   * <p>Blank lines are logged at {@link Level#FINE} and ignored. The socket read timeout controls
   * how long {@link BufferedReader#readLine()} may block; on timeout a
   * {@link SocketTimeoutException} is thrown to the caller (which may choose to retry).</p>
   *
   * @param r the reader bound to the socket's input stream
   *
   * @return the first non-blank line read
   *
   * @throws SocketTimeoutException if the read operation times out
   * @throws EOFException           if the peer closes the connection
   * @throws IOException            on any other I/O errors
   * @throws CancellationException  if interrupted between reads
   */
  private String read(BufferedReader r) throws IOException, CancellationException {
    while (true) {
      checkInterrupted();
      final String line = r.readLine();
      if (line == null) {
        throw new EOFException("server closed");
      }
      if (!line.isBlank()) {
        return line;
      }
      LOG.log(Level.FINE, "EternalAi: got blank line");
    }
  }

  /**
   * Provides the current X coordinate used for horizontal alignment when following a plan.
   *
   * <p>This implementation uses the left-most absolute bound ({@code minX}) of the active
   * tetromino, which corresponds to board-space alignment at the piece's left edge.</p>
   *
   * @param snapshot the current immutable game snapshot (must include a non-null active tetromino)
   *
   * @return the left-edge X coordinate in board space
   *
   * @implNote If your controller interprets {@link OpMove#opX()} as pivot X, override this to
   *     return {@code snapshot.tetromino().position().x()} for consistent alignment.
   */
  @Override
  protected int getCurrentX(PureGame snapshot) {
    return snapshot.tetromino().getAbsoluteBound().minX();
  }

  /**
   * Shuts down the internal executor used for socket I/O.
   *
   * <p>Idempotent; no further tasks will be accepted after shutdown.</p>
   */
  @Override
  public void shutdown() {
    worker.shutdown();
  }
}
