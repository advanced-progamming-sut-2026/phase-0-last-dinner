package network.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ServerUserRepository {
    private final Path storagePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<ServerUserRecord> users = new ArrayList<>();

    public ServerUserRepository(Path storagePath) {
        if (storagePath == null) {
            throw new IllegalArgumentException("Storage path is required");
        }
        this.storagePath = storagePath;
        load();
    }

    synchronized ServerUserRecord findByUsername(String username) {
        if (username == null) {
            return null;
        }
        for (ServerUserRecord user : this.users) {
            if (user != null && user.username != null && user.username.equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    synchronized ServerUserRecord findByRememberedToken(String token) {
        String tokenHash = ServerTokenHasher.hash(token);
        if (tokenHash == null) {
            return null;
        }
        for (ServerUserRecord user : this.users) {
            if (user != null && tokenHash.equals(user.rememberedTokenHash)) {
                return user;
            }
        }
        return null;
    }

    synchronized boolean add(ServerUserRecord user) {
        if (user == null || user.username == null || findByUsername(user.username) != null) {
            return false;
        }
        this.users.add(user);
        save();
        return true;
    }

    synchronized boolean rename(ServerUserRecord user, String username) {
        ServerUserRecord existing = findByUsername(username);
        if (existing != null && existing != user) {
            return false;
        }
        user.username = username;
        save();
        return true;
    }

    synchronized List<ServerUserRecord> getUsers() {
        return new ArrayList<>(this.users);
    }

    synchronized void save() {
        try {
            Path parent = this.storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporary = this.storagePath.resolveSibling(this.storagePath.getFileName() + ".tmp");
            Files.writeString(temporary, this.gson.toJson(this.users), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, this.storagePath,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, this.storagePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save server users", e);
        }
    }

    private synchronized void load() {
        if (!Files.exists(this.storagePath)) {
            return;
        }
        try {
            ServerUserRecord[] stored = this.gson.fromJson(
                    Files.readString(this.storagePath, StandardCharsets.UTF_8),
                    ServerUserRecord[].class);
            if (stored != null) {
                this.users = new ArrayList<>(Arrays.asList(stored));
            }
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Could not load server users", e);
        }
    }
}
