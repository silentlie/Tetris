# Singleton

- `JsonUtil` class documentation and implementation describe a “default singleton” instance exposed
  via `getInstance()` and caution about mutating shared singletons

# Factory

- `PieceSupplierStrategy` enum encapsulates a “factory” per strategy to build
  `Supplier<TetrominoType>` instances, storing it in a `factory` field and using it in `create`
  methods.

## Facade

- **AppContext** – Centralises cross-cutting services and app-level state: navigation via a
  `Router`, configuration persistence via a `ConfigurationService`, and audio playback via an
  `AudioService`. Controllers receive this context and call methods like `show()`, `switchBgm()` or
  `switchSound()` instead of interacting with individual services.
- **Router** – Acts as the primary navigation service; it resolves a `Route` into its FXML view
  through a `ViewLoader` and swaps it into the primary `Stage`, hiding scene-management details.
- **Game** – Orchestrates the mutable `GameState`, the timing curve (`GameClock`), the UI surface (
  `GameView`) and an optional AI. Clients only call `tick(long)` and `apply(GameIntent)` rather than
  dealing directly with state, clock and rendering.
- **AudioService** – Provides high-level methods (`setBgm`, `setSound`, `play`) that abstract away
  the underlying JavaFX media objects; controllers don’t manipulate `MediaPlayer` or `AudioClip`
  directly (see `AudioService` docs).

## Observer

- **GameState.Listener** – Exposes callbacks for `onGameOver()`,
  `onClearLines(cleared, score, streak, highestStreak)`, and `onIntent(GameIntent)`.
  `GameState.apply(GameIntent)` fires `onIntent` for accepted inputs (gravity drops excluded) and
  calls into `lock()`/`setGameOver()` to trigger the other notifications.
- **GameClock.Listener** – Notifies observers when the integer level increases via
  `onLevelUp(int newLevel)` inside `tick()`.
- **PlayController** – Registers both listeners to drive audio cues (e.g., line clears, level-ups,
  movement) whenever the model emits those events.

## Command

- **GameIntent** – Enumerates high-level actions (`ROTATE_CW`, `MOVE_LEFT`, `MOVE_RIGHT`,
  `SOFT_DROP`, `HARD_DROP`, `GRAVITY_DROP`) independent of physical keys.
  `GameState.apply(GameIntent)` switches over these intents and delegates the associated
  movement/rotation to the `Tetromino` via function references, treating `SOFT_DROP` and
  `GRAVITY_DROP` as the same downward step with locking when blocked.
- **Menu/Configuration actions** – Controller methods (e.g., `onPlayClicked`, `onReset`) encapsulate
  user commands; they call into `AppContext` or services to perform navigation or configuration
  updates, separating UI event handlers from business logic.

## State

- **GameState** – Holds mutable game data: the `Board`, active `Tetromino`, upcoming `PieceQueue`,
  score, streak counters and a game-over flag. It exposes methods to apply intents, lock pieces and
  spawn new tetrominoes, evolving its internal state accordingly.
- **GameClock** – Tracks elapsed time, computes a continuous `rawLevel`, derives integer level and
  gravity intervals, and notifies listeners when the floored level increases.
- **Game** – Maintains its own `gravityAccNanos` accumulator and attaches an optional AI; its
  `tick()` method advances the clock, applies gravity drops and AI intents, and renders the view
  based on current state.

## Decorator

- **ResizableCanvas** – Wraps a JavaFX `Canvas` in a `Region`, binds its size to the parent and
  maintains a `dirty` flag that signals when repainting is needed. It decorates the base `Canvas`
  with resize/dirty-tracking behaviour.
- **BoardCanvas** and **NextCanvas** – Extend `ResizableCanvas` and implement Tetris-specific
  drawing (board cells, next-piece preview). They leverage the parent’s dirty logic while adding
  domain-specific rendering.
- **GameView** – Extends `SplitPane` and composes two canvas children plus HUD labels; it propagates
  dirtiness to its children and only repaints them when needed.

## Abstraction-Occurrence

- **TetrominoType** vs. **Tetromino** – `TetrominoType` defines the abstract shapes and caches
  rotation-derived geometry; the `Tetromino` record represents a concrete occurrence with a type,
  position and rotation. Methods such as `withRotation()` return new `Tetromino` instances rather
  than mutating the type.

## General Hierarchy (Template Method)

- **ConfigurationService** – Abstract base class that defines the template for loading/saving
  configuration with an in-memory cache and synchronized methods. Subclasses override
  `loadInternal()` and `saveInternal()` to perform actual I/O.
- **JsonConfigurationService** – Implements the template by reading/writing a `config.json` file. On
  missing or unreadable files it backs up the file, writes defaults and returns them, handling all
  errors internally.
- **ScoresService** – Provides another template-method hierarchy for leaderboard persistence. Its
  final public API keeps a sorted in-memory cache and funnels mutations through template hooks
  `loadInternal()`/`saveInternal(List<Score>)`, guaranteeing non-null lists and copy-on-write
  semantics for implementors.
- **JsonScoresService** – Supplies the concrete JSON-backed storage by implementing the two hooks
  from `ScoresService`. It handles missing/corrupt files, backs them up via `FileBackup`, and writes
  snapshots using `JsonUtil`’s atomic temp-file move.
- **Ai** – Encapsulates the frame-by-frame AI control loop in its final `tick(PureGame)` template
  method. Hooks such as `shouldStartNewPlan(...)`, `submit(PureGame)`, `getCurrentIntent(...)`, and
  `onError(Throwable)` let subclasses specialize the planning trigger, async work, intent emission,
  and error policy while reusing the orchestration logic.
- **HeuristicAi**, **ExternalAi**, **FakeAi** – Concrete `Ai` specializations that plug custom
  planning strategies into the template. They each implement `submit(PureGame)` (and, for
  `ExternalAi`, override `shouldStartNewPlan`) to supply different behaviours while delegating tick
  orchestration to the base class.

## Flyweight

- **ViewLoader** – Caches FXML views in an `EnumMap<Route, Parent>` so that repeated navigation to
  the same route reuses the same scene graph and controller, avoiding redundant FXML parsing.
- **TetrominoType** – Uses a `ConcurrentHashMap` (not shown here) to cache rotation-specific derived
  data; repeated lookups for a given type and rotation reuse precomputed geometry rather than
  recomputing it each time.
- **SharedPieceSource** / **SharedPieceSupplier** – Memoize and share a deterministic tetromino
  sequence across multiple cursors. The source caches each generated `TetrominoType` by index, and
  lightweight suppliers keep only a cursor, letting many consumers reuse the same intrinsic piece
  data without duplicating generation.

## Composite

- **GameView** – Composes HUD labels and two canvases (board and next-piece) into a single view. It
  manages a local dirty flag and coordinates repainting of its children.
- **GameService** – (Not shown here, but referenced in controllers) registers one or more `Game`
  instances and advances them in lockstep using an `AnimationTimer`, acting as a composite
  controller over multiple game objects.
- **Router / ViewLoader / AppContext** – Together form a composite navigation system: the router
  delegates to a view loader and context, and the context holds references to services and the
  router, providing a unified interface.