package model.Menu;

public abstract class MenuContext {
    protected MenuState currentState;
    protected boolean applicationRunning;
    protected boolean loggedIn;

    public abstract MenuType getCurrentMenu();

    public abstract boolean isApplicationRunning();

    public abstract boolean isLoggedIn();

    public abstract void enterMenu(MenuType destination);

    public abstract void exitMenu();

    public abstract void login();

    public abstract void logout();

    protected abstract void changeState(MenuState newState);
}
