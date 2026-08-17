package controller;

import lombok.AccessLevel;
import lombok.Getter;
import model.Greenhouse.GreenhouseBoostService;
import model.Menu.GameMenuContext;
import model.Menu.MenuType;
import model.Plant;
import model.User.AccountResult;
import model.User.AccountService;
import model.User.AccountStatus;
import model.User.User;
import model.User.UserRepository;
import model.chapters.Chapter;
import model.chapters.ChapterAncientEgypt;
import model.chapters.ChapterBigWaveBeach;
import model.chapters.ChapterIceCaves;
import model.chapters.ChapterMedieval;
import model.chapters.ChapterType;
import model.level.Level;
import model.level.LevelFactory;
import model.level.LevelType;
import model.mechanism.Board;
import model.mechanism.PlantZombieGame;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.plant.PlantUnlockService;
import model.plant.PlantUpgradeService;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import view.CommandHandler;
import view.GameCommand;
import view.GameView;
import view.LeaderBoardCommand;
import view.LeaderBoardView;
import view.MidGameView;
import view.NewsCommand;
import view.NewsView;
import view.PlantPickCommand;
import view.PlantPickView;
import view.ProfileCommand;
import view.ProfileView;
import view.SettingCommand;
import view.SettingView;
import view.collection.CollectionView;
import view.collection.CollectionCommands;
import view.greenhouse.GreenhouseCommands;
import view.greenhouse.GreenhouseView;
import view.travellog.TravelLogCommands;
import view.travellog.TravelLogView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// dastur haye matni ro be bakhshe marbut mifreste
@Getter
public class ApplicationController implements CommandHandler {
    private static final int BASE_WAVE_DIFFICULTY = 2400;
    @Getter(AccessLevel.NONE)
    private final ApplicationCommandParser commandParser;
    @Getter(AccessLevel.NONE)
    private final ApplicationCommandRouter commandRouter;
    @Getter(AccessLevel.NONE)
    private final ApplicationAccountCommandHandler accountCommandHandler;
    private final GameMenuContext menuContext;
    private final AccountService accountService;
    private final SignupController signupController;
    private final LoginController loginController;
    private final MainController mainController;
    private final ChapterController chapterController;
    private final GameController gameController;
    private final GameView gameView;
    private User currentUser;
    private String lastMessage;
    // اینا برا گل خونن
    private GreenhouseView greenhouseView;
    private User greenhouseViewUser;
    private TravelLogView travelLogView;
    private User travelLogViewUser;
    // اینا هم برا کالکشن
    private final PlantDefinitionRepository plantDefinitions;
    private final ZombieDefinitionRepository zombieDefinitions;
    private CollectionView collectionView;
    private User collectionViewUser;
    private NewsView newsView;
    private NewsController newsController;
    private User newsViewUser;
    private ProfileView profileView;
    private User profileViewUser;
    private SettingView settingView;
    private User settingViewUser;
    private PlantPickView plantPickView;
    private PlantPickController plantPickController;
    private User plantPickViewUser;
    private PlantZombieGame currentGame;
    private MidGameView midGameView;
    private LevelType pendingLevelType;

    public ApplicationController() {
        this(new UserRepository(), null, null);
    }
    public ApplicationController(UserRepository repository) {
        this(repository, null, null);
    }

    public ApplicationController(UserRepository repository, PlantDefinitionRepository plantDefinitions,
                                 ZombieDefinitionRepository zombieDefinitions) {
        this.menuContext = new GameMenuContext();
        this.commandParser = new ApplicationCommandParser();
        this.commandRouter = new ApplicationCommandRouter();
        this.accountService = new AccountService(repository);
        this.signupController = new SignupController(this.accountService, this.menuContext);
        this.loginController = new LoginController(this.accountService, this.menuContext);
        this.accountCommandHandler = new ApplicationAccountCommandHandler(
            this.commandParser, this.signupController, this.loginController);
        this.mainController = new MainController(this.accountService, this.menuContext);
        this.chapterController = new ChapterController(this.loginController);
        this.gameController = new GameController(
                this.loginController,
                repository,
                this.chapterController
        );
        this.gameView = new GameView();
        this.gameView.setObserver(this.gameController);
        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.currentUser = this.loginController.restoreRememberedLogin();
        this.restoreUnlockedPlantRuntimeData();
        this.lastMessage = "";
    }

