package model.mechanism;

import model.Plant;
import model.zombie.ArmorType;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;
import model.zombie.ZombieCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GameStatusService {
    private Board board;
    private WaveManager waveManager;
    private SunSystem sunSystem;
    private PlantFoodSystem plantFoodSystem;
    private PlantCooldownManager cooldownManager;

    public GameStatusService() {
    }

    public GameStatusService(
            Board board,
            WaveManager waveManager,
            SunSystem sunSystem,
            PlantFoodSystem plantFoodSystem,
            PlantCooldownManager cooldownManager
    ) {
        this.board = board;
        this.waveManager = waveManager;
        this.sunSystem = sunSystem;
        this.plantFoodSystem = plantFoodSystem;
        this.cooldownManager = cooldownManager;
    }

    public int getCurrentWaveNumber() {
        Wave currentWave = this.waveManager == null ? null : this.waveManager.getCurrentWave();
        return currentWave == null ? 0 : currentWave.getNumber();
    }

    public int getSunAmount() {
        return this.sunSystem == null ? 0 : this.sunSystem.getSunAmount();
    }

    public int getPlantFoodAmount() {
        return this.plantFoodSystem == null ? 0 : this.plantFoodSystem.getPlantFoodAmount();
    }

    public List<PlantStatus> getPlantsStatus(List<Plant> selectedPlants) {
        List<PlantStatus> statuses = new ArrayList<>();

        if (selectedPlants == null) {
            return statuses;
        }

        for (Plant plant : selectedPlants) {
            if (plant == null) {
                continue;
            }

            boolean cooldownFinished = this.cooldownManager == null
                    || this.cooldownManager.isAvailable(plant);
            boolean affordable = this.sunSystem == null
                    || this.sunSystem.getSunAmount() >= plant.getSunCost();
            boolean available = cooldownFinished && affordable;
            long remainingTicks = this.cooldownManager == null
                    ? 0
                    : this.cooldownManager.getRemainingTicks(plant);
            statuses.add(new PlantStatus(plant, available, remainingTicks));
        }

        return statuses;
    }

    public List<ZombieStatus> getZombiesStatus() {
        List<ZombieStatus> statuses = new ArrayList<>();

        if (this.board == null) {
            return statuses;
        }

        for (Zombie zombie : this.board.getAllZombies()) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }

            ZombieStatus status = new ZombieStatus(
                    this.getZombieTypeName(zombie),
                    zombie.getPosition(),
                    zombie.getExactX(),
                    zombie.getHealth()
            );
            this.addArmorStatus(zombie, status);
            this.addEffectStatus(zombie, status);
            statuses.add(status);
        }

        return statuses;
    }

    public Tile getTileStatus(Position position) {
        if (position == null || this.board == null) {
            return null;
        }

        return this.board.getTile(position);
    }

    private String getZombieTypeName(Zombie zombie) {
        if (zombie.getDefinition() == null) {
            return "Zombie";
        }

        String displayName = zombie.getDefinition().getDisplayName();

        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.trim();
        }

        String alias = zombie.getDefinition().getAlias();
        return alias == null || alias.trim().isEmpty() ? "Zombie" : alias.trim();
    }

    private void addArmorStatus(Zombie zombie, ZombieStatus status) {
        if (zombie.getArmors() == null) {
            return;
        }

        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor == null || armor.isDropped() || armor.isDestroyed()
                    || armor.getDefinition() == null) {
                continue;
            }

            ArmorType armorType = armor.getDefinition().getType();
            String armorName = armorType == null
                    ? armor.getDefinition().getAlias()
                    : this.toLowerCamelCase(armorType.name());
            status.addArmor(armorName, armor.getCurrentHealth());
        }
    }

    private void addEffectStatus(Zombie zombie, ZombieStatus status) {
        if (zombie.getConditions() == null) {
            return;
        }

        Map<ZombieCondition, Long> remainingTicks = zombie.getConditionRemainingTicks();

        for (ZombieCondition condition : zombie.getConditions()) {
            if (condition == null) {
                continue;
            }

            Long remaining = remainingTicks == null ? null : remainingTicks.get(condition);
            status.addEffect(condition.name().toLowerCase(Locale.ROOT), remaining);
        }
    }

    private String toLowerCamelCase(String enumName) {
        if (enumName == null || enumName.isEmpty()) {
            return "armor";
        }

        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder(words[0]);

        for (int index = 1; index < words.length; index++) {
            if (!words[index].isEmpty()) {
                result.append(Character.toUpperCase(words[index].charAt(0)))
                        .append(words[index].substring(1));
            }
        }

        return result.toString();
    }
}
