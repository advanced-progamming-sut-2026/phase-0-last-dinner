package model.User;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// karbar ha ro az file mikhune va dakhelesh zakhire mikone
public class UserRepository {
    private final Path storagePath;
    private final Gson gson;
    private List<User> users;
    // username karbari ke bayad login bemune
    private String rememberedUsername;

    public UserRepository() {
        // file karbar ha ro birune project va dakhele home mizare
        this(Paths.get(
                System.getProperty("user.home"),
                ".plants-vs-zombies-2",
                "users.json"
        ));
    }

    public UserRepository(Path storagePath) {
        if (storagePath == null) {
            throw new IllegalArgumentException("Storage path is required");
        }

        this.storagePath = storagePath;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(
                        LocalDate.class,
                        new LocalDateAdapter()
                )
                .setPrettyPrinting()
                .create();
        this.users = new ArrayList<>();
        this.load();
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(this.users);
    }

    public User findByUsername(String username) {
        if (username == null) {
            return null;
        }

        for (User user : this.users) {
            if (user != null && user.getUsername() != null
                    && user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }

        return null;
    }

    public boolean add(User user) {
        if (user == null || user.getUsername() == null || this.findByUsername(user.getUsername()) != null) {
            return false;
        }

        this.users.add(user);
        this.save();
        return true;
    }

    public void remember(User user) {
        this.rememberedUsername = user == null ? null : user.getUsername();
        this.save();
    }

    public User getRememberedUser() {
        return this.findByUsername(this.rememberedUsername);
    }

    public void save() {
        try {
            for (User user : this.users) {
                if (user != null) {
                    user.prepareForSave();
                }
            }

            Path parent = this.storagePath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(this.storagePath, StandardCharsets.UTF_8)) {
                this.gson.toJson(new StoredUsers(this.users, this.rememberedUsername), writer);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save users", e);
        }
    }

    private void load() {
        if (!Files.exists(this.storagePath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(this.storagePath, StandardCharsets.UTF_8)) {
            StoredUsers stored = this.gson.fromJson(reader, StoredUsers.class);

            if (stored != null && stored.users != null) {
                this.users = stored.users;
                this.rememberedUsername = stored.rememberedUsername;
                for(User user : this.users) {
                    if(user != null && user.getUsername() != null)
                        user.initializeMissingFields();
                }
            }
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Could not load users", e);
        }
    }

    // shakle etelaati ke dakhele file json zakhire mishe
    private static final class StoredUsers {
        private List<User> users;
        private String rememberedUsername;

        private StoredUsers(List<User> users, String rememberedUsername) {
            this.users = users;
            this.rememberedUsername = rememberedUsername;
        }
    }
}
