package controller;

import model.Menu.GameMenuContext;
import model.Menu.MenuType;
import model.User.AccountResult;
import model.User.AccountService;
import model.User.AccountStatus;
import model.User.User;
import model.User.UserRepository;
import model.plant.PlantUpgradeService;
import view.CommandHandler;
import view.greenhouse.GreenhouseCommands;
import view.greenhouse.GreenhouseView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// dastur haye matni ro be bakhshe marbut mifreste
public class ApplicationController implements CommandHandler {
    // matne dakhele quote ro yek token hesab mikone
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");

    private final GameMenuContext menuContext;
    private final AccountService accountService;
    private final SignupController signupController;
    private final LoginController loginController;
    private final MainController mainController;
    private User currentUser;
    private String lastMessage;

    // اینا برا گل خونن
    private GreenhouseView greenhouseView;
    private User greenhouseViewUser;
    private final PlantUpgradeService plantUpgradeService;

    public ApplicationController() {
        this(new UserRepository(), new PlantUpgradeService());
    }

    public ApplicationController(UserRepository repository) {
        this(repository, new PlantUpgradeService());
    }

    public ApplicationController(UserRepository repository, PlantUpgradeService plantUpgradeService) {
        if (plantUpgradeService == null)
            throw new IllegalArgumentException("Plant upgrade service is required.");


        this.menuContext = new GameMenuContext();

        this.accountService = new AccountService(repository);

        this.signupController = new SignupController(this.accountService, this.menuContext);

        this.loginController = new LoginController(this.accountService, this.menuContext);

        this.mainController = new MainController(this.accountService, this.menuContext);

        this.plantUpgradeService = plantUpgradeService;

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

            if (this.menuContext.getCurrentMenu()
                    == MenuType.GAME_MENU
                    && isGreenhouseCommand(input)) {

                return executeGreenhouseCommand(input);
            }

            if (this.menuContext.getCurrentMenu()
                    == MenuType.GAME_MENU
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

    public User getCurrentUser() {
        return this.currentUser;
    }

    public boolean isApplicationRunning() {
        return this.menuContext.isApplicationRunning();
    }

    public String getLastMessage() {
        return this.lastMessage;
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
            this.menuContext.exitMenu();
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

            return this.mainController.logout();
        }

        if (tokens.size() >= 3 && "enter".equalsIgnoreCase(tokens.get(1))) {
            MenuType destination = this.parseMenuType(this.join(tokens, 2));
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
        if (greenhouseView != null
                && greenhouseViewUser == currentUser) {
            return;
        }

        greenhouseView = new GreenhouseView();

        new GreenhouseController(
                greenhouseView,
                currentUser,
                plantUpgradeService
        );

        greenhouseViewUser = currentUser;
    }

    private void clearGreenhouseConnection() {
        greenhouseView = null;
        greenhouseViewUser = null;
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
}
