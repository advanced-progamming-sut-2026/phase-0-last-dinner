package college.java.project;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.SignupController;
import model.User.AccountResult;
import model.User.AccountStatus;

import java.util.ArrayList;
import java.util.List;

public final class RegisterScreen implements Screen {
    private static final float WORLD_WIDTH = 1280f;
    private static final float WORLD_HEIGHT = 720f;

    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final SignupController signupController;
    private final TextField usernameField;
    private final TextField passwordField;
    private final TextField passwordConfirmField;
    private final TextField nicknameField;
    private final TextField emailField;
    private final TextField answerField;
    private final TextField answerConfirmField;
    private final ImageButton maleButton;
    private final ImageButton femaleButton;
    private final List<String> securityQuestions;
    private final List<ImageButton> questionButtons = new ArrayList<>();
    private final ButtonGroup<ImageButton> questionGroup = new ButtonGroup<>();
    private final TextButton continueButton;
    private final TextButton backButton;
    private final TextButton createAccountButton;
    private final TextButton securityBackButton;
    private final Label messageLabel;

    private final TextButton passwordToggleButton;

    private boolean securityStepVisible;

    public RegisterScreen(Main game) {
        if (game == null)
            throw new IllegalArgumentException("Game is required");

        this.game = game;
        this.skin = game.getSkin();
        this.stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        this.signupController = game.getApplicationController().getSignupController();
        this.securityQuestions = this.signupController.getSecurityQuestions();

        this.usernameField = new TextField("", this.skin);
        this.passwordField = new TextField("", this.skin);
        this.passwordConfirmField = new TextField("", this.skin);
        this.nicknameField = new TextField("", this.skin);
        this.emailField = new TextField("", this.skin);
        this.answerField = new TextField("", this.skin);
        this.answerConfirmField = new TextField("", this.skin);
        this.continueButton = new TextButton("CONTINUE", this.skin, "green");
        this.backButton = new TextButton("BACK", this.skin, "brown");
        this.createAccountButton = new TextButton("CREATE ACCOUNT", this.skin, "green");
        this.securityBackButton = new TextButton("BACK", this.skin, "brown");
        this.messageLabel = new Label("", this.skin, "secondary");

        ImageButton.ImageButtonStyle radioStyle = createRadioStyle();
        this.maleButton = new ImageButton(radioStyle);
        this.femaleButton = new ImageButton(radioStyle);
        ButtonGroup<ImageButton> genderGroup = new ButtonGroup<>(this.maleButton, this.femaleButton);
        genderGroup.setMinCheckCount(1);
        genderGroup.setMaxCheckCount(1);
        this.maleButton.setChecked(true);

        this.passwordToggleButton = AuthUi.createPasswordToggle(this.skin, this.passwordField, this.passwordConfirmField);

        AuthUi.submitOnEnter(this.emailField, this::beginRegistration);
        AuthUi.submitOnEnter(this.answerConfirmField, this::completeRegistration);

        configureQuestionButtons(radioStyle);
        configureFields();
        configureButtons();
        showInformationStep();
    }

    private ImageButton.ImageButtonStyle createRadioStyle() {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = this.skin.getDrawable("image_ui_generic_radio_up");
        style.imageChecked = this.skin.getDrawable("image_ui_generic_radio_down");
        return style;
    }

    private void configureQuestionButtons(ImageButton.ImageButtonStyle style) {
        this.questionGroup.setMinCheckCount(0);
        this.questionGroup.setMaxCheckCount(1);

        for (int i = 0; i < this.securityQuestions.size(); i++) {
            ImageButton button = new ImageButton(style);
            this.questionButtons.add(button);
            this.questionGroup.add(button);
        }

        if (!this.questionButtons.isEmpty())
            this.questionButtons.getFirst().setChecked(true);

        this.questionGroup.setMinCheckCount(1);
    }

    private void configureFields() {
        this.usernameField.setMessageText("Username");
        this.passwordField.setMessageText("Password");
        this.passwordConfirmField.setMessageText("Confirm password");
        this.nicknameField.setMessageText("Nickname");
        this.emailField.setMessageText("Email");
        this.answerField.setMessageText("Security answer");
        this.answerConfirmField.setMessageText("Confirm security answer");

        this.passwordField.setPasswordMode(true);
        this.passwordField.setPasswordCharacter('*');
        this.passwordConfirmField.setPasswordMode(true);
        this.passwordConfirmField.setPasswordCharacter('*');
    }

