package com.example.tetris.ui.screen;

import com.example.tetris.app.context.AppContext;
import com.example.tetris.app.context.FxController;
import com.example.tetris.app.navigation.Route;
import com.example.tetris.core.score.Score;
import com.example.tetris.util.TimeUtil;
import java.util.List;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controller for the High Scores screen.
 *
 * <p>Wires a {@link TableView} to display persisted {@link Score} records from the
 * application's {@link AppContext} via the ScoresService. This controller is instantiated by the
 * JavaFX FXMLLoader and its fields are injected from the corresponding FXML.</p>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>{@link #initialize()} is called by the FXML loader once field injection completes.
 *       It configures the table's column value factories.</li>
 *   <li>{@link #setContext(AppContext)} is called by the app's navigation layer to provide
 *       the shared context; this triggers the first load of scores.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 *
 * <p>All methods are expected to run on the JavaFX Application Thread.</p>
 *
 * <h2>Ranking column note</h2>
 *
 * <p>The "rank" column derives its value from the row index in {@link #table}. If the table
 * is re-sorted or filtered, ranks reflect the current view order. If you require a fixed
 * rank independent of UI sorting, persist rank on the model or compute using the backing list
 * before binding to the table.</p>
 */
public class HighScoresController implements FxController {

  @FXML
  private TableView<Score> table;
  @FXML
  private TableColumn<Score, Number> rank;
  @FXML
  private TableColumn<Score, String> name;
  @FXML
  private TableColumn<Score, Number> score;
  @FXML
  private TableColumn<Score, Number> bestStreak;
  @FXML
  private TableColumn<Score, String> elapsed;
  @FXML
  private TableColumn<Score, String> timestamp;

  private AppContext ctx;

  /**
   * Initializes the table columns and binds cell value factories.
   *
   * <p>Called automatically by the FXMLLoader after FXML field injection. This method does not
   * load data; {@link #setContext(AppContext)} triggers the first data load.</p>
   *
   * <p><strong>Value factories:</strong></p>
   * <ul>
   *   <li><em>rank</em>: index-based (1-based) position within the current table items.</li>
   *   <li><em>name</em>, <em>score</em>, <em>bestStreak</em>: simple property wrappers from the
   *   model.</li>
   *   <li><em>elapsed</em>, <em>timestamp</em>: formatted strings via {@link TimeUtil} helpers.
   *   </li>
   * </ul>
   */
  @FXML
  private void initialize() {
    rank.setCellValueFactory(
        cd -> new ReadOnlyIntegerWrapper(table.getItems().indexOf(cd.getValue()) + 1)
    );
    name.setCellValueFactory(
        cd -> new ReadOnlyStringWrapper(cd.getValue().name()));
    score.setCellValueFactory(
        cd -> new ReadOnlyIntegerWrapper(cd.getValue().score())
    );
    bestStreak.setCellValueFactory(
        cd -> new ReadOnlyIntegerWrapper(cd.getValue().bestStreak())
    );
    elapsed.setCellValueFactory(
        cd -> new ReadOnlyStringWrapper(TimeUtil.formatDuration(cd.getValue().elapsed()))
    );
    timestamp.setCellValueFactory(
        cd -> new ReadOnlyStringWrapper(TimeUtil.formatInstant(cd.getValue().timeStamp()))
    );
  }

  /**
   * Supplies the shared application context and triggers the initial table population.
   *
   * @param ctx the application context providing navigation and services; must not be {@code null}
   */
  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;
    loadScores();
  }

  /**
   * Handles the Back button action: evicts the current route and navigates to the main menu.
   *
   * @param ignore the JavaFX action event (unused)
   */
  @FXML
  private void onBack(ActionEvent ignore) {
    ctx.evict(Route.HIGH_SCORES);
    ctx.show(Route.MENU);
  }

  /**
   * Clears all persisted scores via the ScoresService and refreshes the table view.
   *
   * @param ignore the JavaFX action event (unused)
   */
  @FXML
  public void onReset(ActionEvent ignore) {
    ctx.getScoresService().clear();
    loadScores();
  }

  /**
   * Loads all scores from the ScoresService and binds them to the table.
   *
   * <p>Converts the returned list into an observable list for the {@link TableView}.
   * Sorting of the table (if any) is left to the user's current UI state.</p>
   */
  private void loadScores() {
    List<Score> scores = ctx.getScoresService().all();
    table.setItems(FXCollections.observableArrayList(scores));
  }
}
