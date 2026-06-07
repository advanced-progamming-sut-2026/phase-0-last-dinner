package model.mechanism;

import model.Plant;

import java.util.Map;

public class PlantCooldownManager implements Tickable {
    private Map<Plant, Long> availableAtTick;
    private boolean cooldownDisabled;

    @Override
    public void onTick() {
    }

    public boolean isAvailable(Plant plant) {
        return false;
    }

    public long getRemainingTicks(Plant plant) {
        return 0;
    }

    public void startCooldown(Plant plant) {
    }

    public void removeAllCooldowns() {
    }
}
