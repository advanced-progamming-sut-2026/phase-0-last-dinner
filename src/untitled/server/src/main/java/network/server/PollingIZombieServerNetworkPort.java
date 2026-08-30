package network.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import network.izombie.transport.IZombieServerNetworkPort;
import network.protocol.RequestType;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PollingIZombieServerNetworkPort implements IZombieServerNetworkPort, AutoCloseable {

    private static final long ONLINE_TIMEOUT_MILLIS = 15_000L;
    private static final int MAX_QUEUED_EVENTS = 256;
    private static final int MAX_EVENTS_PER_POLL = 64;

    private final ServerAccountService accountService;

    private final Map<String, ConcurrentLinkedDeque<String>> outboundEvents = new ConcurrentHashMap<>();

    private final Map<String, Long> lastSeenMillis = new ConcurrentHashMap<>();

    private final Map<String, String> displayUsernames = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor;

    private volatile BiConsumer<String, String> messageListener;
    private volatile Consumer<String> disconnectedListener;
    private volatile boolean closed;

    public PollingIZombieServerNetworkPort(RequestRouter router, ServerAccountService accountService) {
        if (router == null || accountService == null) {
            throw new IllegalArgumentException("Router and account service are required");
        }

        this.accountService = accountService;

        router.register(RequestType.IZOMBIE_COMMAND, this::handleCommand);

        router.register(RequestType.IZOMBIE_POLL, this::handlePoll);

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(new CleanupThreadFactory());

        this.cleanupExecutor.scheduleWithFixedDelay(this::removeExpiredUsers, 1L, 1L, TimeUnit.SECONDS);
    }

    @Override
    public boolean isUserOnline(String username) {
        Long lastSeen = this.lastSeenMillis.get(key(username));

        return !this.closed && lastSeen != null && System.currentTimeMillis() - lastSeen <= ONLINE_TIMEOUT_MILLIS;
    }

    @Override
    public void sendIZombiePayload(String username, String payload) {
        if (this.closed || username == null || payload == null) {
            return;
        }

        ConcurrentLinkedDeque<String> queue = this.outboundEvents.computeIfAbsent(key(username),
            ignored -> new ConcurrentLinkedDeque<>());

        queue.addLast(payload);

        while (queue.size() > MAX_QUEUED_EVENTS) {
            queue.pollFirst();
        }
    }

    @Override
    public void setIZombieMessageListener(BiConsumer<String, String> listener) {
        this.messageListener = listener;
    }

    @Override
    public void setUserDisconnectedListener(Consumer<String> listener) {
        this.disconnectedListener = listener;
    }

    private JsonObject handleCommand(JsonObject payload) {
        String username = authenticate(payload);
        String message = string(payload, "message");

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("I, Zombie message is required");
        }

        markOnline(username);

        BiConsumer<String, String> listener = this.messageListener;

        if (listener == null) {
            throw new IllegalArgumentException("I, Zombie service is unavailable");
        }

        listener.accept(username, message);

        return drainEvents(username);
    }

    private JsonObject handlePoll(JsonObject payload) {
        String username = authenticate(payload);

        markOnline(username);

        return drainEvents(username);
    }

    private String authenticate(JsonObject payload) {
        String token = string(payload, "token");

        String username = this.accountService.authenticatedUsername(token);

        if (username == null) {
            throw new IllegalArgumentException("Login is required");
        }

        return username;
    }

    private void markOnline(String username) {
        String userKey = key(username);

        this.displayUsernames.put(userKey, username);

        this.lastSeenMillis.put(userKey, System.currentTimeMillis());

        this.outboundEvents.computeIfAbsent(userKey, ignored -> new ConcurrentLinkedDeque<>());
    }

    private JsonObject drainEvents(String username) {
        JsonArray events = new JsonArray();

        ConcurrentLinkedDeque<String> queue = this.outboundEvents.get(key(username));

        if (queue != null) {
            for (int count = 0; count < MAX_EVENTS_PER_POLL; count++) {

                String event = queue.pollFirst();

                if (event == null) {
                    break;
                }

                events.add(event);
            }
        }

        JsonObject response = new JsonObject();
        response.add("events", events);

        return response;
    }

    private void removeExpiredUsers() {
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : this.lastSeenMillis.entrySet()) {

            boolean expired = now - entry.getValue() > ONLINE_TIMEOUT_MILLIS;

            if (!expired) {
                continue;
            }

            boolean removed = this.lastSeenMillis.remove(entry.getKey(), entry.getValue());

            if (!removed) {
                continue;
            }

            this.outboundEvents.remove(entry.getKey());

            String username = this.displayUsernames.remove(entry.getKey());

            Consumer<String> listener = this.disconnectedListener;

            if (listener != null && username != null) {
                listener.accept(username);
            }
        }
    }

    private String string(JsonObject payload, String name) {
        if (payload == null || !payload.has(name) || payload.get(name).isJsonNull()) {
            return null;
        }

        return payload.get(name).getAsString();
    }

    private String key(String username) {
        if (username == null) {
            return "";
        }

        return username.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;

        this.cleanupExecutor.shutdownNow();

        this.outboundEvents.clear();
        this.lastSeenMillis.clear();
        this.displayUsernames.clear();

        this.messageListener = null;
        this.disconnectedListener = null;
    }

    private static final class CleanupThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "izombie-online-cleanup");

            thread.setDaemon(true);

            return thread;
        }
    }
}
