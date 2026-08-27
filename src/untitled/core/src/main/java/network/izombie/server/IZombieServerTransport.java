package network.izombie.server;

import network.izombie.protocol.IZombieServerEvent;

public interface IZombieServerTransport {
    boolean isUserOnline(String username);

    void sendToUser(String username, IZombieServerEvent event);
}
