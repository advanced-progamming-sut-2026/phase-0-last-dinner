package network.izombie.server;

public interface IZombieMatchLifecycle {
    void onMatchClosed(String matchId, String plantUsername, String zombieUsername);
}
