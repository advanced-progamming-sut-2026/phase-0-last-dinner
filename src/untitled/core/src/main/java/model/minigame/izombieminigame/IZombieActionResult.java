package model.minigame.izombieminigame;

import model.mechanism.Position;
import model.zombie.ZombieDefinition;

public class IZombieActionResult {

    private final IZombieActionStatus status;
    private final String message;

    private final ZombieDefinition zombieDefinition;
    private final Position position;

    private final int sunSpent;
    private final int remainingSun;

    private IZombieActionResult(
            IZombieActionStatus status,
            String message,
            ZombieDefinition zombieDefinition,
            Position position,
            int sunSpent,
            int remainingSun
    ) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null.");
        }

        this.status = status;
        this.message = message == null ? "" : message;
        this.zombieDefinition = zombieDefinition;
        this.position = position;
        this.sunSpent = Math.max(0, sunSpent);
        this.remainingSun = Math.max(0, remainingSun);
    }

    public static IZombieActionResult success(
            String message,
            int remainingSun
    ) {
        return new IZombieActionResult(
                IZombieActionStatus.SUCCESS,
                message,
                null,
                null,
                0,
                remainingSun
        );
    }

    public static IZombieActionResult placementSuccess(
            String message,
            ZombieDefinition zombieDefinition,
            Position position,
            int sunSpent,
            int remainingSun
    ) {
        return new IZombieActionResult(
                IZombieActionStatus.SUCCESS,
                message,
                zombieDefinition,
                position,
                sunSpent,
                remainingSun
        );
    }

    public static IZombieActionResult failure(
            IZombieActionStatus status,
            String message,
            int remainingSun
    ) {
        if (status == IZombieActionStatus.SUCCESS
                || status == IZombieActionStatus.STAGE_WON
                || status == IZombieActionStatus.GAME_WON) {
            throw new IllegalArgumentException(
                    "A successful status cannot be used as a failure."
            );
        }

        return new IZombieActionResult(
                status,
                message,
                null,
                null,
                0,
                remainingSun
        );
    }

    public static IZombieActionResult stageWon(
            String message,
            int remainingSun
    ) {
        return new IZombieActionResult(
                IZombieActionStatus.STAGE_WON,
                message,
                null,
                null,
                0,
                remainingSun
        );
    }

    public static IZombieActionResult gameWon(
            String message,
            int remainingSun
    ) {
        return new IZombieActionResult(
                IZombieActionStatus.GAME_WON,
                message,
                null,
                null,
                0,
                remainingSun
        );
    }

    public static IZombieActionResult gameLost(
            String message,
            int remainingSun
    ) {
        return new IZombieActionResult(
                IZombieActionStatus.GAME_LOST,
                message,
                null,
                null,
                0,
                remainingSun
        );
    }

    public boolean isSuccessful() {
        return status == IZombieActionStatus.SUCCESS
                || status == IZombieActionStatus.STAGE_WON
                || status == IZombieActionStatus.GAME_WON;
    }

    public boolean isTerminal() {
        return status == IZombieActionStatus.GAME_WON
                || status == IZombieActionStatus.GAME_LOST;
    }

    public boolean hasPlacementInformation() {
        return zombieDefinition != null && position != null;
    }

    public IZombieActionStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public ZombieDefinition getZombieDefinition() {
        return zombieDefinition;
    }

    public Position getPosition() {
        return position;
    }

    public int getSunSpent() {
        return sunSpent;
    }

    public int getRemainingSun() {
        return remainingSun;
    }
}