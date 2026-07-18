package model;
import lombok.Setter;
import model.User.User;
import java.util.*;
import lombok.Getter;

@Setter
@Getter
public class Repository {
    private ArrayList<String> securityQuestions;
    private ArrayList<User> users;
    private User currentUser;

    public Repository(ArrayList<String> securityQuestions, ArrayList<User> users, User currentUser) {
        this.securityQuestions = securityQuestions;
        this.users = users;
        this.currentUser = currentUser;
    }
}
