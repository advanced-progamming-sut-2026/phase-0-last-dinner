package model.Menu;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// masir haye mojaz beyn menu ha ro negah midare
public class GameMenuContext extends MenuContext {
    private final Map<MenuType, MenuState> states;
    private MenuType leaderboardExitDestination;
    private MenuType plantPickExitDestination;
    private MenuType midGameExitDestination;

    public GameMenuContext() {
        this.states = this.createStates();
        this.applicationRunning = true;
        this.loggedIn = false;
        this.currentState = this.states.get(MenuType.SIGNUP_MENU);
    }

    @Override
    public MenuType getCurrentMenu() {
        return this.currentState.getType();
    }

    @Override
    public boolean isApplicationRunning() {
        return this.applicationRunning;
    }

    @Override
    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    @Override
    public void enterMenu(MenuType destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Menu name is required");
        }

        if (destination == this.getCurrentMenu()) {
            return;
        }

        if (!this.currentState.canEnter(destination)) {
            throw new IllegalStateException("Cannot enter " + destination + " from " + this.getCurrentMenu());
        }

        if (destination == MenuType.MAIN_MENU && !this.loggedIn) {
            throw new IllegalStateException("Login is required before entering main menu");
        }

        if (destination == MenuType.LEADERBOARD_MENU) {
            this.leaderboardExitDestination = this.getCurrentMenu();
        }

        if (destination == MenuType.PLANT_PICK_MENU) {
            this.plantPickExitDestination = this.getCurrentMenu();
        }

        if (destination == MenuType.MID_GAME_MENU) {
            this.midGameExitDestination = this.getCurrentMenu() == MenuType.PLANT_PICK_MENU
                    && this.plantPickExitDestination != null
                    ? this.plantPickExitDestination
                    : this.getCurrentMenu();
            this.plantPickExitDestination = null;
        }

