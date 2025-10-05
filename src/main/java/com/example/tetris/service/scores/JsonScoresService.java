package com.example.tetris.service.scores;

import com.example.tetris.core.score.Score;
import com.example.tetris.util.FileBackup;
import com.example.tetris.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ScoresService} backed by a JSON file on disk.
 *
 * <p><strong>Location & format:</strong> Persists scores to {@value #SCORES_FILE_NAME} in the
 * provided base directory (defaults to the current working directory). The file contains a JSON
 * array of {@link Score} records (Jackson-serializable).</p>
 *
 * <p><strong>Load behavior:</strong></p>
 * <ul>
 *   <li>If the file does not exist, an empty list is created, saved, and returned.</li>
 *   <li>If reading fails (I/O error, malformed JSON, etc.), the unreadable file is first backed up
 *       using {@link FileBackup#backup(Path)} (e.g., {@code scores.json.bak-20250101-123000}),
 *       then an empty list is written and returned.</li>
 *   <li>Returned lists are further sorted and cached by {@link ScoresService}.</li>
 * </ul>
 *
 * <p><strong>Save behavior:</strong> Writes use {@link JsonUtil#write(Path, Object)}, which
 * implements a temp-file + atomic-move style write on supported filesystems, reducing the chance of
 * partial files on crash. Failures are logged at {@link Level#SEVERE}.</p>
 *
 * <p><strong>Threading:</strong> All public entry points are inherited from {@link ScoresService}
 * and are synchronized on {@code this}. This class performs all I/O within those synchronized
 * methods.</p>
 *
 * <p><strong>Logging:</strong> Missing files are logged at INFO; read failures at WARNING;
 * write failures at SEVERE.</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * ScoresService svc = new JsonScoresService(Path.of("data"));
 * svc.add(new Score("Ada", 42000, 7, Instant.now(), Duration.ofMinutes(9)));
 * List<Score> leaderboard = svc.all(); // sorted snapshot
 * Path onDisk = ((JsonScoresService) svc).path(); // .../data/scores.json
 * }</pre>
 */
public class JsonScoresService extends ScoresService {

  private static final Logger LOG = Logger.getLogger(JsonScoresService.class.getName());

  /**
   * Default file name for persisted scores.
   */
  public static final String SCORES_FILE_NAME = "scores.json";

  private final Path scoresPath;
  private final JsonUtil json = JsonUtil.getInstance();

  /**
   * Creates a service that stores {@value #SCORES_FILE_NAME} in the current working directory.
   */
  public JsonScoresService() {
    this(Path.of("").toAbsolutePath());
  }

  /**
   * Creates a service that stores {@value #SCORES_FILE_NAME} under {@code baseDir}.
   *
   * @param baseDir the directory to contain the JSON file (need not exist yet)
   */
  public JsonScoresService(Path baseDir) {
    this.scoresPath = baseDir.resolve(SCORES_FILE_NAME);
  }

  /**
   * Loads scores from {@link #scoresPath}. If the file is absent, creates and saves an empty list.
   * If reading fails, backs up the corrupt file and resets to an empty list.
   *
   * <p>Never returns {@code null}; {@link ScoresService} will sort and cache the result.</p>
   */
  @Override
  protected List<Score> loadInternal() {
    try {
      if (Files.notExists(scoresPath)) {
        LOG.info(() -> "Scores not found, creating defaults at " + scoresPath);
        return createAndSaveDefault();
      }
      // Read existing file
      return json.read(scoresPath, new TypeReference<>() {
      });
    } catch (IOException e) {
      // Unreadable/corrupt → back it up first, then reset to defaults
      LOG.log(
          Level.WARNING,
          "Failed to read scores; backing up and using defaults: " + scoresPath,
          e
      );
      Path ignore = FileBackup.backup(scoresPath);
      return createAndSaveDefault();
    }
  }

  /**
   * Persists the given ordered list to {@link #scoresPath}. Failures are logged.
   *
   * @param scores immutable snapshot provided by {@link ScoresService}
   */
  @Override
  protected void saveInternal(List<Score> scores) {
    try {
      json.write(scoresPath, scores); // atomic-like write inside JsonUtil
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to save scores to " + scoresPath, e);
    }
  }

  /**
   * Returns the on-disk path used for persistence (e.g., {@code .../scores.json}).
   */
  public Path path() {
    return scoresPath;
  }

  /**
   * Creates and persists an empty leaderboard, then returns it.
   */
  private List<Score> createAndSaveDefault() {
    List<Score> defaults = new ArrayList<>();
    saveInternal(defaults);
    return defaults;
  }
}
