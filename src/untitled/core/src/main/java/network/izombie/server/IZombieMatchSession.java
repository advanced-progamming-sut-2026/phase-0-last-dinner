package network.izombie.server;

import network.izombie.protocol.IZombieClientMessage;
import network.izombie.protocol.IZombieMatchSnapshot;
import network.izombie.protocol.IZombieRole;

public interface IZombieMatchSession {
    String getMatchId();

    IZombieRole getRole(String username);

    IZombieMatchSnapshot getSnapshot();

    void handleMessage(String authenticatedUsername, IZombieClientMessage message);

    void handleDisconnect(String username);
}
