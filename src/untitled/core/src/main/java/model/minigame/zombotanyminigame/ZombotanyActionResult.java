package model.minigame.zombotanyminigame;

import lombok.Getter;

@Getter
public class ZombotanyActionResult {

    private final ZombotanyActionStatus status;
    private final String message;

    private final int stageNumber;
    private final int currentWave;
    private final int waveCount;

    private final int sunAmount;
    private final int plantFoodAmount;
    private final int aliveZombieCount;

    private ZombotanyActionResult(
            ZombotanyActionStatus status,
            String message,
            int stageNumber,
            int currentWave,
            int waveCount,
            int sunAmount,
            int plantFoodAmount,
            int aliveZombieCount
    ) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Zombotany status cannot be null."
            );
        }

        this.status = status;
        this.message = message == null ? "" : message;

        this.stageNumber = Math.max(1, stageNumber);
        this.currentWave = Math.max(0, currentWave);
        this.waveCount = Math.max(0, waveCount);

        this.sunAmount = Math.max(0, sunAmount);
        this.plantFoodAmount =
                Math.max(0, plantFoodAmount);

        this.aliveZombieCount =
                Math.max(0, aliveZombieCount);
    }

    public static ZombotanyActionResult of(
            ZombotanyActionStatus status,
            String message,
            int stageNumber,
            int currentWave,
            int waveCount,
            int sunAmount,
            int plantFoodAmount,
            int aliveZombieCount
    ) {
        return new ZombotanyActionResult(
                status,
                message,
                stageNumber,
                currentWave,
                waveCount,
                sunAmount,
                plantFoodAmount,
                aliveZombieCount
        );
    }

    public boolean isSuccessful() {
        return status == ZombotanyActionStatus.SUCCESS
                || status == ZombotanyActionStatus.STAGE_WON
                || status == ZombotanyActionStatus.GAME_WON;
    }

    public boolean isTerminal() {
        return status == ZombotanyActionStatus.STAGE_WON
                || status == ZombotanyActionStatus.GAME_WON
                || status == ZombotanyActionStatus.GAME_LOST;
    }
}