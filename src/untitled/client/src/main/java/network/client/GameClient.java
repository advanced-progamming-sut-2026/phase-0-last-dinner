package network.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import network.protocol.NetworkRequest;
import network.protocol.NetworkResponse;
import network.protocol.RequestType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class GameClient implements Closeable {
    private static final int DEFAULT_TIMEOUT_MILLIS = 5000;

    private final String host;
    private final int port;
    private final int timeoutMillis;
    private final Gson gson = new Gson();
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public GameClient(String host, int port) {
        this(host, port, DEFAULT_TIMEOUT_MILLIS);
    }

    public GameClient(String host, int port, int timeoutMillis) {
        this.host = Objects.requireNonNull(host, "host");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    public synchronized void connect() {
        if (isConnected()) {
            return;
        }
        try {
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(this.host, this.port), this.timeoutMillis);
            this.socket.setSoTimeout(this.timeoutMillis);
            this.reader = new BufferedReader(new InputStreamReader(
                    this.socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(
                    this.socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            close();
            throw new NetworkException("Could not connect to server", e);
        }
    }

    public synchronized NetworkResponse send(RequestType type, JsonObject payload) {
        connect();
        NetworkRequest request = NetworkRequest.create(type, payload);
        try {
            this.writer.write(this.gson.toJson(request));
            this.writer.newLine();
            this.writer.flush();
            String responseLine = this.reader.readLine();
            if (responseLine == null) {
                throw new NetworkException("Server closed the connection");
            }
            NetworkResponse response = this.gson.fromJson(responseLine, NetworkResponse.class);
            if (response == null || !request.getRequestId().equals(response.getRequestId())) {
                throw new NetworkException("Server returned an invalid response");
            }
            return response;
        } catch (IOException e) {
            close();
            throw new NetworkException("Could not communicate with server", e);
        }
    }

    public synchronized boolean isConnected() {
        return this.socket != null && this.socket.isConnected() && !this.socket.isClosed();
    }

    @Override
    public synchronized void close() {
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException ignored) {
            }
        }
        this.socket = null;
        this.reader = null;
        this.writer = null;
    }
}
