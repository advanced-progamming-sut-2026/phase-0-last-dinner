package network.izombie.protocol;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.HashMap;
import java.util.Map;

public final class ReactionRenderer {
    private final BitmapFont font;
    private final Map<String, Texture> emojiTextures = new HashMap<>();

    public ReactionRenderer(BitmapFont font) {
        this.font = font;
        emojiTextures.put("emoji_laugh", new Texture("reactions/emoji_laugh.png"));
        emojiTextures.put("emoji_angry", new Texture("reactions/emoji_angry.png"));
        emojiTextures.put("emoji_surprised", new Texture("reactions/emoji_surprised.png"));
    }

    public void render(SpriteBatch batch, IZombieReaction reaction, float x, float y) {
        if (reaction.kind() == IZombieReactionKind.TEXT) {
            font.draw(batch, reaction.displayValue(), x, y);
        } else if (reaction.kind() == IZombieReactionKind.EMOJI) {
            Texture texture = emojiTextures.get(reaction.displayValue());
            if (texture != null) {
                batch.draw(texture, x, y);
            }
        }
    }

    public void dispose() {
        for (Texture t : emojiTextures.values()) {
            t.dispose();
        }
    }
}
