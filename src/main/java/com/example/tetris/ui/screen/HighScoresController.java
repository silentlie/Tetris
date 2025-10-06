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

  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;
    loadScores();
  }

  @FXML
  private void onBack(ActionEvent actionEvent) {
    ctx.evict(Route.HIGH_SCORES);
    ctx.show(Route.MENU);
  }

  public void onReset(ActionEvent actionEvent) {
    ctx.getScoresService().clear();
    loadScores();
  }

  private void loadScores() {
    List<Score> scores = ctx.getScoresService().all();
    table.setItems(FXCollections.observableArrayList(scores));
  }
}

