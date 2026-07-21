package controller;

import lombok.Getter;
import model.Greenhouse.GreenhouseBoostService;
import model.Menu.GameMenuContext;
import model.Menu.MenuType;
import model.User.AccountResult;
import model.User.AccountService;
import model.User.AccountStatus;
import model.User.User;
import model.User.UserRepository;
import model.mechanism.PlantZombieGame;
import model.plant.PlantDefinitionRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// dastur haye matni ro be bakhshe marbut mifreste
@Getter
public class ApplicationController implements CommandHandler {
    // matne dakhele quote ro yek token hesab mikone
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");

    private final GameMenuContext menuContext;
    private final AccountService accountService;
    private final SignupController signupController;
    private final LoginController loginController;
    private final MainController mainController;
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

    public ApplicationController() {
        this(new UserRepository(), null, null);
    }

    public ApplicationController(UserRepository repository) {
        this(repository, null, null);
    }

    public ApplicationController(
            UserRepository repository,
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions
    ) {
        this.menuContext = new GameMenuContext();
        this.accountService = new AccountService(repository);
        this.signupController = new SignupController(this.accountService, this.menuContext);
        this.loginController = new LoginController(this.accountService, this.menuContext);
        this.mainController = new MainController(this.accountService, this.menuContext);
        this.gameView = new GameView();
        this.gameView.setObserver(new GameController(
                this.loginController,
                repository,
                new ChapterController(this.loginController)
        ));
        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.currentUser = this.loginController.restoreRememberedLogin();
        this.lastMessage = "";
    }

    @Override
    public void handleCommand(String input) {
        this.lastMessage = this.execute(input);
    }

