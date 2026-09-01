package college.java.project.graphics;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;

public final class SfxDiagnosticGame extends ApplicationAdapter {
    @Override
    public void create() {
        GameplaySoundPlayer soundPlayer = GameplaySoundPlayer.shared();
        try {
            Gdx.app.log("SFX-AUDIT", soundPlayer.auditMappedSounds());
        } finally {
            soundPlayer.dispose();
            Gdx.app.exit();
        }
    }
}
