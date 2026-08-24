package network.izombie.transport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.izombie.protocol.IZombieClientMessage;
import network.izombie.protocol.IZombieServerEvent;

public final class IZombieJsonCodec {

    private final Gson gson;

    public IZombieJsonCodec() {
        gson = new GsonBuilder().create();
    }

    public String encodeClientMessage(IZombieClientMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Client message cannot be null.");
        }

        return gson.toJson(message);
    }

    public IZombieClientMessage decodeClientMessage(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Client payload cannot be empty.");
        }

        IZombieClientMessage message = gson.fromJson(payload, IZombieClientMessage.class);

        if (message == null || message.getType() == null) {
            throw new IllegalArgumentException("Invalid I, Zombie client payload.");
        }

        return message;
    }

    public String encodeServerEvent(IZombieServerEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Server event cannot be null.");
        }

        return gson.toJson(event);
    }

    public IZombieServerEvent decodeServerEvent(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Server payload cannot be empty.");
        }

        IZombieServerEvent event = gson.fromJson(payload, IZombieServerEvent.class);

        if (event == null || event.getType() == null) {
            throw new IllegalArgumentException("Invalid I, Zombie server payload.");
        }

        return event;
    }
}
