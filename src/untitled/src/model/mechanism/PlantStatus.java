package model.mechanism;

import lombok.Getter;import lombok.Setter;import model.Plant;
@Getter
@Setter
public class PlantStatus {
    private Plant plant;
    private int sunCost;
    private boolean available;
    private long remainingCooldownTicks;

    public PlantStatus(Plant plant, boolean available, long remainingCooldownTicks) {
        this.plant = plant;
        this.sunCost = plant.getSunCost();
        this.available = available;
        this.remainingCooldownTicks = remainingCooldownTicks;
    }
    public double getRemainingSeconds() {
        return remainingCooldownTicks / 10.0;
    }
}
