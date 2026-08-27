package network.izombie.client;

import network.izombie.protocol.IZombieClientMessage;

public interface IZombieClientGateway {
    void setListener(IZombieClientListener listener);

    void send(IZombieClientMessage message);

    boolean isConnected();
}
