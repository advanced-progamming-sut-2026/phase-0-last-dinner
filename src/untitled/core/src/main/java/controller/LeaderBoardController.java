package controller;

import model.User.AccountService;
import model.User.LeaderboardEntry;
import model.User.LeaderboardSortField;
import view.LeaderBoardView;
import view.LeaderBoardViewObserver;

import java.util.ArrayList;
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
        if (this.accountService == null) {
            return new ArrayList<>();
        }
        return this.accountService.getLeaderboard(sortField, ascending);
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

}
