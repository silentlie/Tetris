# SOLID

## Single Responsibility Principle (SRP)

- `Board` encapsulates geometry, collision, and line-clear logic only, leaving scoring, input, and
  rendering to other classes.
- `GameClock` is solely concerned with elapsed time, level progression, and gravity calculations.
- `PieceQueue` maintains the next-piece preview by enqueuing `TetrominoType` values from a supplier;
  it never constructs `Tetromino` objects.
- `JsonConfigurationService` focuses only on loading/saving configuration files with error handling,
  without UI or gameplay entanglement.

## Open/Closed Principle (OCP)

- `Ai` is an abstraction: new steering behaviours (`FakeAi`, `EternalAi`, future AIs) can be added
  by subclassing without altering the game loop.
- `PieceSupplierStrategy` exposes factories for different piece generators and can be extended
  without changing clients.
- `ConfigurationService` defines template methods for persistence; new storage backends (e.g., JSON,
  DB) extend it rather than modifying existing logic.

## Liskov Substitution Principle (LSP)

- Subclasses such as `FakeAi` and `EternalAi` respect the `Ai` contract and can replace each other
  transparently.
- `AudioService` defines the audio backend contract, and `JavaFxAudioService` implements it
  faithfully so callers are unaffected by the swap.

## Interface Segregation Principle (ISP)

- Narrow contracts like `Movable` and `Rotatable` split behaviour so models implement only what’s
  relevant.
- `AudioService` exposes only playback responsibilities, avoiding bloated APIs.

## Dependency Inversion Principle (DIP)

- High-level components depend on abstractions rather than concrete classes: `AppContext` wires in
  `Router`, `ConfigurationService`, and `AudioService` through constructor injection.
- `PlayController` currently hardcodes `PieceSupplierStrategy.UNIFORM` and concrete AIs (
  `new FakeAi()`, `new EternalAi()`); this is a **deviation from DIP** and a potential future
  refactor point to improve flexibility.

---

# GRASP

## Controller

- `Game` orchestrates per-frame state updates, gravity timing, AI intents, and rendering, acting as
  the controller for one session.
- `PlayController` coordinates configuration loading, game creation, input handling, and lifecycle
  for the play scene.
- `MenuController` mediates menu actions and delegates navigation via `AppContext` rather than
  manipulating scenes directly.

## Creator

- `PlayController#createGame` builds the `Game`, `Board`, `PieceQueue`, `GameView`, and `GameClock`
  because it owns the configuration and UI container.
- `GameState#spawnFromQueue` constructs `Tetromino` instances from the queued `TetrominoType`.
- `PieceQueue` fills and maintains a queue of `TetrominoType` values pulled from the configured
  supplier.

## Information Expert

- `GameState` owns the board, active tetromino, scoring, and streaks, so it applies inputs, locks
  pieces, tallies scores, and spawns pieces.
- `ConfigurationController` reads/writes form values into `Configuration` objects before persisting,
  since it has the necessary control state.

## Low Coupling

- `GameService` just loops over `Game` ticks and completion checks, staying detached from gameplay
  details.
- The `FxController` + `AppContext` injection pattern decouples controllers from JavaFX stage
  management.
- `ViewLoader` caches FXML roots and hides FXML resolution from controllers.

## High Cohesion

- `GameClock` focuses entirely on timing, level, and gravity.
- `JsonConfigurationService` handles only persistence and related file errors.

## Polymorphism

- The `Ai` hierarchy varies planning logic through overridden hooks like `submit`.
- `PieceSupplierStrategy` lets clients select piece generation polymorphically, without branching.

## Pure Fabrication

- `JsonUtil` centralises JSON serialization outside domain entities.
- `ViewLoader` fabricates a helper to manage FXML caching and loading, reducing coupling.

## Indirection

- `Router` mediates route changes and splash handling, insulating controllers from the JavaFX stage.
- `AppContext` provides navigation and services indirectly so controllers don’t depend on singletons
  or concretes.

## Protected Variations

- `ConfigurationService` stabilises persistence behind a fixed interface so storage can vary.
- `AudioService` shields clients from backend changes with a minimal façade.
- `PieceSupplierStrategy` and `PieceQueue` hide the variability of piece randomisation at the
  `TetrominoType` level, with instantiation handled separately in `GameState`.  
