package network.izombie.local;

import network.izombie.client.IZombieClientGateway;
import network.izombie.client.IZombieClientListener;
import network.izombie.protocol.IZombieClientMessage;
import network.izombie.protocol.IZombieServerEvent;
import network.izombie.server.IZombieMultiplayerService;
import network.izombie.server.IZombieServerTransport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalIZombieGatewayHub implements IZombieServerTransport {

    private final Map<String, LocalGateway> gateways = new ConcurrentHashMap<>();

    private volatile IZombieMultiplayerService service;

    public void attachService(IZombieMultiplayerService service) {
        if (service == null) {
            throw new IllegalArgumentException("IZombie multiplayer service is required.");
        }

        if (this.service != null) {
            throw new IllegalStateException("IZombie multiplayer service is already attached.");
        }

        this.service = service;
    }

    public IZombieClientGateway connect(String authenticatedUsername) {
        ensureServiceIsAttached();

        String username = clean(authenticatedUsername);

        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Authenticated username is required.");
        }

        LocalGateway gateway = new LocalGateway(username);

        LocalGateway previous = this.gateways.putIfAbsent(username, gateway);

        if (previous != null) {
            throw new IllegalStateException("The user is already connected.");
        }

        return gateway;
    }

    public void disconnect(String username) {
        String cleanedUsername = clean(username);

        if (cleanedUsername == null) {
            return;
        }

        LocalGateway removed = this.gateways.remove(cleanedUsername);

        if (removed != null && this.service != null) {
            this.service.handleDisconnect(cleanedUsername);
        }
    }

    @Override
    public boolean isUserOnline(String username) {
        String cleanedUsername = clean(username);

        return cleanedUsername != null && this.gateways.containsKey(cleanedUsername);
    }

    @Override
    public void sendToUser(String username, IZombieServerEvent event) {
        String cleanedUsername = clean(username);

        if (cleanedUsername == null || event == null)
            return;

        LocalGateway gateway = this.gateways.get(cleanedUsername);

        if (gateway != null)
            gateway.receive(event);
    }

    private void ensureServiceIsAttached() {
        if (this.service == null)
            throw new IllegalStateException("Attach the IZombie multiplayer service first.");
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private final class LocalGateway implements IZombieClientGateway {

        private final String username;
        private volatile IZombieClientListener listener;

        private LocalGateway(String username) {
            this.username = username;
        }

        @Override
        public void setListener(IZombieClientListener listener) {
            this.listener = listener;
        }

        @Override
        public void send(IZombieClientMessage message) {
            if (!isConnected())
                throw new IllegalStateException("The local IZombie client is disconnected.");

            IZombieMultiplayerService currentService = LocalIZombieGatewayHub.this.service;

            if (currentService == null)
                throw new IllegalStateException("IZombie multiplayer service is unavailable.");

            currentService.handleMessage(this.username, message);
        }

        @Override
        public boolean isConnected() {
            return LocalIZombieGatewayHub.this.isUserOnline(this.username);
        }

        private void receive(IZombieServerEvent event) {
            IZombieClientListener currentListener = this.listener;

            if (currentListener != null)
                currentListener.onIZombieEvent(event);
        }
    }
}
