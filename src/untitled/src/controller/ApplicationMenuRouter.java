package controller;

import model.Menu.MenuType;

import java.util.List;

final class ApplicationMenuRouter {
    String execute(ApplicationController application, List<String> tokens) {
        if (!"menu".equalsIgnoreCase(tokens.get(0))) {
            return null;
        }
        if (application.matches(tokens, "menu", "show", "current")) {
            return showCurrentMenu(application);
        }
        if (application.matches(tokens, "menu", "exit")) {
            return exitMenu(application);
        }
        if (application.matches(tokens, "menu", "logout")) {
            return application.logoutFromMainMenu();
        }
        if (tokens.size() >= 3 && "enter".equalsIgnoreCase(tokens.get(1))) {
            return enterMenu(application, tokens);
        }
        if (application.matches(tokens, "menu", "meow-point")) {
            application.getMenuContext().enterMenu(MenuType.MEOW_POINT_MENU);
            return application.getMenuContext().getCurrentMenu().name();
        }
        return "Invalid menu command";
    }

    private String showCurrentMenu(ApplicationController application) {
        if (application.getMenuContext().getCurrentMenu() == MenuType.MAIN_MENU
                && application.getCurrentUser() != null
                && application.getCurrentUser().hasUnreadNews()) {
            return MenuType.MAIN_MENU.name() + " [new news]";
        }
        return application.getMenuContext().getCurrentMenu().name();
    }

    private String exitMenu(ApplicationController application) {
        MenuType previousMenu = application.getMenuContext().getCurrentMenu();
        application.getMenuContext().exitMenu();
        application.cancelPendingAccountAction(previousMenu);
        if (previousMenu == MenuType.PLANT_PICK_MENU
                || previousMenu == MenuType.MID_GAME_MENU) {
            application.clearGameConnections();
        }
        return application.getMenuContext().isApplicationRunning()
                ? application.getMenuContext().getCurrentMenu().name()
                : "Application closed";
    }

    private String enterMenu(ApplicationController application, List<String> tokens) {
        MenuType destination = application.parseMenuType(application.join(tokens, 2));
        MenuType previousMenu = application.getMenuContext().getCurrentMenu();
        if (destination == MenuType.MID_GAME_MENU) {
            return "Use start game from plant pick menu";
        }
        if (destination == MenuType.PLANT_PICK_MENU
                && previousMenu == MenuType.MEOW_POINT_MENU) {
            return application.startMeowPointSelection();
        }
        if (destination == MenuType.PLANT_PICK_MENU
                && previousMenu == MenuType.CHAPTER_MENU
                && application.getChapterController().getSelectedChapter() != null
                && application.getChapterController().getSelectedLevel() == null) {
            if (!application.getChapterController().selectLevel(model.level.LevelType.NORMAL)) {
                return "Level is not available.";
            }
            return application.getMenuContext().getCurrentMenu().name();
        }
        application.getMenuContext().enterMenu(destination);
        application.cancelPendingAccountAction(previousMenu);
        return application.getMenuContext().getCurrentMenu().name();
    }
}
