package controller;

import model.Menu.MenuType;
import model.User.User;

public class LoginController implements MenuController{
    private boolean passwordValidation(){return true;}
    public User findByUsername(){return null;}
    public String login(){return "";}
    private String passwordRecovery(){return "";}
    private boolean checkAnswer(){return true;}

    @Override
    public void changeMenu() {

    }

    @Override
    public MenuType getCurrentMenu() {
        return null;
    }
}
