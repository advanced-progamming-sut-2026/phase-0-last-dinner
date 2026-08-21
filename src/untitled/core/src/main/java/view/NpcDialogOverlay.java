package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
public final class NpcDialogOverlay extends Group {
    private static final String SPEECH_BUBBLE_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_NPC_COMMON_768_00/SpeechBubble2.png";
    private static final float CHARACTER_HEIGHT_RATIO = 0.88f;
    private static final float BUBBLE_WIDTH_RATIO = 0.23f;
    private static final float BUBBLE_TOP_MARGIN = 90f;
    private static final float SLIDE_IN_SECONDS = 0.45f;

    private final List<Texture> loadedTextures = new ArrayList<>();
    private final List<NpcDialogLine> lines;
    private final Runnable onComplete;
    private final Stage stage;
    private final Actor blocker;
    private final Image character;
    private final Stack bubble;
    private final Label nameLabel;
    private final Label textLabel;
    private int index;
    private String currentPortraitPath;

    private NpcDialogOverlay(Stage stage, List<NpcDialogLine> lines, Runnable onComplete) {
        this.stage = stage;
        this.lines = lines;
        this.onComplete = onComplete;
        Skin skin = PvzSkin.get();

        this.setSize(stage.getWidth(), stage.getHeight());

        this.blocker = new Actor();
        this.blocker.setSize(stage.getWidth(), stage.getHeight());
        this.addActor(this.blocker);

        this.character = new Image();
        this.character.setScaling(Scaling.fit);
        this.character.setAlign(Align.bottom);
        this.addActor(this.character);

        this.nameLabel = new Label("", skin, "medium");
        this.nameLabel.setFontScale(1.75f);
        this.textLabel = new Label("", skin, "default");
        this.textLabel.setFontScale(1.75f);
        this.textLabel.setWrap(true);
        this.textLabel.setColor(Color.BLACK);
        this.textLabel.setAlignment(Align.left);

        TextButton continueButton = new TextButton("Continue", skin, "green");
        continueButton.getLabel().setFontScale(0.6f);
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                advance();
            }
        });

        this.bubble = this.createSpeechBubble(continueButton);
        this.addActor(this.bubble);

        this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                advance();
            }
        });

        stage.addActor(this);
        this.showCurrentLine();
        this.layoutForCurrentSize();
        this.playEntrance();
    }
    private Stack createSpeechBubble(TextButton continueButton) {
        Texture texture = new Texture(Gdx.files.internal(SPEECH_BUBBLE_PATH));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.loadedTextures.add(texture);

        float bubbleWidth = this.stage.getWidth() * BUBBLE_WIDTH_RATIO;
        float aspect = (float) texture.getWidth() / (float) texture.getHeight();
        float bubbleHeight = bubbleWidth / aspect;

        Image bubbleArt = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        Table bubbleBox = new Table();
        bubbleBox.add(bubbleArt).size(bubbleWidth, bubbleHeight);

        Table content = new Table();
        content.add(this.nameLabel).left().padBottom(6).row();
        content.add(this.textLabel).width(bubbleWidth * 0.78f).left().row();
        content.add(continueButton).right().width(80).height(28).padTop(18);

        Container<Table> contentContainer = new Container<>(content);
        contentContainer.pad(bubbleHeight * 0.12f, bubbleWidth * 0.08f, bubbleHeight * 0.2f, bubbleWidth * 0.08f);
        contentContainer.align(Align.top);
        Table contentBox = new Table();
        contentBox.add(contentContainer).size(bubbleWidth, bubbleHeight);

        Stack stack = new Stack();
        stack.add(bubbleBox);
        stack.add(contentBox);
        return stack;
    }
    public static boolean show(Stage stage, List<NpcDialogLine> lines, Runnable onComplete) {
        if (stage == null || lines == null || lines.isEmpty()) {
            return false;
        }
        new NpcDialogOverlay(stage, lines, onComplete);
        return true;
    }

    private void advance() {
        this.index++;
        if (this.index >= this.lines.size()) {
            this.close();
            return;
        }
        this.showCurrentLine();
        this.layoutForCurrentSize();
    }

    private void showCurrentLine() {
        NpcDialogLine line = this.lines.get(this.index);
        this.nameLabel.setText(line.getSpeakerName());
        this.textLabel.setText(line.getText());

        if (line.getPortraitPath() != null && !line.getPortraitPath().equals(this.currentPortraitPath)) {
            this.currentPortraitPath = line.getPortraitPath();
            Texture texture = new Texture(Gdx.files.internal(line.getPortraitPath()));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            this.loadedTextures.add(texture);
            this.character.setDrawable(new TextureRegionDrawable(new TextureRegion(texture)));

            float characterHeight = this.stage.getHeight() * CHARACTER_HEIGHT_RATIO;
            float aspect = (float) texture.getWidth() / (float) texture.getHeight();
            this.character.setSize(characterHeight * aspect, characterHeight);
        }
    }

    private void layoutForCurrentSize() {
        this.character.setPosition(0f, 0f);

        float bubbleX = (this.stage.getWidth() - this.bubble.getWidth()) / 2f;
        float bubbleY = this.stage.getHeight() - this.bubble.getHeight() - BUBBLE_TOP_MARGIN;
        this.bubble.setPosition(bubbleX, bubbleY);
    }

    private void playEntrance() {
        float restingX = this.character.getX();
        this.character.setPosition(-this.character.getWidth(), this.character.getY());
        this.character.addAction(Actions.moveTo(restingX, this.character.getY(), SLIDE_IN_SECONDS, Interpolation.exp10Out));
    }

    private void close() {
        this.remove();
        for (Texture texture : this.loadedTextures) {
            texture.dispose();
        }
        this.loadedTextures.clear();
        if (this.onComplete != null) {
            this.onComplete.run();
        }
    }
}
