package com.example.tetris.controllers;

import com.example.tetris.helpers.AppContext;
import com.example.tetris.helpers.FxController;
import com.example.tetris.helpers.Route;
import com.example.tetris.models.Configuration;
import com.example.tetris.models.Configuration.PlayerType;
import com.example.tetris.services.AudioService;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public final class ConfigurationController implements FxController {

  private AppContext ctx;


  // sliders + numeric labels
  @FXML private Slider widthSlider;
  @FXML private Label widthValue;
  @FXML private Slider heightSlider;
  @FXML private Label heightValue;
  @FXML private Slider levelSlider;
  @FXML private Label levelValue;

  // toggles
  @FXML private CheckBox musicCheck;
  @FXML private CheckBox soundCheck;
  @FXML private CheckBox extendedCheck;

  // players
  @FXML private ChoiceBox<PlayerType> player1Choice;
  @FXML private ChoiceBox<PlayerType> player2Choice;

  // actions
  @FXML private Button saveButton;
  @FXML private Button resetButton;
  @FXML private Button backButton;

  @FXML
  private void initialize() {
    // ranges are set in FXML; here we only bind integer labels
    IntegerBinding w = Bindings.createIntegerBinding(
        () -> (int) Math.round(widthSlider.getValue()), widthSlider.valueProperty());
    IntegerBinding h = Bindings.createIntegerBinding(
        () -> (int) Math.round(heightSlider.getValue()), heightSlider.valueProperty());
    IntegerBinding l = Bindings.createIntegerBinding(
        () -> (int) Math.round(levelSlider.getValue()), levelSlider.valueProperty());
    widthValue.textProperty().bind(w.asString());
    heightValue.textProperty().bind(h.asString());
    levelValue.textProperty().bind(l.asString());

    // populate player choices
    player1Choice.getItems().setAll(PlayerType.values());
    player2Choice.getItems().setAll(PlayerType.values());

    // toggle P2 availability with extend mode
    extendedCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
      player2Choice.setDisable(!newVal);
    });

    // buttons
    saveButton.setOnAction(e -> handleSave());
    resetButton.setOnAction(e -> handleReset());
    backButton.setOnAction(e -> handleBack());
  }

  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;

    // Load baseline config through appcontext service
    Configuration baseline = ctx.getConfigurationCache().getOrLoad(ctx.getConfigurationService());
    setFormFromConfig(baseline);

    // AUDIO
    // Get shared audio service from context.
    AudioService audio = ctx.getAudioService();
    // When music/sound checkboxes changes reflect it in the service.
    musicCheck.selectedProperty().addListener((obs, oldVal, on) -> {
      audio.setMusicEnabled(on);
      if (on) {
        audio.startMusic();
      }else {
        audio.stopMusic();
      }
    });

    soundCheck.selectedProperty().addListener((obs, oldVal, on) -> {
      audio.setSoundEnabled(on);
    });

    // initial state for P2
    player2Choice.setDisable(!extendedCheck.isSelected());
  }

  // --- button handlers ---

  private void handleSave() {
    Configuration updated = formToConfig();
    ctx.getConfigurationService().save(updated);
    ctx.getConfigurationCache().set(updated);
  }

  private void handleReset() {
    setFormFromConfig(new Configuration()); // UI only; do not save
  }


  private void handleBack() {
    Configuration current = formToConfig();
    Configuration baseline = ctx.getConfigurationCache().getOrLoad(ctx.getConfigurationService());

    if (!current.equals(baseline)) {
      Alert alert = new Alert(AlertType.CONFIRMATION,
          "Are you sure you want to exit? Unsaved changes will be lost.", ButtonType.YES, ButtonType.NO);
      alert.setHeaderText(null);
      alert.showAndWait().ifPresent(btn -> {
        if (btn == ButtonType.YES) {
          setFormFromConfig(baseline); // Reverts ui to back to baseline
          ctx.show(Route.MENU);
        }
      });
    } else {
      ctx.show(Route.MENU);
    }
  }

  // --- Helpers ---

  private Configuration formToConfig() {
    PlayerType p1 = (player1Choice.getValue() == null) ? PlayerType.HUMAN : player1Choice.getValue();
    PlayerType p2 = (player2Choice.getValue() == null) ? PlayerType.HUMAN   : player2Choice.getValue();
    return new Configuration(
        (int) widthSlider.getValue(),
        (int) heightSlider.getValue(),
        (int) levelSlider.getValue(),
        musicCheck.isSelected(),
        soundCheck.isSelected(),
        extendedCheck.isSelected(),
        p1, p2
    );
  }

  private void setFormFromConfig(Configuration cfg) {
    widthSlider.setValue(cfg.width());
    heightSlider.setValue(cfg.height());
    levelSlider.setValue(cfg.level());
    musicCheck.setSelected(cfg.music());
    soundCheck.setSelected(cfg.sound());
    extendedCheck.setSelected(cfg.extendMode());
    player1Choice.setValue(cfg.player1());
    player2Choice.setValue(cfg.player2());
    player2Choice.setDisable(!cfg.extendMode());
  }
}
