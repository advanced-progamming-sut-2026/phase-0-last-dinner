package network.izombie.client;

public record IZombieClientCommandResult(boolean sent, String message) {

    public static IZombieClientCommandResult success() {
        return new IZombieClientCommandResult(true, null);
    }

    public static IZombieClientCommandResult failure(String message) {
        return new IZombieClientCommandResult(false, message);
    }
}
