package view;

import java.util.regex.Matcher;

public class GameView implements CommandHandler, GameEventListener {
    private GameViewObserver observer;

    public void setObserver(GameViewObserver observer) {
        this.observer = observer;
    }

    @Override
    public void handleCommand(String input) {
        for (GameCommand command : GameCommand.values()) {
            Matcher matcher = command.getMatcher(input);
            if (matcher == null) continue;

            switch (command) {
                case ENTER_CHAPTER: {
                    String chapterName = matcher.group("name");
                    if (!observer.onEnterChapterRequested(chapterName)) {
                        System.out.println("Chapter " + chapterName + " is locked or invalid.");
                    }
                    break;
                }
                case GREENHOUSE: {
                    observer.onGreenhouseRequested();
                    break;
                }
                case TRAVEL_LOG: {
                    observer.onTravelLogRequested();
                    break;
                }
                case LEADERBOARD: {
                    observer.onLeaderboardRequested();
                    break;
                }
                case COIN_WALLET: {
                    System.out.println("Coins: " + observer.onCoinWalletRequested());
                    break;
                }
                case GEM_WALLET: {
                    System.out.println("Gems: " + observer.onGemWalletRequested());
                    break;
                }
                case CHEAT_ADD: {
                    int count = Integer.parseInt(matcher.group("count"));
                    String currency = matcher.group("currency");
                    observer.onCheatAddRequested(count, currency);
                    break;
                }
                default: {
                    System.out.println("Invalid command.");
                    break;
                }
            }
            return;
        }

        System.out.println("Invalid command.");
    }

    @Override
    public void onGameEvent(String message) {
        System.out.println(message);
    }
}