package controller;

import model.Plant;
import model.mechanism.*;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import view.MidGameViewObserver;

import java.util.ArrayList;
import java.util.List;

public class MidGameController implements MidGameViewObserver {
    private final PlantZombieGame game;

    public MidGameController(PlantZombieGame game) {
        this.game = game;
    }

    @Override
    public void onAdvanceTimeRequested(int ticks) {
        if (ticks <= 0) return;
        game.advanceTime(ticks);
    }

    @Override
    public Board onShowMapRequested() {
        return game.getBoard();
    }

    @Override
    public int onShowSunAmountRequested() {
        return game.getSunSystem().getSunAmount();
    }

    @Override
    public List<PlantStatus> onShowPlantsStatusRequested() {
        List<Plant> allPlants = game.getBoard().getAllPlants();
        return game.getGameStatusService().getPlantsStatus(allPlants);
    }

    @Override
    public Tile onShowTileStatusRequested(int x, int y) {
        return game.getGameStatusService().getTileStatus(new Position(x, y));
    }

    @Override
    public boolean onCollectSunRequested(int x, int y) {
        int collected = game.getSunSystem().collectSun(new Position(x, y));
        return collected > 0;
    }

    @Override
    public boolean onPlantPlantRequested(String type, int x, int y) {
        return game.plant(type, new Position(x, y));
    }

    @Override
    public boolean onPluckPlantRequested(int x, int y) {
        Position position = new Position(x, y);
        Tile tile = game.getBoard().getTile(position);
        if (tile == null || tile.getPlants().isEmpty()) return false;
        game.getPlantingSystem().pluck(position);
        return true;
    }

    @Override
    public boolean onFeedPlantRequested(int x, int y) {
        return game.getPlantFoodSystem().feedPlant(new Position(x, y));
    }

    @Override
    public void onCheatAddSunsRequested(int count) {
        game.getSunSystem().addSun(count);
    }

    @Override
    public void onCheatRemoveCooldownRequested() {
        game.getPlantingSystem().removeAllCooldowns();
    }

    @Override
    public void onCheatAddPlantFoodRequested() {
        game.getPlantFoodSystem().cheatAddPlantFood();
    }

    @Override
    public void onReleaseTheNukeRequested() {
        for (Zombie zombie : game.getBoard().getAllZombies()) {
            if (zombie != null && !zombie.isDead()) {
                game.getCombatSystem().killZombieIgnoringAllegiance(zombie);
            }
        }
    }

    @Override
    public List<ZombieStatus> onZombiesInfoRequested() {
        return game.getGameStatusService().getZombiesStatus();
    }

    @Override
    public boolean onSpawnZombieRequested(String type, int x, int y) {
        if (x < 0 || x >= 9 || y < 0 || y >= 5) return false;
        Zombie zombie = game.getZombieSpawner().spawnZombie(
                findZombieDefinition(type),
                null,
                y
        );
        return zombie != null;
    }

    @Override
    public boolean isGameOver() {
        return !game.getEngine().isGameRunning();
    }

    public int getCurrentWaveNumber() {
        Wave wave = game.getWaveManager().getCurrentWave();
        return wave == null ? 0 : wave.getNumber();
    }

    public int getPlantFoodCount() {
        return game.getPlantFoodSystem().getPlantFoodAmount();
    }

    private ZombieDefinition findZombieDefinition(String type) {
        ZombieDefinitionRepository repo = game.getZombieDefinitions();
        if (repo == null || type == null) return null;
        ZombieDefinition def = repo.findByAlias(type);
        if (def != null) return def;
        for (ZombieDefinition d : repo.findAll()) {
            if (d != null && type.equalsIgnoreCase(d.getDisplayName())) return d;
        }
        return null;
    }
    @Override
    public int onShowPlantFoodCountRequested() {
        return game.getPlantFoodSystem().getPlantFoodAmount();
    }
}