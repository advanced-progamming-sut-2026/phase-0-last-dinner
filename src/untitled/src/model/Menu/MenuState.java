package model.Menu;

import java.util.List;

public interface MenuState {
    MenuType getType();

    List<MenuType> getAllowedDestinations();

    MenuType getExitDestination();

    boolean canEnter(MenuType destination);
}