        this.changeState(this.states.get(destination));
    }

    @Override
    public void exitMenu() {
        MenuType currentMenu = this.getCurrentMenu();

        if (currentMenu == MenuType.SIGNUP_MENU) {
            this.applicationRunning = false;
            return;
        }

        if (currentMenu == MenuType.MAIN_MENU) {
            throw new IllegalStateException("Use logout to leave main menu");
        }

        MenuType destination;
        if (currentMenu == MenuType.LEADERBOARD_MENU && this.leaderboardExitDestination != null) {
            destination = this.leaderboardExitDestination;
        } else if (currentMenu == MenuType.PLANT_PICK_MENU && this.plantPickExitDestination != null) {
            destination = this.plantPickExitDestination;
        } else if (currentMenu == MenuType.MID_GAME_MENU && this.midGameExitDestination != null) {
            destination = this.midGameExitDestination;
        } else {
            destination = this.currentState.getExitDestination();
        }

        if (destination != null) {
            this.changeState(this.states.get(destination));
        }

        if (currentMenu == MenuType.LEADERBOARD_MENU) {
            this.leaderboardExitDestination = null;
        }
        if (currentMenu == MenuType.PLANT_PICK_MENU) {
            this.plantPickExitDestination = null;
        }
        if (currentMenu == MenuType.MID_GAME_MENU) {
            this.midGameExitDestination = null;
        }
    }

    public void finishGame(boolean won) {
        if (this.getCurrentMenu() != MenuType.MID_GAME_MENU) {
            return;
        }

        MenuType destination = won
                ? MenuType.MAIN_MENU
                : this.midGameExitDestination == null
                        ? MenuType.GAME_MENU
                        : this.midGameExitDestination;
        this.midGameExitDestination = null;
        this.changeState(this.states.get(destination));
    }

    @Override
    public void login() {
        this.loggedIn = true;
        this.changeState(this.states.get(MenuType.MAIN_MENU));
    }

    @Override
    public void logout() {
        this.loggedIn = false;
        this.leaderboardExitDestination = null;
        this.plantPickExitDestination = null;
        this.midGameExitDestination = null;
        this.changeState(this.states.get(MenuType.SIGNUP_MENU));
    }

    @Override
    protected void changeState(MenuState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("Menu state is required");
        }

        this.currentState = newState;
    }

    private Map<MenuType, MenuState> createStates() {
        // masire vorud va khoruje har menu ro misaze
        Map<MenuType, MenuState> result = new EnumMap<>(MenuType.class);
        this.addAccountStates(result);
        this.addMainStates(result);
        this.addGameStates(result);
        this.addLeafStates(result);
        return result;
    }

    private void addAccountStates(Map<MenuType, MenuState> statesByType) {
        statesByType.put(MenuType.SIGNUP_MENU, new State(
                MenuType.SIGNUP_MENU,
                list(MenuType.LOGIN_MENU),
                null
        ));
        statesByType.put(MenuType.LOGIN_MENU, new State(
                MenuType.LOGIN_MENU,
                list(MenuType.MAIN_MENU),
                MenuType.SIGNUP_MENU
        ));
    }

    private void addMainStates(Map<MenuType, MenuState> statesByType) {
        statesByType.put(MenuType.MAIN_MENU, new State(
                MenuType.MAIN_MENU,
                list(
                        MenuType.GAME_MENU,
                        MenuType.SETTINGS_MENU,
                        MenuType.NETWORK_MENU,
                        MenuType.NEWS_MENU,
                        MenuType.PROFILE_MENU,
                        MenuType.LEADERBOARD_MENU,
                        MenuType.MEOW_POINT_MENU
                ),
                null
        ));
        statesByType.put(MenuType.GAME_MENU, new State(
                MenuType.GAME_MENU,
                list(
                        MenuType.COLLECTION_MENU,
                        MenuType.GREENHOUSE_MENU,
                        MenuType.TRAVEL_LOG_MENU,
                        MenuType.LEADERBOARD_MENU,
                        MenuType.CHAPTER_MENU
                ),
                MenuType.MAIN_MENU
        ));
    }

    private void addGameStates(Map<MenuType, MenuState> statesByType) {
        statesByType.put(MenuType.COLLECTION_MENU, new State(
                MenuType.COLLECTION_MENU,
                Collections.<MenuType>emptyList(),
                MenuType.GAME_MENU
        ));
        statesByType.put(MenuType.GREENHOUSE_MENU, this.gameMenuChild(MenuType.GREENHOUSE_MENU));
        statesByType.put(MenuType.TRAVEL_LOG_MENU, this.gameMenuChild(MenuType.TRAVEL_LOG_MENU));
        statesByType.put(MenuType.LEADERBOARD_MENU, leaf(MenuType.LEADERBOARD_MENU));
        statesByType.put(MenuType.MEOW_POINT_MENU, new State(
                MenuType.MEOW_POINT_MENU,
                list(MenuType.PLANT_PICK_MENU),
                MenuType.MAIN_MENU
        ));
        statesByType.put(MenuType.CHAPTER_MENU, new State(
                MenuType.CHAPTER_MENU,
                list(MenuType.PLANT_PICK_MENU, MenuType.MID_GAME_MENU),
                MenuType.GAME_MENU
        ));
        statesByType.put(MenuType.PLANT_PICK_MENU, new State(
                MenuType.PLANT_PICK_MENU,
                list(MenuType.MID_GAME_MENU),
                MenuType.CHAPTER_MENU
        ));
        statesByType.put(MenuType.MID_GAME_MENU, new State(
                MenuType.MID_GAME_MENU,
                Collections.<MenuType>emptyList(),
                MenuType.GAME_MENU
        ));
    }

    private void addLeafStates(Map<MenuType, MenuState> statesByType) {
        statesByType.put(MenuType.SETTINGS_MENU, leaf(MenuType.SETTINGS_MENU));
        statesByType.put(MenuType.NETWORK_MENU, leaf(MenuType.NETWORK_MENU));
        statesByType.put(MenuType.NEWS_MENU, leaf(MenuType.NEWS_MENU));
        statesByType.put(MenuType.PROFILE_MENU, leaf(MenuType.PROFILE_MENU));
    }

    private State leaf(MenuType type) {
        // menu haye bedune zir menu ke zire MAIN_MENU hastan ro misaze
        return new State(type, Collections.<MenuType>emptyList(), MenuType.MAIN_MENU);
    }

    private State gameMenuChild(MenuType type) {
        // menu haye bedune zir menu ke zire GAME_MENU hastan ro misaze
        return new State(type, Collections.<MenuType>emptyList(), MenuType.GAME_MENU);
    }

    private static List<MenuType> list(MenuType... menuTypes) {
        return Collections.unmodifiableList(Arrays.asList(menuTypes));
    }

    // etelaate jabejayi marbut be yek menu ro negah midare
    private static final class State implements MenuState {
        private final MenuType type;
        private final List<MenuType> allowedDestinations;
        private final MenuType exitDestination;

        private State(MenuType type, List<MenuType> allowedDestinations, MenuType exitDestination) {
            this.type = type;
            this.allowedDestinations = allowedDestinations;
            this.exitDestination = exitDestination;
        }

        @Override
        public MenuType getType() {
            return this.type;
        }

        @Override
        public List<MenuType> getAllowedDestinations() {
            return this.allowedDestinations;
        }

        @Override
        public MenuType getExitDestination() {
            return this.exitDestination;
        }

        @Override
        public boolean canEnter(MenuType destination) {
            return this.allowedDestinations.contains(destination);
        }
    }
}
