package model.mechanism;

import model.Plant;

import java.util.HashMap;
import java.util.Map;

public class PlantCooldownManager implements Tickable {
    private Map<String, Long> availableAtTick;
    private boolean cooldownDisabled;
    private GameClock gameClock;

    public PlantCooldownManager(GameClock gameClock) {
        this.gameClock = gameClock;
        this.cooldownDisabled = false;
        this.availableAtTick = new HashMap<>();
    }

    @Override
    public void onTick() {
    }

    public boolean isAvailable(Plant plant) {
        if (plant == null || this.cooldownDisabled) {
            return true;
        }

        Long availableAt = this.availableAtTick.get(plant.getName());

        if (availableAt == null) {
            return true;
        }

        return this.gameClock == null || this.gameClock.getCurrentTick() >= availableAt;
    }

    public long getRemainingTicks(Plant plant) {
        if (plant == null || this.cooldownDisabled) {
            return 0;
        }

        Long availableAt = this.availableAtTick.get(plant.getName());

        if (availableAt == null || this.gameClock == null) {
            return 0;
        }

        return Math.max(0, availableAt - this.gameClock.getCurrentTick());
    }

    public void startCooldown(Plant plant) {
        if (plant == null || this.gameClock == null || this.cooldownDisabled) {
            return;
        }

        long availableAt = this.gameClock.getCurrentTick() + plant.getCooldownTicks();
        this.availableAtTick.put(plant.getName(), availableAt);
    }

    public void removeAllCooldowns() {
        this.cooldownDisabled = true;
        this.availableAtTick.clear();
    }

    public void resetCooldown(Plant plant) {
        if (plant != null && plant.getName() != null) {
            this.availableAtTick.remove(plant.getName());
        }
    }
}
