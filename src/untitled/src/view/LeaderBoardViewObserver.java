package view;

import model.User.LeaderboardEntry;

import java.util.List;

public interface LeaderBoardViewObserver {
    List<LeaderboardEntry> onShowLeaderboardRequested();
}
