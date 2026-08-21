package controller;

import lombok.Getter;
import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Sun;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;
import model.minigame.zombotanyminigame.ZombotanyTrait;
import model.minigame.zombotanyminigame.ZombotanyActionResult;
import model.minigame.zombotanyminigame.ZombotanyActionStatus;
import model.minigame.zombotanyminigame.ZombotanyStateResult;
import model.minigame.zombotanyminigame.ZombotanyZombieState;
import model.plant.PlantDefinition;
import model.zombie.Zombie;
import view.zombotany.ZombotanyView;
import view.zombotany.ZombotanyViewObserver;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ZombotanyController implements ZombotanyViewObserver {

    private final ZombotanyMiniGame game;

    public ZombotanyController(ZombotanyMiniGame game) {
        if (game == null)
            throw new IllegalArgumentException("Zombotany game cannot be null.");

        this.game = game;
    }

    public ZombotanyController(ZombotanyView view, ZombotanyMiniGame game) {
        this(game);

        if (view == null)
            throw new IllegalArgumentException("Zombotany view cannot be null.");

        view.setObserver(this);
    }

    @Override
    public ZombotanyActionResult onStartRequested(int stageNumber) {
        if (stageNumber < 1 || stageNumber > 3) {
            return result(
                    ZombotanyActionStatus.INVALID_STAGE,
                    "Zombotany stage must be between 1 and 3."
            );
        }
        if (stageNumber
                > game.getHighestUnlockedStage()) {
            return result(
                    ZombotanyActionStatus.STAGE_LOCKED,
                    "Complete the previous Zombotany stage first."
            );
        }
        if (!game.preparePlantSelection(stageNumber)) {
            return result(
                    ZombotanyActionStatus.INVALID_STAGE,
                    "The requested stage could not be started."
            );
        }
        return result(
                ZombotanyActionStatus.SUCCESS,
                "Plant selection opened for Zombotany stage "
                        + stageNumber
                        + "."
        );
    }

    @Override
    public List<PlantDefinition> onShowAvailablePlantsRequested() {
        return game.getAvailablePlants();
    }

    @Override
    public List<PlantDefinition> onShowSelectedPlantsRequested() {
        return game.getSelectedPlants();
    }

    @Override
    public ZombotanyActionResult onAddPlantRequested(String plantName) {
        if (!game.isPlantSelectionPrepared() || game.isStarted()) {
            return result(ZombotanyActionStatus.NOT_STARTED, "Open plant selection first.");
        }
        if (!game.addSelectedPlant(plantName)) {
            return result(ZombotanyActionStatus.INVALID_PLANT,
                    "The plant is unavailable already selected or all 8 slots are full.");
        }
        return result(ZombotanyActionStatus.SUCCESS, plantName.trim() + " selected.");
    }

    @Override
    public ZombotanyActionResult onRemovePlantRequested(String plantName) {
        if (!game.removeSelectedPlant(plantName)) {
            return result(ZombotanyActionStatus.INVALID_PLANT, "The plant is not selected.");
        }
        return result(ZombotanyActionStatus.SUCCESS, plantName.trim() + " removed.");
    }

    @Override
    public ZombotanyActionResult onStartGameRequested() {
        if (!game.startSelectedStage()) {
            return result(ZombotanyActionStatus.NOT_STARTED,
                    "Select at least one plant before starting the game.");
        }
        return result(ZombotanyActionStatus.SUCCESS, "Zombotany game started.");
    }

    @Override
    public ZombotanyActionResult onPlantRequested(
            String plantName,
            Position position
    ) {
        ZombotanyActionResult invalidState =
                validateRunningGame();
        if (invalidState != null) {
            return invalidState;
        }
        if (!isInsideBoard(position)) {
            return result(
                    ZombotanyActionStatus.INVALID_POSITION,
                    "Plant position must be inside the 9 by 5 board."
            );
        }
        PlantDefinition definition =
                findAvailablePlant(plantName);
        ZombotanyActionResult invalidPlant = validatePlantRequest(plantName, definition);
        if (invalidPlant != null) {
            return invalidPlant;
        }
        if (!game.plant(plantName, position)) {
            return result(
                    ZombotanyActionStatus.CANNOT_PLANT,
                    "The tile may be occupied or the plant may be on cooldown."
            );
        }
        return result(
                ZombotanyActionStatus.SUCCESS,
                definition.getName()
                        + " planted at "
                        + position
                        + "."
        );
    }

    private ZombotanyActionResult validatePlantRequest(
            String plantName,
            PlantDefinition definition
    ) {
        if (definition == null) {
            return result(
                    ZombotanyActionStatus.INVALID_PLANT,
                    "This plant is not available in the current stage."
            );
        }
        if (!game.isPlantSelected(plantName)) {
            return result(
                    ZombotanyActionStatus.INVALID_PLANT,
                    "This plant was not selected for the current stage."
            );
        }
        if (game.getSunAmount() < definition.getCost()) {
            return result(
                    ZombotanyActionStatus.NOT_ENOUGH_SUN,
                    definition.getName() + " costs " + definition.getCost() + " sun."
            );
        }
        return null;
    }

    @Override
    public ZombotanyActionResult onCollectSunRequested(
            Position position
    ) {
        ZombotanyActionResult invalidState =
                validateRunningGame();
        if (invalidState != null) {
            return invalidState;
        }
        if (!isInsideBoard(position)) {
            return result(
                    ZombotanyActionStatus.INVALID_POSITION,
                    "Sun position must be inside the board."
            );
        }
        int collected =
                game.collectSun(position);
        if (collected <= 0) {
            return result(
                    ZombotanyActionStatus.NO_SUN_AT_POSITION,
                    "There is no collectible sun at "
                            + position
                            + "."
            );
        }
        return result(
                ZombotanyActionStatus.SUCCESS,
                collected
                        + " sun collected at "
                        + position
                        + "."
        );
    }

    @Override
    public ZombotanyActionResult onUsePlantFoodRequested(
            Position position
    ) {
        ZombotanyActionResult invalidState =
                validateRunningGame();
        if (invalidState != null) {
            return invalidState;
        }
        if (!isInsideBoard(position)) {
            return result(
                    ZombotanyActionStatus.INVALID_POSITION,
                    "Plant Food position must be inside the board."
            );
        }
        if (game.getPlantFoodAmount() <= 0) {
            return result(
                    ZombotanyActionStatus.NO_PLANT_FOOD,
                    "No Plant Food is currently available."
            );
        }
        if (!game.usePlantFood(position)) {
            return result(
                    ZombotanyActionStatus.CANNOT_USE_PLANT_FOOD,
                    "There is no plant that can receive Plant Food at this position."
            );
        }
        return result(
                ZombotanyActionStatus.SUCCESS,
                "Plant Food used at "
                        + position
                        + "."
        );
    }

    @Override
    public ZombotanyActionResult onAdvanceTicksRequested(
            int ticks
    ) {
        if (ticks <= 0) {
            return result(
                    ZombotanyActionStatus.INVALID_TICK_COUNT,
                    "Tick count must be greater than zero."
            );
        }
        ZombotanyActionResult invalidState =
                validateRunningGame();
        if (invalidState != null) {
            return invalidState;
        }
        int advancedTicks = 0;
        for (int tick = 0; tick < ticks; tick++) {
            game.onTick();
            advancedTicks++;
            if (game.isCompleted()) {
                break;
            }
        }
        return successfulResult(
                "Time advanced by "
                        + advancedTicks
                        + " ticks."
        );
    }

    @Override
    public ZombotanyStateResult onShowRequested() {
        Board board = game.getBoard();
        List<List<String>> plantGrid =
                createPlantGrid(board);
        List<ZombotanyZombieState> zombies =
                createZombieStates(board);
        List<Position> sunPositions =
                createSunPositions(board);
        boolean won = game.isCompleted()
                && !game.isLost();
        return new ZombotanyStateResult(
                game.getCurrentStageNumber(),
                game.getHighestUnlockedStage(),
                game.getCurrentWaveNumber(),
                game.getWaveCount(),
                game.getSunAmount(),
                game.getPlantFoodAmount(),
                game.getAvailablePlants(),
                game.getZombieTraits(),
                plantGrid,
                zombies,
                sunPositions,
                game.isStarted(),
                game.isCompleted(),
                won,
                game.isLost()
        );
    }

    private List<List<String>> createPlantGrid(
            Board board
    ) {
        List<List<String>> grid =
                new ArrayList<>();
        for (int y = 0;
             y < ZombotanyStateResult.ROW_COUNT;
             y++) {
            List<String> row =
                    new ArrayList<>();
            for (int x = 0;
                 x < ZombotanyStateResult.COLUMN_COUNT;
                 x++) {
                String plantName = "";
                if (board != null) {
                    List<Plant> plants =
                            board.getPlantsAt(
                                    new Position(x, y)
                            );
                    if (!plants.isEmpty()) {
                        Plant plant = plants.get(
                                plants.size() - 1
                        );
                        if (plant != null
                                && plant.getName() != null) {
                            plantName = plant.getName();
                        }
                    }
                }
                row.add(plantName);
            }
            grid.add(row);
        }
        return grid;
    }

    private List<ZombotanyZombieState>
    createZombieStates(Board board) {
        List<ZombotanyZombieState> states =
                new ArrayList<>();
        if (board == null) {
            return states;
        }
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null
                    || zombie.isDead()
                    || zombie.getPosition() == null) {
                continue;
            }
            ZombotanyTrait trait =
                    game.getTrait(zombie);
            states.add(
                    new ZombotanyZombieState(
                            getZombieName(zombie),
                            toExternalPosition(
                                    zombie.getPosition()
                            ),
                            zombie.getHealth(),
                            trait
                    )
            );
        }
        return states;
    }

    private List<Position> createSunPositions(
            Board board
    ) {
        List<Position> positions =
                new ArrayList<>();
        if (board == null
                || board.getSunSystem() == null
                || board.getSunSystem().getSuns() == null) {
            return positions;
        }
        for (Sun sun
                : board.getSunSystem().getSuns()) {
            if (sun == null
                    || sun.isCollected()
                    || sun.getPosition() == null) {
                continue;
            }
            positions.add(
                    toExternalPosition(
                            sun.getPosition()
                    )
            );
        }
        return positions;
    }

    private PlantDefinition findAvailablePlant(
            String plantName
    ) {
        if (plantName == null
                || plantName.trim().isEmpty()) {
            return null;
        }
        for (PlantDefinition definition
                : game.getAvailablePlants()) {
            if (definition != null
                    && definition.getName() != null
                    && definition.getName()
                    .equalsIgnoreCase(
                            plantName.trim()
                    )) {
                return definition;
            }
        }
        return null;
    }

    private String getZombieName(Zombie zombie) {
        if (zombie.getDefinition() == null) {
            return "Zombie";
        }
        String displayName =
                zombie.getDefinition().getDisplayName();
        if (displayName != null
                && !displayName.trim().isEmpty()) {
            return displayName;
        }
        String alias =
                zombie.getDefinition().getAlias();
        return alias == null || alias.trim().isEmpty()
                ? "Zombie"
                : alias;
    }

    private ZombotanyActionResult
    validateRunningGame() {
        if (!game.isStarted()) {
            return result(
                    ZombotanyActionStatus.NOT_STARTED,
                    "Start a Zombotany stage first."
            );
        }
        if (game.isCompleted()) {
            if (game.isLost()) {
                return result(
                        ZombotanyActionStatus.GAME_LOST,
                        "The zombies reached the house."
                );
            }
            return result(
                    game.getCurrentStageNumber() >= 3
                            ? ZombotanyActionStatus.GAME_WON
                            : ZombotanyActionStatus.STAGE_WON,
                    "This Zombotany stage is complete."
            );
        }
        return null;
    }

    private ZombotanyActionResult successfulResult(
            String message
    ) {
        if (game.isLost()) {
            return result(
                    ZombotanyActionStatus.GAME_LOST,
                    "The zombies reached the house."
            );
        }
        if (game.isCompleted()) {
            if (game.getCurrentStageNumber() >= 3) {
                return result(
                        ZombotanyActionStatus.GAME_WON,
                        message
                                + " All Zombotany stages are complete."
                );
            }
            return result(
                    ZombotanyActionStatus.STAGE_WON,
                    message
                            + " The next stage is unlocked."
            );
        }
        return result(
                ZombotanyActionStatus.SUCCESS,
                message
        );
    }

    private boolean isInsideBoard(Position position) {
        return position != null
                && position.getX() >= 1
                && position.getX()
                <= ZombotanyStateResult.COLUMN_COUNT
                && position.getY() >= 1
                && position.getY()
                <= ZombotanyStateResult.ROW_COUNT;
    }

    private Position toExternalPosition(
            Position boardPosition
    ) {
        return new Position(
                boardPosition.getX() + 1,
                boardPosition.getY() + 1
        );
    }

    private ZombotanyActionResult result(
            ZombotanyActionStatus status,
            String message
    ) {
        return ZombotanyActionResult.of(
                status,
                message,
                game.getCurrentStageNumber(),
                game.getCurrentWaveNumber(),
                game.getWaveCount(),
                game.getSunAmount(),
                game.getPlantFoodAmount(),
                game.getAliveZombieCount()
        );
    }

}
