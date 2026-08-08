package model.Greenhouse;

import lombok.Getter;
import model.mechanism.Position;

@Getter
public class GreenhousePotState {

    private final Position position;
    private final boolean unlocked;
    private final String plantName;
    private final boolean ready;
    private final int remainingGrowthHours;

    public GreenhousePotState(
            Position position,
            boolean unlocked,
            String plantName,
            boolean ready,
            int remainingGrowthHours
    ) {
        this.position = position;
        this.unlocked = unlocked;
        this.plantName = plantName;
        this.ready = ready;
        this.remainingGrowthHours =
                Math.max(0, remainingGrowthHours);
    }

    public boolean isEmpty() {
        return this.plantName == null
                || this.plantName.trim().isEmpty();
    }
}