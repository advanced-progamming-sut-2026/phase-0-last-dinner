package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

public final class GameAssetManager implements Disposable {
    private static final String ASSET_RESOLUTION = "768";

    private final TextureBank textureBank;
    private final PamPlayer pamPlayer;

    public GameAssetManager() {
        FileHandle assetRoot = Gdx.files.internal("");
        this.textureBank = new TextureBank(ASSET_RESOLUTION, assetRoot);
        this.pamPlayer = new PamPlayer(this.textureBank, assetRoot);
    }

    public TextureBank getTextureBank() {
        return this.textureBank;
    }

    public PamPlayer getPamPlayer() {
        return this.pamPlayer;
    }

    public void update() {
        this.textureBank.update();
    }

    @Override
    public void dispose() {
        this.textureBank.dispose();
    }
}