    public String execute(String input) {
        List<String> tokens = this.tokenize(input);

        if (tokens.isEmpty()) {
            return "Command is required";
        }

        try {
            if (this.isNewsCommand(input)) {
                return this.executeNewsCommand(input);
            }

            if (this.isProfileCommand(input)) {
                return this.executeProfileCommand(input);
            }

            if (this.isLeaderboardCommand(input)) {
                return this.executeLeaderboardCommand(input);
            }

            if (this.isSettingCommand(input)) {
                return this.executeSettingCommand(input);
            }

            if (this.isGameCommand(input)) {
                return this.executeGameCommand(input);
            }

            if (this.isCollectionCommand(input)) {
                return this.executeCollectionCommand(input);
            }

            if (this.isTravelLogCommand(input)) {
                return this.executeTravelLogCommand(input);
            }

            String menuResult = this.executeMenuCommand(tokens);

            if (menuResult != null) {
                return menuResult;
            }

            if (this.menuContext.getCurrentMenu() == MenuType.SIGNUP_MENU) {
                return this.executeSignupCommand(tokens);
            }

            if (this.menuContext.getCurrentMenu() == MenuType.LOGIN_MENU) {
                return this.executeLoginCommand(tokens);
            }

            if (this.menuContext.getCurrentMenu() == MenuType.PLANT_PICK_MENU) {
                return this.executePlantPickCommand(input);
            }

            if (this.menuContext.getCurrentMenu() == MenuType.MID_GAME_MENU) {
                return this.executeMidGameCommand(input);
            }

            if (this.menuContext.getCurrentMenu()
                    == MenuType.GREENHOUSE_MENU
                    && isGreenhouseCommand(input)) {

                return executeGreenhouseCommand(input);
            }

            if (this.menuContext.getCurrentMenu()
                    == MenuType.GREENHOUSE_MENU
                    && (isGreenhouseCommand(input)
                    || isShopCommand(input))) {

                return executeGreenhouseCommand(input);
            }

            return "Command is not available in " + this.menuContext.getCurrentMenu();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return e.getMessage();
        }
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

    private String executeMenuCommand(List<String> tokens) {
        if (!"menu".equalsIgnoreCase(tokens.get(0))) {
            return null;
        }

        if (this.matches(tokens, "menu", "show", "current")) {
            return this.menuContext.getCurrentMenu().name();
        }

        if (this.matches(tokens, "menu", "exit")) {
            MenuType previousMenu = this.menuContext.getCurrentMenu();
            this.menuContext.exitMenu();

            if (previousMenu == MenuType.PLANT_PICK_MENU
                    || previousMenu == MenuType.MID_GAME_MENU) {
                this.clearGameConnections();
            }

            return this.menuContext.isApplicationRunning()
                    ? this.menuContext.getCurrentMenu().name()
                    : "Application closed";
        }

        if (this.matches(tokens, "menu", "logout")) {
            if (this.menuContext.getCurrentMenu()
                    != MenuType.MAIN_MENU) {

                return "Logout is only available in main menu";
            }

            this.currentUser = null;
            clearGreenhouseConnection();
            clearCollectionConnection();
            clearNewsConnection();
            clearProfileConnection();
            clearSettingConnection();
            clearTravelLogConnection();
            clearGameConnections();

            return this.mainController.logout();
        }

        if (tokens.size() >= 3 && "enter".equalsIgnoreCase(tokens.get(1))) {
            MenuType destination = this.parseMenuType(this.join(tokens, 2));

            if (destination == MenuType.MID_GAME_MENU) {
                return "Use start game from plant pick menu";
            }

            this.menuContext.enterMenu(destination);
            return this.menuContext.getCurrentMenu().name();
        }

        return "Invalid menu command";
    }

    private String executeSignupCommand(List<String> tokens) {
        if ("register".equalsIgnoreCase(tokens.get(0))) {
            return this.register(tokens);
        }

        if (this.startsWith(tokens, "pick", "question")) {
            return this.pickQuestion(tokens);
        }

        return "Invalid signup command";
    }

    private String executeLoginCommand(List<String> tokens) {
        if ("login".equalsIgnoreCase(tokens.get(0))) {
            return this.login(tokens);
        }

        if (this.startsWith(tokens, "forget", "password")) {
            return this.beginPasswordRecovery(tokens);
        }

        if ("answer".equalsIgnoreCase(tokens.get(0))) {
            return this.answerSecurityQuestion(tokens);
        }

        if (this.startsWith(tokens, "new", "password")) {
            return this.setNewPassword(tokens);
        }

        return "Invalid login command";
    }

    private String register(List<String> tokens) {
        String username = this.valueAfter(tokens, "-u", 1);
        String password = this.valueAfter(tokens, "-p", 1);
        String passwordConfirm = this.valueAfter(tokens, "-p", 2);
        String nickname = this.valueAfter(tokens, "-n", 1);
        String email = this.valueAfter(tokens, "-e", 1);
        String gender = this.valueAfter(tokens, "-g", 1);

        if (this.hasMissingValue(username, password, passwordConfirm, nickname, email, gender)) {
            return "Register command is incomplete";
        }

        AccountResult result = this.signupController.register(
                username,
                password,
                passwordConfirm,
                nickname,
                email,
                gender
        );

        if (result.getStatus() == AccountStatus.SECURITY_QUESTION_REQUIRED) {
            return this.formatQuestions(result.getMessage());
        }

        return result.getMessage();
    }

    private String pickQuestion(List<String> tokens) {
        String questionValue = this.valueAfter(tokens, "-q", 1);
        String answer = this.valueAfter(tokens, "-a", 1);
        String answerConfirm = this.valueAfter(tokens, "-c", 1);

        if (this.hasMissingValue(questionValue, answer, answerConfirm)) {
            return "Pick question command is incomplete";
        }

        try {
            int questionNumber = Integer.parseInt(questionValue);
            return this.signupController.pickQuestion(questionNumber, answer, answerConfirm).getMessage();
        } catch (NumberFormatException e) {
            return "Security question number is invalid";
        }
    }

    private String login(List<String> tokens) {
        String username = this.valueAfter(tokens, "-u", 1);
        String password = this.valueAfter(tokens, "-p", 1);

        if (this.hasMissingValue(username, password)) {
            return "Login command is incomplete";
        }

        AccountResult result = this.loginController.login(
                username,
                password,
                this.containsIgnoreCase(tokens, "-stay-logged-in")
        );

        if (result.isSuccessful()) {
            this.currentUser = result.getUser();
            clearGreenhouseConnection();
            clearCollectionConnection();
            clearNewsConnection();
            clearProfileConnection();
            clearSettingConnection();
            clearTravelLogConnection();
            clearGameConnections();
        }

        return result.getMessage();
    }

    private String beginPasswordRecovery(List<String> tokens) {
        String username = this.valueAfter(tokens, "-u", 1);
        String email = this.valueAfter(tokens, "-e", 1);

        if (this.hasMissingValue(username, email)) {
            return "Forget password command is incomplete";
        }

        AccountResult result = this.loginController.beginPasswordRecovery(username, email);
        return result.isSuccessful()
                ? result.getMessage() + System.lineSeparator() + result.getSecurityQuestion()
                : result.getMessage();
    }

    private String answerSecurityQuestion(List<String> tokens) {
        String answer = this.valueAfter(tokens, "-a", 1);

        if (answer == null) {
            return "Answer is required";
        }

        return this.loginController.answerSecurityQuestion(answer).getMessage();
    }

    private String setNewPassword(List<String> tokens) {
        String password = this.valueAfter(tokens, "-p", 1);
        String passwordConfirm = this.valueAfter(tokens, "-p", 2);

        if (this.hasMissingValue(password, passwordConfirm)) {
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

    private MenuType parseMenuType(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("menu", "")
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");

        if ("signup".equals(normalized)) {
            return MenuType.SIGNUP_MENU;
        }
        if ("login".equals(normalized)) {
            return MenuType.LOGIN_MENU;
        }
        if ("main".equals(normalized)) {
            return MenuType.MAIN_MENU;
        }
        if ("game".equals(normalized) || "play".equals(normalized)) {
            return MenuType.GAME_MENU;
        }
        if ("settings".equals(normalized) || "setting".equals(normalized)) {
            return MenuType.SETTINGS_MENU;
        }
        if ("network".equals(normalized)) {
            return MenuType.NETWORK_MENU;
        }
        if ("news".equals(normalized)) {
            return MenuType.NEWS_MENU;
        }
        if ("profile".equals(normalized)) {
            return MenuType.PROFILE_MENU;
        }
        if ("collection".equals(normalized)) {
            return MenuType.COLLECTION_MENU;
        }
        if ("travellog".equals(normalized)) {
            return MenuType.TRAVEL_LOG_MENU;
        }
        if ("plantpick".equals(normalized)
                || "plantselection".equals(normalized)) {
            return MenuType.PLANT_PICK_MENU;
        }
        if ("midgame".equals(normalized)
                || "gameplay".equals(normalized)) {
            return MenuType.MID_GAME_MENU;
        }

        throw new IllegalArgumentException("Menu name is invalid");
    }

    private String valueAfter(List<String> tokens, String flag, int offset) {
        // chandomin meghdare bade flag ro peyda mikone
        for (int i = 0; i < tokens.size(); i++) {
            if (flag.equalsIgnoreCase(tokens.get(i)) && i + offset < tokens.size()) {
                String value = tokens.get(i + offset);
                return value.startsWith("-") ? null : value;
            }
        }

        return null;
    }

    private boolean hasMissingValue(String... values) {
        for (String value : values) {
            if (value == null || value.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean matches(List<String> tokens, String... expected) {
        return tokens.size() == expected.length && this.startsWith(tokens, expected);
    }

    private boolean startsWith(List<String> tokens, String... expected) {
        if (tokens.size() < expected.length) {
            return false;
        }

        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equalsIgnoreCase(tokens.get(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean containsIgnoreCase(List<String> tokens, String value) {
        for (String token : tokens) {
            if (value.equalsIgnoreCase(token)) {
                return true;
            }
        }

        return false;
    }

    private String join(List<String> tokens, int startIndex) {
        StringBuilder result = new StringBuilder();

        for (int i = startIndex; i < tokens.size(); i++) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(tokens.get(i));
        }

        return result.toString();
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();

        if (input == null) {
            return tokens;
        }

        Matcher matcher = TOKEN_PATTERN.matcher(input.trim());

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                tokens.add(matcher.group(2));
            } else {
                tokens.add(matcher.group(3));
            }
        }

        return tokens;
    }

    private boolean isGreenhouseCommand(
            String input
    ) {
        for (GreenhouseCommands command
                : GreenhouseCommands.values()) {

            if (command.getMatcher(input) != null) {
                return true;
            }
        }

        return false;
    }

    private String executeGreenhouseCommand(
            String input
    ) {
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
        greenhouseView = new GreenhouseView();

        new GreenhouseController(
                greenhouseView,
                currentUser,
                currentUser.getPlantUpgradeService()
        );

        greenhouseViewUser = currentUser;
    }

    private void clearGreenhouseConnection() {
        greenhouseView = null;
        greenhouseViewUser = null;
    }

    private boolean isTravelLogCommand(String input) {
        for (TravelLogCommands command : TravelLogCommands.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }

        return false;
    }

    private String executeTravelLogCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }
        if (this.menuContext.getCurrentMenu() != MenuType.TRAVEL_LOG_MENU) {
            return "Travel Log commands are only available in Travel Log menu.";
        }

        this.ensureTravelLogConnected();
        this.travelLogView.handleCommand(input);
        this.accountService.save();
        return "";
    }

    private void ensureTravelLogConnected() {
        if (this.travelLogView != null && this.travelLogViewUser == this.currentUser) {
            return;
        }

        this.currentUser.initializeMissingFields();
        this.travelLogView = new TravelLogView();
        new TravelLogController(
                this.travelLogView,
                this.currentUser.getTravelLog(),
                this.currentUser,
                this.plantDefinitions
        );
        this.travelLogViewUser = this.currentUser;
    }

    private void clearTravelLogConnection() {
        this.travelLogView = null;
        this.travelLogViewUser = null;
    }

    private boolean isShopCommand(String input) {
        if (input == null)
            return false;

        String normalized = input.trim().toLowerCase(Locale.ROOT);

        return normalized.equals("shop") || normalized.startsWith("shop ");
    }

    public void save() {
        this.accountService.save();
    }

    private boolean isCollectionCommand(String input) {
        for (CollectionCommands command : CollectionCommands.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }

        return false;
    }

    private String executeCollectionCommand(String input) {
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

        new CollectionController(
                this.collectionView,
                this.currentUser,
                this.plantDefinitions,
                this.zombieDefinitions
        );

        this.collectionViewUser = this.currentUser;
    }

    private void clearCollectionConnection() {
        this.collectionView = null;
        this.collectionViewUser = null;
    }

    private boolean isNewsCommand(String input) {
        for (NewsCommand command : NewsCommand.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }

        return false;
    }

    private String executeNewsCommand(String input) {
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
        new NewsController(this.newsView, this.currentUser);
        this.newsViewUser = this.currentUser;
    }

    private void clearNewsConnection() {
        this.newsView = null;
        this.newsViewUser = null;
    }

    private boolean isProfileCommand(String input) {
        for (ProfileCommand command : ProfileCommand.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }

        return false;
    }

    private String executeProfileCommand(String input) {
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

    private void clearProfileConnection() {
        this.profileView = null;
        this.profileViewUser = null;
    }

    private boolean isLeaderboardCommand(String input) {
        return LeaderBoardCommand.SHOW.getMatcher(input) != null;
    }

    private boolean isGameCommand(String input) {
        for (GameCommand command : GameCommand.values()) {
            if (command.getMatcher(input) != null) {
                return true;
            }
        }

        return false;
    }

    private String executeGameCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }

        if (this.menuContext.getCurrentMenu() != MenuType.GAME_MENU) {
            return "Game menu command is only available in game menu.";
        }

        this.gameView.handleCommand(input);
        this.accountService.save();
        return "";
    }

    private boolean isSettingCommand(String input) {
        return SettingCommand.CHANGE_DIFFICULTY.getMatcher(input) != null;
    }

    private String executeSettingCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }

        if (this.menuContext.getCurrentMenu() != MenuType.SETTINGS_MENU) {
            return "Settings commands are only available in settings menu.";
        }

        this.ensureSettingConnected();
        this.settingView.handleCommand(input);
        this.accountService.save();
        return "";
    }

    private void ensureSettingConnected() {
        if (this.settingView != null && this.settingViewUser == this.currentUser) {
            return;
        }

        this.settingView = new SettingView();
        this.settingView.setObserver(new SettingController(this.currentUser));
        this.settingViewUser = this.currentUser;
    }

    private void clearSettingConnection() {
        this.settingView = null;
        this.settingViewUser = null;
    }

    private String executeLeaderboardCommand(String input) {
        if (this.currentUser == null) {
            return "Login is required.";
        }

        if (this.menuContext.getCurrentMenu() != MenuType.GAME_MENU) {
            return "Leaderboard is only available in game menu.";
        }

        LeaderBoardView view = new LeaderBoardView();
        new LeaderBoardController(view, this.accountService);
        view.handleCommand(input);
        return "";
    }

    private String executePlantPickCommand(String input) {
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
        if (this.plantPickView != null
                && this.plantPickViewUser == this.currentUser) {
            return;
        }

        this.currentUser.initializeMissingFields();
        this.plantPickView = new PlantPickView();
        this.plantPickController = new PlantPickController(
                this.plantPickView,
                this.currentUser,
                this.plantDefinitions,
                null,
                PlantPickController.DEFAULT_SLOT_COUNT
        );
        this.plantPickViewUser = this.currentUser;
    }

    private void startSelectedGame() {
        this.currentUser.initializeMissingFields();
        this.currentUser.setGamesPlayed(this.currentUser.getGamesPlayed() + 1);
        this.currentGame = new PlantZombieGame(
                this.plantDefinitions,
                this.zombieDefinitions,
                new ZombieFactory(this.zombieDefinitions),
                this.currentUser.getPlantUpgradeService(),
                new GreenhouseBoostService(this.currentUser.getGreenhouse()),
                this.currentUser
        );
        this.currentGame.configurePlantSelection(
                this.plantPickController.getSelectedPlants(),
                this.plantPickController.getBoostedPlantNames()
        );
        this.currentGame.getBoard().setZombieEncounterListener(definition -> {
            if (definition != null
                    && this.currentUser.recordEncounteredZombie(definition.getAlias())) {
                this.accountService.save();
            }
        });
        this.transferStoredPlantFood();
        this.midGameView = new MidGameView();
        this.currentGame.setEventListener(this.midGameView);
        this.midGameView.setObserver(new MidGameController(this.currentGame));
        this.menuContext.enterMenu(MenuType.MID_GAME_MENU);
    }

    private void transferStoredPlantFood() {
        int storedPlantFood = Math.max(
                0,
                Math.min(3, this.currentUser.getNextLevelPlantFood())
        );
        int transferredPlantFood = 0;

        for (int i = 0; i < storedPlantFood; i++) {
            if (!this.currentGame.getPlantFoodSystem().addPlantFood()) {
                break;
            }
            transferredPlantFood++;
        }

        this.currentUser.setNextLevelPlantFood(
                storedPlantFood - transferredPlantFood
        );
    }

    private String executeMidGameCommand(String input) {
        if (this.currentGame == null || this.midGameView == null) {
            return "Game is not available.";
        }

        this.midGameView.handleCommand(input);
        return "";
    }

    private void clearGameConnections() {
        this.plantPickView = null;
        this.plantPickController = null;
        this.plantPickViewUser = null;
        this.currentGame = null;
        this.midGameView = null;
    }
}