    @Override
    public void handleCommand(String input) {
        this.lastMessage = this.execute(input);
    }

    public String execute(String input) {
        List<String> tokens = this.tokenize(input);
        return this.commandRouter.execute(this, input, tokens);
    }

    public MenuType getCurrentMenu() {
        return this.menuContext.getCurrentMenu();
    }

    public boolean isApplicationRunning() {
        return this.menuContext.isApplicationRunning();
    }

    public void close() {
        this.accountService.save();
    }
    public AccountResult registerUser(String username, String password, String passwordConfirm, String nickname,
                                      String email, String gender, int securityQuestionNumber, String securityAnswer,
                                      String securityAnswerConfirm) {
        AccountResult registrationResult = this.signupController.register(username, password, passwordConfirm, nickname,
            email, gender);

        if (registrationResult.getStatus() != AccountStatus.SECURITY_QUESTION_REQUIRED) {
            return registrationResult;
        }
        if (registrationResult.getStatus() != AccountStatus.SECURITY_QUESTION_REQUIRED)
            return registrationResult;

        return this.signupController.pickQuestion(securityQuestionNumber, securityAnswer, securityAnswerConfirm);
    }

    public List<String> getSecurityQuestions() {
        return this.signupController.getSecurityQuestions();
    }

    public void cancelRegistration() {
        this.signupController.cancelPendingRegistration();
    }

    public AccountResult loginUser(String username, String password, boolean stayLoggedIn) {
        this.loginController.cancelPendingPasswordRecovery();

        AccountResult result = this.loginController.login(username, password, stayLoggedIn);

        if (result.isSuccessful()) {
            this.currentUser = result.getUser();
            this.restoreUnlockedPlantRuntimeData();
            this.clearUserConnections();
        }

        return result;
    }

    public AccountResult beginPasswordRecovery(String username, String email) {
        return this.loginController.beginPasswordRecovery(username, email);
    }

    public AccountResult answerPasswordRecoveryQuestion(String answer) {
        return this.loginController.answerSecurityQuestion(answer);
    }

    public AccountResult completePasswordRecovery(String password, String passwordConfirm) {
        return this.loginController.setNewPassword(password, passwordConfirm);
    }

    public void cancelPasswordRecovery() {
        this.loginController.cancelPendingPasswordRecovery();
    }

    boolean hasOpenMiniGame() {
        return this.menuContext.getCurrentMenu() == MenuType.TRAVEL_LOG_MENU && this.travelLogView != null
            && this.travelLogView.isMiniGameOpen();
    }

    String startMeowPointSelection() {
        this.pendingLevelType = LevelType.MEOW_POINT;
        this.menuContext.enterMenu(MenuType.PLANT_PICK_MENU);
        return this.menuContext.getCurrentMenu().name();
    }

    void cancelPendingAccountAction(MenuType previousMenu) {
        if (previousMenu == MenuType.SIGNUP_MENU) {
            this.signupController.cancelPendingRegistration();
        } else if (previousMenu == MenuType.LOGIN_MENU) {
            this.loginController.cancelPendingPasswordRecovery();
        }
    }

    String logoutFromMainMenu() {
        if (this.menuContext.getCurrentMenu() != MenuType.MAIN_MENU) {
            return "Logout is only available in main menu";
        }
        this.currentUser = null;
        this.clearUserConnections();
        return this.mainController.logout();
    }

    String executeSignupCommand(List<String> tokens) {
        return this.accountCommandHandler.executeSignupCommand(tokens);
    }

    String executeLoginCommand(List<String> tokens) {
        if ("login".equalsIgnoreCase(tokens.get(0))) {
            return this.login(tokens);
        }
        return this.accountCommandHandler.executeRecoveryCommand(tokens);
    }

    private String login(List<String> tokens) {
        String username = this.valueAfter(tokens, "-u", 1);
        String password = this.valueAfter(tokens, "-p", 1);

        if (this.hasMissingValue(username, password)) {
            return "Login command is incomplete";
        }
        if (this.hasMissingValue(username, password))
            return "Login command is incomplete";

        AccountResult result = this.loginUser(username, password,
            this.containsIgnoreCase(tokens, "-stay-logged-in"));

        return result.getMessage();
    }

