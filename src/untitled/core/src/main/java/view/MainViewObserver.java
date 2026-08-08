package view;

public interface MainViewObserver {
    boolean onOpenGameMenuRequested();

    boolean onOpenSettingsMenuRequested();

    boolean onOpenNewsMenuRequested();

    boolean onOpenProfileMenuRequested();

    String onLogoutRequested();
}