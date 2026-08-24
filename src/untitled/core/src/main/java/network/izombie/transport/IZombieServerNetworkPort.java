package network.izombie.transport;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface IZombieServerNetworkPort {

    boolean isUserOnline(String username);

    void sendIZombiePayload(String username, String payload);

    void setIZombieMessageListener(BiConsumer<String, String> listener);

    void setUserDisconnectedListener(Consumer<String> listener);
}
