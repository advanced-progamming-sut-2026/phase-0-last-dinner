package model.User;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class ClientSessionStore {
    private final Path path;
    private final Gson gson = new Gson();

    ClientSessionStore(Path path) {
        this.path = path;
    }

    String load() {
        if (!Files.exists(this.path)) {
            return null;
        }
        try {
            JsonObject stored = this.gson.fromJson(
                    Files.readString(this.path, StandardCharsets.UTF_8), JsonObject.class);
            return stored == null || !stored.has("token") ? null : stored.get("token").getAsString();
        } catch (IOException | JsonParseException | IllegalStateException e) {
            clear();
            return null;
        }
    }

    void save(String token) {
        if (token == null || token.isBlank()) {
            clear();
            return;
        }
        try {
            Path parent = this.path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonObject stored = new JsonObject();
            stored.addProperty("token", token);
            Files.writeString(this.path, this.gson.toJson(stored), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save client session", e);
        }
    }

    void clear() {
        try {
            Files.deleteIfExists(this.path);
        } catch (IOException ignored) {
        }
    }
}