    String executeChapterCommand(List<String> tokens) {
        if (this.matches(tokens, "show", "levels")) {
            return this.chapterController.getAvailableLevels().toString();
        }
        if (this.startsWith(tokens, "select", "level")) {
            String levelName = this.valueAfter(tokens, "-t", 1);
            if (levelName == null) {
                return "Level type is required.";
            }
            LevelType levelType = this.parseLevelType(levelName);
            if (!this.chapterController.selectLevel(levelType)) {
                return "Level is not available in this chapter.";
            }
            if (levelType == LevelType.CONVEYOR_BELT) {
                this.startSelectedGame();
                this.accountService.save();
            }
            return this.menuContext.getCurrentMenu().name();
        }
        return "Invalid chapter command";
    }

    private LevelType parseLevelType(String value) {
        return this.commandParser.parseLevelType(value);
    }

    MenuType parseMenuType(String value) {
        return this.commandParser.parseMenuType(value);
    }

    private String valueAfter(List<String> tokens, String flag, int offset) {
        return this.commandParser.valueAfter(tokens, flag, offset);
    }

    private boolean hasMissingValue(String... values) {
        return this.commandParser.hasMissingValue(values);
    }

    boolean matches(List<String> tokens, String... expected) {
        return this.commandParser.matches(tokens, expected);
    }

    private boolean startsWith(List<String> tokens, String... expected) {
        return this.commandParser.startsWith(tokens, expected);
    }

    private boolean containsIgnoreCase(List<String> tokens, String value) {
        return this.commandParser.containsIgnoreCase(tokens, value);
    }

    String join(List<String> tokens, int startIndex) {
        return this.commandParser.join(tokens, startIndex);
    }

    private List<String> tokenize(String input) {
        return this.commandParser.tokenize(input);
    }

    boolean isGreenhouseCommand(String input) {
        return ApplicationCommandMatchers.isGreenhouseCommand(input);
    }

    String executeGreenhouseCommand(String input) {
        if (currentUser == null) {
            return "Login is required.";
        }
        ensureGreenhouseConnected();
        greenhouseView.handleCommand(input);
        accountService.save();
        return "";
    }

    private void ensureGreenhouseConnected() {
        if (greenhouseView != null && greenhouseViewUser == currentUser) {
            return;
        }
        currentUser.initializeMissingFields();
        this.restoreUnlockedPlantRuntimeData();
        greenhouseView = new GreenhouseView();
        new GreenhouseController(greenhouseView, currentUser, currentUser.getPlantUpgradeService());
        greenhouseViewUser = currentUser;
    }

    boolean isTravelLogCommand(String input) {
        return ApplicationCommandMatchers.isTravelLogCommand(input);
    }

