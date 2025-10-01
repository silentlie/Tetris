package com.example.tetris.ui.controller;

import com.example.tetris.app.context.AppContext;
import com.example.tetris.app.context.FxController;
import com.example.tetris.app.navigation.Route;
import com.example.tetris.core.config.Configuration;
import com.example.tetris.core.state.PlayerType;
import com.example.tetris.service.ConfigurationService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;

/**
 * JavaFX controller for the Configuration screen.
 *
 * <p>This controller bridges the configuration form (sliders, checkboxes, choices)
 * and the {@link ConfigurationService} provided by the app {@link AppContext}. It:
 * <ul>
 *   <li>Initializes UI controls (value ranges, options, and inter-control bindings).</li>
 *   <li>Loads the current baseline configuration from the service cache.</li>
 *   <li>Builds a validated {@link Configuration} from the form and saves it.</li>
 *   <li>Warns on navigation if there are unsaved changes.</li>
 * </ul>
 *
 * <p><strong>Lifecycle:</strong>
 * JavaFX calls {@link #initialize()} after FXML is loaded. The application then injects the
 * {@link AppContext} via {@link #setContext(AppContext)}, which triggers population of the form
 * from the persisted configuration.</p>
 *
 * <p><strong>Notes:</strong>
 * Sliders produce {@code double} values; we round to integers with {@link #intRound(double)}
 * to match the domain ({@code width/height/level} are ints) and avoid "phantom diffs".</p>
 */
public final class ConfigurationController implements FxController {

  private AppContext ctx;

  // --- FXML-injected controls ---

  @FXML
  private Slider widthSlider;
  @FXML
  private Slider heightSlider;
  @FXML
  private Slider levelSlider;
  @FXML
  private CheckBox musicCheck;
  @FXML
  private CheckBox soundCheck;
  @FXML
  private CheckBox extendedCheck;
  @FXML
  private ChoiceBox<PlayerType> player1Choice;
  @FXML
  private ChoiceBox<PlayerType> player2Choice;

  /**
   * Rounds a {@code double} to the nearest {@code int}.
   *
   * <p>Used to convert slider values to the domain's integer fields while avoiding truncation.</p>
   *
   * @param v value to round
   *
   * @return nearest integer
   */
  private static int intRound(double v) {
    return (int) Math.round(v);
  }

  /**
   * Called by the JavaFX loader after the FXML has been constructed.
   *
   * <p>Sets slider ranges from {@link Configuration} invariants, populates player choices, and
   * binds Player&nbsp;2 availability to the "Extend Mode" toggle.</p>
   *
   * <p>Control state (specific values) is applied later in
   * {@link #setFormFromConfig(Configuration)} once the {@link AppContext} is available.</p>
   */
  @FXML
  private void initialize() {
    widthSlider.setMin(Configuration.MIN_WIDTH);
    widthSlider.setMax(Configuration.MAX_WIDTH);
    heightSlider.setMin(Configuration.MIN_HEIGHT);
    heightSlider.setMax(Configuration.MAX_HEIGHT);
    levelSlider.setMin(Configuration.MIN_LEVEL);
    levelSlider.setMax(Configuration.MAX_LEVEL);

    player1Choice.getItems().setAll(PlayerType.values());
    player2Choice.getItems().setAll(PlayerType.values());

    // Player 2 is only enabled in Extend Mode.
    player2Choice.disableProperty().bind(extendedCheck.selectedProperty().not());
  }

  /**
   * Supplies the application context after FXML loading and wires the form to the current
   * baseline.
   *
   * @param ctx the shared application context (non-null)
   */
  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;
    setFormFromConfig(getBaseline());
    wireAudioToggles();
  }

  /**
   * Collects current form values, constructs a {@link Configuration}, and persists it via the
   * configuration service. The service is responsible for updating its cache.
   */
  @FXML
  private void onSave() {
    Configuration updated = formToConfig();
    ctx.getConfigService().save(updated);
  }

  /**
   * Resets the form to factory defaults using {@link Configuration#Configuration()}.
   */
  @FXML
  private void onReset() {
    setFormFromConfig(new Configuration());
  }

  /**
   * Navigates back to the menu. If the form differs from the baseline in the configuration service
   * cache, asks the user to confirm discarding unsaved changes.
   */
  @FXML
  private void onBack() {
    if (!formToConfig().equals(getBaseline())) {
      Alert alert = new Alert(AlertType.CONFIRMATION);
      alert.setTitle("Unsaved changes");
      alert.setHeaderText("Discard changes?");
      alert.setContentText("You’ll lose any unsaved edits.");
      ButtonType discard = new ButtonType("Discard", ButtonBar.ButtonData.OK_DONE);
      ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
      alert.getButtonTypes().setAll(discard, cancel);
      alert.initOwner(ctx.getStage());
      alert.showAndWait().ifPresent(btn -> {
        if (btn == discard) {
          setFormFromConfig(getBaseline());
          ctx.show(Route.MENU);
        }
      });
    } else {
      ctx.show(Route.MENU);
    }
  }

  /**
   * Returns the authoritative baseline configuration from the service cache.
   *
   * <p>Assumes {@link #setContext(AppContext)} has been invoked.</p>
   *
   * @return non-null baseline configuration
   */
  private Configuration getBaseline() {
    return ctx.getConfigService().getCache();
  }

  /**
   * Builds a validated {@link Configuration} from current UI control state.
   *
   * <p>Uses {@link #intRound(double)} for sliders and defaults missing choices to
   * {@link PlayerType#HUMAN} to prevent nulls.</p>
   *
   * @return non-null configuration reflecting the form
   *
   * @throws IllegalArgumentException if any value violates {@link Configuration} ranges
   */
  private Configuration formToConfig() {
    PlayerType p1 =
        (player1Choice.getValue() == null) ? PlayerType.HUMAN : player1Choice.getValue();
    PlayerType p2 =
        (player2Choice.getValue() == null) ? PlayerType.HUMAN : player2Choice.getValue();
    return new Configuration(
        intRound(widthSlider.getValue()),
        intRound(heightSlider.getValue()),
        intRound(levelSlider.getValue()),
        musicCheck.isSelected(),
        soundCheck.isSelected(),
        extendedCheck.isSelected(),
        p1,
        p2
    );
  }

  /**
   * Pushes a {@link Configuration} into the form controls.
   *
   * <p>Does not modify the {@code disable} state of {@code player2Choice} directly; that property
   * is bound to {@code extendedCheck} in {@link #initialize()}.</p>
   *
   * @param cfg configuration to display (non-null)
   */
  private void setFormFromConfig(Configuration cfg) {
    widthSlider.setValue(cfg.width());
    heightSlider.setValue(cfg.height());
    levelSlider.setValue(cfg.level());
    musicCheck.setSelected(cfg.music());
    soundCheck.setSelected(cfg.sound());
    extendedCheck.setSelected(cfg.extendMode());
    player1Choice.setValue(cfg.player1());
    player2Choice.setValue(cfg.player2());
  }

  /**
   * Wire the music and sound checkboxes so they immediately affect audio.
   * Persistence happens when user clicks save not on toggle.
   */
  private void wireAudioToggles() {
    musicCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
      ctx.getAudioService().setMusicEnabled(newVal);
    });

    soundCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
      ctx.getAudioService().setSoundEnabled(newVal);
    });

  }

}