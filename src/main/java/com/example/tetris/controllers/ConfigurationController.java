package com.example.tetris.controllers;

import com.example.tetris.helpers.AppContext;
import com.example.tetris.helpers.ConfigurationCache;
import com.example.tetris.helpers.FxController;
import com.example.tetris.helpers.Route;

import com.example.tetris.models.Configuration;
import com.example.tetris.models.Configuration.PlayerType;
import com.example.tetris.services.ConfigurationService;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;



/**
 * Merged: uses AppContext (from dev) + persistence (your feature).
 * - Gets ConfigurationService/Cache from AppContext
 * - Uses IntBinding for slider value labels
 * - No save on Reset (per review)
 * - Back warns on unsaved changes, then navigates via ctx.show(Route.MENU)
 */
public final class ConfigurationController implements FxController {

  private AppContext ctx;
  private ConfigurationService service;
  private Configuration cachedConfig;
  private ConfigurationCache cache;

  // sliders + numeric labels
  @FXML private Slider widthSlider;
  @FXML private Label widthValue;
  @FXML private Slider heightSlider;
  @FXML private Label heightValue;
  @FXML private Slider levelSlider;
  @FXML private Label levelValue;

  // toggles (+ optional On/Off mirror labels if present in FXML)
  @FXML private CheckBox musicCheck;
  @FXML private Label musicValue;     // optional (may be null if not in FXML)
  @FXML private CheckBox soundCheck;
  @FXML private Label soundValue;     // optional
  @FXML private CheckBox extendedCheck;
  @FXML private Label extendedValue;  // optional

  // player selectors
  @FXML private ChoiceBox<PlayerType> player1Choice;
  @FXML private ChoiceBox<PlayerType> player2Choice;


  // actions
  @FXML private Button saveButton;
  @FXML private Button resetButton;
  @FXML private Button backButton;

  private static void bindOnOffIfPresent(Label label, CheckBox box) {
    if (label != null) {
      label.textProperty().bind(Bindings.when(box.selectedProperty()).then("On").otherwise("Off"));
    }
  }

  @FXML
  private void initialize() {
    // Slider ranges should be in FXML; keep here only if not already set there.
    // width: 5..15, height: 15..30, level: 1..10

    // IntBinding for live numeric labels
    IntegerBinding w = Bindings.createIntegerBinding(
        () -> (int) Math.round(widthSlider.getValue()), widthSlider.valueProperty());
    IntegerBinding h = Bindings.createIntegerBinding(
        () -> (int) Math.round(heightSlider.getValue()), heightSlider.valueProperty());
    IntegerBinding l = Bindings.createIntegerBinding(
        () -> (int) Math.round(levelSlider.getValue()), levelSlider.valueProperty());
    widthValue.textProperty().bind(w.asString());
    heightValue.textProperty().bind(h.asString());
    levelValue.textProperty().bind(l.asString());

    // On/Off mirror labels (if those labels exist in your FXML)
    bindOnOffIfPresent(musicValue, musicCheck);
    bindOnOffIfPresent(soundValue, soundCheck);
    bindOnOffIfPresent(extendedValue, extendedCheck);

    // Populate player choices
    player1Choice.getItems().setAll(PlayerType.values());
    player2Choice.getItems().setAll(PlayerType.values());

    // Disable/enable P2 block when extend mode changes
    extendedCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
      player2Choice.setDisable(!newVal);
      if (!newVal) player2Choice.setValue(PlayerType.OFF);
    });

    // Buttons
    saveButton.setOnAction(e -> handleSave());
    resetButton.setOnAction(e -> handleReset());
    backButton.setOnAction(e -> handleBack());
  }

  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;
    // Pull persistence from AppContext (per review comment)
    this.service = ctx.getConfigurationService();
    this.cache = ctx.getConfigurationCache();

    // First load (from cache/service), then apply to UI
    this.cachedConfig = cache.getOrLoad(service);
    setFormFromConfig(cachedConfig);

    // Ensure correct initial P2 enabled state
    player2Choice.setDisable(!extendedCheck.isSelected());
  }

  // ------- Button handlers -------

  private void handleSave() {
    Configuration updated = formToConfig();
    service.save(updated);
    cache.set(updated);
    cachedConfig = updated; // update change detection baseline
  }

  private void handleReset() {
    // Reset to defaults in UI ONLY (per reviewer: don't save here)
    Configuration defaults = new Configuration();
    setFormFromConfig(defaults);
  }

  private void handleBack() {
    Configuration current = formToConfig();
    if (!current.equals(cachedConfig)) {
      Alert alert = new Alert(AlertType.CONFIRMATION,
          "You have unsaved changes. Discard and go back?");
      alert.setHeaderText(null);
      alert.showAndWait().ifPresent(btn -> {
        // If user confirms, navigate
        if (btn == javafx.scene.control.ButtonType.OK ||
            btn == javafx.scene.control.ButtonType.YES) {
          ctx.show(Route.MENU);
        }
      });
    } else {
      ctx.show(Route.MENU);
    }
  }

  // ------- Helpers -------

  private Configuration formToConfig() {
    PlayerType p1 = (player1Choice.getValue() == null) ? PlayerType.HUMAN : player1Choice.getValue();
    PlayerType p2 = (player2Choice.getValue() == null) ? PlayerType.OFF   : player2Choice.getValue();
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
