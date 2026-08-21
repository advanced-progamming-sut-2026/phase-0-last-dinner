package college.java.project.graphics;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.level.LevelType;
import model.plant.PlantCategory;
import model.plant.PlantTag;
import model.plant.PlantUpgradeSpecialEffect;

import java.util.Locale;

/**
 * Reusable graphical core for gameplay. It keeps Phase 1 state authoritative
 * while supplying the mandatory Phase 2 HUD, pause, mission and result shell.
 */

@Getter
public final class GameplayWorldScene extends Group {
    private static final float EXPLOSION_SHAKE = 10f;
    private static final float GARGANTUAR_SHAKE = 7f;
    private final GameplaySeedBankDataSource seedDataSource;
    private final GameplayWorldDataSource worldDataSource;
    private final GameAssetManager assets;
    private final GameplayBackgroundLayer backgroundLayer;
    private final GameplayTerrainLayer terrainLayer;
    private final GameplayLevelRulesLayer levelRulesLayer;
    private final GameplayLawnMowerLayer mowerLayer;
    private final Group boardEntityLayer;
    private final GameplayPlantLayer plantLayer;
    private final GameplayZombieLayer zombieLayer;
    private final GameplayProjectileLayer projectileLayer;
    private final GameplaySunLayer sunLayer;
    private final GameplaySeedBank seedBank;
    private final GameplayConveyorBelt conveyorBelt;
    private final GameplayBoardInteractionLayer interactionLayer;
    private final GameplayInteractionHud interactionHud;
    private final GameplayWaveProgressBar waveProgressBar;
    private final GameplayRewardNotificationLayer rewardNotificationLayer;
    private final GameplayResourceStrip resourceStrip;
    private final GameplayPauseButton pauseButton;
    private final GameplayAlertLayer alertLayer;
    private final GameplayChapterEventLayer chapterEventLayer;
    private final GameplayMissionOverlay missionOverlay;
    private final GameplayPauseOverlay pauseOverlay;
    private final GameplayOutcomeOverlay outcomeOverlay;
    private boolean paused;
    private boolean outcomeShown;
    private Runnable restartAction;
    private Runnable saveAndExitAction;
    private Runnable exitAction;
    private Runnable retryAction;
    @Setter
    private Runnable outcomeAction;

