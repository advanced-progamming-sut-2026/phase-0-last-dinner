package model.Greenhouse;

import lombok.Getter;
import lombok.NoArgsConstructor;
import model.mechanism.Position;

@NoArgsConstructor
@Getter
public class Pot {

    private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;

    private Position position;
    private boolean unlocked;
    private String plantName;
    private long plantedAtMillis;
    private long readyAtMillis;

    public Pot(Position position, boolean unlocked) {
        if (position == null) {
            throw new IllegalArgumentException(
                    "Pot position cannot be null."
            );
        }

        this.position = position;
        this.unlocked = unlocked;
    }

    public boolean plant(
            String plantName,
            long currentTimeMillis,
            long growthDurationMillis
    ) {
        if (!this.unlocked
                || !this.isEmpty()
                || plantName == null
                || plantName.trim().isEmpty()
                || growthDurationMillis <= 0) {

            return false;
        }

        this.plantName = plantName.trim();
        this.plantedAtMillis = currentTimeMillis;
        this.readyAtMillis =
                currentTimeMillis + growthDurationMillis;

        return true;
    }

    public boolean isEmpty() {
        return this.plantName == null
                || this.plantName.trim().isEmpty();
    }

    public boolean isReady() {
        return this.isReady(
                System.currentTimeMillis()
        );
    }

    public boolean isReady(long currentTimeMillis) {
        return !this.isEmpty()
                && currentTimeMillis >= this.readyAtMillis;
    }

    public long getRemainingGrowthMillis() {
        return this.getRemainingGrowthMillis(
                System.currentTimeMillis()
        );
    }

    public long getRemainingGrowthMillis(
            long currentTimeMillis
    ) {
        if (this.isEmpty() || this.isReady(currentTimeMillis)) {
            return 0;
        }

        return this.readyAtMillis - currentTimeMillis;
    }

    public int getRemainingGrowthHours(
            long currentTimeMillis
    ) {
        long remainingMillis =
                this.getRemainingGrowthMillis(
                        currentTimeMillis
                );

        if (remainingMillis <= 0) {
            return 0;
        }

        return (int) Math.ceil(
                (double) remainingMillis
                        / MILLIS_PER_HOUR
        );
    }

    public boolean accelerateGrowth(
            long currentTimeMillis
    ) {
        if (this.isEmpty()
                || this.isReady(currentTimeMillis)) {

            return false;
        }

        this.readyAtMillis = currentTimeMillis;
        return true;
    }

    public String harvest(long currentTimeMillis) {
        if (!this.isReady(currentTimeMillis)) {
            return null;
        }

        String harvestedPlantName = this.plantName;
        this.clear();

        return harvestedPlantName;
    }

    public void unlock() {
        this.unlocked = true;
    }

    private void clear() {
        this.plantName = null;
        this.plantedAtMillis = 0;
        this.readyAtMillis = 0;
    }
}