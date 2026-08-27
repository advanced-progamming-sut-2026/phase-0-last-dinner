package network.izombie.transport;

import network.izombie.protocol.IZombieClientMessage;
import network.izombie.protocol.IZombieServerEvent;
import network.izombie.server.IZombieMultiplayerService;
import network.izombie.server.IZombieServerTransport;

import java.util.Objects;

public final class IZombieNetworkServerAdapter implements IZombieServerTransport, AutoCloseable {

    private final IZombieServerNetworkPort networkPort;
    private final IZombieJsonCodec codec;

    private volatile IZombieMultiplayerService service;
    private volatile boolean closed;

    public IZombieNetworkServerAdapter(IZombieServerNetworkPort networkPort) {
        this(networkPort, new IZombieJsonCodec());
    }

    public IZombieNetworkServerAdapter(IZombieServerNetworkPort networkPort, IZombieJsonCodec codec) {
        this.networkPort = Objects.requireNonNull(networkPort);

        this.codec = Objects.requireNonNull(codec);
    }

    public synchronized void attachService(IZombieMultiplayerService service) {
        if (closed) {
            throw new IllegalStateException("I, Zombie server adapter is closed.");
        }

        if (this.service != null) {
            throw new IllegalStateException("I, Zombie service is already attached.");
        }

        this.service = Objects.requireNonNull(service);

        networkPort.setIZombieMessageListener(this::handleClientPayload);

        networkPort.setUserDisconnectedListener(this::handleDisconnect);
    }

    @Override
    public boolean isUserOnline(String username) {
        return !closed && username != null && networkPort.isUserOnline(username);
    }

    @Override
    public void sendToUser(String username, IZombieServerEvent event) {
        if (closed || username == null || event == null) {
            return;
        }

        String payload = codec.encodeServerEvent(event);

        networkPort.sendIZombiePayload(username, payload);
    }

    private void handleClientPayload(String authenticatedUsername, String payload) {
        if (closed || authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return;
        }

        IZombieMultiplayerService currentService = service;

        if (currentService == null) {
            return;
        }

        IZombieClientMessage message;

        try {
            message = codec.decodeClientMessage(payload);
        } catch (RuntimeException exception) {
            sendToUser(authenticatedUsername, IZombieServerEvent.error(null, "Invalid I, Zombie request."));

            return;
        }

        currentService.handleMessage(authenticatedUsername, message);
    }

    private void handleDisconnect(String username) {
        if (closed || username == null || username.isBlank()) {
            return;
        }

        IZombieMultiplayerService currentService = service;

        if (currentService != null) {
            currentService.handleDisconnect(username);
        }
    }

    @Override
    public synchronized void close() {
        if (closed)
            return;

        closed = true;
        service = null;

        networkPort.setIZombieMessageListener(null);
        networkPort.setUserDisconnectedListener(null);
    }
}
