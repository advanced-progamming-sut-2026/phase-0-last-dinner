package controller;

import model.User.AccountService;
import model.User.LeaderboardEntry;
import model.User.LeaderboardSortField;
import model.User.User;
import view.LeaderBoardView;
import view.LeaderBoardViewObserver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderBoardController implements LeaderBoardViewObserver {
    private final AccountService accountService;

    public LeaderBoardController() {
        this.accountService = null;
    }

    public LeaderBoardController(LeaderBoardView view, AccountService accountService) {
        if (view == null || accountService == null) {
            throw new IllegalArgumentException("Leaderboard view and account service are required");
        }

        this.accountService = accountService;
        view.setObserver(this);
    }

    public List<LeaderboardEntry> sort() {
        return this.sort(LeaderboardSortField.MEOW_POINTS, false);
    }

    public List<LeaderboardEntry> sort(
            LeaderboardSortField sortField,
            boolean ascending
    ) {
        List<User> rankedUsers = new ArrayList<>();

        if (this.accountService == null) {
            return new ArrayList<>();
        }

        for (User user : this.accountService.getUsers()) {
            if (user != null) {
                rankedUsers.add(user);
            }
        }

        Comparator<User> comparator = this.comparatorFor(sortField);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(
                User::getUsername,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
        rankedUsers.sort(comparator);

        List<LeaderboardEntry> entries = new ArrayList<>();

        for (int i = 0; i < rankedUsers.size(); i++) {
            entries.add(new LeaderboardEntry(i + 1, rankedUsers.get(i)));
        }

        return entries;
    }

    public List<LeaderboardEntry> showLeaderboard() {
        return this.sort();
    }

    public List<LeaderboardEntry> showLeaderboard(
            LeaderboardSortField sortField,
            boolean ascending
    ) {
        return this.sort(sortField, ascending);
    }

    @Override
    public List<LeaderboardEntry> onShowLeaderboardRequested() {
        return this.showLeaderboard();
    }

    @Override
    public List<LeaderboardEntry> onShowLeaderboardRequested(
            LeaderboardSortField sortField,
            boolean ascending
    ) {
        return this.showLeaderboard(sortField, ascending);
    }

    private Comparator<User> comparatorFor(LeaderboardSortField sortField) {
        LeaderboardSortField selected = sortField == null
                ? LeaderboardSortField.MEOW_POINTS
                : sortField;

        switch (selected) {
            case USERNAME:
                return Comparator.comparing(
                        User::getUsername,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                );
            case PROGRESS:
                return Comparator.comparingInt(this::progressChapterIndex)
                        .thenComparingInt(this::progressLevel);
            case MINIGAMES:
                return Comparator.comparingInt(User::getCompletedMinigames);
            case DAILY_QUESTS:
                return Comparator.comparingInt(User::getCompletedDailyQuests);
            case NON_DAILY_QUESTS:
                return Comparator.comparingInt(User::getCompletedNonDailyQuests);
            case MEOW_POINTS:
            default:
                return Comparator.comparingInt(User::getMaxObtainedMeowPoints);
        }
    }

    private int progressChapterIndex(User user) {
        if (user == null) {
            return -1;
        }
        if (user.getLastCompletedChapterType() != null) {
            return user.getLastCompletedChapterType().ordinal();
        }
        return user.getChapter() == null || user.getChapter().getChapter() == null
                ? -1
                : user.getChapter().getChapter().ordinal();
    }

    private int progressLevel(User user) {
        if (user == null) {
            return 0;
        }
        if (user.getLastCompletedLevel() > 0) {
            return user.getLastCompletedLevel();
        }
        return user.getChapter() == null ? 0 : Math.max(0, user.getLevel() - 1);
    }
}
