package network.server;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import network.protocol.NetworkRequest;
import network.protocol.NetworkResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class GameServer implements Closeable {
    private final String host;
    private final int requestedPort;
    private final RequestRouter router;
    private final Gson gson = new Gson();
    private final ExecutorService clientExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public GameServer(String host, int port, RequestRouter router) {
        this.host = Objects.requireNonNull(host, "host");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        this.requestedPort = port;
        this.router = Objects.requireNonNull(router, "router");
    }

    public synchronized void start() throws IOException {
        if (this.running) {
            return;
        }
        this.serverSocket = new ServerSocket();
        this.serverSocket.bind(new InetSocketAddress(this.host, this.requestedPort));
        this.running = true;
        this.acceptThread = Thread.ofPlatform().name("game-server-accept").start(this::acceptClients);
    }

    private void acceptClients() {
        while (this.running) {
            try {
                Socket client = this.serverSocket.accept();
                this.clientExecutor.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (this.running) {
                    System.err.println("Could not accept client: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try (client;
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     client.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                     client.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                NetworkResponse response = parseAndRoute(line);
                writer.write(this.gson.toJson(response));
                writer.newLine();
                writer.flush();
            }
        } catch (IOException ignored) {
        }
    }

    private NetworkResponse parseAndRoute(String line) {
        try {
            return this.router.route(this.gson.fromJson(line, NetworkRequest.class));
        } catch (JsonParseException e) {
            return NetworkResponse.failure(null, "Invalid JSON request");
        }
    }

    public int getPort() {
        ServerSocket currentSocket = this.serverSocket;
        return currentSocket == null ? this.requestedPort : currentSocket.getLocalPort();
    }

    public boolean isRunning() {
        return this.running;
    }

    public void awaitTermination() throws InterruptedException {
        Thread currentAcceptThread = this.acceptThread;
        if (currentAcceptThread != null) {
            currentAcceptThread.join();
        }
    }

    @Override
    public synchronized void close() {
        this.running = false;
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        this.clientExecutor.shutdownNow();
        try {
            this.clientExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
