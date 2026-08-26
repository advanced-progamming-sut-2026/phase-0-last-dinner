package network.izombie.server;

public interface IZombieMatchFactory {
    IZombieMatchSession create(String matchId, String plantUsername, String zombieUsername,
                               int stageNumber, IZombieServerTransport transport, IZombieMatchLifecycle lifecycle);
}
