package controller;

import model.Menu.MenuType;

public class CollectionController implements MenuController{
    @Override
    public MenuType getCurrentMenu() {
        return null;
    }

    @Override
    public void changeMenu() {

    }
    public void showUnlockedPlants(){}
    public void showAllPlants(){}
    public void showUnlockedZombies(){}
    public void showAllZombies(){}
    public void showSpecificPlant(){}
    public void showSpecificZombie(){}
    public void upgradePlant(){}
    public void buyPlant(){}
}
