package network.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import network.protocol.RequestType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ServerLeaderboardService {
    private static final List<String> CHAPTER_ORDER = List.of(
            "ANCIENT_EGYPT",
            "ICE_CAVES",
            "BIG_WAVE_BEACH",
            "MEDIEVAL"
    );

    private final ServerUserRepository repository;
    private final ServerAccountService accountService;

    public ServerLeaderboardService(
            ServerUserRepository repository,
            ServerAccountService accountService
    ) {
        if (repository == null || accountService == null) {
            throw new IllegalArgumentException("Repository and account service are required");
        }
        this.repository = repository;
        this.accountService = accountService;
    }

    public void registerRoutes(RequestRouter router) {
        router.register(RequestType.GET_LEADERBOARD, this::getLeaderboard);
    }

    private synchronized JsonObject getLeaderboard(JsonObject payload) {
        if (this.accountService.authenticatedUser(string(payload, "token")) == null) {
            return result("SESSION_INVALID", "Login is required");
        }

        String sortField = string(payload, "sortField");
        boolean ascending = bool(payload, "ascending");
        List<LeaderboardRow> rows = new ArrayList<>();
        for (ServerUserRecord user : this.repository.getUsers()) {
            if (user != null) {
                rows.add(LeaderboardRow.from(user));
            }
        }

        Comparator<LeaderboardRow> comparator = comparatorFor(sortField);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(
                row -> row.username,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
        rows.sort(comparator);

        JsonArray entries = new JsonArray();
        for (int i = 0; i < rows.size(); i++) {
            entries.add(rows.get(i).toJson(i + 1));
        }
        JsonObject response = result("SUCCESS", "Leaderboard loaded");
        response.add("entries", entries);
        return response;
    }

    private Comparator<LeaderboardRow> comparatorFor(String sortField) {
        String selected = sortField == null ? "MEOW_POINTS" : sortField;
        return switch (selected) {
            case "USERNAME" -> Comparator.comparing(
                    row -> row.username,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "PROGRESS" -> Comparator.comparingInt((LeaderboardRow row) -> row.chapterIndex)
                    .thenComparingInt(row -> row.lastLevel);
            case "MINIGAMES" -> Comparator.comparingInt(row -> row.completedMinigames);
            case "DAILY_QUESTS" -> Comparator.comparingInt(row -> row.completedDailyQuests);
            case "NON_DAILY_QUESTS" -> Comparator.comparingInt(row -> row.completedNonDailyQuests);
            default -> Comparator.comparingInt(row -> row.meowPoints);
        };
    }

    private JsonObject result(String status, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", status);
        response.addProperty("message", message);
        return response;
    }

    private String string(JsonObject payload, String name) {
        JsonElement value = payload == null ? null : payload.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private boolean bool(JsonObject payload, String name) {
        JsonElement value = payload == null ? null : payload.get(name);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    private static final class LeaderboardRow {
        private final String username;
        private final String nickname;
        private final String lastChapter;
        private final int chapterIndex;
        private final int lastLevel;
        private final int completedMinigames;
        private final int completedDailyQuests;
        private final int completedNonDailyQuests;
        private final int meowPoints;

        private LeaderboardRow(
                String username,
                String nickname,
                String lastChapter,
                int lastLevel,
                int completedMinigames,
                int completedDailyQuests,
                int completedNonDailyQuests,
                int meowPoints
        ) {
            this.username = username;
            this.nickname = nickname;
            this.lastChapter = lastChapter == null ? "NOT_STARTED" : lastChapter;
            this.chapterIndex = CHAPTER_ORDER.indexOf(this.lastChapter);
            this.lastLevel = Math.max(0, lastLevel);
            this.completedMinigames = Math.max(0, completedMinigames);
            this.completedDailyQuests = Math.max(0, completedDailyQuests);
            this.completedNonDailyQuests = Math.max(0, completedNonDailyQuests);
            this.meowPoints = Math.max(0, meowPoints);
        }

        private static LeaderboardRow from(ServerUserRecord record) {
            JsonObject user = record.user == null ? new JsonObject() : record.user;
            String chapter = text(user, "lastCompletedChapterType");
            if (chapter == null) {
                chapter = text(user, "savedChapterType");
            }
            int level = number(user, "lastCompletedLevel");
            if (level <= 0 && chapter != null) {
                level = Math.max(0, number(user, "level") - 1);
            }
            return new LeaderboardRow(
                    record.username,
                    record.nickname,
                    chapter,
                    level,
                    number(user, "completedMinigames"),
                    number(user, "completedDailyQuests"),
                    number(user, "completedNonDailyQuests"),
                    number(user, "maxObtainedMeowPoints")
            );
        }

        private JsonObject toJson(int rank) {
            JsonObject entry = new JsonObject();
            entry.addProperty("rank", rank);
            entry.addProperty("username", this.username);
            entry.addProperty("nickname", this.nickname);
            entry.addProperty("lastChapter", this.lastChapter);
            entry.addProperty("lastLevel", this.lastLevel);
            entry.addProperty("completedMinigames", this.completedMinigames);
            entry.addProperty("completedDailyQuests", this.completedDailyQuests);
            entry.addProperty("completedNonDailyQuests", this.completedNonDailyQuests);
            entry.addProperty("meowPoints", this.meowPoints);
            return entry;
        }

        private static String text(JsonObject object, String name) {
            JsonElement value = object.get(name);
            return value == null || value.isJsonNull() ? null : value.getAsString();
        }

        private static int number(JsonObject object, String name) {
            JsonElement value = object.get(name);
            return value == null || value.isJsonNull() ? 0 : value.getAsInt();
        }
    }
}
