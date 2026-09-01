package college.java.project.graphics;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.Viewport;

public final class SfxStage extends Stage {
    public SfxStage() {
        super();
        installUiSound();
    }

    public SfxStage(Viewport viewport) {
        super(viewport);
        installUiSound();
    }

    public SfxStage(Viewport viewport, Batch batch) {
        super(viewport, batch);
        installUiSound();
    }

    private void installUiSound() {
        getRoot().addCaptureListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                Actor target = event.getTarget();
                if (hasButtonAncestor(target)) {
                    GameplaySoundPlayer.shared().play(GameplaySoundPlayer.Effect.BUTTON);
                } else if (hasCardAncestor(target)) {
                    GameplaySoundPlayer.shared().play(GameplaySoundPlayer.Effect.CARD);
                } else if (hasAdjustableControlAncestor(target)
                        || hasMenuClickAncestor(target)) {
                    GameplaySoundPlayer.shared().play(GameplaySoundPlayer.Effect.BUTTON);
                }
                return false;
            }
        });
    }

    private boolean hasButtonAncestor(Actor actor) {
        for (Actor current = actor; current != null; current = current.getParent()) {
            if (current instanceof Button) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCardAncestor(Actor actor) {
        for (Actor current = actor; current != null; current = current.getParent()) {
            if (current instanceof PlantCard plantCard) {
                return !plantCard.isGameplayMode();
            }
            if (current instanceof ZombieCard
                    || current.getClass().getSimpleName().endsWith("UnitCard")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdjustableControlAncestor(Actor actor) {
        for (Actor current = actor; current != null; current = current.getParent()) {
            if (current instanceof Slider || current instanceof SelectBox<?>) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMenuClickAncestor(Actor actor) {
        if (isWorldInput(actor)) {
            return false;
        }
        for (Actor current = actor; current != null; current = current.getParent()) {
            for (com.badlogic.gdx.scenes.scene2d.EventListener listener : current.getListeners()) {
                if (listener instanceof ClickListener) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWorldInput(Actor actor) {
        for (Actor current = actor; current != null; current = current.getParent()) {
            String name = current.getClass().getSimpleName();
            if (name.equals("GameplayBoardInteractionLayer")
                    || name.equals("GameplaySunLayer")
                    || name.equals("GameplayConveyorBelt")
                    || name.equals("BeghouledLayer")
                    || name.equals("IZombieLayer")
                    || name.equals("IZombieMultiplayerLayer")
                    || name.equals("VaseLayer")
                    || name.equals("VasebreakerSeedPacketLayer")
                    || name.equals("WallnutBowlingLayer")) {
                return true;
            }
        }
        return false;
    }
}
