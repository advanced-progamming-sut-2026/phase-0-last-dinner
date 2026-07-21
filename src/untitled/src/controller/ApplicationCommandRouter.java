package controller;

import model.Menu.MenuType;

import java.util.List;

final class ApplicationCommandRouter {
    private final ApplicationMenuRouter menuRouter = new ApplicationMenuRouter();

    String execute(ApplicationController application, String input, List<String> tokens) {
        if (tokens.isEmpty()) {
            return "Command is required";
        }
        try {
            String result = executeGlobalCommand(application, input);
            if (result != null) {
                return result;
            }
            return executeMenuSpecificCommand(application, input, tokens);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return e.getMessage();
        }
    }

    private String executeGlobalCommand(ApplicationController application, String input) {
        if (application.isNewsCommand(input)) {
            return application.executeNewsCommand(input);
        }
        if (application.isProfileCommand(input)) {
            return application.executeProfileCommand(input);
        }
        if (application.isLeaderboardCommand(input)) {
            return application.executeLeaderboardCommand(input);
        }
        if (application.isSettingCommand(input)) {
            return application.executeSettingCommand(input);
        }
        if (application.isGameCommand(input)) {
            return application.executeGameCommand(input);
        }
        if (application.isCollectionCommand(input)) {
            return application.executeCollectionCommand(input);
        }
        if (application.isTravelLogCommand(input) || application.hasOpenMiniGame()) {
            return application.executeTravelLogCommand(input);
        }
        return null;
    }

    private String executeMenuSpecificCommand(
            ApplicationController application,
            String input,
            List<String> tokens
    ) {
        String result = this.menuRouter.execute(application, tokens);
        if (result != null) {
            return result;
        }
        MenuType currentMenu = application.getMenuContext().getCurrentMenu();
        if (currentMenu == MenuType.CHAPTER_MENU) {
            return application.executeChapterCommand(tokens);
        }
        if (currentMenu == MenuType.MEOW_POINT_MENU
                && application.matches(tokens, "start", "game")) {
            return application.startMeowPointSelection();
        }
        if (currentMenu == MenuType.SIGNUP_MENU) {
            return application.executeSignupCommand(tokens);
        }
        if (currentMenu == MenuType.LOGIN_MENU) {
            return application.executeLoginCommand(tokens);
        }
        return executePlayCommand(application, input, currentMenu);
    }

    private String executePlayCommand(
            ApplicationController application,
            String input,
            MenuType currentMenu
    ) {
        if (currentMenu == MenuType.PLANT_PICK_MENU) {
            return application.executePlantPickCommand(input);
        }
        if (currentMenu == MenuType.MID_GAME_MENU) {
            return application.executeMidGameCommand(input);
        }
        if (currentMenu == MenuType.GREENHOUSE_MENU
                && (application.isGreenhouseCommand(input)
                || application.isShopCommand(input))) {
            return application.executeGreenhouseCommand(input);
        }
        return "Command is not available in " + currentMenu;
    }
}
