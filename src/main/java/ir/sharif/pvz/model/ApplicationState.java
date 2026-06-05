package ir.sharif.pvz.model;

public class ApplicationState {
    private Menu currentMenu;
    private boolean running;

    public ApplicationState() {
        currentMenu = Menu.REGISTER;
        running = true;
    }

    public Menu getCurrentMenu() {
        return currentMenu;
    }

    public void setCurrentMenu(Menu currentMenu) {
        this.currentMenu = currentMenu;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }
}
