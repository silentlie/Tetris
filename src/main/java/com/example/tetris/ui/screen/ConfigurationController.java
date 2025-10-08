package com.example.tetris.ui.screen;

import com.example.tetris.app.context.AppContext;
import com.example.tetris.app.context.FxController;
import com.example.tetris.app.navigation.Route;
import com.example.tetris.core.config.Configuration;
import com.example.tetris.core.state.PlayerType;
import com.example.tetris.service.config.ConfigurationService;
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
 * <p>Bridges the configuration form (sliders, checkboxes, choices) and the
 * {@link ConfigurationService} available via the app {@link AppContext}. Responsibilities:</p>
 * <ul>
 *   <li>Initialize control ranges/options and inter-control bindings.</li>
 *   <li>Load the baseline {@link Configuration} from the service cache into the form.</li>
 *   <li>Build a validated {@link Configuration} from the form and persist it.</li>
 *   <li>Warn when navigating away with unsaved changes.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 *
 * <p>JavaFX invokes {@link #initialize()} after FXML loading. The application then calls
 * {@link #setContext(AppContext)}, which populates the form from the persisted configuration.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>All methods are expected to run on the JavaFX application thread.</p>
 *
 * <h2>Notes</h2>
 *
 * <p>Sliders produce {@code double} values; values are rounded to {@code int} via
 * {@link #intRound(double)} to match the domain ({@code width/height/level}) and avoid
 * “phantom diffs”.</p>
 *
 * @since 1.0
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
   * Round a {@code double} to the nearest {@code int}.
   *
   * <p>Used to convert slider values to integer configuration fields.</p>
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
   * <p>Sets slider ranges from {@link Configuration} invariants, populates player-type choices,
   * and binds Player&nbsp;2 availability to the “Extend Mode” toggle.</p>
   *
   * <p>Control values are applied later by {@link #setContext(AppContext)}.</p>
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
   * Supply the application context and populate the form from the persisted baseline.
   *
   * @param ctx shared application context (must not be {@code null})
   *
   * @throws NullPointerException if {@code ctx} is {@code null}
   */
  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;
    setFormFromConfig(getBaseline());
  }

  /**
   * Collect current form values, construct a validated {@link Configuration}, and persist it via
   * the configuration service.
   *
   * <p>Also applies the audio toggles immediately to the runtime {@code AudioService} so the
   * change is audible without a restart.</p>
   *
   * @throws IllegalArgumentException if any value violates {@link Configuration} ranges
   */
  @FXML
  private void onSave() {
    Configuration updated = formToConfig();
    ctx.getAudio().setBgm(updated.music());
    ctx.getAudio().setSound(updated.sound());
    ctx.getConfigService().save(updated);
  }

  /**
   * Reset the form to factory defaults ({@link Configuration#Configuration()}).
   *
   * <p>Does not persist automatically; the user must click Save.</p>
   */
  @FXML
  private void onReset() {
    setFormFromConfig(new Configuration());
  }

  /**
   * Navigate back to the menu. If the form differs from the baseline in the configuration cache,
   * prompt to discard changes.
   *
   * <p>On “Discard”, the form is reset to the baseline and the route changes to
   * {@link Route#MENU}. On “Cancel”, the user remains on the page.</p>
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
   * Return the authoritative baseline configuration from the service cache.
   *
   * <p>Assumes {@link #setContext(AppContext)} has been called.</p>
   *
   * @return non-null baseline configuration
   */
  private Configuration getBaseline() {
    return ctx.getConfigService().getCache();
  }

  /**
   * Build a validated {@link Configuration} from current UI control state.
   *
   * <p>Slider values are rounded with {@link #intRound(double)}. Missing player choices default to
   * {@link PlayerType#HUMAN} to avoid {@code null} values.</p>
   *
   * @return configuration reflecting the form
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
   * Push a {@link Configuration} into the form controls.
   *
   * <p>Does not modify the {@code disable} state of {@code player2Choice}; that property is
   * bound to {@code extendedCheck} in {@link #initialize()}.</p>
   *
   * @param cfg configuration to display (must not be {@code null})
   *
   * @throws NullPointerException if {@code cfg} is {@code null}
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
}
