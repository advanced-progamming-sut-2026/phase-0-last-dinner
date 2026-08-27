package controller;

import model.Menu.MenuType;
import model.User.AccountService;
import model.User.User;
import model.chapters.ChapterType;
import view.GameViewObserver;

public class GameController implements GameViewObserver {

    private final LoginController loginController;
    private final ChapterController chapterController;

    private final AccountService accountService;

    public GameController(
            LoginController loginController,
            AccountService accountService,
            ChapterController chapterController
    ) {
        if (loginController == null || accountService == null || chapterController == null) {
            throw new IllegalArgumentException("loginController, accountService and chapterController are required");
        }

        this.loginController = loginController;
        this.accountService = accountService;
        this.chapterController = chapterController;
    }

    public void enterGreenhouse() {
        this.loginController.getMenuContext().enterMenu(MenuType.GREENHOUSE_MENU);
    }

    public void enterTravelLog() {
        this.loginController.getMenuContext().enterMenu(MenuType.TRAVEL_LOG_MENU);
    }

    public void enterLeaderboard() {
        this.loginController.getMenuContext().enterMenu(MenuType.LEADERBOARD_MENU);
    }

    public int coinWallet() {
        User user = this.loginController.getCurrentUser();
        return user == null ? 0 : user.getGold();
    }

    public int gemWallet() {
        User user = this.loginController.getCurrentUser();
        return user == null ? 0 : user.getDiamond();
    }

    public void cheatCode(int count, String currencyType) {
        User user = this.loginController.getCurrentUser();

        if (user == null || count <= 0 || currencyType == null) {
            return;
        }

        if ("coin".equalsIgnoreCase(currencyType)) {
            user.setGold(this.safeAdd(user.getGold(), count));
        } else if ("diamond".equalsIgnoreCase(currencyType)) {
            user.setDiamond(this.safeAdd(user.getDiamond(), count));
        } else {
            return;
        }

        this.accountService.save();
    }

    private int safeAdd(int currentValue, int amount) {
        long result = (long) currentValue + amount;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    @Override
    public boolean onEnterChapterRequested(String chapterName) {
        User user = this.loginController.getCurrentUser();

        if (user == null || chapterName == null) {
            return false;
        }

        ChapterType requestedChapter;

        try {
            requestedChapter = this.parseChapter(chapterName);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!user.isChapterUnlocked(requestedChapter)) {
            return false;
        }

        try {
            this.chapterController.enterChapterMenu(requestedChapter);
        } catch (IllegalStateException e) {
            return false;
        }

        return true;
    }

    @Override
    public void onGreenhouseRequested() {
        this.enterGreenhouse();
    }

    @Override
    public void onTravelLogRequested() {
        this.enterTravelLog();
    }

    @Override
    public void onLeaderboardRequested() {
        this.enterLeaderboard();
    }

    @Override
    public int onCoinWalletRequested() {
        return this.coinWallet();
    }

    @Override
    public int onGemWalletRequested() {
        return this.gemWallet();
    }

    @Override
    public void onCheatAddRequested(int count, String currencyType) {
        this.cheatCode(count, currencyType);
    }

    private ChapterType parseChapter(String chapterName) {
        if (chapterName == null) {
            throw new IllegalArgumentException("Chapter name is required");
        }

        String normalized = chapterName.trim()
                .replace("\"", "")
                .replace("'", "")
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase();

        return ChapterType.valueOf(normalized);
    }
}
