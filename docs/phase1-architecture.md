# Phase 1 Architecture

This repository currently contains only a Java-shaped UML skeleton.
There is no game logic and no method implementation.

## Main Rule

- `model`: game data and domain concepts
- `controller`: receives user commands and coordinates the model
- `view`: displays data and messages
- `repository`: saves and loads external data
- `util`: parsing, validation, and hashing contracts

## Entity vs Service

Entities are concrete classes that hold state:

```text
Board
Tile
Plant
Zombie
User
Level
GameSession
```

Services are interfaces that describe operations:

```text
BoardService
PlantService
ZombieService
LevelService
UserService
GameEngine
```

This prevents data classes from being marked `abstract` only because method
bodies have not been implemented yet.

## Design Patterns

### Strategy

Replace large inheritance trees with interchangeable behavior:

```text
Plant -> PlantBehavior
Zombie -> ZombieBehavior
Level -> LevelRule
Chapter -> ChapterRule
ScoreTracker -> ScoringStrategy
```

### Factory

Create configured game objects:

```text
PlantFactory
ZombieFactory
GameSessionFactory
MiniGameFactory
```

### State

Represent menu transitions:

```text
MenuContext -> MenuState
```

### Observer

Publish typed game events without coupling the game engine to every system:

```text
GameEngine -> EventPublisher

QuestTracker
ScoreTracker
ProgressTracker
NewsTracker
    -> GameEventListener
```

Events are typed records such as `ZombieDiedEvent` and `SunCollectedEvent`.
There is no generic `Object payload`.

### Repository

Hide file persistence behind contracts:

```text
UserRepository
SaveRepository
GameDataRepository
QuestRepository
```
