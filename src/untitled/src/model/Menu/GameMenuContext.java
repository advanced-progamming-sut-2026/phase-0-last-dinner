package model.Menu;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// masir haye mojaz beyn menu ha ro negah midare
public class GameMenuContext extends MenuContext {
    private final Map<MenuType, MenuState> states;

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

        MenuType destination = this.currentState.getExitDestination();

        if (destination != null) {
            this.changeState(this.states.get(destination));
        }
    }

    @Override
    public void login() {
        this.loggedIn = true;
        this.changeState(this.states.get(MenuType.MAIN_MENU));
    }

    @Override
    public void logout() {
        this.loggedIn = false;
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
        result.put(MenuType.SIGNUP_MENU, new State(
                MenuType.SIGNUP_MENU,
                list(MenuType.LOGIN_MENU),
                null
        ));
        result.put(MenuType.LOGIN_MENU, new State(
                MenuType.LOGIN_MENU,
                list(MenuType.MAIN_MENU),
                MenuType.SIGNUP_MENU
        ));
        result.put(MenuType.MAIN_MENU, new State(
                MenuType.MAIN_MENU,
                list(
                        MenuType.GAME_MENU,
                        MenuType.SETTINGS_MENU,
                        MenuType.NETWORK_MENU,
                        MenuType.NEWS_MENU,
                        MenuType.PROFILE_MENU
                ),
                null
        ));
        result.put(MenuType.GAME_MENU, new State(
                MenuType.GAME_MENU,
                list(
                        MenuType.COLLECTION_MENU,
                        MenuType.PLANT_PICK_MENU
                ),
                MenuType.MAIN_MENU
        ));
        result.put(MenuType.COLLECTION_MENU, new State(
                MenuType.COLLECTION_MENU,
                Collections.<MenuType>emptyList(),
                MenuType.GAME_MENU
        ));
        result.put(MenuType.PLANT_PICK_MENU, new State(
                MenuType.PLANT_PICK_MENU,
                list(MenuType.MID_GAME_MENU),
                MenuType.GAME_MENU
        ));
        result.put(MenuType.MID_GAME_MENU, new State(
                MenuType.MID_GAME_MENU,
                Collections.<MenuType>emptyList(),
                MenuType.GAME_MENU
        ));
        result.put(MenuType.SETTINGS_MENU, leaf(MenuType.SETTINGS_MENU));
        result.put(MenuType.NETWORK_MENU, leaf(MenuType.NETWORK_MENU));
        result.put(MenuType.NEWS_MENU, leaf(MenuType.NEWS_MENU));
        result.put(MenuType.PROFILE_MENU, leaf(MenuType.PROFILE_MENU));
        return result;
    }

    private State leaf(MenuType type) {
        // menu haye bedune zir menu ro misaze
        return new State(type, Collections.<MenuType>emptyList(), MenuType.MAIN_MENU);
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
