package network.izombie.transport;

import network.izombie.client.IZombieClientGateway;
import network.izombie.client.IZombieClientListener;
import network.izombie.protocol.IZombieClientMessage;
import network.izombie.protocol.IZombieServerEvent;

import java.util.Objects;

public final class NetworkIZombieClientGateway implements IZombieClientGateway, AutoCloseable {

    private final IZombieClientNetworkPort networkPort;
    private final IZombieJsonCodec codec;

    private volatile IZombieClientListener listener;
    private volatile boolean closed;

    public NetworkIZombieClientGateway(IZombieClientNetworkPort networkPort) {
        this(networkPort, new IZombieJsonCodec());
    }

    public NetworkIZombieClientGateway(IZombieClientNetworkPort networkPort, IZombieJsonCodec codec) {
        this.networkPort = Objects.requireNonNull(networkPort);

        this.codec = Objects.requireNonNull(codec);

        networkPort.setIZombiePayloadListener(this::handleServerPayload);
    }

    @Override
    public void setListener(IZombieClientListener listener) {
        this.listener = listener;
    }

    @Override
    public void send(IZombieClientMessage message) {
        if (closed) {
            throw new IllegalStateException("I, Zombie gateway is closed.");
        }

        if (!networkPort.isConnected()) {
            throw new IllegalStateException("Connection to the server is unavailable.");
        }

        String payload = codec.encodeClientMessage(message);

        networkPort.sendIZombiePayload(payload);
    }

    @Override
    public boolean isConnected() {
        return !closed && networkPort.isConnected();
    }

    private void handleServerPayload(String payload) {
        if (closed) {
            return;
        }

        IZombieServerEvent event;

        try {
            event = codec.decodeServerEvent(payload);
        } catch (RuntimeException exception) {
            return;
        }

        IZombieClientListener currentListener = listener;

        if (currentListener != null) {
            currentListener.onIZombieEvent(event);
        }
    }

    @Override
    public void close() {
        if (closed)
            return;

        closed = true;
        listener = null;

        networkPort.setIZombiePayloadListener(null);
    }
}
