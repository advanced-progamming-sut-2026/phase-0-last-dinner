package view;

public interface GameViewObserver {
    boolean onEnterChapterRequested(String chapterName);

    void onGreenhouseRequested();

    void onTravelLogRequested();

    void onLeaderboardRequested();

    int onCoinWalletRequested();

    int onGemWalletRequested();

    void onCheatAddRequested(int count, String currencyType);
}