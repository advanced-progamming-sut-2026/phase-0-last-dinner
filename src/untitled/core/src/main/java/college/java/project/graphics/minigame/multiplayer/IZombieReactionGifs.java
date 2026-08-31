package college.java.project.graphics.minigame.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IZombieReactionGifs {

    private static final float FRAME_DURATION = 1f / 12f;

    private final Map<String, Animation<TextureRegion>> animations = new HashMap<>();
    private final List<Texture> loadedTextures = new ArrayList<>();

    public IZombieReactionGifs() {
        load("gif_reaction1", 115);
        load("gif_reaction2", 72);
        load("gif_reaction3", 15);
    }

    private void load(String reactionId, int frameCount) {
        Array<TextureRegion> frames = new Array<>();

        for (int i = 1; i <= frameCount; i++) {
            String path = String.format("reactions/gifs/%s/%s_%03d.png", reactionId, reactionId, i);

            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

            loadedTextures.add(texture);
            frames.add(new TextureRegion(texture));
        }

        animations.put(reactionId, new Animation<>(FRAME_DURATION, frames, Animation.PlayMode.LOOP));
    }

    public Animation<TextureRegion> get(String reactionId) {
        if (reactionId == null) {
            return null;
        }

        return animations.get(reactionId);
    }

    public void dispose() {
        for (Texture texture : loadedTextures) {
            texture.dispose();
        }
    }
}
