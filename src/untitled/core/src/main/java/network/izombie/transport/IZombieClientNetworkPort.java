package network.izombie.transport;

import java.util.function.Consumer;

public interface IZombieClientNetworkPort {

    boolean isConnected();

    void sendIZombiePayload(String payload);

    void setIZombiePayloadListener(Consumer<String> listener);
}
