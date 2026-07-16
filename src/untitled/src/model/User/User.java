package model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import model.Plant;
import model.chapters.Chapter;
import model.Greenhouse.Greenhouse;
import model.GameMenuRelated.TravelLog;
import model.zombie.Zombie;

import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String username;
    private String hashedPassword;
    private String nickname;
    private String email;
    private int questionNum;
    private String securityAnswer;
    private UserGender gender;
    private boolean stayLoggedIn=false;
    private Greenhouse greenhouse;
    private TravelLog travelLog;
    private int diamond;
    private int gold;
    private Chapter chapter;
    private int level;
    private int difficultyLevel=3;
    private int completedMinigames;
    private int completedQuests;
    private int maxObtainedMeowPoints;
    private ArrayList<String> unreadNews;
    private ArrayList<String> allNews;
    private ArrayList<Plant> unlockedPlants;
    private ArrayList<Zombie> zombies;

    public User(
            String username,
            String hashedPassword,
            String nickname,
            String email,
            int questionNum,
            String securityAnswer,
            UserGender gender
    ) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.nickname = nickname;
        this.email = email;
        this.questionNum = questionNum;
        this.securityAnswer = securityAnswer;
        this.gender = gender;
        this.greenhouse = new Greenhouse();
        this.level = 1;
        this.difficultyLevel = 3;
        this.unreadNews = new ArrayList<>();
        this.allNews = new ArrayList<>();
        this.unlockedPlants = new ArrayList<>();
        this.zombies = new ArrayList<>();
    }
}
