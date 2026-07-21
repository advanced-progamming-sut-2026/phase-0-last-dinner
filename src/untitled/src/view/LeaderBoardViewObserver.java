package view;

import model.User.LeaderboardEntry;
import model.User.LeaderboardSortField;

import java.util.List;

public interface LeaderBoardViewObserver {
    List<LeaderboardEntry> onShowLeaderboardRequested();

    default List<LeaderboardEntry> onShowLeaderboardRequested(
            LeaderboardSortField sortField,
            boolean ascending
    ) {
        return onShowLeaderboardRequested();
    }
}
