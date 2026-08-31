package view;

import model.User.LeaderboardEntry;
import model.User.LeaderboardUnavailableException;
import model.User.LeaderboardSortField;

import java.util.List;
import java.util.regex.Matcher;

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

        Matcher matcher = LeaderBoardCommand.SHOW.getMatcher(input);
        if (matcher == null) {
            System.out.println("Invalid leaderboard command.");
            return;
        }

        try {
            LeaderboardSortField sortField = LeaderboardSortField.from(matcher.group("sort"));
            boolean ascending = "asc".equalsIgnoreCase(matcher.group("order"));
            this.printLeaderboard(
                    this.observer.onShowLeaderboardRequested(sortField, ascending)
            );
        } catch (IllegalArgumentException | LeaderboardUnavailableException exception) {
            System.out.println(exception.getMessage());
        }
    }

    private void printLeaderboard(List<LeaderboardEntry> entries) {
        System.out.println("Leaderboard");
        System.out.println(
                "rank | username | nickname | chapter | level | minigames"
                        + " | daily quests | non daily quests | meow points"
        );

        if (entries == null || entries.isEmpty()) {
            System.out.println("No players were found.");
            return;
        }

        for (LeaderboardEntry entry : entries) {
            System.out.println(
                    entry.getRank()
                            + " | " + entry.getUsername()
                            + " | " + entry.getNickname()
                            + " | " + entry.getLastChapter()
                            + " | " + entry.getLastLevel()
                            + " | " + entry.getCompletedMinigames()
                            + " | " + entry.getCompletedDailyQuests()
                            + " | " + entry.getCompletedNonDailyQuests()
                            + " | " + entry.getMeowPoints()
            );
        }
    }
}
