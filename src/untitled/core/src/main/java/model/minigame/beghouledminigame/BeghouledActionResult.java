package model.minigame.beghouledminigame;

import lombok.Getter;

@Getter
public class BeghouledActionResult {

    private final BeghouledActionStatus status;
    private final String message;

    private final int stageNumber;
    private final int sunAmount;
    private final int completedMatchCount;
    private final int targetMatchCount;

    private BeghouledActionResult(
            BeghouledActionStatus status,
            String message,
            int stageNumber,
            int sunAmount,
            int completedMatchCount,
            int targetMatchCount
    ) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Beghouled action status cannot be null."
            );
        }

        this.status = status;
        this.message = message == null ? "" : message;
        this.stageNumber = Math.max(1, stageNumber);
        this.sunAmount = Math.max(0, sunAmount);
        this.completedMatchCount =
                Math.max(0, completedMatchCount);
        this.targetMatchCount =
                Math.max(0, targetMatchCount);
    }

    public static BeghouledActionResult of(
            BeghouledActionStatus status,
            String message,
            int stageNumber,
            int sunAmount,
            int completedMatchCount,
            int targetMatchCount
    ) {
        return new BeghouledActionResult(
                status,
                message,
                stageNumber,
                sunAmount,
                completedMatchCount,
                targetMatchCount
        );
    }

    public boolean isSuccessful() {
        return status == BeghouledActionStatus.SUCCESS
                || status == BeghouledActionStatus.STAGE_WON
                || status == BeghouledActionStatus.GAME_WON;
    }

    public boolean isTerminal() {
        return status == BeghouledActionStatus.STAGE_WON
                || status == BeghouledActionStatus.GAME_WON
                || status == BeghouledActionStatus.GAME_LOST;
    }
}