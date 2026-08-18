package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import controller.ApplicationController;
import pvz.skin.PvzSkin;
import controller.ProfileController;
import model.User.ProfileInformation;
import com.badlogic.gdx.utils.Align;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import model.User.AccountResult;
import model.User.User;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public final class ProfileMenuScreen implements Screen {
    public interface Navigator {
        void onBack();
    }

    private static final String BACKGROUND_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_MAINMENU_BACKGROUND_768_00/mainmenu_background.png";
    private static final String PANEL_DRAWABLE = "image_ui_mainmenu_mm_settings_tab_10";
    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private final ApplicationController controller;
    private ProfileController profileController;
    private final Navigator navigator;
    private final List<Texture> loadedTextures = new ArrayList<>();
    private Stage stage;

    private Table informationTable;
    private Table modalOverlay;

    public ProfileMenuScreen(ApplicationController controller, Navigator navigator) {
        if (controller == null || navigator == null)
            throw new IllegalArgumentException("Controller and navigator are required");

        this.controller = controller;
        this.navigator = navigator;
    }

    @Override
    public void show() {
        this.stage = new Stage(new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(this.stage);

        Skin skin = PvzSkin.get();
        this.profileController = this.controller.getOrCreateProfileController();
        this.stage.addActor(createBackground());

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24f);
        this.stage.addActor(root);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable(PANEL_DRAWABLE));
        panel.pad(35f);

        Label title = new Label("PROFILE", skin, "big_outline");
        TextButton backButton = new TextButton("BACK", skin, "brown");

        TextButton editButton = new TextButton("EDIT PROFILE", skin, "green");
        editButton.addListener(createListener(this::openEditProfile));

        TextButton passwordButton = new TextButton("CHANGE PASSWORD", skin, "purple");
        passwordButton.addListener(createListener(this::openChangePassword));

        backButton.addListener(createListener(this::returnToMainMenu));

        Table informationTable = createInformationTable();

        panel.add(title).padBottom(25f).row();
        panel.add(informationTable).width(520f).padBottom(25f).row();

        Table buttons = new Table();
        buttons.add(editButton).size(200f, 55f).padRight(10f);
        buttons.add(passwordButton).size(230f, 55f).padRight(10f);
        buttons.add(backButton).size(150f, 55f);
        panel.add(buttons);

        root.add(panel).center();
    }

    private void returnToMainMenu() {
        this.controller.execute("menu exit");
        this.navigator.onBack();
    }

    private Table createInformationTable() {
        Table table = new Table();
        table.setBackground(PvzSkin.get().getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        table.pad(22f);

        this.informationTable = table;
        refreshInformation();
        return table;
    }

    private void refreshInformation() {
        this.informationTable.clearChildren();

        ProfileInformation information = this.profileController.showInformation();
        if (information == null) {
            this.informationTable.add(new Label("Profile information is not available.", PvzSkin.get(), "secondary"));
            return;
        }

        addInformationRow(this.informationTable, "Username", information.getUsername());
        addInformationRow(this.informationTable, "Nickname", information.getNickname());
        addInformationRow(this.informationTable, "Games played", String.valueOf(information.getGamesPlayed()));
        addCurrencyRow(this.informationTable, "Coins", String.valueOf(information.getCoins()), "image_ui_hud_ingame_coin");
        addCurrencyRow(this.informationTable, "Diamonds", String.valueOf(information.getDiamonds()), "image_ui_hud_ingame_gem");
        addInformationRow(this.informationTable, "Completed levels", String.valueOf(information.getCompletedLevels()));
        addInformationRow(this.informationTable, "Maximum Meow Points", String.valueOf(information.getMaximumMeowPoints()));
    }

    private void addInformationRow(Table table, String title, String value) {
        Label titleLabel = new Label(title + ":", PvzSkin.get(), "medium");
        Label valueLabel = new Label(value == null ? "-" : value, PvzSkin.get(), "secondary");

        titleLabel.setAlignment(Align.left);
        valueLabel.setAlignment(Align.right);

        table.add(titleLabel).left().width(280f).padBottom(10f);
        table.add(valueLabel).right().width(180f).padBottom(10f);
        table.row();
    }

    private void addCurrencyRow(Table table, String title, String value, String drawableName) {
        Skin skin = PvzSkin.get();
        Label titleLabel = new Label(title + ":", skin, "medium");
        Label valueLabel = new Label(value == null ? "0" : value, skin, "secondary");
        Image icon = new Image(skin.getDrawable(drawableName));

        titleLabel.setAlignment(Align.left);
        valueLabel.setAlignment(Align.right);
        icon.setScaling(Scaling.fit);

        Table valueGroup = new Table();
        valueGroup.add(valueLabel).right().expandX();
        valueGroup.add(icon).size(38f, 32f).padLeft(8f);

        table.add(titleLabel).left().width(280f).padBottom(10f);
        table.add(valueGroup).right().width(180f).padBottom(10f);
        table.row();
    }

    private void openEditProfile() {
        User user = this.controller.getCurrentUser();
        if (user == null)
            return;

        Skin skin = PvzSkin.get();
        TextField usernameField = createTextField(user.getUsername(), "Username");
        TextField nicknameField = createTextField(user.getNickname(), "Nickname");
        TextField emailField = createTextField(user.getEmail(), "Email");
        Label messageLabel = new Label("", skin, "secondary");

        Table content = new Table();
        content.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        content.pad(28f);
        content.add(new Label("EDIT PROFILE", skin, "big_outline")).colspan(3).padBottom(22f).row();

        addEditableRow(content, "Username", usernameField, () -> {
            AccountResult result = this.profileController.changeUsername(usernameField.getText().trim());
            handleEditResult(result, messageLabel);
        });

        addEditableRow(content, "Nickname", nicknameField, () -> {
            AccountResult result = this.profileController.changeNickname(nicknameField.getText().trim());
            handleEditResult(result, messageLabel);
        });

        addEditableRow(content, "Email", emailField, () -> {
            AccountResult result = this.profileController.changeEmail(emailField.getText().trim());
            handleEditResult(result, messageLabel);
        });

        TextButton closeButton = new TextButton("CLOSE", skin, "brown");
        closeButton.addListener(createListener(this::closeModal));

        content.add(messageLabel).colspan(3).width(520f).padTop(8f).row();
        content.add(closeButton).colspan(3).size(150f, 50f).padTop(15f);
        openModal(content);
        this.stage.setKeyboardFocus(usernameField);
    }

    private TextField createTextField(String text, String hint) {
        TextField field = new TextField(text == null ? "" : text, PvzSkin.get(), "default");
        field.setMessageText(hint);
        field.setMaxLength(100);
        return field;
    }

    private void addEditableRow(Table table, String title, TextField field, Runnable action) {
        TextButton changeButton = new TextButton("CHANGE", PvzSkin.get(), "green_small");
        changeButton.addListener(createListener(action));

        submitOnEnter(field, action);

        table.add(new Label(title, PvzSkin.get(), "medium")).right().padRight(12f).padBottom(12f);
        table.add(field).width(290f).height(50f).padBottom(12f);
        table.add(changeButton).width(130f).height(46f).padLeft(8f).padBottom(12f).row();
    }

    private void handleEditResult(AccountResult result, Label messageLabel) {
        messageLabel.setText(result.getMessage());
        messageLabel.setColor(result.isSuccessful() ? Color.GREEN : Color.RED);

        if (result.isSuccessful())
            refreshInformation();
    }

    private void openChangePassword() {
        Skin skin = PvzSkin.get();
        TextField oldPasswordField = createPasswordField("Current password");
        TextField newPasswordField = createPasswordField("New password");
        TextField confirmPasswordField = createPasswordField("Confirm new password");
        Label messageLabel = new Label("", skin, "secondary");

        Table content = new Table();
        content.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        content.pad(28f);
        content.add(new Label("CHANGE PASSWORD", skin, "big_outline")).colspan(2).padBottom(22f).row();

        addPasswordRow(content, "Current password", oldPasswordField);
        addPasswordRow(content, "New password", newPasswordField);
        addPasswordRow(content, "Confirm password", confirmPasswordField);

        TextButton toggleButton = createPasswordToggle(oldPasswordField, newPasswordField, confirmPasswordField);
        content.add(toggleButton).colspan(2).size(110f, 44f).padTop(2f).row();

        TextButton cancelButton = new TextButton("CANCEL", skin, "brown");
        TextButton changeButton = new TextButton("CHANGE", skin, "green");

        cancelButton.addListener(createListener(this::closeModal));
        Runnable changeAction = () -> changePassword(
            oldPasswordField,
            newPasswordField,
            confirmPasswordField,
            messageLabel
        );

        changeButton.addListener(createListener(changeAction));
        submitOnEnter(confirmPasswordField, changeAction);

        content.add(cancelButton).size(160f, 52f).padTop(16f).padRight(10f);
        content.add(changeButton).size(190f, 52f).padTop(16f).row();
        content.add(messageLabel).colspan(2).width(520f).padTop(10f);

        openModal(content);
        this.stage.setKeyboardFocus(oldPasswordField);
    }

    private TextField createPasswordField(String hint) {
        TextField field = createTextField("", hint);
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    private void addPasswordRow(Table table, String title, TextField field) {
        table.add(new Label(title, PvzSkin.get(), "medium")).right().padRight(12f).padBottom(12f);
        table.add(field).width(310f).height(50f).padBottom(12f).row();
    }

    private TextButton createPasswordToggle(TextField... fields) {
        TextButton button = new TextButton("SHOW", PvzSkin.get(), "green_small");

        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean visible = button.isChecked();

                for (TextField field : fields)
                    field.setPasswordMode(!visible);

                button.setText(visible ? "HIDE" : "SHOW");
            }
        });

        return button;
    }

    private void changePassword(TextField oldField, TextField newField, TextField confirmField, Label messageLabel) {
        String oldPassword = oldField.getText();
        String newPassword = newField.getText();
        String confirmation = confirmField.getText();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmation.isEmpty()) {
            showPasswordMessage(messageLabel, "All password fields are required", false);
            return;
        }

        if (!newPassword.equals(confirmation)) {
            showPasswordMessage(messageLabel, "New passwords do not match", false);
            return;
        }

        AccountResult result = this.profileController.changePassword(newPassword, oldPassword);
        showPasswordMessage(messageLabel, result.getMessage(), result.isSuccessful());

        if (result.isSuccessful()) {
            oldField.setText("");
            newField.setText("");
            confirmField.setText("");
        }
    }

    private void showPasswordMessage(Label label, String message, boolean successful) {
        label.setText(message);
        label.setColor(successful ? Color.GREEN : Color.RED);
    }

    private void openModal(Table content) {
        closeModal();

        Table frame = new Table();
        frame.setBackground(PvzSkin.get().getDrawable("image_ui_dialog_asset_dialogborder_10"));
        frame.pad(16f);
        frame.add(content).grow();

        this.modalOverlay = new Table();
        this.modalOverlay.setFillParent(true);
        this.modalOverlay.setTouchable(Touchable.enabled);
        this.modalOverlay.setBackground(PvzSkin.get().getDrawable("modal_background"));
        this.modalOverlay.add(frame).width(760f);
        this.stage.addActor(this.modalOverlay);
    }

    private void closeModal() {
        if (this.modalOverlay == null)
            return;

        this.modalOverlay.remove();
        this.modalOverlay = null;
        this.stage.setKeyboardFocus(null);
    }

    private ChangeListener createListener(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        };
    }

    private void submitOnEnter(TextField field, Runnable action) {
        field.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode != Input.Keys.ENTER)
                    return false;

                action.run();
                return true;
            }
        });
    }

    private Image createBackground() {
        Texture texture = new Texture(Gdx.files.internal(BACKGROUND_PATH));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.loadedTextures.add(texture);

        Image background = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        background.setScaling(Scaling.fill);
        background.setFillParent(true);
        return background;
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (this.modalOverlay != null)
                closeModal();
            else
                returnToMainMenu();

            return;
        }

        ScreenUtils.clear(Color.valueOf("2f4b2f"));
        this.stage.act(Math.min(delta, 1f / 30f));
        this.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        this.stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (this.stage != null)
            this.stage.dispose();

        for (Texture texture : this.loadedTextures)
            texture.dispose();

        this.loadedTextures.clear();
    }
}
