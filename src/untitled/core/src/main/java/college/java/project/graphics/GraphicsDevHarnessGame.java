package college.java.project.graphics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.viewport.FitViewport;
import pvz.skin.PvzSkin;
import model.Plant;
import model.chapters.ChapterType;
import model.collection.CollectionActionResult;
import model.collection.CollectionActionStatus;
import model.collection.PlantCollectionState;
import model.level.LevelType;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.mechanism.GameEngine;
import model.mechanism.LawnMower;
import model.mechanism.Position;
import model.mechanism.Tile;
import model.mechanism.TerrainType;
import model.mechanism.PlantStatus;
import model.mechanism.PlantingSystem;
import model.plant.CsvPlantDefinitionRepository;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.plant.PlantUpgradeResult;
import model.plant.PlantUpgradeService;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GraphicsDevHarnessGame extends Game {
    private static final String PLANTS_RESOURCE = "data/plants.csv";
    private static final String ZOMBIES_RESOURCE = "data/zombies.json";
    private static final String ARMOR_RESOURCE = "data/ArmorTypeData.json";

    private final String mode;

    public GraphicsDevHarnessGame(String mode) {
        this.mode = mode == null ? "collection" : mode.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void create() {
        try {
            PlantDefinitionRepository plants = CsvPlantDefinitionRepository.fromClasspath(PLANTS_RESOURCE);
            ZombieDefinitionRepository zombies = JsonZombieDefinitionRepository.fromClasspath(
                    ZOMBIES_RESOURCE,
                    ARMOR_RESOURCE
            );
            HarnessData data = new HarnessData(plants, zombies, chapterForMode(this.mode));
            setScreen(screenForMode(data));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load dev harness data", exception);
        }
    }


    private Screen screenForMode(HarnessData data) {
        Screen screen = switch (this.mode) {
            case "calibration-plants", "calibration-plants1" -> {
                data.preparePlantCalibrationPage(1);
                yield new HarnessGameplayScreen(data);
            }
            case "calibration-plants2" -> {
                data.preparePlantCalibrationPage(2);
                yield new HarnessGameplayScreen(data);
            }
            case "calibration-plants3" -> {
                data.preparePlantCalibrationPage(3);
                yield new HarnessGameplayScreen(data);
            }
            case "calibration-plants4" -> {
                data.preparePlantCalibrationPage(4);
                yield new HarnessGameplayScreen(data);
            }
            case "calibration-plants5" -> {
                data.preparePlantCalibrationPage(5);
                yield new HarnessGameplayScreen(data);
            }
            case "calibration-plants6" -> {
                data.preparePlantCalibrationPage(6);
                yield new HarnessGameplayScreen(data);
            }
            case "calibration-zombies", "calibration-zombies1" -> {
                data.prepareZombieCalibration(1);
                yield new HarnessGameplayScreen(data);
            }
            case "calibration-zombies2" -> {
                data.prepareZombieCalibration(2);
                yield new HarnessGameplayScreen(data);
            }
            case "calibration-zombies3" -> {
                data.prepareZombieCalibration(3);
                yield new HarnessGameplayScreen(data);
            }
            case "pick", "plant-pick" -> createPlantPickScreen(data);
            case "zombies", "zombie-collection" -> new ZombieCollectionScreen(data.zombieDefinitions);
            case "details-goo", "goo-details" -> new HarnessPlantDetailsScreen(data, "Goo Peashooter");
            case "details", "plant-details" -> new HarnessPlantDetailsScreen(data, "Rotobaga");
            case "effects", "gameplay-effects" -> new HarnessGameplayScreen(data, false, true);
            case "abilities", "gameplay-abilities" -> new HarnessGameplayScreen(
                    data, false, false, true, false, false, false, false, false
            );
            case "obstacles", "gameplay-obstacles" -> new HarnessGameplayScreen(
                    data, false, false, false, true, false, false, false, false
            );
            case "projectiles", "gameplay-projectiles" -> new HarnessGameplayScreen(
                    data, false, false, false, false, true, false, false, false
            );
            case "plantfood", "gameplay-plantfood" -> new HarnessGameplayScreen(
                    data, false, false, false, false, false, true, false, false
            );
            case "advanced", "advanced-abilities", "specials2" -> new HarnessGameplayScreen(
                    data, false, false, false, false, false, false, true, false
            );
            case "magnet", "magnet-ability" -> new HarnessGameplayScreen(
                    data, false, false, false, false, false, false, false, true
            );
            case "interaction", "gameplay-interaction" -> new HarnessGameplayScreen(data, true);
            case "egypt", "gameplay-egypt" -> new HarnessGameplayScreen(data);
            case "ice", "gameplay-ice" -> new HarnessGameplayScreen(data);
            case "beach", "gameplay-beach" -> new HarnessGameplayScreen(data);
            case "dark", "medieval", "gameplay-dark" -> new HarnessGameplayScreen(data);
            default -> new PlantCollectionScreen(data);
        };
        return screen instanceof HarnessGameplayScreen ? screen : new HarnessCaptureScreen(screen);
    }

    private Screen createPlantPickScreen(HarnessData data) {
        PlantPickScreen plantPickScreen = new PlantPickScreen(data, data.getChapterType());
        plantPickScreen.setOnStart(() -> setScreen(
                new HarnessGameplayScreen(data, true).enableLiveSimulation()
        ));
        return plantPickScreen;
    }

    private static void writeFrameBufferPng(String path) {
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, width, height, true);
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
        PixmapIO.writePNG(Gdx.files.absolute(path), pixmap);
        pixmap.dispose();
    }

    private ChapterType chapterForMode(String value) {
        return switch (value) {
            case "ice", "gameplay-ice", "obstacles", "gameplay-obstacles" -> ChapterType.ICE_CAVES;
            case "beach", "gameplay-beach" -> ChapterType.BIG_WAVE_BEACH;
            case "dark", "medieval", "gameplay-dark" -> ChapterType.MEDIEVAL;
            default -> ChapterType.ANCIENT_EGYPT;
        };
    }


    private static final class HarnessCaptureScreen extends ScreenAdapter {
        private final Screen delegate;
        private long firstRenderNanos;
        private boolean captureDone;

        private HarnessCaptureScreen(Screen delegate) {
            this.delegate = delegate;
        }

        @Override
        public void show() {
            this.delegate.show();
        }

        @Override
        public void render(float delta) {
            if (this.firstRenderNanos == 0L) {
                this.firstRenderNanos = System.nanoTime();
            }
            this.delegate.render(delta);
            captureFrameIfRequested(
                    (System.nanoTime() - this.firstRenderNanos) / 1_000_000_000f
            );
        }

        private void captureFrameIfRequested(float wallSeconds) {
            String path = System.getProperty("pvz.capture", "").trim();
            if (this.captureDone || path.isEmpty()) {
                return;
            }
            float captureAt;
            try {
                captureAt = Float.parseFloat(System.getProperty("pvz.captureAt", "0.8"));
            } catch (NumberFormatException ignored) {
                captureAt = 0.8f;
            }
            if (wallSeconds < captureAt) {
                return;
            }
            writeFrameBufferPng(path);
            this.captureDone = true;
        }

        @Override
        public void resize(int width, int height) {
            this.delegate.resize(width, height);
        }

        @Override
        public void pause() {
            this.delegate.pause();
        }

        @Override
        public void resume() {
            this.delegate.resume();
        }

        @Override
        public void hide() {
            this.delegate.hide();
        }

        @Override
        public void dispose() {
            this.delegate.dispose();
        }
    }

    private static final class HarnessPlantDetailsScreen extends ScreenAdapter {
        private final Stage stage;
        private final GameAssetManager assets;

        private HarnessPlantDetailsScreen(HarnessData data, String plantName) {
            this.stage = new SfxStage(new FitViewport(
                    PlantCollectionScreen.WORLD_WIDTH,
                    PlantCollectionScreen.WORLD_HEIGHT
            ));
            this.assets = new GameAssetManager();
            Skin skin = PvzSkin.get();
            PlantDetailsPanel panel = new PlantDetailsPanel(skin, this.assets, new PamAnimationCatalog());
            panel.setBounds(0f, 0f, PlantCollectionScreen.WORLD_WIDTH, PlantCollectionScreen.WORLD_HEIGHT);
            panel.setResources(data.getMints(), data.getGems(), data.getCoins());
            PlantCollectionState state = data.findPlantState(plantName);
            if (state == null && !data.getPlants().isEmpty()) {
                state = data.getPlants().get(0);
            }
            if (state != null) {
                panel.showPlant(state);
                panel.setVisible(true);
            }
            this.stage.addActor(panel);
        }

        @Override
        public void show() {
            Gdx.input.setInputProcessor(this.stage);
        }

        @Override
        public void render(float delta) {
            ScreenUtils.clear(Color.BLACK);
            this.assets.update();
            this.stage.act(Math.min(Math.max(delta, 0f), 1f / 20f));
            this.stage.draw();
        }

        @Override
        public void resize(int width, int height) {
            if (width > 0 && height > 0) {
                this.stage.getViewport().update(width, height, true);
            }
        }

        @Override
        public void dispose() {
            this.stage.dispose();
            this.assets.dispose();
        }
    }

    private static final class HarnessGameplayScreen extends ScreenAdapter {
        private final Stage stage;
        private final GameplayWorldScene scene;
        private final HarnessData data;
        private final boolean interactionDemo;
        private final boolean explosionDemo;
        private final boolean abilityDemo;
        private final boolean obstacleDemo;
        private final boolean projectileDemo;
        private final boolean plantFoodDemo;
        private final boolean advancedDemo;
        private final boolean magnetDemo;
        private boolean liveSimulation;
        private float simulationAccumulator;
        private long firstRenderNanos;
        private boolean damageTriggered;
        private boolean explosionTriggered;
        private boolean butterStarted;
        private boolean butterLanded;
        private boolean gargantuarStarted;
        private boolean impThrown;
        private boolean mineIntroStarted;
        private boolean bowlingFirstShot;
        private boolean bowlingSecondShot;
        private boolean bowlingThirdShot;
        private boolean armorBroken;
        private boolean bodyPartDeathTriggered;
        private boolean captureDone;
        private boolean autoPauseTriggered;
        private Projectile butterProjectile;
        private Zombie gargantuar;
        private Zombie armoredBeachZombie;
        private Zombie explosionZombie;
        private Zombie bodyPartZombie;
        private Plant mine;
        private Plant bowlingBulb;
        private Zombie hunter;
        private Zombie fisherman;
        private Zombie octopusZombie;
        private Zombie wizard;
        private Zombie king;
        private Zombie troglobite;
        private Zombie arcade;
        private Zombie barrelRoller;
        private Zombie prospector;
        private Zombie extinguishedProspector;
        private Zombie newspaper;
        private Zombie allStar;
        private Zombie dodo;
        private Zombie piano;
        private Plant allStarTarget;
        private Plant magnetShroom;
        private final List<Plant> projectilePlants = new ArrayList<>();
        private final List<Plant> plantFoodPlants = new ArrayList<>();
        private int demoStep;

        private HarnessGameplayScreen(HarnessData data) {
            this(data, false, false, false, false, false, false, false, false);
        }

        private HarnessGameplayScreen(HarnessData data, boolean selectPlant) {
            this(data, selectPlant, false, false, false, false, false, false, false);
        }

        private HarnessGameplayScreen(HarnessData data, boolean selectPlant, boolean explosionDemo) {
            this(data, selectPlant, explosionDemo, false, false, false, false, false, false);
        }

        private HarnessGameplayScreen(
                HarnessData data,
                boolean selectPlant,
                boolean explosionDemo,
                boolean abilityDemo,
                boolean obstacleDemo,
                boolean projectileDemo,
                boolean plantFoodDemo,
                boolean advancedDemo,
                boolean magnetDemo
        ) {
            this.stage = new SfxStage(new FitViewport(
                    GameplayWorldLayout.STAGE_WIDTH,
                    GameplayWorldLayout.STAGE_HEIGHT
            ));
            this.data = data;
            this.interactionDemo = selectPlant;
            this.explosionDemo = explosionDemo;
            this.abilityDemo = abilityDemo;
            this.obstacleDemo = obstacleDemo;
            this.projectileDemo = projectileDemo;
            this.plantFoodDemo = plantFoodDemo;
            this.advancedDemo = advancedDemo;
            this.magnetDemo = magnetDemo;
            if (explosionDemo) {
                data.addPlant("Cherry Bomb", 3, 2);
                data.addPlant("Kernel-pult", 1, 4);
                this.mine = data.addPlant("Primal Potato Mine", 2, 0);
                this.bowlingBulb = data.addPlant("Bowling Bulb", 2, 1);
                this.explosionZombie = data.addZombie("ZombieDefault", 7, 4);
                this.armoredBeachZombie = data.addZombie("ZombieNewspaper", 7, 1);
                this.bodyPartZombie = data.addZombie("ZombieDefault", 6, 3);
            } else if (abilityDemo) {
                prepareAbilityDemo();
            } else if (obstacleDemo) {
                prepareObstacleDemo();
            } else if (projectileDemo) {
                prepareProjectileDemo();
            } else if (plantFoodDemo) {
                preparePlantFoodDemo();
            } else if (advancedDemo) {
                prepareAdvancedDemo();
            } else if (magnetDemo) {
                prepareMagnetDemo();
            }
            String interactionPlant = null;
            if (selectPlant) {
                interactionPlant = System.getProperty("pvz.interactionPlant", "Peashooter").trim();
                if (interactionPlant.isEmpty()) {
                    interactionPlant = "Peashooter";
                }
                data.addSelected(interactionPlant);
                data.addZombie("ZombieDefault", 7, 2);
                if (interactionPlant.toLowerCase(Locale.ROOT).contains("mint")) {
                    data.addPlant("Repeater", 2, 3);
                    data.addZombie("ZombieDefault", 7, 3);
                }
            }
            this.scene = new GameplayWorldScene(data, data);
            this.scene.setPosition(0f, 0f);
            this.stage.addActor(this.scene);
            if (selectPlant) {
                this.scene.getSeedBank().selectPlant(interactionPlant);
            }
        }

        private HarnessGameplayScreen enableLiveSimulation() {
            this.liveSimulation = true;
            return this;
        }

        private void prepareAbilityDemo() {
            this.data.clearBoardEntities();
            this.data.addPlant("Peashooter", 2, 0);
            this.data.addPlant("Wall-nut", 2, 1);
            this.data.addPlant("Sunflower", 2, 2);
            this.data.addPlant("Repeater", 2, 3);
            this.hunter = this.data.addZombie("ZombieIceAgeHunter", 7, 0);
            this.fisherman = this.data.addZombie("ZombieBeachFisherman", 7, 1);
            this.octopusZombie = this.data.addZombie("ZombieBeachOctopus", 7, 2);
            this.wizard = this.data.addZombie("ZombieWizard", 7, 3);
            this.king = this.data.addZombie("ZombieDarkKing", 7, 4);
            this.data.addZombie("ZombieDefault", 5, 4);
        }

        private void prepareObstacleDemo() {
            this.data.clearBoardEntities();
            this.data.addPlant("Snow Pea", 2, 0);
            this.data.addPlant("Wall-nut", 2, 2);
            this.data.addPlant("Wall-nut", 2, 4);
            this.troglobite = this.data.addZombie("ZombieIceAgeTroglobite", 7, 0);
            this.arcade = this.data.addZombie("ZombieArcade", 8, 2);
            this.barrelRoller = this.data.addZombie("ZombieBarrelRoller", 7, 4);
        }

        private void prepareProjectileDemo() {
            this.data.clearBoardEntities();
            String projectileSet = System.getProperty("pvz.projectileSet", "core").toLowerCase(Locale.ROOT);
            String[] names;
            if ("homing".equals(projectileSet)) {
                names = new String[]{
                        "Caulipower", "Electric Blueberry", "Goo Peashooter", "Puff-shroom", "Sea-shroom"
                };
            } else if ("round14".equals(projectileSet)) {
                names = new String[]{"Snow Pea", "Grapeshot", "Repeater", "Pea Pod", "Mega Gatling Pea"};
            } else {
                names = new String[]{"Cabbage-pult", "Kernel-pult", "Citron", "Starfruit", "Bowling Bulb"};
            }
            for (int row = 0; row < names.length; row++) {
                Plant plant = this.data.addPlant(names[row], 1, row);
                if (plant != null) {
                    this.projectilePlants.add(plant);
                }
                int targetColumn = names[row].toLowerCase(Locale.ROOT).contains("shroom") ? 3 : 7;
                this.data.addZombie("ZombieDefault", targetColumn, row);
            }
        }

        private void preparePlantFoodDemo() {
            this.data.clearBoardEntities();
            String[] names = {"Rotobaga", "Sea-shroom", "Endurian", "Pumpkin", "Tall-nut"};
            for (int row = 0; row < names.length; row++) {
                Plant plant = this.data.addPlant(names[row], 2, row);
                if (plant != null) {
                    this.plantFoodPlants.add(plant);
                }
                this.data.addZombie("ZombieDefault", 7, row);
            }
        }

        private void prepareAdvancedDemo() {
            this.data.clearBoardEntities();
            this.data.addPlant("Wall-nut", 2, 0);
            this.allStarTarget = this.data.addPlant("Wall-nut", 3, 1);
            this.data.addPlant("Peashooter", 2, 2);
            this.data.addPlant("Sunflower", 2, 3);
            this.data.addPlant("Repeater", 2, 4);
            this.extinguishedProspector = this.data.addZombie("ZombieProspector", 5, 0);
            this.prospector = this.data.addZombie("ZombieProspector", 7, 0);
            this.allStar = this.data.addZombie("ZombieModernAllStar", 7, 1);
            this.dodo = this.data.addZombie("ZombieIceAgeDodo", 7, 2);
            this.newspaper = this.data.addZombie("ZombieNewspaper", 7, 3);
            this.piano = this.data.addZombie("ZombiePiano", 7, 4);
            if (this.allStar != null) {
                this.allStar.onTick();
            }
        }

        private void prepareMagnetDemo() {
            this.data.clearBoardEntities();
            this.magnetShroom = this.data.addPlant("Magnet-shroom", 2, 2);
            this.data.addZombie("ZombieArmor2", 5, 2);
            this.data.addZombie("ZombieDarkArmor3", 6, 1);
        }

        @Override
        public void show() {
            Gdx.input.setInputProcessor(this.stage);
        }

        @Override
        public void render(float delta) {
            ScreenUtils.clear(Color.BLACK);
            float frameDelta = Math.min(Math.max(delta, 0f), 1f / 20f);
            if (this.liveSimulation) {
                this.simulationAccumulator += Math.min(Math.max(delta, 0f), 0.25f);
                while (this.simulationAccumulator >= 0.1f) {
                    this.data.advanceSimulationTick();
                    this.simulationAccumulator -= 0.1f;
                }
            }
            if (this.firstRenderNanos == 0L) {
                this.firstRenderNanos = System.nanoTime();
            }
            float wallSeconds = (System.nanoTime() - this.firstRenderNanos) / 1_000_000_000f;
            if (this.interactionDemo && !this.liveSimulation
                    && this.demoStep == 0 && wallSeconds >= 0.55f) {
                this.scene.getInteractionLayer().hoverCell(3, 2);
                this.scene.getInteractionLayer().applyAtCell(3, 2);
                this.demoStep = 1;
            }
            if (this.explosionDemo && !this.mineIntroStarted && wallSeconds >= 0.35f) {
                this.mineIntroStarted = true;
                this.scene.getPlantLayer().playIntro(this.mine);
            }
            if (this.explosionDemo && !this.bowlingFirstShot && wallSeconds >= 0.65f) {
                this.bowlingFirstShot = true;
                this.scene.getPlantLayer().playAttack(this.bowlingBulb);
            }
            if (this.explosionDemo && !this.bowlingSecondShot && wallSeconds >= 1.45f) {
                this.bowlingSecondShot = true;
                this.scene.getPlantLayer().playAttack(this.bowlingBulb);
            }
            if (this.explosionDemo && !this.bowlingThirdShot && wallSeconds >= 2.25f) {
                this.bowlingThirdShot = true;
                this.scene.getPlantLayer().playAttack(this.bowlingBulb);
            }
            if (this.explosionDemo && !this.armorBroken && wallSeconds >= 3.20f) {
                this.armorBroken = true;
                if (this.armoredBeachZombie != null) {
                    this.armoredBeachZombie.takeDamage(850);
                }
            }
            if (this.explosionDemo && !this.butterStarted && wallSeconds >= 0.8f) {
                this.butterStarted = true;
                this.butterProjectile = this.data.addButterProjectile(1, 4, 7, 4);
            }
            if (this.explosionDemo && !this.butterLanded && wallSeconds >= 1.55f) {
                this.butterLanded = true;
                this.data.removeProjectile(this.butterProjectile);
            }
            if (this.explosionDemo && !this.damageTriggered && wallSeconds >= 2f) {
                this.damageTriggered = true;
                this.data.damagePlantAt(4, 3, 2800);
            }
            if (this.explosionDemo && !this.gargantuarStarted && wallSeconds >= 2.35f) {
                this.gargantuarStarted = true;
                this.gargantuar = this.data.addZombie("ZombieEgyptGargantuar", 6, 0);
                this.data.damageZombieToHalf(this.gargantuar);
            }
            if (this.explosionDemo && !this.impThrown && wallSeconds >= 2.80f) {
                this.impThrown = true;
                this.data.addZombie("ZombieEgyptImpDefault", 2, 0);
            }
            if (this.explosionDemo && !this.explosionTriggered && wallSeconds >= 4f) {
                this.explosionTriggered = true;
                this.data.removePlantAt(3, 2);
                if (this.explosionZombie != null) {
                    this.explosionZombie.takeDamage(Math.max(1, this.explosionZombie.getMaximumHealth() * 2));
                }
            }
            if (this.explosionDemo && !this.bodyPartDeathTriggered && wallSeconds >= 4.75f) {
                this.bodyPartDeathTriggered = true;
                if (this.bodyPartZombie != null) {
                    this.bodyPartZombie.takeDamage(Math.max(1, this.bodyPartZombie.getMaximumHealth() * 2));
                }
            }
            runAbilityDemo(wallSeconds);
            runObstacleDemo(wallSeconds);
            runProjectileDemo(wallSeconds);
            runPlantFoodDemo(wallSeconds);
            runAdvancedDemo(wallSeconds);
            runMagnetDemo(wallSeconds);
            if (!this.autoPauseTriggered
                    && Boolean.parseBoolean(System.getProperty("pvz.autoPause", "false"))
                    && wallSeconds >= 4.45f) {
                this.autoPauseTriggered = true;
                this.scene.openPauseMenu();
            }
            this.stage.act(frameDelta);
            this.stage.draw();
            captureFrameIfRequested(wallSeconds);
        }

        private void runAbilityDemo(float wallSeconds) {
            if (!this.abilityDemo) {
                return;
            }
            float[] times = {0.75f, 1.45f, 2.15f, 2.85f, 3.55f};
            Zombie[] zombies = {this.hunter, this.fisherman, this.octopusZombie, this.wizard, this.king};
            while (this.demoStep < times.length && wallSeconds >= times[this.demoStep]) {
                Zombie zombie = zombies[this.demoStep];
                if (zombie != null) {
                    zombie.activateAbility();
                }
                this.demoStep++;
            }
        }

        private void runObstacleDemo(float wallSeconds) {
            if (!this.obstacleDemo || this.demoStep > 0 || wallSeconds < 0.75f) {
                return;
            }
            Zombie[] zombies = {this.troglobite, this.arcade, this.barrelRoller};
            for (Zombie zombie : zombies) {
                if (zombie != null) {
                    zombie.activateAbility();
                }
            }
            this.demoStep = 1;
        }

        private void runProjectileDemo(float wallSeconds) {
            if (!this.projectileDemo || this.demoStep > 0 || wallSeconds < 0.75f) {
                return;
            }
            boolean round14 = "round14".equalsIgnoreCase(System.getProperty("pvz.projectileSet", "core"));
            for (Plant plant : this.projectilePlants) {
                String name = plant == null || plant.getName() == null
                        ? ""
                        : plant.getName().toLowerCase(Locale.ROOT);
                if (round14 && (name.contains("repeater") || name.contains("pea pod")
                        || name.contains("mega gatling"))) {
                    plant.receivePlantFood();
                } else if (plant != null) {
                    plant.useAbility();
                }
            }
            this.demoStep = 1;
        }

        private void runPlantFoodDemo(float wallSeconds) {
            if (!this.plantFoodDemo || this.demoStep > 0 || wallSeconds < 0.75f) {
                return;
            }
            for (int row = 0; row < this.plantFoodPlants.size(); row++) {
                Plant plant = this.plantFoodPlants.get(row);
                if (plant != null && plant.canReceivePlantFood()) {
                    plant.receivePlantFood();
                    this.scene.getPlantLayer().playPlantFoodAt(2, row);
                }
            }
            this.demoStep = 1;
        }

        private void runAdvancedDemo(float wallSeconds) {
            if (!this.advancedDemo) {
                return;
            }
            if (this.demoStep == 0 && wallSeconds >= 0.55f) {
                if (this.extinguishedProspector != null) {
                    Projectile ice = new Projectile(
                            "0", this.extinguishedProspector.getPosition(), 0.25d,
                            ProjectileType.ICE, this.extinguishedProspector
                    );
                    if (this.extinguishedProspector.getBehavior() != null) {
                        this.extinguishedProspector.getBehavior().onProjectileHit(
                                this.extinguishedProspector, ice, this.data.board
                        );
                    }
                    this.scene.getZombieLayer().noteProjectileImpact(ice);
                }
                this.demoStep = 1;
            }
            if (this.demoStep == 1 && wallSeconds >= 0.75f) {
                if (this.dodo != null) {
                    this.dodo.activateAbility();
                }
                this.demoStep = 2;
            }
            if (this.demoStep == 2 && wallSeconds >= 1.15f) {
                if (this.prospector != null) {
                    this.prospector.activateAbility();
                }
                this.demoStep = 3;
            }
            if (this.demoStep == 3 && wallSeconds >= 2.15f) {
                if (this.newspaper != null) {
                    this.newspaper.takeDamage(900);
                    this.newspaper.onTick();
                }
                this.demoStep = 4;
            }
            if (this.demoStep == 4 && wallSeconds >= 2.75f) {
                if (this.allStar != null && this.allStarTarget != null) {
                    this.allStar.attack(this.allStarTarget);
                    this.allStar.onTick();
                }
                this.demoStep = 5;
            }
            if (this.demoStep == 5 && wallSeconds >= 3.45f) {
                if (this.piano != null) {
                    this.piano.activateAbility();
                }
                this.demoStep = 6;
            }
            if (this.demoStep == 6 && wallSeconds >= 4.20f) {
                if (this.dodo != null) {
                    for (int index = 0; index < 75; index++) {
                        this.dodo.move();
                        this.dodo.onTick();
                    }
                }
                this.demoStep = 7;
            }
        }

        private void runMagnetDemo(float wallSeconds) {
            if (!this.magnetDemo || this.demoStep > 0 || wallSeconds < 0.85f) {
                return;
            }
            if (this.magnetShroom != null) {
                this.magnetShroom.useAbility();
            }
            this.demoStep = 1;
        }

        private void captureFrameIfRequested(float wallSeconds) {
            String path = System.getProperty("pvz.capture", "").trim();
            if (this.captureDone || path.isEmpty()) {
                return;
            }
            float captureAt;
            try {
                captureAt = Float.parseFloat(System.getProperty("pvz.captureAt", "4.15"));
            } catch (NumberFormatException ignored) {
                captureAt = 4.15f;
            }
            if (wallSeconds < captureAt) {
                return;
            }
            writeFrameBufferPng(path);
            this.captureDone = true;
        }

        @Override
        public void resize(int width, int height) {
            if (width > 0 && height > 0) {
                this.stage.getViewport().update(width, height, true);
            }
        }

        @Override
        public void hide() {
            if (Gdx.input.getInputProcessor() == this.stage) {
                Gdx.input.setInputProcessor(null);
            }
        }

        @Override
        public void dispose() {
            this.scene.dispose();
            this.stage.dispose();
        }
    }

    private static final class HarnessData implements PlantCollectionDataSource,
            PlantPickDataSource, GameplaySeedBankDataSource, GameplayWorldDataSource {
        private final PlantDefinitionRepository plantDefinitions;
        private final ZombieDefinitionRepository zombieDefinitions;
        private final PlantUpgradeService upgrades;
        private final PlantFactory plantFactory;
        private final ZombieFactory zombieFactory;
        private final ChapterType chapterType;
        private final Board board;
        private final GameEngine gameEngine;
        private final CombatSystem combatSystem;
        private final Set<String> selectedNames = new LinkedHashSet<>();
        private final Set<String> boostedNames = new LinkedHashSet<>();
        private final Set<String> lockedNames = new LinkedHashSet<>();
        private int coins = 25000;
        private int gems = 80;
        private int sun = 650;
        private int plantFood = 3;
        private boolean started;

        private HarnessData(
                PlantDefinitionRepository plantDefinitions,
                ZombieDefinitionRepository zombieDefinitions,
                ChapterType chapterType
        ) {
            this.plantDefinitions = plantDefinitions;
            this.zombieDefinitions = zombieDefinitions;
            this.upgrades = new PlantUpgradeService(200000);
            this.plantFactory = new PlantFactory(this.upgrades);
            this.zombieFactory = new ZombieFactory(zombieDefinitions);
            this.chapterType = chapterType;
            this.board = new Board();
            this.gameEngine = new GameEngine(this.board);
            this.combatSystem = new CombatSystem(this.board);
            this.gameEngine.register(this.combatSystem);
            prepareProgress();
            prepareBoard();
        }

        private void advanceSimulationTick() {
            this.gameEngine.advanceTime();
        }

        private void prepareProgress() {
            List<PlantDefinition> definitions = safeDefinitions();
            for (int index = 0; index < definitions.size(); index++) {
                PlantDefinition definition = definitions.get(index);
                if (definition == null || definition.getName() == null) {
                    continue;
                }
                this.upgrades.addSeedPackets(definition.getName(), 40 + index % 5 * 5);
            }
            addSelected("Peashooter");
            addSelected("Sunflower");
            addSelected("Wall-nut");
            addSelected("Snow Pea");
            addSelected("Kernel-pult");
            addSelected("Bonk Choy");
            addSelected("Cabbage-pult");
            addSelected("Potato Mine");
            this.boostedNames.add(normalize("Peashooter"));
        }

        private void prepareBoard() {
            setChapterTerrain();
            addPlant("Sunflower", 1, 1);
            addPlant("Peashooter", 2, 2);
            addPlant("Wall-nut", 4, 3);
            String zombieAlias = switch (this.chapterType) {
                case ICE_CAVES -> "ZombieIceAgeHunter";
                case BIG_WAVE_BEACH -> "ZombieBeachSnorkel";
                case MEDIEVAL -> "ZombieDarkArmor3";
                default -> "ZombieArmor1";
            };
            addZombie(zombieAlias, 7, 2);
        }

        private void preparePlantCalibrationPage(int page) {
            clearBoardEntities();
            String[] names = switch (page) {
                case 2 -> new String[]{
                        "Kernel-pult", "Cabbage-pult", "Potato Mine", "Primal Potato Mine",
                        "Cherry Bomb", "Squash", "Bonk Choy", "Tall-nut",
                        "Pumpkin", "Torchwood", "Magnet-shroom", "Lily Pad"
                };
                case 3 -> new String[]{
                        "Twin Sunflower", "Sun-shroom", "Primal Sunflower", "Gold Bloom",
                        "Threepeater", "Snow Pea", "Pea Pod", "Split Pea",
                        "Citron", "Caulipower", "Electric Blueberry", "Bowling Bulb"
                };
                case 4 -> new String[]{
                        "Fire Peashooter", "Starfruit", "Mega Gatling Pea", "Sea-shroom",
                        "Puff-shroom", "Grapeshot", "Jalapeno", "Doom-shroom",
                        "Tangle Kelp", "Phat Beet", "Chomper", "Wasabi Whip"
                };
                case 5 -> new String[]{
                        "Kiwibeast", "Endurian", "Garlic", "Sweet Potato",
                        "Explode-o-nut", "Sun Bean", "Hypno-shroom", "Imitater",
                        "Ice-shroom", "Hot Potato", "Grave Buster", "Cat-tail"
                };
                case 6 -> new String[]{
                        "Enlighten-mint", "Appease-mint", "Arma-mint", "Bombard-mint",
                        "Enforce-mint", "Reinforce-mint", "Enchant-mint", "Pierce-mint",
                        "catTail-mint"
                };
                default -> new String[]{
                        "Sunflower", "Peashooter", "Repeater", "Rotobaga",
                        "Goo Peashooter", "Cactus", "Fume-shroom", "Melon-pult",
                        "Winter Melon", "Pepper-pult", "Iceberg Lettuce", "Wall-nut"
                };
            };
            addCalibrationPlants(names);
        }

        private void addCalibrationPlants(String[] names) {
            int[] columns = {1, 3, 5, 7};
            for (int index = 0; index < names.length; index++) {
                addPlant(names[index], columns[index % columns.length], index / columns.length);
            }
        }

        private void prepareZombieCalibration(int page) {
            clearBoardEntities();
            String[] aliases = switch (page) {
                case 2 -> new String[]{
                        "ZombieIceAgeHunter", "ZombieIceAgeTroglobite",
                        "ZombieBeachFisherman", "ZombieBeachOctopus",
                        "ZombieBeachSnorkel", "ZombieDarkJuggler",
                        "ZombieWizard", "ZombieDarkKing",
                        "ZombieCrystalSkull", "ZombieLostCityJane"
                };
                case 3 -> new String[]{
                        "ZombieArmor2", "ZombieArmor4",
                        "ZombieDarkArmor3", "ZombieGargantuar",
                        "ZombieImp", "ZombieRa",
                        "ZombieExplorer", "ZombieTombRaiser"
                };
                default -> new String[]{
                        "ZombieDefault", "ZombieArmor1",
                        "ZombieModernAllStar", "ZombieNewspaper",
                        "ZombieArcade", "ZombiePiano",
                        "ZombieProspector", "ZombieDarkImpDragon",
                        "ZombieBarrelRoller", "ZombieIceAgeDodo"
                };
            };
            int[] columns = {3, 7};
            for (int index = 0; index < aliases.length; index++) {
                addZombie(aliases[index], columns[index % columns.length], index / columns.length);
            }
        }

        private void clearBoardEntities() {
            for (Plant plant : new ArrayList<>(this.board.getAllPlants())) {
                this.board.removePlant(plant);
            }
            for (Zombie zombie : new ArrayList<>(this.board.getAllZombies())) {
                this.board.removeZombie(zombie);
            }
        }

        private void setChapterTerrain() {
            if (this.chapterType == ChapterType.ICE_CAVES) {
                this.board.setTerrain(new Position(4, 1), TerrainType.SLIPPERY_UP);
                this.board.setTerrain(new Position(5, 3), TerrainType.SLIPPERY_DOWN);
                this.board.setTerrain(new Position(6, 2), TerrainType.FROZEN);
            } else if (this.chapterType == ChapterType.BIG_WAVE_BEACH) {
                for (int row = 0; row < 5; row++) {
                    this.board.setTerrain(new Position(6, row), TerrainType.WATER);
                    this.board.setTerrain(new Position(7, row), TerrainType.WATER);
                    this.board.setTerrain(new Position(8, row), TerrainType.LOW_BEACH);
                }
            } else if (this.chapterType == ChapterType.MEDIEVAL) {
                this.board.setTerrain(new Position(5, 1), TerrainType.GRAVE);
                this.board.setTerrain(new Position(6, 2), TerrainType.NECROMANCY);
                this.board.setTerrain(new Position(5, 3), TerrainType.GRAVE);
            } else {
                this.board.setTerrain(new Position(5, 1), TerrainType.GRAVE);
                this.board.setTerrain(new Position(6, 3), TerrainType.GRAVE);
            }
        }

        private Plant addPlant(String name, int column, int row) {
            PlantDefinition definition = this.plantDefinitions.findByName(name);
            if (definition == null) {
                return null;
            }
            Plant plant = this.plantFactory.create(definition);
            Position position = new Position(column, row);
            plant.setPosition(position);
            plant.setBoard(this.board);
            Tile tile = this.board.getTile(position);
            if (tile == null) {
                return null;
            }
            tile.addPlant(plant);
            return plant;
        }

        private void damagePlantAt(int column, int row, int damage) {
            Tile tile = this.board.getTile(new Position(column, row));
            if (tile == null || tile.getPlants() == null || tile.getPlants().isEmpty()) {
                return;
            }
            Plant plant = tile.getPlants().get(tile.getPlants().size() - 1);
            plant.takeDamage(Math.max(0, damage));
        }

        private void removePlantAt(int column, int row) {
            Tile tile = this.board.getTile(new Position(column, row));
            if (tile == null || tile.getPlants() == null || tile.getPlants().isEmpty()) {
                return;
            }
            this.board.removePlant(tile.getPlants().get(tile.getPlants().size() - 1));
        }

        private Zombie addZombie(String alias, int column, int row) {
            ZombieDefinition definition = this.zombieDefinitions.findByAlias(alias);
            if (definition == null) {
                return null;
            }
            Zombie zombie = this.zombieFactory.create(definition, new Position(column, row));
            this.board.addZombie(zombie, new Position(column, row));
            return zombie;
        }

        private Projectile addButterProjectile(
                int sourceColumn,
                int sourceRow,
                int targetColumn,
                int targetRow
        ) {
            Plant source = getTopPlantAt(sourceColumn, sourceRow);
            Zombie target = findZombieAt(targetColumn, targetRow);
            if (source == null || target == null) {
                return null;
            }
            Projectile projectile = new Projectile(
                    "40",
                    new Position(sourceColumn, sourceRow),
                    0.28d,
                    ProjectileType.LOBBED,
                    target,
                    0,
                    0,
                    0,
                    15L,
                    0,
                    100,
                    0,
                    8,
                    new ArrayList<>()
            );
            projectile.setSourcePlant(source);
            projectile.setLobbed(true);
            this.board.addProjectile(projectile);
            return projectile;
        }

        private Zombie findZombieAt(int column, int row) {
            Tile tile = this.board.getTile(new Position(column, row));
            if (tile == null || tile.getZombies() == null || tile.getZombies().isEmpty()) {
                return null;
            }
            return tile.getZombies().get(0);
        }

        private void removeProjectile(Projectile projectile) {
            if (projectile != null) {
                this.board.getProjectiles().remove(projectile);
            }
        }

        private void damageZombieToHalf(Zombie zombie) {
            if (zombie == null) {
                return;
            }
            zombie.takeDamage(Math.max(1, zombie.getMaximumHealth() / 2));
        }

        private void addSelected(String name) {
            if (this.plantDefinitions.findByName(name) != null) {
                this.selectedNames.add(normalize(name));
            }
        }

        private PlantCollectionState findPlantState(String plantName) {
            for (PlantCollectionState state : getPlants()) {
                if (state != null && normalize(state.getName()).equals(normalize(plantName))) {
                    return state;
                }
            }
            return null;
        }

        @Override
        public List<PlantCollectionState> getPlants() {
            List<PlantCollectionState> states = new ArrayList<>();
            for (PlantDefinition definition : safeDefinitions()) {
                if (definition == null) {
                    continue;
                }
                PlantCollectionState state = PlantCollectionState.from(
                        definition,
                        this.upgrades,
                        !this.lockedNames.contains(normalize(definition.getName()))
                );
                if (state != null) {
                    states.add(state);
                }
            }
            states.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
            return states;
        }

        @Override
        public int getGold() {
            return this.coins;
        }

        @Override
        public int getCoins() {
            return this.coins;
        }

        @Override
        public int getGems() {
            return this.gems;
        }

        @Override
        public CollectionActionResult upgradePlant(String plantName) {
            PlantDefinition definition = this.plantDefinitions.findByName(plantName);
            if (definition == null) {
                return failure("Plant was not found.", plantName);
            }
            int previousLevel = this.upgrades.getLevel(definition.getName());
            PlantUpgradeResult result = this.upgrades.upgrade(definition);
            if (result != PlantUpgradeResult.SUCCESS) {
                return failure("Upgrade is not available in this harness state.", plantName);
            }
            return CollectionActionResult.plantUpgraded(
                    definition.getName(),
                    previousLevel,
                    this.upgrades.getLevel(definition.getName()),
                    this.upgrades.getSeedPackets(definition.getName()),
                    this.coins,
                    0
            );
        }

        @Override
        public CollectionActionResult purchasePlant(String plantName) {
            String normalized = normalize(plantName);
            if (!this.lockedNames.contains(normalized)) {
                return failure("Plant is already unlocked.", plantName);
            }
            if (this.coins < 2000) {
                return failure("Not enough coins.", plantName);
            }
            this.coins -= 2000;
            this.lockedNames.remove(normalized);
            return CollectionActionResult.plantPurchased(plantName, this.coins);
        }

        @Override
        public boolean isAvailable(String plantName) {
            return this.plantDefinitions.findByName(plantName) != null
                    && !this.lockedNames.contains(normalize(plantName));
        }

        @Override
        public boolean isSelected(String plantName) {
            return this.selectedNames.contains(normalize(plantName));
        }

        @Override
        public boolean isBoosted(String plantName) {
            return this.boostedNames.contains(normalize(plantName));
        }

        @Override
        public boolean isGreenhouseBoosted(String plantName) {
            return normalize(plantName).equals(normalize("Sunflower"));
        }

        @Override
        public int getSelectedCount() {
            return this.selectedNames.size();
        }

        @Override
        public int getSlotCount() {
            return 8;
        }

        @Override
        public String togglePlant(String plantName) {
            String normalized = normalize(plantName);
            if (this.selectedNames.remove(normalized)) {
                this.boostedNames.remove(normalized);
                return plantName + " was removed.";
            }
            if (!isAvailable(plantName)) {
                return "Plant is locked.";
            }
            if (this.selectedNames.size() >= 8) {
                return "Plant selection is full.";
            }
            this.selectedNames.add(normalized);
            return plantName + " was added.";
        }

        @Override
        public String boostPlant(String plantName) {
            String normalized = normalize(plantName);
            if (!this.selectedNames.contains(normalized)) {
                return "Plant is not selected.";
            }
            if (this.boostedNames.contains(normalized)) {
                return "Plant is already boosted.";
            }
            if (this.gems < 2) {
                return "Not enough diamonds. Required: 2";
            }
            this.gems -= 2;
            this.boostedNames.add(normalized);
            return plantName + " was boosted.";
        }

        @Override
        public String startGame() {
            if (this.selectedNames.isEmpty()) {
                return "Select at least one plant before starting the game.";
            }
            this.started = true;
            return "Game started.";
        }

        @Override
        public boolean isStarted() {
            return this.started;
        }

        @Override
        public List<PlantCollectionState> getSelectedPlants() {
            List<PlantCollectionState> selected = new ArrayList<>();
            for (PlantCollectionState state : getPlants()) {
                if (state != null && this.selectedNames.contains(normalize(state.getName()))) {
                    selected.add(state);
                }
            }
            return selected;
        }

        @Override
        public List<PlantStatus> getPlantStatuses() {
            List<PlantStatus> statuses = new ArrayList<>();
            for (String selectedName : this.selectedNames) {
                PlantDefinition definition = findDefinition(selectedName);
                if (definition != null) {
                    statuses.add(new PlantStatus(this.plantFactory.create(definition), true, 0L));
                }
            }
            return statuses;
        }

        @Override
        public int getSunAmount() {
            return this.sun;
        }

        @Override
        public int getPlantFoodCount() {
            return this.plantFood;
        }

        @Override
        public boolean plant(String plantName, int column, int row) {
            PlantDefinition definition = this.plantDefinitions.findByName(plantName);
            Position position = new Position(column, row);
            if (definition == null || !this.board.isInsideBoard(position)) {
                return false;
            }
            Plant plant = this.plantFactory.create(definition);
            if (this.sun < plant.getSunCost()) {
                return false;
            }
            PlantingSystem plantingSystem = new PlantingSystem(this.board, null, null);
            if (!plantingSystem.plantWithoutCost(plant, position)) {
                return false;
            }
            this.sun -= plant.getSunCost();
            if (isBoosted(plantName) && plant.canReceivePlantFood()) {
                plant.receivePlantFood();
            }
            return true;
        }

        @Override
        public boolean canPlant(String plantName, int column, int row) {
            PlantDefinition definition = this.plantDefinitions.findByName(plantName);
            Position position = new Position(column, row);
            if (definition == null || !this.board.isInsideBoard(position)) {
                return false;
            }
            Plant plant = this.plantFactory.create(definition);
            Tile tile = this.board.getTile(position);
            return tile != null && tile.canPlacePlant(plant) && this.sun >= plant.getSunCost();
        }

        @Override
        public String getPlantingFailureMessage(String plantName, int column, int row) {
            PlantDefinition definition = this.plantDefinitions.findByName(plantName);
            if (definition != null && this.sun < definition.getCost()) {
                return "Not enough sun.";
            }
            return "That plant cannot be placed on this tile.";
        }

        @Override
        public boolean hasPlantAt(int column, int row) {
            return getTopPlantAt(column, row) != null;
        }

        @Override
        public Plant getTopPlantAt(int column, int row) {
            Tile tile = this.board.getTile(new Position(column, row));
            if (tile == null || tile.getPlants() == null || tile.getPlants().isEmpty()) {
                return null;
            }
            return tile.getPlants().get(tile.getPlants().size() - 1);
        }

        @Override
        public boolean canFeedPlantAt(int column, int row) {
            Plant plant = getTopPlantAt(column, row);
            return this.plantFood > 0 && plant != null && plant.canReceivePlantFood();
        }

        @Override
        public boolean pluckPlant(int column, int row) {
            Plant plant = getTopPlantAt(column, row);
            if (plant == null) {
                return false;
            }
            this.board.removePlant(plant);
            return true;
        }

        @Override
        public boolean feedPlant(int column, int row) {
            Plant plant = getTopPlantAt(column, row);
            if (this.plantFood <= 0 || plant == null || !plant.canReceivePlantFood()) {
                return false;
            }
            plant.receivePlantFood();
            this.plantFood--;
            return true;
        }

        @Override
        public List<Plant> getPlantsOnBoard() {
            return new ArrayList<>(this.board.getAllPlants());
        }

        @Override
        public List<Zombie> getZombiesOnBoard() {
            return new ArrayList<>(this.board.getAllZombies());
        }

        @Override
        public List<Projectile> getProjectiles() {
            return new ArrayList<>(this.board.getProjectiles());
        }

        @Override
        public List<LawnMower> getLawnMowers() {
            return new ArrayList<>(this.board.getLawnMowers());
        }

        @Override
        public List<Tile> getTiles() {
            return new ArrayList<>(this.board.getTiles());
        }

        @Override
        public ChapterType getChapterType() {
            return this.chapterType;
        }

        @Override
        public LevelType getLevelType() {
            return LevelType.NORMAL;
        }

        @Override
        public int getCoinCount() {
            return this.coins;
        }

        @Override
        public int getGemCount() {
            return this.gems;
        }

        @Override
        public int getPlantFoodAmount() {
            return this.plantFood;
        }

        @Override
        public boolean isDebugModeEnabled() {
            return false;
        }

        @Override
        public boolean supportsCurrencyCheats() {
            return true;
        }

        @Override
        public void setDebugModeEnabled(boolean enabled) {
        }

        @Override
        public void cheatAddGold(int amount) {
            cheatAddCoins(amount);
        }

        @Override
        public void cheatAddCoins(int amount) {
            this.coins += Math.max(0, amount);
        }

        @Override
        public void cheatAddGems(int amount) {
            this.gems += Math.max(0, amount);
        }

        @Override
        public void cheatAddSun(int amount) {
            this.sun += Math.max(0, amount);
        }

        @Override
        public void cheatAddPlantFood() {
            this.plantFood = Math.min(3, this.plantFood + 1);
        }

        @Override
        public void save() {
        }

        @Override
        public int getMaximumWaterColumn() {
            return this.chapterType == ChapterType.BIG_WAVE_BEACH ? 4 : -1;
        }

        @Override
        public List<Position> getNecromancyCells() {
            if (this.chapterType != ChapterType.MEDIEVAL) {
                return Collections.emptyList();
            }
            return List.of(new Position(6, 2));
        }

        private List<PlantDefinition> safeDefinitions() {
            List<PlantDefinition> definitions = this.plantDefinitions.findAll();
            return definitions == null ? Collections.emptyList() : definitions;
        }

        private PlantDefinition findDefinition(String normalizedName) {
            for (PlantDefinition definition : safeDefinitions()) {
                if (definition != null && normalize(definition.getName()).equals(normalizedName)) {
                    return definition;
                }
            }
            return null;
        }

        private CollectionActionResult failure(String message, String plantName) {
            return CollectionActionResult.failure(
                    CollectionActionStatus.INVALID,
                    message,
                    plantName,
                    this.upgrades.getLevel(plantName),
                    this.upgrades.getSeedPackets(plantName),
                    this.coins
            );
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }
    }
}
