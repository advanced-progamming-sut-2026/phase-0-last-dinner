package controller;

import model.User.AccountService;
import model.User.LeaderboardEntry;
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
        List<User> rankedUsers = new ArrayList<>();

        if (this.accountService == null) {
            return new ArrayList<>();
        }

        for (User user : this.accountService.getUsers()) {
            if (user != null) {
                rankedUsers.add(user);
            }
        }

        rankedUsers.sort(
                Comparator.comparingInt(User::getMaxObtainedMeowPoints)
                        .reversed()
                        .thenComparing(
                                User::getUsername,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
        );

        List<LeaderboardEntry> entries = new ArrayList<>();

        for (int i = 0; i < rankedUsers.size(); i++) {
            entries.add(new LeaderboardEntry(i + 1, rankedUsers.get(i)));
        }

        return entries;
    }

    public List<LeaderboardEntry> showLeaderboard() {
        return this.sort();
    }

    @Override
    public List<LeaderboardEntry> onShowLeaderboardRequested() {
        return this.showLeaderboard();
    }
}