    public GameplayWorldScene(
            GameplaySeedBankDataSource seedDataSource,
            GameplayWorldDataSource worldDataSource
    ) {
        if (seedDataSource == null || worldDataSource == null) {
            throw new IllegalArgumentException("Gameplay scene data sources are required");
        }
        this.seedDataSource = seedDataSource;
        this.worldDataSource = worldDataSource;
        this.assets = new GameAssetManager();
        setSize(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);
        setTouchable(Touchable.childrenOnly);

        this.backgroundLayer = new GameplayBackgroundLayer(worldDataSource, this.assets);
        this.backgroundLayer.setBounds(0f, 0f, getWidth(), getHeight());
        addActor(this.backgroundLayer);

        this.terrainLayer = new GameplayTerrainLayer(worldDataSource, this.assets);
        setLawnBounds(this.terrainLayer);
        addActor(this.terrainLayer);

        this.levelRulesLayer = new GameplayLevelRulesLayer(worldDataSource, this.assets);
        setLawnBounds(this.levelRulesLayer);
        addActor(this.levelRulesLayer);

        this.mowerLayer = new GameplayLawnMowerLayer(worldDataSource, this.assets);
        setLawnBounds(this.mowerLayer);
        addActor(this.mowerLayer);

        this.boardEntityLayer = new Group();
        this.boardEntityLayer.setTouchable(Touchable.disabled);
        setLawnBounds(this.boardEntityLayer);
        addActor(this.boardEntityLayer);

        this.plantLayer = new GameplayPlantLayer(seedDataSource, worldDataSource, this.assets);
        this.plantLayer.setRenderHost(this.boardEntityLayer);
        setLawnBounds(this.plantLayer);
        addActor(this.plantLayer);

        this.zombieLayer = new GameplayZombieLayer(worldDataSource, this.assets);
        this.zombieLayer.setRenderHost(this.boardEntityLayer);
        this.zombieLayer.setGargantuarImpactListener(() -> triggerScreenShake(GARGANTUAR_SHAKE));
        this.zombieLayer.setMagnetCatchListener(this.plantLayer::playMagnetCatch);
        this.plantLayer.setExplosionListener(() -> {
            triggerScreenShake(EXPLOSION_SHAKE);
            this.zombieLayer.markExplosionDeathWindow();
        });
        setLawnBounds(this.zombieLayer);
        addActor(this.zombieLayer);

        this.projectileLayer = new GameplayProjectileLayer(worldDataSource, this.assets);
        this.projectileLayer.setRenderHost(this.boardEntityLayer);
        this.projectileLayer.setSpawnListener(projectile -> {
            if (projectile != null) {
                this.plantLayer.playAttack(projectile);
            }
        });
        this.projectileLayer.setImpactListener(this.zombieLayer::noteProjectileImpact);
        this.projectileLayer.setLaunchPointProvider(this.plantLayer::getProjectileLaunchPoint
        );
        this.projectileLayer.setReleaseDelayProvider(this.plantLayer::getAttackReleaseDelay
        );
        setLawnBounds(this.projectileLayer);
        addActor(this.projectileLayer);

        this.seedBank = new GameplaySeedBank(seedDataSource, this.assets);
        layoutSeedBankAtScreenEdge();
        addActor(this.seedBank);

        this.conveyorBelt = new GameplayConveyorBelt(worldDataSource, this.assets);
        this.conveyorBelt.setBounds(20f, 902f, 1400f, 158f);
        addActor(this.conveyorBelt);

        this.interactionLayer = new GameplayBoardInteractionLayer(seedDataSource, this.seedBank, this.assets);
        setLawnBounds(this.interactionLayer);
        addActor(this.interactionLayer);

        this.sunLayer = new GameplaySunLayer(worldDataSource, this.assets);
        // Aim collected suns at the visual center of the full-size original
        // sun icon.  These offsets match the 70x71 768p sprite after the
        // gameplay-background scale is applied.
        this.sunLayer.setCollectionTarget(
                this.seedBank.getX() + 49f - GameplayWorldLayout.LAWN_X,
                this.seedBank.getY() + this.seedBank.getHeight() - 50f - GameplayWorldLayout.LAWN_Y
        );
        this.sunLayer.setSpawnListener(sun -> {
            if (sun != null && sun.getProducer() != null) {
                this.plantLayer.playSunProduction(sun.getProducer());
            }
        });
        setLawnBounds(this.sunLayer);
        addActor(this.sunLayer);

        this.interactionHud = new GameplayInteractionHud(
                seedDataSource,
                this.seedBank,
                this.interactionLayer,
                this.assets
        );
        this.interactionHud.setBounds(0f, 0f, getWidth(), getHeight());
        addActor(this.interactionHud);

        this.waveProgressBar = new GameplayWaveProgressBar(worldDataSource, this.assets);
        this.waveProgressBar.setBounds(596f, 1014f, 384f, 46.4f);
        addActor(this.waveProgressBar);

        this.resourceStrip = new GameplayResourceStrip(seedDataSource, this.assets);
        this.resourceStrip.setBounds(1430f, 1015f, 350f, 58f);
        addActor(this.resourceStrip);

        this.pauseButton = new GameplayPauseButton(this.assets, this::openPauseMenu);
        this.pauseButton.setBounds(1828f, 997f, 82f, 82f);
        addActor(this.pauseButton);

        this.rewardNotificationLayer = new GameplayRewardNotificationLayer(worldDataSource, this.assets);
        this.rewardNotificationLayer.setBounds(0f, 0f, getWidth(), getHeight());
        addActor(this.rewardNotificationLayer);

        this.alertLayer = new GameplayAlertLayer(worldDataSource);
        this.alertLayer.setBounds(0f, 0f, getWidth(), getHeight());
        addActor(this.alertLayer);

        this.chapterEventLayer = new GameplayChapterEventLayer(seedDataSource, worldDataSource);
        setLawnBounds(this.chapterEventLayer);
        addActor(this.chapterEventLayer);

        this.missionOverlay = new GameplayMissionOverlay();
        addActor(this.missionOverlay);

        this.pauseOverlay = new GameplayPauseOverlay(this.assets);
        this.pauseOverlay.setActions(this::resumeGame, this::runRestart, this::runSaveAndExit);
        addActor(this.pauseOverlay);

        this.outcomeOverlay = new GameplayOutcomeOverlay(
                this.assets,
                this.worldDataSource.getChapterType()
        );
        this.outcomeOverlay.setActions(this::runExit, this::runRetry);
        addActor(this.outcomeOverlay);

        wireSeedBank();
        updatePlantSelectorMode();
    }

    @Override
    public void act(float delta) {
        if (isWorldFrozen()) {
            super.act(0f);
            GameplayBoardDepthOrder.sort(this.boardEntityLayer);
            updatePlantSelectorMode();
            return;
        }
        super.act(delta);
        this.seedBank.refresh();
        GameplayBoardDepthOrder.sort(this.boardEntityLayer);
        updatePlantSelectorMode();
        updateOutcome();
    }

