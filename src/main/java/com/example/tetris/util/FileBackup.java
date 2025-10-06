package com.example.tetris.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File backup utilities.
 *
 * <p>Provides helper methods to move an existing file to a timestamped <em>.bak</em> copy in the
 * same directory. If the target file does not exist, or the move fails, methods return {@code null}
 * rather than throwing to the caller. Errors are logged at {@link Level#WARNING}.</p>
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>Backups are placed alongside the source file (or in the current working directory if the
 *       source has no parent directory).</li>
 *   <li>Backup filenames use the pattern {@code &lt;baseName&gt;.bak-yyyyMMdd-HHmmss}.</li>
 *   <li>If a backup with the same name already exists, it is overwritten
 *       ({@link StandardCopyOption#REPLACE_EXISTING}).</li>
 *   <li>Moves request atomic semantics ({@link StandardCopyOption#ATOMIC_MOVE}); if the
 *       underlying filesystem does not support atomic moves, the provider may ignore this option.
 *       </li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 *
 * <p>Methods are stateless and thread-safe; they operate only on arguments and the filesystem.</p>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * Path cfg = Path.of("config.json");
 * Path backup = FileBackup.backup(cfg);
 * if (backup != null) {
 *   System.out.println("Backed up to: " + backup);
 * }
 *
 * // Provide an explicit base name
 * Path scores = Path.of("scores.dat");
 * Path b = FileBackup.backupWithTimestamp(scores, "scores.dat");
 * }</pre>
 */
public class FileBackup {

  private static final Logger LOG = Logger.getLogger(FileBackup.class.getName());

  /**
   * Timestamp format used in backup filenames: {@code yyyyMMdd-HHmmss}.
   */
  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  /**
   * Moves the given {@code path} to a timestamped {@code .bak} file in the same directory using the
   * provided {@code baseName} to construct the backup filename.
   *
   * <p>If {@code path} does not exist, this method returns {@code null} and performs no I/O.
   * On I/O failure, it logs a warning and returns {@code null}.</p>
   *
   * <p>The resulting backup file name is {@code baseName + ".bak-" + yyyyMMdd-HHmmss} and resides
   * in {@code path.getParent()} (or {@code "."} if the path has no parent).</p>
   *
   * @param path     the existing file to back up; must not be {@code null}
   * @param baseName the base name to use for the backup file; must not be {@code null}
   *
   * @return the path of the created backup file, or {@code null} if {@code path} does not exist or
   *     if the move fails
   *
   * @throws NullPointerException if {@code path} or {@code baseName} is {@code null}
   */
  public static Path backupWithTimestamp(Path path, String baseName) {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(baseName, "baseName");

    if (!Files.exists(path)) {
      return null;
    }

    Path parent = (path.getParent() != null) ? path.getParent() : Path.of(".");
    String ts = TS.format(LocalDateTime.now());
    Path backup = parent.resolve(baseName + ".bak-" + ts);
    try {
      return Files.move(
          path,
          backup,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE
      );
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to move " + path + " to " + backup, e);
      return null;
    }
  }

  /**
   * Convenience overload that backs up {@code path} using the file's own name as the
   * {@code baseName}.
   *
   * <p>If the path's {@linkplain Path#getFileName() file name} is blank (uncommon) this method
   * returns {@code null}. If the source file does not exist, it also returns {@code null}.</p>
   *
   * @param path the existing file to back up; must not be {@code null}
   *
   * @return the path of the created backup file, or {@code null} if the source does not exist, its
   *     file name is blank, or the move fails
   *
   * @throws NullPointerException if {@code path} is {@code null}
   * @see #backupWithTimestamp(Path, String)
   */
  public static Path backup(Path path) {
    Objects.requireNonNull(path, "path");
    String baseName = path.getFileName().toString();
    if (baseName.isBlank()) {
      return null;
    }
    return backupWithTimestamp(path, baseName);
  }
}