    String executeTravelLogCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }
        if (this.menuContext.getCurrentMenu() != MenuType.TRAVEL_LOG_MENU) {
            return "Travel Log commands are only available in Travel Log menu.";
        }
        boolean backToGame = TravelLogCommands.BACK_TO_GAME.getMatcher(input) != null;
        this.ensureTravelLogConnected();
        this.travelLogView.handleCommand(input);
        this.accountService.save();
        if (backToGame) {
            this.menuContext.exitMenu();
            this.travelLogView = null;
            this.travelLogViewUser = null;
        }
        return "";
    }

    private void ensureTravelLogConnected() {
        if (this.travelLogView != null && this.travelLogViewUser == this.currentUser) {
            return;
        }
        this.currentUser.initializeMissingFields();
        this.travelLogView = new TravelLogView();
        new TravelLogController(this.travelLogView, this.currentUser.getTravelLog(),
            this.currentUser, this.plantDefinitions);
        this.travelLogViewUser = this.currentUser;
    }

    boolean isShopCommand(String input) {
        return ApplicationCommandMatchers.isShopCommand(input);
    }

    public void save() {
        this.accountService.save();
    }

    public void finishGraphicalGame(boolean won) {
        this.accountService.save();
        if (this.menuContext.getCurrentMenu() == MenuType.MID_GAME_MENU) {
            this.menuContext.finishGame(won);
        }
        this.clearGameConnections();
    }

    boolean isCollectionCommand(String input) {
        return ApplicationCommandMatchers.isCollectionCommand(input);
    }

    String executeCollectionCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }
        if (this.menuContext.getCurrentMenu() != MenuType.COLLECTION_MENU) {
            return "Collection commands are only available in collection menu.";
        }
        if (this.plantDefinitions == null || this.zombieDefinitions == null) {
            return "Collection definitions are not available.";
        }
        this.ensureCollectionConnected();
        this.collectionView.handleCommand(input);
        this.accountService.save();
        return "";
    }

    private void ensureCollectionConnected() {
        if (this.collectionView != null && this.collectionViewUser == this.currentUser) {
            return;
        }
        this.currentUser.initializeMissingFields();
        this.collectionView = new CollectionView();
        new CollectionController(this.collectionView, this.currentUser,
            this.plantDefinitions, this.zombieDefinitions);
        this.collectionViewUser = this.currentUser;
    }

    boolean isNewsCommand(String input) {
        return ApplicationCommandMatchers.isNewsCommand(input);
    }

    String executeNewsCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }
        if (this.menuContext.getCurrentMenu() != MenuType.NEWS_MENU) {
            return "News commands are only available in news menu.";
        }
        this.ensureNewsConnected();
        this.newsView.handleCommand(input);
        this.accountService.save();
        return "";
    }

    private void ensureNewsConnected() {
        if (this.newsView != null && this.newsViewUser == this.currentUser) {
            return;
        }
        this.newsView = new NewsView();
        this.newsController = new NewsController(this.newsView, this.currentUser);
        this.newsViewUser = this.currentUser;
    }

    // barresi UI graphical (masalan NewsDialog) mostaghim az in estefade mikone,
    // chon NewsView.handleCommand faghat toye console print mikone va chizi barnemigardune
    public NewsController getOrCreateNewsController() {
        if (this.currentUser == null) {
            return new NewsController();
        }
        this.ensureNewsConnected();
        return this.newsController;
    }

    boolean isProfileCommand(String input) {
        return ApplicationCommandMatchers.isProfileCommand(input);
    }

    String executeProfileCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }
        if (this.menuContext.getCurrentMenu() != MenuType.PROFILE_MENU) {
            return "Profile commands are only available in profile menu.";
        }
        this.ensureProfileConnected();
        this.profileView.handleCommand(input);
        return "";
    }

    private void ensureProfileConnected() {
        if (this.profileView != null && this.profileViewUser == this.currentUser) {
            return;
        }
        this.profileView = new ProfileView();
        new ProfileController(this.profileView, this.accountService, this.currentUser);
        this.profileViewUser = this.currentUser;
    }

    boolean isLeaderboardCommand(String input) {
        return LeaderBoardCommand.SHOW.getMatcher(input) != null;
    }

    boolean isGameCommand(String input) {
        return ApplicationCommandMatchers.isGameCommand(input);
    }

    String executeGameCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }
        if (this.menuContext.getCurrentMenu() != MenuType.GAME_MENU) {
            return "Game menu command is only available in game menu.";
        }
        this.gameView.handleCommand(input);
        this.accountService.save();
        if (GameCommand.ENTER_CHAPTER.getMatcher(input) != null) {
            return this.menuContext.getCurrentMenu().name();
        }
        return "";
    }

    boolean isSettingCommand(String input) {
        return SettingCommand.CHANGE_DIFFICULTY.getMatcher(input) != null;
    }

    String executeSettingCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }
        if (this.menuContext.getCurrentMenu() != MenuType.SETTINGS_MENU) {
            return "Settings commands are only available in settings menu.";
        }
        this.ensureSettingConnected();
        String result = this.settingView.handleCommand(input);
        this.accountService.save();
        return result;
    }

    public SettingView getOrCreateSettingView() {
        this.ensureSettingConnected();
        return this.settingView;
    }

    private void ensureSettingConnected() {
        if (this.settingView != null && this.settingViewUser == this.currentUser) {
            return;
        }
        this.settingView = new SettingView();
        this.settingView.setObserver(new SettingController(this.currentUser));
        this.settingViewUser = this.currentUser;
    }

    String executeLeaderboardCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }
        MenuType currentMenu = this.menuContext.getCurrentMenu();
        if (currentMenu != MenuType.MAIN_MENU && currentMenu != MenuType.GAME_MENU
            && currentMenu != MenuType.LEADERBOARD_MENU) {
            return "Leaderboard is only available from main or game menu.";
        }
        LeaderBoardView view = new LeaderBoardView();
        new LeaderBoardController(view, this.accountService);
        view.handleCommand(input);
        return "";
    }

    String executePlantPickCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }
        if (this.plantDefinitions == null || this.zombieDefinitions == null) {
            return "Plant and zombie definitions are not available.";
        }
        this.ensurePlantPickConnected();
        boolean startCommand = PlantPickCommand.START_GAME.getMatcher(input) != null;
        this.plantPickView.handleCommand(input);
        if (startCommand && this.plantPickController.isStarted()) {
            this.startSelectedGame();
        }
        this.accountService.save();
        return "";
    }

    private void ensurePlantPickConnected() {
        if (this.plantPickView != null && this.plantPickViewUser == this.currentUser) {
            return;
        }
        this.currentUser.initializeMissingFields();
        this.restoreUnlockedPlantRuntimeData();
        this.plantPickView = new PlantPickView();
        this.plantPickController = new PlantPickController(this.plantPickView, this.currentUser,
            this.plantDefinitions, null, PlantPickController.DEFAULT_SLOT_COUNT);
        this.plantPickViewUser = this.currentUser;
    }

    private void startSelectedGame() {
        ApplicationStartedGame startedGame = ApplicationGameStarter.start(this.currentUser, this.plantDefinitions,
            this.zombieDefinitions, this.plantPickController, this.pendingLevelType,
            this.chapterController.getSelectedChapter(), this.chapterController.getSelectedLevel(),
            this.accountService, BASE_WAVE_DIFFICULTY);
        this.currentGame = startedGame.getGame();
        this.midGameView = startedGame.getView();
        this.menuContext.enterMenu(MenuType.MID_GAME_MENU);
    }

    String executeMidGameCommand(String input) {
        if (this.currentGame == null || this.midGameView == null) {
            return "Game is not available.";
        }
        this.midGameView.handleCommand(input);
        this.accountService.save();
        if (!this.currentGame.getEngine().isGameRunning()) {
            boolean won = this.currentGame.getActiveLevel() != null && this.currentGame.getActiveLevel().isCompleted();
            this.menuContext.finishGame(won);
            this.clearGameConnections();
        }
        return "";
    }

    private void restoreUnlockedPlantRuntimeData() {
        ApplicationPlantRuntime.restoreUnlockedPlants(this.currentUser, this.plantDefinitions);
    }

    private void clearUserConnections() {
        this.greenhouseView = null;
        this.greenhouseViewUser = null;
        this.travelLogView = null;
        this.travelLogViewUser = null;
        this.collectionView = null;
        this.collectionViewUser = null;
        this.newsView = null;
        this.newsController = null;
        this.newsViewUser = null;
        this.profileView = null;
        this.profileViewUser = null;
        this.settingView = null;
        this.settingViewUser = null;
        this.clearGameConnections();
    }

    void clearGameConnections() {
        this.plantPickView = null;
        this.plantPickController = null;
        this.plantPickViewUser = null;
        this.currentGame = null;
        this.midGameView = null;
        this.pendingLevelType = null;
    }
}