    public boolean shouldAdvanceModel() {
        return !isWorldFrozen();
    }

    public void openPauseMenu() {
        if (this.outcomeShown || this.missionOverlay.isVisible()) {
            return;
        }
        this.paused = true;
        this.interactionLayer.clearMode();
        this.seedBank.clearSelectionSilently();
        this.interactionHud.refresh();
        this.pauseOverlay.setVisible(true);
    }
    public void showInitialMissionIfNeeded() {
        this.showInitialMissionIfRequired();
    }

    public void resumeGame() {
        this.paused = false;
        this.pauseOverlay.setVisible(false);
    }

    public void setSessionActions(
            Runnable restart,
            Runnable saveAndExit,
            Runnable exit,
            Runnable retry
    ) {
        this.restartAction = restart;
        this.saveAndExitAction = saveAndExit;
        this.exitAction = exit;
        this.retryAction = retry;
    }

    public void setDebugModeEnabled(boolean enabled) {
        this.seedDataSource.setDebugModeEnabled(enabled);
        refreshHud();
    }

    public void refreshHud() {
        this.seedBank.refresh();
        this.interactionHud.refresh();
        this.resourceStrip.refresh();
    }

    public void dispose() {
        this.assets.dispose();
    }

    private boolean isWorldFrozen() {
        return this.paused || this.missionOverlay.isVisible() || this.outcomeShown;
    }

    private void showInitialMissionIfRequired() {
        if (!this.worldDataSource.shouldShowMissionAtStart()) {
            return;
        }
        this.missionOverlay.showMission(
                this.worldDataSource.getMissionTitle(),
                this.worldDataSource.getMissionDescription(),
                () -> this.missionOverlay.setVisible(false)
        );
    }

    private void updateOutcome() {
        if (this.outcomeShown) {
            return;
        }
        boolean lost = this.worldDataSource.isLevelLost();
        boolean won = !lost && this.worldDataSource.isLevelWon();
        if (!lost && !won) {
            return;
        }
        this.outcomeShown = true;
        this.outcomeOverlay.showResult(lost);
        if (this.outcomeAction != null) {
            this.outcomeAction.run();
        }
    }

    private void runRestart() {
        if (this.restartAction != null) {
            this.restartAction.run();
        }
    }

    private void runSaveAndExit() {
        if (this.saveAndExitAction != null) {
            this.saveAndExitAction.run();
        }
    }

    private void runExit() {
        if (this.exitAction != null) {
            this.exitAction.run();
        }
    }

    private void runRetry() {
        if (this.retryAction != null) {
            this.retryAction.run();
        }
    }

    private void wireSeedBank() {
        this.conveyorBelt.setPacketSelectionListener(this.seedBank::selectExternalPlant);
        this.interactionLayer.setActionListener(new GameplayBoardInteractionLayer.InteractionActionListener() {
            @Override
            public void onActionApplied(GameplayInteractionMode mode, int column, int row) {
                if (mode == GameplayInteractionMode.PLANT) {
                    plantLayer.playIntroAt(column, row);
                    playAutomaticPlantFoodFeedback(column, row);
                } else if (mode == GameplayInteractionMode.SHOVEL) {
                    plantLayer.suppressRemovalEffectAt(column, row);
                    seedBank.showInteractionStatus("Plant removed.");
                } else if (mode == GameplayInteractionMode.PLANT_FOOD) {
                    plantLayer.playPlantFoodAt(column, row);
                    seedBank.showInteractionStatus("Plant Food used.");
                }
                interactionHud.refresh();
            }

            @Override
            public void onActionRejected(GameplayInteractionMode mode, int column, int row) {
                if (mode == GameplayInteractionMode.PLANT) {
                    seedBank.showInteractionStatus(seedDataSource.getPlantingFailureMessage(
                            interactionLayer.getSelectedPlantName(),
                            column,
                            row
                    ));
                } else if (mode == GameplayInteractionMode.SHOVEL) {
                    seedBank.showInteractionStatus("There is no plant to remove there.");
                } else if (mode == GameplayInteractionMode.PLANT_FOOD) {
                    seedBank.showInteractionStatus("That plant cannot receive Plant Food.");
                }
            }
        });
        this.seedBank.setPlantSelectionListener(new GameplaySeedBank.PlantSelectionListener() {
            @Override
            public void onPlantSelected(String plantName) {
                interactionLayer.selectPlant(plantName);
                interactionHud.refresh();
            }

            @Override
            public void onImitaterCopyTargetSelected(String plantName) {
                interactionLayer.updatePlantPreview(plantName);
            }

            @Override
            public void onPlantSelectionCleared() {
                interactionLayer.clearMode();
                interactionHud.refresh();
            }
        });
    }


