package model.minigame.izombieminigame.multiplayer;

public record IZombieMatchActionResult(boolean successful, String message) {

    public static IZombieMatchActionResult success() {
        return new IZombieMatchActionResult(true, null);
    }

    public static IZombieMatchActionResult failure(String message) {
        return new IZombieMatchActionResult(false, message);
    }
}
