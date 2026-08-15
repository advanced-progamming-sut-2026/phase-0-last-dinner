package college.java.project.graphics;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;


final class CollectionUiAnimator {
    private static final float HOVER_SCALE = 1.045f;
    private static final float ENTER_DURATION = 0.09f;
    private static final float EXIT_DURATION = 0.08f;

    private CollectionUiAnimator() {
    }

    static void installHoverScale(Actor actor) {
        if (actor == null) {
            return;
        }
        actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
        actor.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer >= 0) {
                    return;
                }
                actor.clearActions();
                actor.addAction(Actions.scaleTo(HOVER_SCALE, HOVER_SCALE, ENTER_DURATION));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer >= 0) {
                    return;
                }
                actor.clearActions();
                actor.addAction(Actions.scaleTo(1f, 1f, EXIT_DURATION));
            }
        });
    }

    static void showPopup(Actor popup) {
        if (popup == null) {
            return;
        }
        popup.clearActions();
        popup.setVisible(true);
        popup.getColor().a = 0f;
        popup.setScale(0.96f);
        popup.setOrigin(popup.getWidth() / 2f, popup.getHeight() / 2f);
        popup.addAction(Actions.parallel(
                Actions.fadeIn(0.10f),
                Actions.scaleTo(1f, 1f, 0.12f)
        ));
    }

    static void hidePopup(Actor popup) {
        if (popup == null || !popup.isVisible()) {
            return;
        }
        popup.clearActions();
        popup.addAction(Actions.sequence(
                Actions.parallel(Actions.fadeOut(0.07f), Actions.scaleTo(0.98f, 0.98f, 0.07f)),
                Actions.visible(false)
        ));
    }

    static void enterScreen(Stage stage) {
        if (stage == null) {
            return;
        }
        Actor root = stage.getRoot();
        root.clearActions();
        root.getColor().a = 0f;
        root.setScale(1.01f);
        root.setOrigin(960f, 540f);
        root.addAction(Actions.parallel(
                Actions.fadeIn(0.16f),
                Actions.scaleTo(1f, 1f, 0.18f)
        ));
    }

    static void leaveScreen(Stage stage, Runnable action) {
        if (stage == null) {
            if (action != null) {
                action.run();
            }
            return;
        }
        Actor root = stage.getRoot();
        root.clearActions();
        root.addAction(Actions.sequence(
                Actions.fadeOut(0.10f),
                Actions.run(() -> {
                    if (action != null) {
                        action.run();
                    }
                })
        ));
    }
}
