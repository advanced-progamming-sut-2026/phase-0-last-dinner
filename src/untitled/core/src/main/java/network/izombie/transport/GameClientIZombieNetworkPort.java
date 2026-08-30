package network.izombie.transport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import network.client.GameClient;
import network.client.NetworkException;
import network.protocol.NetworkResponse;
import network.protocol.RequestType;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GameClientIZombieNetworkPort implements IZombieClientNetworkPort, AutoCloseable {

    private static final long POLL_INTERVAL_MILLIS = 100L;

    private final GameClient client;
    private final Supplier<String> authTokenSupplier;
    private final ScheduledExecutorService pollExecutor;

    private volatile Consumer<String> payloadListener;
    private volatile boolean closed;

    private final Object requestLock = new Object();
    private volatile boolean serverReachable;

    public GameClientIZombieNetworkPort(GameClient client, Supplier<String> authTokenSupplier) {
        this.client = Objects.requireNonNull(client, "client");
        this.authTokenSupplier = Objects.requireNonNull(authTokenSupplier, "authTokenSupplier");

        this.pollExecutor = Executors.newSingleThreadScheduledExecutor(new PollThreadFactory());

        this.pollExecutor.scheduleWithFixedDelay(this::pollSafely, 0L, POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isConnected() {
        return !this.closed && token() != null && this.serverReachable;
    }

    @Override
    public void sendIZombiePayload(String payload) {
        if (this.closed)
            throw new IllegalStateException("I, Zombie network port is closed");

        if (payload == null || payload.isBlank())
            throw new IllegalArgumentException("I, Zombie payload cannot be empty");

        JsonObject request = authenticatedPayload();
        request.addProperty("message", payload);

        NetworkResponse response;

        try {
            response = sendRequest(RequestType.IZOMBIE_COMMAND, request);
            this.serverReachable = response != null;
        } catch (RuntimeException exception) {
            this.serverReachable = false;
            throw exception;
        }

        requireSuccessful(response);
        deliverEvents(response.getPayload());
    }

    @Override
    public void setIZombiePayloadListener(Consumer<String> listener) {
        this.payloadListener = listener;
    }

    private void pollSafely() {
        if (this.closed || this.payloadListener == null || token() == null)
            return;

        try {
            NetworkResponse response = sendRequest(RequestType.IZOMBIE_POLL, authenticatedPayload());

            this.serverReachable = response != null;

            if (response != null && response.isSuccessful())
                deliverEvents(response.getPayload());
        } catch (RuntimeException exception) {
            this.serverReachable = false;
        }
    }

    private NetworkResponse sendRequest(RequestType requestType, JsonObject payload) {
        synchronized (this.requestLock) {
            if (this.closed)
                throw new IllegalStateException("I, Zombie network port is closed");

            return this.client.send(requestType, payload);
        }
    }

    private JsonObject authenticatedPayload() {
        String token = token();

        if (token == null) {
            throw new NetworkException("Login is required for I, Zombie multiplayer");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("token", token);

        return payload;
    }

    private String token() {
        String value = this.authTokenSupplier.get();

        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

    private void requireSuccessful(NetworkResponse response) {
        if (response != null && response.isSuccessful()) {
            return;
        }

        String message = response == null ? "Server did not respond" : response.getMessage();

        if (message == null || message.isBlank()) {
            message = "I, Zombie request failed";
        }

        throw new NetworkException(message);
    }

    private void deliverEvents(JsonObject payload) {
        if (payload == null || !payload.has("events") || !payload.get("events").isJsonArray()) {
            return;
        }

        JsonArray events = payload.getAsJsonArray("events");

        for (JsonElement event : events) {
            Consumer<String> listener = this.payloadListener;

            if (listener == null) {
                return;
            }

            if (event == null || !event.isJsonPrimitive()) {
                continue;
            }

            listener.accept(event.getAsString());
        }
    }

    @Override
    public void close() {
        if (this.closed)
            return;

        this.closed = true;
        this.serverReachable = false;
        this.payloadListener = null;
        this.pollExecutor.shutdownNow();
    }

    private static final class PollThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "izombie-client-poll");

            thread.setDaemon(true);
            return thread;
        }
    }
}
