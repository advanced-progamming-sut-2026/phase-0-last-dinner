package college.java.project.graphics.minigame.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.HashMap;
import java.util.Map;

public final class IZombieReactionIcons {

    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, TextureRegionDrawable> drawables = new HashMap<>();

    public IZombieReactionIcons() {
        load("emoji_laugh");
        load("emoji_angry");
        load("emoji_surprised");
    }

    private void load(String reactionId) {
        Texture texture = new Texture(Gdx.files.internal("reactions/" + reactionId + ".png"));

        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        textures.put(reactionId, texture);
        drawables.put(reactionId, new TextureRegionDrawable(new TextureRegion(texture)));
    }

    public TextureRegionDrawable get(String reactionId) {
        if (reactionId == null) {
            return null;
        }

        return drawables.get(reactionId);
    }

    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
    }
}
