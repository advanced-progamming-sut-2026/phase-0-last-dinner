package network.izombie.client;

import network.izombie.protocol.IZombieServerEvent;

public interface IZombieClientListener {
    void onIZombieEvent(IZombieServerEvent event);
}