final class ApplicationAccountCommandHandler {
    private final ApplicationCommandParser commandParser;
    private final SignupController signupController;
    private final LoginController loginController;

    ApplicationAccountCommandHandler(
        ApplicationCommandParser commandParser,
        SignupController signupController,
        LoginController loginController
    ) {
        this.commandParser = commandParser;
        this.signupController = signupController;
        this.loginController = loginController;
    }

    String executeSignupCommand(List<String> tokens) {
        if ("register".equalsIgnoreCase(tokens.get(0))) {
            return this.register(tokens);
        }
        if (this.commandParser.startsWith(tokens, "pick", "question")) {
            return this.pickQuestion(tokens);
        }
        return "Invalid signup command";
    }

    String executeRecoveryCommand(List<String> tokens) {
        if (this.commandParser.startsWith(tokens, "forget", "password")) {
            return this.beginPasswordRecovery(tokens);
        }
        if ("answer".equalsIgnoreCase(tokens.get(0))) {
            return this.answerSecurityQuestion(tokens);
        }
        if (this.commandParser.startsWith(tokens, "new", "password")) {
            return this.setNewPassword(tokens);
        }
        return "Invalid login command";
    }

    private String register(List<String> tokens) {
        this.signupController.cancelPendingRegistration();
        String username = this.commandParser.valueAfter(tokens, "-u", 1);
        String password = this.commandParser.valueAfter(tokens, "-p", 1);
        String passwordConfirm = this.commandParser.valueAfter(tokens, "-p", 2);
        String nickname = this.commandParser.valueAfter(tokens, "-n", 1);
        String email = this.commandParser.valueAfter(tokens, "-e", 1);
        String gender = this.commandParser.valueAfter(tokens, "-g", 1);
        if (this.commandParser.hasMissingValue(username, password, passwordConfirm, nickname, email, gender)) {
            return "Register command is incomplete";
        }
        AccountResult result = this.signupController.register(
            username, password, passwordConfirm, nickname, email, gender
        );
        if (result.getStatus() == AccountStatus.SECURITY_QUESTION_REQUIRED) {
            return this.formatQuestions(result.getMessage());
        }
        return result.getMessage();
    }

