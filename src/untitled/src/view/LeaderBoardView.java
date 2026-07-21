package view;

import model.User.LeaderboardEntry;

import java.util.List;

public class LeaderBoardView implements CommandHandler {
    private LeaderBoardViewObserver observer;

    public void setObserver(LeaderBoardViewObserver observer) {
        this.observer = observer;
    }

    @Override
    public void handleCommand(String input) {
        if (this.observer == null) {
            System.out.println("Leaderboard controller is not connected.");
            return;
        }

        if (LeaderBoardCommand.SHOW.getMatcher(input) == null) {
            System.out.println("Invalid leaderboard command.");
            return;
        }

        this.printLeaderboard(this.observer.onShowLeaderboardRequested());
    }

    private void printLeaderboard(List<LeaderboardEntry> entries) {
        System.out.println("Leaderboard");

        if (entries == null || entries.isEmpty()) {
            System.out.println("No players were found.");
            return;
        }

        for (LeaderboardEntry entry : entries) {
            System.out.println(
                    entry.getRank()
                            + " | " + entry.getUsername()
                            + " | " + entry.getNickname()
                            + " | " + entry.getMeowPoints()
            );
        }
    }
}
