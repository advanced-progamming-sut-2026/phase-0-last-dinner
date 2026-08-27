package model.mechanism;
import lombok.Getter;
import model.Greenhouse.Greenhouse;
import model.Greenhouse.GreenhouseBoostService;
import model.Plant;
import model.User.User;
import model.chapters.Chapter;
import model.chapters.ChapterMedieval;
import model.chapters.ChapterIceCaves;
import model.chapters.ChapterType;
import model.level.*;
import model.minigame.beghouledminigame.BeghouledMiniGame;
import model.minigame.beghouledminigame.PlantZombieBeghouledIntegration;
import model.minigame.izombieminigame.IZombieMiniGame;
import model.minigame.izombieminigame.PlantZombieIZombieIntegration;
import model.minigame.vasebreakerminigame.PlantZombieVasebreakerIntegration;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.minigame.wallnutbowlingminigame.PlantZombieWallnutBowlingIntegration;
import model.minigame.wallnutbowlingminigame.WallnutBowlingMiniGame;
import model.minigame.zombotanyminigame.PlantZombieZombotanyIntegration;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.plant.PlantUnlockService;
import model.plant.PlantUpgradeService;
import model.zombie.Zombie;
import model.zombie.ZombieChapter;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import view.GameEventListener;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
@Getter
// system haye plant va zombie ro baraye yek bazi be ham vasl mikone
public class PlantZombieGame {
    private static final String[] ADVENTURE_PLANT_REWARDS = {
            "Sunflower",
            "Wall-nut",
            "Potato Mine",
            "Cabbage-pult",
            "Iceberg Lettuce",
            "Grave Buster",
            "Bonk Choy",
            "Repeater"
    };
    private final PlantDefinitionRepository plantDefinitions;
    private final ZombieDefinitionRepository zombieDefinitions;
    private final PlantFactory plantFactory;
    // upgrade haye daemi ro az birun migire ta beyn game ha moshtarak bemune
    private final PlantUpgradeService plantUpgradeService;
    private final ZombieFactory zombieFactory;
    private final Board board;
    private final GameEngine engine;
    private final SunSystem sunSystem;
    private final PlantFoodSystem plantFoodSystem;
    private final PlantCooldownManager cooldownManager;
    private final PlantingSystem plantingSystem;
    private final LootSystem lootSystem;
    private final CombatSystem combatSystem;
    private final ZombieSpawner zombieSpawner;
    private final WaveManager waveManager;
    private final GameStatusService gameStatusService;
    // null yani bazi mahdudiate entekhabe giah nadare
    private Set<String> selectedPlantNames;
    private final Set<String> boostedPlantNames;
    private final User user;
    private Chapter activeChapter;
    private Level activeLevel;
    private QuestProgressTracker questProgressTracker;
    private boolean levelFinalized;
    // baraye ghazaye giahaye tuye greenhouse
    private final GreenhouseBoostService greenhouseBoostService;
    private DifficultyConfig difficultyConfig;
    private double pendingDifficultyTicks;
    private final Tickable levelMonitor;
    public PlantZombieGame(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory
    ) {
        this(plantDefinitions, zombieDefinitions, zombieFactory, new PlantUpgradeService(), null, null, null);
    }
    public PlantZombieGame(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory,
            PlantUpgradeService plantUpgradeService
    ) {
        this(plantDefinitions, zombieDefinitions, zombieFactory, plantUpgradeService, null, null, null);
    }
    public PlantZombieGame(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory,
            PlantUpgradeService plantUpgradeService,
            GreenhouseBoostService greenhouseBoostService
    ) {
        this(plantDefinitions, zombieDefinitions, zombieFactory, plantUpgradeService,
                greenhouseBoostService, null, null);
    }
    public PlantZombieGame(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory,
            PlantUpgradeService plantUpgradeService,
            GreenhouseBoostService greenhouseBoostService,
            User user
    ) {
        this(plantDefinitions, zombieDefinitions, zombieFactory, plantUpgradeService,
                greenhouseBoostService, user, null);
    }
    public PlantZombieGame(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory,
            PlantUpgradeService plantUpgradeService,
            GreenhouseBoostService greenhouseBoostService,
            User user,
            Board board
    ) {
        if (plantDefinitions == null || zombieDefinitions == null || zombieFactory == null
                || plantUpgradeService == null) {
            throw new IllegalArgumentException("Plant and zombie definitions are required");
        }
        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.zombieFactory = zombieFactory;
        this.plantUpgradeService = plantUpgradeService;
        this.plantFactory = new PlantFactory(this.plantUpgradeService);
        this.user = user;
        this.board = board == null ? new Board() : board;
        this.engine = new GameEngine(this.board);
        this.sunSystem = new SunSystem(this.board, this.engine.getClock());
        this.plantFoodSystem = new PlantFoodSystem(this.board);
        this.cooldownManager = new PlantCooldownManager(this.engine.getClock());
        this.plantingSystem = new PlantingSystem(this.board, this.sunSystem, this.cooldownManager);
        this.lootSystem = new LootSystem();
        this.lootSystem.setUser(this.user);
        this.combatSystem = new CombatSystem(this.board, this.lootSystem);
        this.zombieSpawner = new ZombieSpawner(this.zombieFactory, this.zombieDefinitions, this.board);
        this.waveManager = new WaveManager(null, this.zombieSpawner, this.engine);
        this.levelMonitor = this::monitorActiveLevel;
        this.engine.setGameEndObserver(this.levelMonitor::onTick);
        this.board.setPlantRemovalObserver(plant -> this.recordDestroyedPlant(this.activeLevel));
        this.gameStatusService = new GameStatusService(
                this.board,
                this.waveManager,
                this.sunSystem,
                this.plantFoodSystem,
                this.cooldownManager
        );
        this.greenhouseBoostService = greenhouseBoostService;
        this.boostedPlantNames = new LinkedHashSet<>();
        this.difficultyConfig = new DifficultyConfig(this.user);
        this.pendingDifficultyTicks = 0;
        this.zombieSpawner.setDifficultyConfig(this.difficultyConfig);
        this.sunSystem.setSpawnRateMultiplier(this.difficultyConfig.getInverseMultiplier());
        this.registerCoreSystems();
    }
    private void registerCoreSystems() {
        this.engine.register(this.sunSystem);
        this.engine.register(this.cooldownManager);
        this.engine.register(this.combatSystem);
        this.engine.register(this.waveManager);
    }
    public boolean plant(String plantName, Position position) {
        if (this.activeLevel instanceof ConveyorBeltLevel) {
            return this.plantFromConveyor(plantName, position);
        }
        if (!this.isPlantSelected(plantName)) {
            return false;
        }
        PlantDefinition definition = this.plantDefinitions.findByName(plantName);
        if (definition == null) {
            return false;
        }
        return this.placePlant(this.plantFactory.create(definition), position);
    }
    private boolean plantFromConveyor(String plantName, Position position) {
        ConveyorBeltLevel conveyorLevel = (ConveyorBeltLevel) this.activeLevel;
        Plant selectedPlant = null;
        for (Plant plant : conveyorLevel.getConveyorPlants()) {
            if (plant != null && plant.getName() != null
                    && plant.getName().equalsIgnoreCase(plantName)) {
                selectedPlant = plant;
                break;
            }
        }
        if (selectedPlant == null || !this.plantingSystem.plantWithoutCost(selectedPlant, position)) {
            return false;
        }
        conveyorLevel.takePlantFromConveyor(selectedPlant);
        if (this.questProgressTracker != null) {
            this.questProgressTracker.onPlantPlaced(selectedPlant, position);
        }
        return true;
    }
    public boolean plantImitater(String copiedPlantName, Position position) {
        if (!this.isPlantSelected("Imitater")) {
            return false;
        }
        PlantDefinition imitater = this.plantDefinitions.findByName("Imitater");
        PlantDefinition copiedDefinition = this.plantDefinitions.findByName(copiedPlantName);
        if (imitater == null || copiedDefinition == null || copiedDefinition == imitater) {
            return false;
        }
        return this.placePlant(this.plantFactory.createImitater(imitater, copiedDefinition), position);
    }
    public boolean pluckPlant(Position position) {
        Tile tile = position == null ? null : this.board.getTile(position);
        if (tile == null || tile.getPlants().isEmpty()) {
            return false;
        }
        this.plantingSystem.pluck(position);
        return true;
    }
    public Zombie spawnZombie(String alias, int row) {
        if (row < 0 || row >= 5) {
            return null;
        }
        return this.spawnZombie(alias, new Position(8, row));
    }
    public Zombie spawnZombie(String alias, Position position) {
        if (position == null || !this.board.isInsideBoard(position)) {
            return null;
        }
        ZombieDefinition definition = this.zombieDefinitions.findByAlias(alias);
        if (definition == null) {
            return null;
        }
        Zombie zombie = this.zombieFactory.create(definition, position);
        zombie.applyDifficulty(this.difficultyConfig.getMultiplier());
        this.board.addZombie(zombie, position);
        return zombie;
    }
    public void setEventListener(GameEventListener listener) {
        this.engine.setListener(listener);
        this.sunSystem.setListener(listener);
        this.plantFoodSystem.setListener(listener);
        this.lootSystem.setListener(listener);
        this.combatSystem.setListener(listener);
        this.zombieSpawner.setListener(listener);
        this.waveManager.setListener(listener);
        this.board.setListener(listener);
        if (this.activeChapter instanceof ChapterMedieval) {
            ((ChapterMedieval) this.activeChapter).setListener(listener);
        }
    }
    public void advanceTime(int tickCount) {
        if (tickCount <= 0) {
            return;
        }
        this.pendingDifficultyTicks += tickCount * this.difficultyConfig.getMultiplier();
        int adjustedTickCount = (int) this.pendingDifficultyTicks;
        this.pendingDifficultyTicks -= adjustedTickCount;
        for (int i = 0; i < adjustedTickCount && this.engine.isGameRunning(); i++) {
            this.engine.advanceTime(1);
            this.monitorActiveLevel();
        }
        if (adjustedTickCount == 0) {
            this.monitorActiveLevel();
        }
    }
    public void configureWaves(List<Wave> waves) {
        this.waveManager.configureWaves(waves);
    }
    public void configureChapter(Chapter chapter) {
        this.activeChapter = chapter;
        this.zombieSpawner.setChapter(chapter);
        this.waveManager.setChapter(chapter);
        this.zombieSpawner.setActiveChapter(this.toZombieChapter(chapter));
        this.combatSystem.setColdImmuneZombies(chapter instanceof ChapterIceCaves);
        this.sunSystem.setSkySunEnabled(!(chapter instanceof ChapterMedieval));
        if (chapter instanceof ChapterMedieval) {
            ChapterMedieval medieval = (ChapterMedieval) chapter;
            medieval.setZombieSpawner(this.zombieSpawner);
        }
        if (chapter instanceof ChapterIceCaves) {
            ((ChapterIceCaves) chapter).setZombieSpawner(this.zombieSpawner);
        }
    }
    public void configureLevel(Level level) {
        if (level == null) {
            return;
        }
        if (this.activeLevel instanceof Tickable) {
            this.engine.unregister((Tickable) this.activeLevel);
        }
        this.engine.unregister(this.levelMonitor);
        this.activeLevel = level;
        this.levelFinalized = false;
        if (this.activeChapter instanceof ChapterIceCaves) {
            ((ChapterIceCaves) this.activeChapter).setFrozenZombieStartEnabled(
                    level instanceof NormalLevel
            );
        }
        level.setBoard(this.board);
        this.configureWaves(level.getWaves());
        this.combatSystem.setGameClock(this.engine.getClock());
        boolean daytime = !(level instanceof NightOpsLevel)
                && !(this.activeChapter instanceof ChapterMedieval);
        this.sunSystem.setSkySunEnabled(daytime);
        this.configureLevelDifficulty(level);
        this.configureQuestTracking(level, daytime);
        if (level instanceof Tickable) {
            this.engine.register((Tickable) level);
        }
        level.start();
        this.engine.register(this.levelMonitor);
    }
    private void configureLevelDifficulty(Level level) {
        if (level instanceof MeowPointLevel) {
            MeowPointLevel meowPointLevel = (MeowPointLevel) level;
            this.zombieSpawner.setDifficultyConfig(new DifficultyConfig(null));
            this.sunSystem.setSpawnRateMultiplier(1.0);
            this.zombieSpawner.setRandom(new Random(meowPointLevel.getDailySeed()));
        } else {
            this.zombieSpawner.setDifficultyConfig(this.difficultyConfig);
            this.sunSystem.setSpawnRateMultiplier(this.difficultyConfig.getInverseMultiplier());
        }
    }
    private void configureQuestTracking(Level level, boolean daytime) {
        this.questProgressTracker = this.user == null
                ? null
                : new QuestProgressTracker(
                        this.user,
                        this.board,
                        this.engine.getClock(),
                        this.activeChapter,
                        this.difficultyConfig,
                        daytime
                );
        this.combatSystem.setKillObserver(event -> {
            if (level instanceof MeowPointLevel) {
                ((MeowPointLevel) level).onZombieKilled(event);
            }
            if (this.questProgressTracker != null) {
                this.questProgressTracker.onZombieKilled(event);
            }
        });
        this.board.setLawnMowerObserver(this.questProgressTracker == null
                ? null
                : this.questProgressTracker::onMowerKills);
    }
    public void configurePlantSelection(
            List<PlantDefinition> selectedPlants,
            Set<String> boostedPlants
    ) {
        this.selectedPlantNames = new LinkedHashSet<>();
        this.boostedPlantNames.clear();
        if (selectedPlants != null) {
            for (PlantDefinition definition : selectedPlants) {
                if (definition != null && definition.getName() != null) {
                    this.selectedPlantNames.add(this.normalizePlantName(definition.getName()));
                }
            }
        }
        if (boostedPlants != null) {
            for (String plantName : boostedPlants) {
                if (plantName != null) {
                    this.boostedPlantNames.add(this.normalizePlantName(plantName));
                }
            }
        }
    }
    // minigame ro ba hamin data va factory haye bazi misaze
    public VasebreakerMiniGame createVasebreakerMiniGame() {
        return new VasebreakerMiniGame(
                new PlantZombieVasebreakerIntegration(
                this.plantDefinitions,
                this.zombieDefinitions,
                this.zombieFactory
        ));
    }
    public WallnutBowlingMiniGame createWallnutBowlingMiniGame() {
        return new WallnutBowlingMiniGame(
                new PlantZombieWallnutBowlingIntegration(
                        this.plantDefinitions,
                        this.zombieDefinitions,
                        this.zombieFactory
                )
        );
    }
    public IZombieMiniGame createIZombieMiniGame() {
        return new IZombieMiniGame(
                new PlantZombieIZombieIntegration(
                        this.plantDefinitions,
                        this.zombieDefinitions,
                        this.zombieFactory
                )
        );
    }
    public BeghouledMiniGame createBeghouledMiniGame() {
        return new BeghouledMiniGame(
                new PlantZombieBeghouledIntegration(
                        this.plantDefinitions,
                        this.zombieDefinitions,
                        this.zombieFactory
                )
        );
    }
    public ZombotanyMiniGame createZombotanyMiniGame() {
        return new ZombotanyMiniGame(
                new PlantZombieZombotanyIntegration(
                        plantDefinitions,
                        zombieDefinitions,
                        zombieFactory
                )
        );
    }
    private boolean placePlant(Plant plant, Position position) {
        if (!this.plantingSystem.canPlant(plant, position))
            return false;
        this.plantingSystem.plant(plant, position);
        if (this.boostedPlantNames.contains(this.normalizePlantName(plant.getName()))) {
            plant.receivePlantFood();
        }
        if(greenhouseBoostService != null)
            greenhouseBoostService.castBoost(plant);
        if (this.questProgressTracker != null) {
            this.questProgressTracker.onPlantPlaced(plant, position);
        }
        return true;
    }
    public int collectSun(Position position) {
        int collected = this.sunSystem.collectSun(position);
        if (collected > 0 && this.questProgressTracker != null) {
            this.questProgressTracker.onSunCollected(collected);
        }
        return collected;
    }
    private boolean isPlantSelected(String plantName) {
        return this.selectedPlantNames == null
                || this.selectedPlantNames.contains(this.normalizePlantName(plantName));
    }
    private String normalizePlantName(String plantName) {
        return plantName == null
                ? ""
                : plantName.trim().toLowerCase(Locale.ROOT);
    }
    private ZombieChapter toZombieChapter(Chapter chapter) {
        if (chapter == null || chapter.getChapter() == null) {
            return ZombieChapter.ALL_CHAPTERS;
        }
        if (chapter.getChapter() == ChapterType.ICE_CAVES) {
            return ZombieChapter.FROSTBITE_CAVES;
        }
        try {
            return ZombieChapter.valueOf(chapter.getChapter().name());
        } catch (IllegalArgumentException exception) {
            return ZombieChapter.ALL_CHAPTERS;
        }
    }
    private void recordDestroyedPlant(Level level) {
        if (level instanceof LoveYourPlantsLevel) {
            ((LoveYourPlantsLevel) level).recordDestroyedPlant();
        }
        if (level instanceof MeowPointLevel) {
            ((MeowPointLevel) level).recordDestroyedPlant();
        }
        if (this.questProgressTracker != null) {
            this.questProgressTracker.onPlantDestroyed();
        }
    }
    private void monitorActiveLevel() {
        if (this.activeLevel == null || this.levelFinalized) {
            return;
        }
        if (this.activeLevel.isLoseConditionMet()) {
            this.levelFinalized = true;
            if (this.questProgressTracker != null) {
                this.questProgressTracker.onLevelFinished(false);
            }
            this.finalizeMeowPoints();
            if (this.engine.isGameRunning()) {
                this.engine.endGame();
            }
            return;
        }
        if (!this.activeLevel.isWinConditionMet()) {
            return;
        }
        this.levelFinalized = true;
        this.activeLevel.setCompleted(true);
        if (this.questProgressTracker != null) {
            this.questProgressTracker.onLevelFinished(true);
        }
        if (this.user != null && this.activeChapter != null
                && this.user.recordAdventureLevelCompletion(
                    this.activeChapter.getChapter(),
                    this.activeLevel.getLevelType()
            )) {
            this.unlockNextAdventurePlant();
        }
        this.finalizeMeowPoints();
    }
    private void unlockNextAdventurePlant() {
        for (String plantName : ADVENTURE_PLANT_REWARDS) {
            PlantDefinition definition = this.plantDefinitions.findByName(plantName);
            if (definition == null) {
                continue;
            }
            Plant plant = this.plantFactory.create(definition);
            if (PlantUnlockService.unlock(this.user, plant)) {
                this.user.addNews("New plant unlocked: " + definition.getName());
                return;
            }
        }
    }
    private void finalizeMeowPoints() {
        if (!(this.activeLevel instanceof MeowPointLevel)) {
            return;
        }
        MeowPointLevel meowPointLevel = (MeowPointLevel) this.activeLevel;
        meowPointLevel.calculatePoint();
        if (this.user != null) {
            this.user.setMaxObtainedMeowPoints(Math.max(
                    this.user.getMaxObtainedMeowPoints(),
                    meowPointLevel.getPoint()
            ));
        }
    }
}
