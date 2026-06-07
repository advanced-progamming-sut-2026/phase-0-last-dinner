package model;
import model.User.User;
import java.util.*;
import lombok.Getter;

@Getter


public class Repository {
    private ArrayList<String> securityQuestions;
    private ArrayList<User> users;
    private User currentUser;
}
