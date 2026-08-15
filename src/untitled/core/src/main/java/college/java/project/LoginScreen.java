package college.java.project;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import controller.LoginController;
import model.User.AccountResult;

public final class LoginScreen implements Screen {
    private static final float WORLD_WIDTH = 1280f;
    private static final float WORLD_HEIGHT = 720f;

    private final Main game;
    private final Stage stage;
    private final Skin skin;

    private final TextField usernameField;
    private final TextField passwordField;
    private final TextButton loginButton;
    private final TextButton registerButton;
    private final TextButton forgotPasswordButton;
    private final Label messageLabel;
    private final ImageButton stayLoggedInButton;

    private final LoginController loginController;
    private final TextField recoveryUsernameField;
    private final TextField recoveryEmailField;
    private final TextField recoveryAnswerField;
    private final TextField newPasswordField;
    private final TextField newPasswordConfirmField;
    private final Label recoveryMessageLabel;
    private Table recoveryOverlay;
    private String recoveryQuestion;

    private final TextButton passwordToggleButton;
    private final TextButton recoveryPasswordToggleButton;

    public LoginScreen(Main game) {
        if (game == null)
            throw new IllegalArgumentException("Game is required");

        this.game = game;
        this.skin = game.getSkin();
        this.stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));

        AuthUi.addBackground(this.stage, this.game.getAuthBackground());

        this.loginController = game.getApplicationController().getLoginController();

        this.usernameField = new TextField("", this.skin, "default");
        this.passwordField = new TextField("", this.skin, "default");
        this.loginButton = new TextButton("LOGIN", this.skin, "green");
        this.registerButton = new TextButton("CREATE ACCOUNT", this.skin, "purple");
        this.forgotPasswordButton = new TextButton("FORGOT PASSWORD", this.skin, "green_small");
        this.messageLabel = new Label("", this.skin, "secondary");

        this.recoveryUsernameField = new TextField("", this.skin);
        this.recoveryEmailField = new TextField("", this.skin);
        this.recoveryAnswerField = new TextField("", this.skin);
        this.newPasswordField = new TextField("", this.skin);
        this.newPasswordConfirmField = new TextField("", this.skin);
        this.recoveryMessageLabel = new Label("", this.skin, "secondary");

        ImageButton.ImageButtonStyle checkboxStyle = new ImageButton.ImageButtonStyle();
        checkboxStyle.imageUp = this.skin.getDrawable("checkbox_off");
        checkboxStyle.imageChecked = this.skin.getDrawable("checkbox_on");
        this.stayLoggedInButton = new ImageButton(checkboxStyle);

        this.passwordToggleButton = AuthUi.createPasswordToggle(this.skin, this.passwordField);
        this.recoveryPasswordToggleButton = AuthUi.createPasswordToggle(this.skin, this.newPasswordField,
            this.newPasswordConfirmField);

        configureFields();
        configureButtons();
        buildLayout();
    }

    private void configureFields() {
        this.usernameField.setMessageText("Username");

        this.passwordField.setMessageText("Password");
        this.passwordField.setPasswordMode(true);
        this.passwordField.setPasswordCharacter('*');

        this.usernameField.setMaxLength(30);
        this.passwordField.setMaxLength(100);

        this.recoveryUsernameField.setMessageText("Username");
        this.recoveryEmailField.setMessageText("Email");
        this.recoveryAnswerField.setMessageText("Security answer");
        this.newPasswordField.setMessageText("New password");
        this.newPasswordConfirmField.setMessageText("Confirm new password");

        this.newPasswordField.setPasswordMode(true);
        this.newPasswordField.setPasswordCharacter('*');
        this.newPasswordConfirmField.setPasswordMode(true);
        this.newPasswordConfirmField.setPasswordCharacter('*');

        AuthUi.submitOnEnter(this.passwordField, this::handleLogin);
        AuthUi.submitOnEnter(this.recoveryEmailField, this::beginPasswordRecovery);
        AuthUi.submitOnEnter(this.recoveryAnswerField, this::verifyRecoveryAnswer);
        AuthUi.submitOnEnter(this.newPasswordConfirmField, this::completePasswordRecovery);
    }

    private void configureButtons() {
        this.loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleLogin();
            }
        });

        this.registerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showRegisterScreen();
            }
        });

        this.forgotPasswordButton.addListener(createListener(this::openPasswordRecovery));
    }

    private void handleLogin() {
        String username = this.usernameField.getText().trim();
        String password = this.passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Username and password are required", Color.RED);
            return;
        }

        AccountResult result = this.game.getApplicationController().loginUser(username, password, this.stayLoggedInButton.isChecked());

        if (!result.isSuccessful()) {
            showMessage(result.getMessage(), Color.RED);
            return;
        }

        showMessage(result.getMessage(), Color.GREEN);
        this.game.showMainMenuScreen();
    }

    private void showMessage(String message, Color color) {
        this.messageLabel.clearActions();
        this.messageLabel.setText(message);
        this.messageLabel.setColor(color);
        this.messageLabel.getColor().a = 1f;
        this.messageLabel.addAction(Actions.sequence(Actions.delay(3f), Actions.fadeOut(0.4f)));
    }

    private ChangeListener createListener(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        };
    }

    private void openPasswordRecovery() {
        this.loginController.cancelPendingPasswordRecovery();
        resetRecoveryFields();
        this.recoveryUsernameField.setText(this.usernameField.getText().trim());

        this.recoveryOverlay = new Table();
        this.recoveryOverlay.setFillParent(true);
        this.recoveryOverlay.setTouchable(Touchable.enabled);
        this.recoveryOverlay.setBackground(this.skin.getDrawable("modal_background"));
        this.stage.addActor(this.recoveryOverlay);

        showRecoveryIdentityStep();
    }

    private void showRecoveryIdentityStep() {
        Table panel = createRecoveryPanel("PASSWORD RECOVERY");
        addRecoveryField(panel, "Username", this.recoveryUsernameField);
        addRecoveryField(panel, "Email", this.recoveryEmailField);

        TextButton cancelButton = new TextButton("CANCEL", this.skin, "brown");
        TextButton continueButton = new TextButton("CONTINUE", this.skin, "green");

        cancelButton.addListener(createListener(this::closePasswordRecovery));
        continueButton.addListener(createListener(this::beginPasswordRecovery));

        panel.add(cancelButton).width(180f).height(55f).padRight(10f);
        panel.add(continueButton).width(220f).height(55f);
        panel.row();
        panel.add(this.recoveryMessageLabel).colspan(2).padTop(10f);

        setRecoveryPanel(panel);
        this.stage.setKeyboardFocus(this.recoveryUsernameField);
    }

    private void beginPasswordRecovery() {
        AccountResult result = this.loginController.beginPasswordRecovery(
            this.recoveryUsernameField.getText(), this.recoveryEmailField.getText());

        if (!result.isSuccessful()) {
            showRecoveryMessage(result.getMessage());
            return;
        }

        this.recoveryQuestion = result.getSecurityQuestion();
        showRecoveryAnswerStep();
    }

    private void showRecoveryAnswerStep() {
        Table panel = createRecoveryPanel("SECURITY QUESTION");
        Label questionLabel = new Label(this.recoveryQuestion, this.skin, "medium");
        questionLabel.setWrap(true);

        panel.add(questionLabel).colspan(2).width(520f).padBottom(20f);
        panel.row();
        addRecoveryField(panel, "Answer", this.recoveryAnswerField);

        TextButton cancelButton = new TextButton("CANCEL", this.skin, "brown");
        TextButton verifyButton = new TextButton("VERIFY", this.skin, "green");

        cancelButton.addListener(createListener(this::closePasswordRecovery));
        verifyButton.addListener(createListener(this::verifyRecoveryAnswer));

        panel.add(cancelButton).width(180f).height(55f).padRight(10f);
        panel.add(verifyButton).width(220f).height(55f);
        panel.row();
        panel.add(this.recoveryMessageLabel).colspan(2).padTop(10f);

        setRecoveryPanel(panel);
        this.stage.setKeyboardFocus(this.recoveryAnswerField);
    }

    private void verifyRecoveryAnswer() {
        AccountResult result = this.loginController.answerSecurityQuestion(this.recoveryAnswerField.getText());

        if (!result.isSuccessful()) {
            showRecoveryIdentityStep();
            showRecoveryMessage(result.getMessage());
            return;
        }

        showRecoveryPasswordStep();
    }

    private void showRecoveryPasswordStep() {
        Table panel = createRecoveryPanel("NEW PASSWORD");

        Table recoveryPasswordInput = new Table();
        recoveryPasswordInput.add(this.newPasswordField).width(260f).height(50f);
        recoveryPasswordInput.add(this.recoveryPasswordToggleButton).width(80f).height(42f).padLeft(6f);

        panel.add(new Label("New password", this.skin)).right().padRight(15f).padBottom(15f);
        panel.add(recoveryPasswordInput).left().padBottom(15f);
        panel.row();

        addRecoveryField(panel, "Confirm password", this.newPasswordConfirmField);

        TextButton cancelButton = new TextButton("CANCEL", this.skin, "brown");
        TextButton changeButton = new TextButton("CHANGE PASSWORD", this.skin, "green");

        cancelButton.addListener(createListener(this::closePasswordRecovery));
        changeButton.addListener(createListener(this::completePasswordRecovery));

        panel.add(cancelButton).width(180f).height(55f).padRight(10f);
        panel.add(changeButton).width(250f).height(55f);
        panel.row();
        panel.add(this.recoveryMessageLabel).colspan(2).padTop(10f);

        setRecoveryPanel(panel);
        this.stage.setKeyboardFocus(this.newPasswordField);
    }

    private void completePasswordRecovery() {
        AccountResult result = this.loginController.setNewPassword(
            this.newPasswordField.getText(), this.newPasswordConfirmField.getText());

        if (!result.isSuccessful()) {
            showRecoveryMessage(result.getMessage());
            return;
        }

        closePasswordRecovery();
        showMessage(result.getMessage(), Color.GREEN);
    }

    private Table createRecoveryPanel(String title) {
        this.recoveryMessageLabel.clearActions();
        this.recoveryMessageLabel.setText("");
        this.recoveryMessageLabel.setColor(Color.RED);
        this.recoveryMessageLabel.getColor().a = 1f;

        Table panel = new Table();
        panel.setBackground(this.skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(35f);
        panel.add(new Label(title, this.skin, "big_outline")).colspan(2).padBottom(25f);
        panel.row();
        return panel;
    }

    private void addRecoveryField(Table panel, String label, TextField field) {
        panel.add(new Label(label, this.skin)).right().padRight(15f).padBottom(15f);
        panel.add(field).width(350f).height(50f).padBottom(15f);
        panel.row();
    }

    private void setRecoveryPanel(Table panel) {
        this.recoveryOverlay.clearChildren();
        this.recoveryOverlay.add(AuthUi.createFrame(this.skin, panel)).width(700f);
    }

    private void showRecoveryMessage(String message) {
        this.recoveryMessageLabel.clearActions();
        this.recoveryMessageLabel.setText(message);
        this.recoveryMessageLabel.setColor(Color.RED);
        this.recoveryMessageLabel.getColor().a = 1f;
        this.recoveryMessageLabel.addAction(Actions.sequence(Actions.delay(3f), Actions.fadeOut(0.4f)));
    }

    private void closePasswordRecovery() {
        this.loginController.cancelPendingPasswordRecovery();

        resetRecoveryFields();

        if (this.recoveryOverlay != null) {
            this.recoveryOverlay.remove();
            this.recoveryOverlay = null;
        }

        this.stage.setKeyboardFocus(this.usernameField);
    }

    private void buildLayout() {
        Label title = new Label("PLANTS VS. ZOMBIES 2", this.skin, "big_outline");

        Label subtitle = new Label("LOGIN TO YOUR ACCOUNT", this.skin, "medium");

        Table form = AuthUi.createContent(this.skin, 40f);

        form.add(title).colspan(2).padBottom(10f);form.row();

        form.add(subtitle).colspan(2).padBottom(30f);
        form.row();

        form.add(new Label("Username", this.skin)).right().padRight(15f).padBottom(15f);

        form.add(this.usernameField).width(330f).height(55f).padBottom(15f);
        form.row();

        form.add(new Label("Password", this.skin)).right().padRight(15f).padBottom(20f);

        Table passwordInput = new Table();
        passwordInput.add(this.passwordField).width(245f).height(55f);
        passwordInput.add(this.passwordToggleButton).width(80f).height(45f).padLeft(5f);

        form.add(passwordInput).padBottom(20f);
        form.row();

        form.add(new Label("Stay logged in", this.skin)).right().padRight(15f).padBottom(20f);
        form.add(this.stayLoggedInButton).left().size(42f).padBottom(20f);
        form.row();

        form.add(this.loginButton).colspan(2).width(300f).height(65f).padBottom(12f);
        form.row();

        form.add(this.forgotPasswordButton).colspan(2).width(260f).height(50f).padBottom(12f);
        form.row();

        form.add(this.registerButton).colspan(2).width(300f).height(60f).padBottom(15f);
        form.row();

        form.add(this.messageLabel).colspan(2).padTop(5f);

        Table frame = AuthUi.createFrame(this.skin, form);
        frame.getColor().a = 0f;
        frame.addAction(Actions.fadeIn(0.35f));

        Table root = new Table();
        root.setFillParent(true);
        root.add(frame).width(650f);
        this.stage.addActor(root);
    }

    private void resetRecoveryFields() {
        this.recoveryUsernameField.setText("");
        this.recoveryEmailField.setText("");
        this.recoveryAnswerField.setText("");
        this.newPasswordField.setText("");
        this.newPasswordConfirmField.setText("");

        this.recoveryMessageLabel.clearActions();
        this.recoveryMessageLabel.setText("");

        this.recoveryPasswordToggleButton.setChecked(false);
        this.recoveryPasswordToggleButton.setText("SHOW");
        this.newPasswordField.setPasswordMode(true);
        this.newPasswordConfirmField.setPasswordMode(true);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
        this.stage.setKeyboardFocus(this.usernameField);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && this.recoveryOverlay != null) {
            closePasswordRecovery();
            return;
        }

        ScreenUtils.clear(0.015f, 0.035f, 0.06f, 1f);
        this.stage.act(Math.min(delta, 1f / 30f));
        this.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0)
            return;

        this.stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        InputProcessor currentProcessor = Gdx.input.getInputProcessor();

        if (currentProcessor == this.stage)
            Gdx.input.setInputProcessor(null);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        this.stage.dispose();
    }
}