    private String pickQuestion(List<String> tokens) {
        String questionValue = this.commandParser.valueAfter(tokens, "-q", 1);
        String answer = this.commandParser.valueAfter(tokens, "-a", 1);
        String answerConfirm = this.commandParser.valueAfter(tokens, "-c", 1);
        if (this.commandParser.hasMissingValue(questionValue, answer, answerConfirm)) {
            return "Pick question command is incomplete";
        }
        try {
            int questionNumber = Integer.parseInt(questionValue);
            return this.signupController.pickQuestion(questionNumber, answer, answerConfirm).getMessage();
        } catch (NumberFormatException e) {
            return "Security question number is invalid";
        }
    }

    private String beginPasswordRecovery(List<String> tokens) {
        this.loginController.cancelPendingPasswordRecovery();
        String username = this.commandParser.valueAfter(tokens, "-u", 1);
        String email = this.commandParser.valueAfter(tokens, "-e", 1);
        if (this.commandParser.hasMissingValue(username, email)) {
            return "Forget password command is incomplete";
        }
        AccountResult result = this.loginController.beginPasswordRecovery(username, email);
        return result.isSuccessful()
            ? result.getMessage() + System.lineSeparator() + result.getSecurityQuestion()
            : result.getMessage();
    }

    private String answerSecurityQuestion(List<String> tokens) {
        String answer = this.commandParser.valueAfter(tokens, "-a", 1);
        if (answer == null) {
            return "Answer is required";
        }
        return this.loginController.answerSecurityQuestion(answer).getMessage();
    }

    private String setNewPassword(List<String> tokens) {
        String password = this.commandParser.valueAfter(tokens, "-p", 1);
        String passwordConfirm = this.commandParser.valueAfter(tokens, "-p", 2);
        if (this.commandParser.hasMissingValue(password, passwordConfirm)) {
            return "New password command is incomplete";
        }
        return this.loginController.setNewPassword(password, passwordConfirm).getMessage();
    }

    private String formatQuestions(String message) {
        StringBuilder result = new StringBuilder(message);
        List<String> questions = this.signupController.getSecurityQuestions();
        for (int i = 0; i < questions.size(); i++) {
            result.append(System.lineSeparator()).append(i + 1).append(" ").append(questions.get(i));
        }
        return result.toString();
    }
}

final class ApplicationCommandMatchers {
    private ApplicationCommandMatchers() {
    }

    static boolean isGreenhouseCommand(String input) {
        for (GreenhouseCommands command : GreenhouseCommands.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }
        return false;
    }

    static boolean isTravelLogCommand(String input) {
        for (TravelLogCommands command : TravelLogCommands.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }
        return false;
    }

    static boolean isShopCommand(String input) {
        if (input == null) {
            return false;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("shop") || normalized.startsWith("shop ");
    }

    static boolean isCollectionCommand(String input) {
        for (CollectionCommands command : CollectionCommands.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }
        return false;
    }

    static boolean isNewsCommand(String input) {
        for (NewsCommand command : NewsCommand.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }
        return false;
    }

    static boolean isProfileCommand(String input) {
        for (ProfileCommand command : ProfileCommand.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }
        return false;
    }

    static boolean isGameCommand(String input) {
        for (GameCommand command : GameCommand.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }
        return false;
    }
}

final class ApplicationPlantRuntime {
    private ApplicationPlantRuntime() {
    }

    static Chapter createChapter(ChapterType chapterType) {
        if (chapterType == null) {
            return null;
        }
        switch (chapterType) {
            case ANCIENT_EGYPT:
                return new ChapterAncientEgypt();
            case ICE_CAVES:
                return new ChapterIceCaves();
            case BIG_WAVE_BEACH:
                return new ChapterBigWaveBeach();
            case MEDIEVAL:
                return new ChapterMedieval();
            default:
                return null;
        }
    }

