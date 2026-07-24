package view.travellog;

import lombok.Getter;
import lombok.Setter;
import model.GameMenuRelated.Page;
import model.GameMenuRelated.PageName;
import model.GameMenuRelated.QuestObj;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import view.CommandHandler;

import java.util.Locale;
import java.util.regex.Matcher;

@Getter
@Setter
public class TravelLogView
        implements CommandHandler {

    private TravelLogViewObserver observer;

    private CommandHandler activeMiniGameHandler;

    private MiniGameType activeMiniGameType;

    @Override
    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println(
                    "Travel Log controller "
                            + "is not connected."
            );
            return;
        }

        if (activeMiniGameHandler != null) {
            handleActiveMiniGameCommand(input);
            return;
        }

        if (handlePageCommand(input) || handleOtherCommand(input)) {
            return;
        }

        System.out.println(
                "Invalid Travel Log command."
        );

        showHelp();
    }

    private boolean handlePageCommand(String input) {
        Matcher matcher = TravelLogCommands
                .CHANGE_PAGE
                .getMatcher(input);

        if (matcher != null) {
            handleChangePage(matcher);
            return true;
        }

        matcher = TravelLogCommands
                .SHOW_PAGE
                .getMatcher(input);

        if (matcher != null) {
            showPage(
                    observer
                            .onShowCurrentPageRequested()
            );
            return true;
        }

        matcher = TravelLogCommands
                .PLAY_MINIGAME
                .getMatcher(input);

        if (matcher != null) {
            handleOpenMiniGame(matcher);
            return true;
        }

        return false;
    }

    private boolean handleOtherCommand(String input) {
        Matcher matcher = TravelLogCommands
                .CLAIM_QUEST
                .getMatcher(input);

        if (matcher != null) {
            System.out.println(
                    observer.onClaimQuestRequested(
                            clean(matcher.group("quest"))
                    )
            );
            return true;
        }

        matcher = TravelLogCommands
                .HELP
                .getMatcher(input);

        if (matcher != null) {
            showHelp();
            return true;
        }

        matcher = TravelLogCommands
                .BACK_TO_GAME
                .getMatcher(input);

        if (matcher != null) {
            System.out.println(
                    "Returning to game menu."
            );
            return true;
        }

        return false;
    }

    private void handleActiveMiniGameCommand(
            String input
    ) {
        Matcher gameMatcher =
                TravelLogCommands
                        .BACK_TO_GAME
                        .getMatcher(input);

        if (gameMatcher != null) {
            activeMiniGameHandler = null;
            activeMiniGameType = null;
            System.out.println(
                    "Returning to game menu."
            );
            return;
        }

        Matcher backMatcher =
                TravelLogCommands
                        .BACK_TO_MINIGAMES
                        .getMatcher(input);

        if (backMatcher != null) {
            activeMiniGameHandler = null;
            activeMiniGameType = null;

            observer.onChangePageRequested(
                    PageName.MINIGAMES
            );

            System.out.println(
                    "Returned to the minigame page."
            );

            showPage(
                    observer
                            .onShowCurrentPageRequested()
            );

            return;
        }

        activeMiniGameHandler.handleCommand(input);
    }

    private void handleChangePage(
            Matcher matcher
    ) {
        String pageText =
                matcher.group("page");

        PageName pageName =
                parsePageName(pageText);

        if (pageName == null) {
            System.out.println(
                    "Unknown Travel Log page: "
                            + pageText
            );
            return;
        }

        Page page =
                observer.onChangePageRequested(
                        pageName
                );

        if (page == null) {
            System.out.println(
                    "Travel Log page is not available."
            );
            return;
        }

        showPage(page);
    }

    private void handleOpenMiniGame(
            Matcher matcher
    ) {
        Page currentPage =
                observer.onShowCurrentPageRequested();

        if (currentPage == null
                || currentPage.getPageName()
                != PageName.MINIGAMES) {

            System.out.println(
                    "Open the MINIGAMES page first."
            );
            return;
        }

        String gameText =
                matcher.group("game");

        MiniGameType miniGameType =
                parseMiniGameType(gameText);

        if (miniGameType == null) {
            System.out.println(
                    "Unknown minigame: " + gameText
            );
            return;
        }

        openMiniGame(miniGameType);
    }

    private void openMiniGame(MiniGameType miniGameType) {
        CommandHandler handler =
                observer.onOpenMiniGameRequested(
                        miniGameType
                );

        if (handler == null) {
            System.out.println(
                    displayMiniGameName(miniGameType)
                            + " is not connected yet."
            );
            return;
        }

        activeMiniGameHandler = handler;
        activeMiniGameType = miniGameType;

        System.out.println(
                displayMiniGameName(miniGameType)
                        + " opened."
        );

        System.out.println(
                "Use 'Back to minigame menu' "
                        + "to return."
        );
    }

    private void showPage(Page page) {
        if (page == null) {
            System.out.println(
                    "No Travel Log page is selected."
            );
            return;
        }

        System.out.println(
                "Travel Log page: "
                        + page.getPageName()
        );

        if (page.getPageName()
                == PageName.MINIGAMES) {

            showMiniGames(page);
            return;
        }

        if (page.getQuestObjects() == null
                || page.getQuestObjects().isEmpty()) {

            System.out.println(
                    "This page currently has no quests."
            );
            return;
        }

        System.out.println("Quests:");

        for (QuestObj questObject : page.getQuestObjects()) {
            if (questObject == null || questObject.getQuest() == null) {
                continue;
            }

            String rewardState = questObject.isRewardClaimed()
                    ? "claimed"
                    : questObject.isCompleted() ? "ready" : "locked";

            System.out.println(
                    "- " + questObject.getQuest().getDisplayName()
                            + " | " + questObject.getQuest().getPriority()
                            + " | " + questObject.getCompletionPercentage() + "%"
                            + " | reward " + rewardState
            );
            System.out.println("  condition: " + questObject.getCompletionCondition());
            System.out.println("  reward: " + questObject.getReward());
        }
    }

    private void showMiniGames(Page page) {
        System.out.println("Minigames:");

        if (page.getMiniGames() == null
                || page.getMiniGames().isEmpty()) {

            System.out.println(
                    "- No minigames are available."
            );
            return;
        }

        for (MiniGame miniGame
                : page.getMiniGames()) {

            if (miniGame == null) {
                continue;
            }

            String state;

            if (miniGame.isAllStagesCompleted()) {
                state = "completed";
            } else if (miniGame.isStarted()) {
                state = "started";
            } else {
                state = "not started";
            }

            System.out.println(
                    "- "
                            + displayMiniGameName(
                            miniGame.getType()
                    )
                            + " | "
                            + state
            );
        }

        System.out.println(
                "Use: minigame play <name>"
        );
    }

    private PageName parsePageName(
            String pageText
    ) {
        if (pageText == null
                || pageText.trim().isEmpty()) {

            return null;
        }

        String normalized =
                pageText.trim()
                        .replace('-', '_')
                        .toUpperCase(Locale.ROOT);

        try {
            return PageName.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private MiniGameType parseMiniGameType(
            String gameText
    ) {
        if (gameText == null
                || gameText.trim().isEmpty()) {

            return null;
        }

        String normalized =
                gameText.toLowerCase(Locale.ROOT)
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("_", "")
                        .replace(",", "");

        switch (normalized) {
            case "vasebreaker":
                return MiniGameType.VASEBREAKER;
            case "wallnutbowling":
            case "walnutbowling":
                return MiniGameType.WALLNUT_BOWLING;
            case "izombie":
                return MiniGameType.I_ZOMBIE;
            case "beghouled":
                return MiniGameType.BEGHOULED;
            case "zombotany":
                return MiniGameType.ZOMBOTANY;
            default:
                return null;
        }
    }

    private String displayMiniGameName(
            MiniGameType type
    ) {
        if (type == null) {
            return "Unknown";
        }

        switch (type) {
            case VASEBREAKER:
                return "Vasebreaker";
            case WALLNUT_BOWLING:
                return "Wall-nut Bowling";
            case I_ZOMBIE:
                return "I, Zombie";
            case BEGHOULED:
                return "Beghouled";
            case ZOMBOTANY:
                return "Zombotany";
            default:
                return "Unknown";
        }
    }

    private void showHelp() {
        System.out.println(
                "Travel Log commands:"
        );

        System.out.println(
                "- travel log page <page_name>"
        );

        System.out.println(
                "- travel log show"
        );

        System.out.println(
                "- minigame play <name>"
        );

        System.out.println(
                "- quest claim <quest_name>"
        );

        System.out.println(
                "- Back to game menu"
        );

        System.out.println(
                "Pages: ADVENTURE, SPECIAL, "
                        + "MINIGAMES, COMMUNITY, "
                        + "CHALLENGES, MYSTERY"
        );
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();
        if (cleaned.length() >= 2
                && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'")))) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }

        return cleaned;
    }

    public boolean isMiniGameOpen() {
        return activeMiniGameHandler != null;
    }
}
