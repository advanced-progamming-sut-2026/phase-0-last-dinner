package controller;

import model.Plant;
import model.level.ConveyorBeltLevel;
import model.mechanism.*;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.plant.PlantDefinition;
import view.MidGameViewObserver;
import view.MidGameCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

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
        List<Plant> selectedPlants = new ArrayList<>();
        for (PlantDefinition definition : this.game.getPlantDefinitions().findAll()) {
            if (definition == null || !this.isSelected(definition.getName())) {
                continue;
            }
            selectedPlants.add(this.game.getPlantFactory().create(definition));
        }
        return game.getGameStatusService().getPlantsStatus(selectedPlants);
    }

    @Override
    public List<String> onShowConveyorPlantsRequested() {
        List<String> plantNames = new ArrayList<>();
        if (!(this.game.getActiveLevel() instanceof ConveyorBeltLevel)) {
            return plantNames;
        }

        for (Plant plant : ((ConveyorBeltLevel) this.game.getActiveLevel()).getConveyorPlants()) {
            if (plant != null && plant.getName() != null) {
                plantNames.add(plant.getName());
            }
        }

        return plantNames;
    }

    @Override
    public Tile onShowTileStatusRequested(int x, int y) {
        return game.getGameStatusService().getTileStatus(new Position(x, y));
    }

    @Override
    public boolean onCollectSunRequested(int x, int y) {
        int collected = game.collectSun(new Position(x, y));
        return collected > 0;
    }

    @Override
    public boolean onPlantPlantRequested(String type, int x, int y) {
        return game.plant(this.cleanType(type), new Position(x, y));
    }

    @Override
    public boolean onPlantImitaterRequested(String type, int x, int y) {
        return game.plantImitater(this.cleanType(type), new Position(x, y));
    }

    @Override
    public boolean onPluckPlantRequested(int x, int y) {
        return game.pluckPlant(new Position(x, y));
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
        return this.spawnZombieCheat(type, x + 1, y + 1) != null;
    }

    public Zombie spawnZombieCheat(String type, int x, int y) {
        if (x < 1 || x > 9 || y < 1 || y > 5) {
            return null;
        }

        ZombieDefinition definition = this.findZombieDefinition(this.cleanType(type));
        if (definition == null) {
            return null;
        }

        return this.game.spawnZombie(definition.getAlias(), new Position(x - 1, y - 1));
    }

    public String executeCommand(String input) {
        Matcher zombiesInfo = MidGameCommand.ZOMBIES_INFO.getMatcher(input);
        if (zombiesInfo != null) {
            return this.formatZombiesInfo(this.onZombiesInfoRequested());
        }

        Matcher spawnZombie = MidGameCommand.SPAWN_ZOMBIE.getMatcher(input);
        if (spawnZombie != null) {
            int x = Integer.parseInt(spawnZombie.group("x"));
            int y = Integer.parseInt(spawnZombie.group("y"));
            Zombie zombie = this.spawnZombieCheat(spawnZombie.group("type"), x, y);
            return zombie == null
                    ? "Could not spawn zombie."
                    : "Zombie spawned at " + x + ", " + y + ".";
        }

        return "Invalid mid-game command.";
    }

    @Override
    public boolean isGameOver() {
        return !game.getEngine().isGameRunning();
    }

    public int getCurrentWaveNumber() {
        Wave wave = game.getWaveManager().getCurrentWave();
        return wave == null ? 0 : wave.getNumber();
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

    private String formatZombiesInfo(List<ZombieStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return "No zombies on the board.";
        }

        StringBuilder result = new StringBuilder();
        for (ZombieStatus status : statuses) {
            if (result.length() > 0) {
                result.append(System.lineSeparator());
            }

            result.append(status.getZombieType()).append(":").append(System.lineSeparator());
            result.append("  position: ")
                    .append(this.formatNumber(status.getExactX() + 1))
                    .append(", ").append(status.getPosition().getY() + 1)
                    .append(System.lineSeparator());
            result.append("  health: ").append(status.getHealth()).append(System.lineSeparator());
            result.append("  armor:").append(System.lineSeparator());
            for (Map.Entry<String, Integer> armor : status.getArmorHealth().entrySet()) {
                result.append("    ").append(armor.getKey()).append(": ")
                        .append(armor.getValue()).append(System.lineSeparator());
            }
            result.append("  effects:");
            for (Map.Entry<String, Long> effect : status.getEffectRemainingTicks().entrySet()) {
                result.append(System.lineSeparator()).append("    ").append(effect.getKey());
                if (effect.getValue() != null) {
                    result.append(": ")
                            .append(this.formatNumber(effect.getValue() / 10.0))
                            .append("s");
                }
            }
        }
        return result.toString();
    }

    private String cleanType(String type) {
        if (type == null) {
            return null;
        }
        String clean = type.trim();
        if (clean.length() >= 2 && clean.startsWith("\"") && clean.endsWith("\"")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        return clean;
    }

    private boolean isSelected(String plantName) {
        return this.game.getSelectedPlantNames() == null
                || this.game.getSelectedPlantNames().contains(
                        plantName == null ? "" : plantName.trim().toLowerCase(java.util.Locale.ROOT)
                );
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
    @Override
    public int onShowPlantFoodCountRequested() {
        return game.getPlantFoodSystem().getPlantFoodAmount();
    }
}