    static void restoreUnlockedPlants(User user, PlantDefinitionRepository plantDefinitions) {
        if (user == null || plantDefinitions == null) {
            return;
        }
        user.initializeMissingFields();
        ArrayList<Plant> restoredPlants = new ArrayList<>();
        PlantFactory plantFactory = new PlantFactory(user.getPlantUpgradeService());
        for (Plant plant : user.getUnlockedPlants()) {
            if (plant == null || plant.getName() == null) {
                continue;
            }
            PlantDefinition definition = findPlantDefinition(plantDefinitions, plant.getName());
            restoredPlants.add(definition == null ? plant : plantFactory.create(definition));
        }
        user.setUnlockedPlants(restoredPlants);
        PlantDefinition starterPlant = findPlantDefinition(plantDefinitions, "Peashooter");
        if (starterPlant != null) {
            PlantUnlockService.unlock(user, plantFactory.create(starterPlant));
        }
    }

    private static PlantDefinition findPlantDefinition(
        PlantDefinitionRepository plantDefinitions,
        String plantName
    ) {
        if (plantName == null || plantDefinitions.findAll() == null) {
            return null;
        }
        for (PlantDefinition definition : plantDefinitions.findAll()) {
            if (definition != null && definition.getName() != null
                && definition.getName().equalsIgnoreCase(plantName.trim())) {
                return definition;
            }
        }
        return null;
    }
}

final class ApplicationGameStarter {
    private ApplicationGameStarter() {
    }

    static ApplicationStartedGame start(User user, PlantDefinitionRepository plantDefinitions,
                                        ZombieDefinitionRepository zombieDefinitions,
                                        PlantPickController plantPickController, LevelType pendingLevelType,
                                        ChapterType selectedChapter, LevelType selectedLevel,
                                        AccountService accountService, int baseWaveDifficulty) {
        user.initializeMissingFields();
        user.setGamesPlayed(user.getGamesPlayed() + 1);
        Chapter chapter = pendingLevelType == LevelType.MEOW_POINT
            ? null
            : ApplicationPlantRuntime.createChapter(selectedChapter);
        Board board = chapter == null ? new Board() : chapter.buildBoard();
        PlantZombieGame game = new PlantZombieGame(
            plantDefinitions,
            zombieDefinitions,
            new ZombieFactory(zombieDefinitions),
            user.getPlantUpgradeService(),
            new GreenhouseBoostService(user.getGreenhouse()),
            user,
            board
        );
        if (plantPickController != null) {
            game.configurePlantSelection(
                plantPickController.getSelectedPlants(),
                plantPickController.getBoostedPlantNames()
            );
        }
        LevelType levelType = pendingLevelType != null
            ? pendingLevelType
            : selectedLevel == null ? LevelType.NORMAL : selectedLevel;
        Level level = new LevelFactory().create(
            levelType,
            null,
            user.getUnlockedPlants(),
            baseWaveDifficulty,
            game.getEngine().getClock()
        );
        game.configureChapter(chapter);
        game.configureLevel(level);
        game.getBoard().setZombieEncounterListener(definition -> {
            if (definition != null && user.recordEncounteredZombie(definition.getAlias())) {
                accountService.save();
            }
        });
        transferStoredPlantFood(user, game);
        MidGameView view = new MidGameView();
        game.setEventListener(view);
        view.setObserver(new MidGameController(game));
        return new ApplicationStartedGame(game, view);
    }

    private static void transferStoredPlantFood(User user, PlantZombieGame game) {
        int storedPlantFood = Math.max(0, Math.min(3, user.getNextLevelPlantFood()));
        int transferredPlantFood = 0;
        for (int i = 0; i < storedPlantFood; i++) {
            if (!game.getPlantFoodSystem().addPlantFood()) {
                break;
            }
            transferredPlantFood++;
        }
        user.setNextLevelPlantFood(storedPlantFood - transferredPlantFood);
    }
}

final class ApplicationStartedGame {
    private final PlantZombieGame game;
    private final MidGameView view;

    ApplicationStartedGame(PlantZombieGame game, MidGameView view) {
        this.game = game;
        this.view = view;
    }

    PlantZombieGame getGame() {
        return this.game;
    }

    MidGameView getView() {
        return this.view;
    }
}