    private void configureButtons() {
        this.backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                returnToLogin();
            }
        });

        this.continueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                beginRegistration();
            }
        });

        this.securityBackButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                returnToInformationStep();
            }
        });

        this.createAccountButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                completeRegistration();
            }
        });
    }

    private void beginRegistration() {
        String gender = this.maleButton.isChecked() ? "MALE" : "FEMALE";
        AccountResult result = this.signupController.register(this.usernameField.getText(), this.passwordField.getText(),
            this.passwordConfirmField.getText(), this.nicknameField.getText(), this.emailField.getText(), gender);

        if (result.getStatus() != AccountStatus.SECURITY_QUESTION_REQUIRED) {
            showMessage(result.getMessage());
            return;
        }

        showSecurityStep();
    }

    private void completeRegistration() {
        ImageButton selectedButton = this.questionGroup.getChecked();
        int questionNumber = this.questionButtons.indexOf(selectedButton) + 1;

        AccountResult result = this.signupController.pickQuestion(questionNumber, this.answerField.getText(),
            this.answerConfirmField.getText());

        if (!result.isSuccessful()) {
            showMessage(result.getMessage());
            return;
        }

        this.game.showLoginScreen();
    }

    private void showInformationStep() {
        prepareStage();

        this.securityStepVisible = false;
        resetPasswordVisibility();

        Label title = new Label("CREATE ACCOUNT", this.skin, "big_outline");
        Label subtitle = new Label("ENTER YOUR INFORMATION", this.skin, "medium");
        Table form = createForm();

        form.add(title).colspan(2).padBottom(5f);
        form.row();
        form.add(subtitle).colspan(2).padBottom(20f);
        form.row();

        addField(form, "Username", this.usernameField);

        Table passwordInput = new Table();
        passwordInput.add(this.passwordField).width(260f).height(50f);
        passwordInput.add(this.passwordToggleButton).width(80f).height(42f).padLeft(6f);

        form.add(new Label("Password", this.skin)).right().padRight(15f).padBottom(10f);
        form.add(passwordInput).left().padBottom(10f);
        form.row();

        addField(form, "Confirm password", this.passwordConfirmField);
        addField(form, "Nickname", this.nicknameField);
        addField(form, "Email", this.emailField);

        Table genderOptions = new Table();
        genderOptions.add(this.maleButton).size(36f).padRight(7f);
        genderOptions.add(new Label("Male", this.skin)).padRight(25f);
        genderOptions.add(this.femaleButton).size(36f).padRight(7f);
        genderOptions.add(new Label("Female", this.skin));

        form.add(new Label("Gender", this.skin)).right().padRight(15f).padBottom(15f);
        form.add(genderOptions).left().padBottom(15f);
        form.row();

        form.add(this.backButton).width(180f).height(55f).padRight(10f);
        form.add(this.continueButton).width(220f).height(55f);
        form.row();
        form.add(this.messageLabel).colspan(2).padTop(10f);

        addFormToStage(form, 720f);
        this.stage.setKeyboardFocus(this.usernameField);
    }

    private void showSecurityStep() {
        prepareStage();

        this.securityStepVisible = true;

        Label title = new Label("SECURITY QUESTION", this.skin, "big_outline");
        Label subtitle = new Label("SELECT ONE QUESTION", this.skin, "medium");
        Table form = createForm();

        form.add(title).colspan(2).padBottom(5f);
        form.row();
        form.add(subtitle).colspan(2).padBottom(20f);
        form.row();

        for (int i = 0; i < this.securityQuestions.size(); i++) {
            form.add(this.questionButtons.get(i)).size(36f).padRight(10f).padBottom(12f);
            form.add(new Label(this.securityQuestions.get(i), this.skin)).left().padBottom(12f);
            form.row();
        }

        addField(form, "Answer", this.answerField);
        addField(form, "Confirm answer", this.answerConfirmField);

        form.add(this.securityBackButton).width(180f).height(55f).padRight(10f);
        form.add(this.createAccountButton).width(240f).height(55f);
        form.row();
        form.add(this.messageLabel).colspan(2).padTop(10f);

        addFormToStage(form, 760f);
        this.stage.setKeyboardFocus(this.answerField);
    }

    private Table createForm() {
        Table form = new Table();
        form.setBackground(this.skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        form.pad(30f);
        return form;
    }

    private void addField(Table form, String label, TextField field) {
        form.add(new Label(label, this.skin)).right().padRight(15f).padBottom(10f);
        form.add(field).width(350f).height(50f).padBottom(10f);
        form.row();
    }

    private void addFormToStage(Table form, float width) {
        Table frame = AuthUi.createFrame(this.skin, form);

        Table root = new Table();
        root.setFillParent(true);
        root.add(frame).width(width);
        this.stage.addActor(root);
    }

    private void prepareStage() {
        this.stage.clear();
        AuthUi.addBackground(this.stage, this.game.getAuthBackground());

        this.messageLabel.clearActions();
        this.messageLabel.setText("");
        this.messageLabel.setColor(Color.RED);
        this.messageLabel.getColor().a = 1f;
    }

    private void showMessage(String message) {
        this.messageLabel.clearActions();
        this.messageLabel.setText(message);
        this.messageLabel.setColor(Color.RED);
        this.messageLabel.getColor().a = 1f;
        this.messageLabel.addAction(Actions.sequence(Actions.delay(3f), Actions.fadeOut(0.4f)));
    }

    private void resetPasswordVisibility() {
        this.passwordToggleButton.setChecked(false);
        this.passwordToggleButton.setText("SHOW");
        this.passwordField.setPasswordMode(true);
        this.passwordConfirmField.setPasswordMode(true);
    }

    private void returnToLogin() {
        this.signupController.cancelPendingRegistration();
        this.game.showLoginScreen();
    }

    private void returnToInformationStep() {
        this.signupController.cancelPendingRegistration();
        showInformationStep();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
        this.stage.setKeyboardFocus(this.usernameField);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (this.securityStepVisible)
                returnToInformationStep();
            else
                returnToLogin();

            return;
        }

        ScreenUtils.clear(0.015f, 0.035f, 0.06f, 1f);
        this.stage.act(Math.min(delta, 1f / 30f));
        this.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0)
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