    private void playAutomaticPlantFoodFeedback(int column, int row) {
        Plant placed = this.seedDataSource.getTopPlantAt(column, row);
        if (placed == null) {
            return;
        }
        boolean boosted = this.interactionLayer.wasLastAppliedPlantBoosted();
        boolean upgradeTriggered = placed.hasUpgradeSpecialEffect(
                PlantUpgradeSpecialEffect.PLANT_FOOD_ON_PLANTING
        );
        if (placed.canReceivePlantFood() && (boosted || upgradeTriggered)) {
            this.plantLayer.playPlantFoodAt(column, row);
        }
        playMintFamilyFeedback(placed);
    }

    private void playMintFamilyFeedback(Plant mint) {
        if (mint == null || !normalizePlantName(mint.getName()).contains("mint")) {
            return;
        }
        for (Plant plant : this.seedDataSource.getPlantsOnBoard()) {
            if (plant == null || plant == mint || plant.getPosition() == null
                    || !plant.canReceivePlantFood() || !matchesMintFamily(mint.getName(), plant)) {
                continue;
            }
            this.plantLayer.playPlantFoodAt(plant.getPosition().getX(), plant.getPosition().getY());
        }
    }

    private boolean matchesMintFamily(String mintName, Plant plant) {
        String name = normalizePlantName(mintName);
        if (name.contains("appease")) {
            return plant.getTags() != null && plant.getTags().contains(PlantTag.PEA);
        }
        if (name.contains("enchant")) {
            return plant.getTags() != null && plant.getTags().contains(PlantTag.MAGIC);
        }
        PlantCategory category = mintFamilyCategory(name);
        return category != null && plant.getCategories() != null
                && plant.getCategories().contains(category);
    }

    private PlantCategory mintFamilyCategory(String name) {
        if (name.contains("enlighten")) return PlantCategory.SUN_PRODUCER;
        if (name.contains("arma")) return PlantCategory.LOBBER;
        if (name.contains("bombard")) return PlantCategory.EXPLOSIVE;
        if (name.contains("enforce")) return PlantCategory.MELEE_ATTACKER;
        if (name.contains("reinforce")) return PlantCategory.DEFENDER;
        if (name.contains("pierce")) return PlantCategory.STRIKE_THROUGH;
        if (name.contains("cattail")) return PlantCategory.HOMING;
        return null;
    }

    private String normalizePlantName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private void triggerScreenShake(float strength) {
        shakeActor(this.backgroundLayer, strength * 0.35f);
        shakeActor(this.terrainLayer, strength);
        shakeActor(this.levelRulesLayer, strength);
        shakeActor(this.mowerLayer, strength);
        shakeActor(this.boardEntityLayer, strength);
        shakeActor(this.sunLayer, strength);
        shakeActor(this.chapterEventLayer, strength);
    }

    private void shakeActor(Actor actor, float strength) {
        if (actor == null || strength <= 0f) {
            return;
        }
        actor.addAction(Actions.sequence(
                Actions.moveBy(strength, strength * 0.25f, 0.035f),
                Actions.moveBy(-strength * 1.75f, -strength * 0.75f, 0.045f),
                Actions.moveBy(strength * 1.25f, strength, 0.050f),
                Actions.moveBy(-strength * 0.50f, -strength * 0.50f, 0.055f)
        ));
    }

    private void updatePlantSelectorMode() {
        boolean conveyor = this.worldDataSource.getLevelType() == LevelType.CONVEYOR_BELT;
        this.seedBank.setVisible(!conveyor);
        this.conveyorBelt.setVisible(conveyor);
    }

    /**
     * Keeps the gameplay seed bank in HUD/screen space rather than registering
     * it to the lawn rectangle. Ratios are evaluated against the fixed virtual
     * stage, so FitViewport resizing preserves the same PvZ2-like left/top anchor.
     */
    private void layoutSeedBankAtScreenEdge() {
        float leftMargin = getWidth() * 0.0022f;
        float bottomMargin = getHeight() * 0.010f;
        float topMargin = getHeight() * 0.0075f;
        float bankWidth = getWidth() * 0.100f;
        float bankHeight = getHeight() - bottomMargin - topMargin;
        this.seedBank.setBounds(
                leftMargin,
                bottomMargin,
                bankWidth,
                bankHeight
        );
    }

    private void setLawnBounds(Group actor) {
        actor.setBounds(
                GameplayWorldLayout.LAWN_X,
                GameplayWorldLayout.LAWN_Y,
                GameplayWorldLayout.LAWN_WIDTH,
                GameplayWorldLayout.LAWN_HEIGHT
        );
    }

}
