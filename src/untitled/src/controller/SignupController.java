package controller;

import model.Menu.MenuType;

public class SignupController implements MenuController {
    public String authentication() {
        return "";
    }

    public String register() {
        return "";
    }

    private String hashing() {
        return "";
    }

    private boolean usernameValidation() {
        return true;
    }

    private boolean emailValidation() {
        return true;
    }

    private boolean passwordValidation() {
        return true;
    }

    private boolean displayNameValidation() {
        return true;
    }

    private boolean genderValidation() {
        return true;
    }

    @Override
    public MenuType getCurrentMenu() {
        return null;
    }

    @Override
    public void changeMenu() {

    }
}
