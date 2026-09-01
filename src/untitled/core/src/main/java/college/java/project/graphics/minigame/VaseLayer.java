package college.java.project.graphics.minigame;

import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.GameplayPamScale;
import college.java.project.graphics.PamAnimationActor;
import college.java.project.graphics.PamAnimationCatalog;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import controller.VasebreakerController;
import college.java.project.graphics.GameplaySoundPlayer;
import model.minigame.vasebreakerminigame.Vase;
import model.minigame.vasebreakerminigame.VaseType;
import model.minigame.vasebreakerminigame.VasebreakerActionResult;
import model.minigame.vasebreakerminigame.VasebreakerActionStatus;
import model.minigame.vasebreakerminigame.VasebreakerStateResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VaseLayer extends Group {
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;
    private static final float DEFAULT_BREAK_SECONDS = 1.8f;

    private final VasebreakerController controller;
    private final GameAssetManager assets;
    private final PamAnimationCatalog animationCatalog;
    private final Map<Vase, RenderedVase> renderedVases = new IdentityHashMap<>();

    public VaseLayer(VasebreakerController controller, GameAssetManager assets) {
        if (controller == null || assets == null) {
            throw new IllegalArgumentException("Vase layer dependencies are required");
        }

        this.controller = controller;
        this.assets = assets;
        this.animationCatalog = new PamAnimationCatalog();
        setTouchable(Touchable.childrenOnly);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        syncVases(Math.max(0f, delta));
    }

    private void syncVases(float delta) {
        VasebreakerStateResult state = this.controller.onShowVasebreakerRequested();
        if (state == null) {
            return;
        }

        Set<Vase> activeVases = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Vase> missingVases = new ArrayList<>();

        for (Vase vase : state.getVases()) {
            if (vase == null || vase.isBroken() || vase.getPosition() == null) {
                continue;
            }

            activeVases.add(vase);
            if (!this.renderedVases.containsKey(vase)) {
                missingVases.add(vase);
            }
        }

        Iterator<Map.Entry<Vase, RenderedVase>> iterator =
            this.renderedVases.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Vase, RenderedVase> entry = iterator.next();
            RenderedVase rendered = entry.getValue();

            if (rendered.breaking) {
                rendered.breakRemaining -= delta;

                if (rendered.breakRemaining <= 0f) {
                    rendered.root.remove();
                    iterator.remove();
                }

                continue;
            }

            if (!activeVases.contains(entry.getKey())) {
                rendered.root.remove();
                iterator.remove();
            }
        }

        missingVases.sort(Comparator.comparingInt(vase ->
            vase.getPosition().getY()));

        for (Vase vase : missingVases) {
            createVase(vase);
        }
    }

    private void createVase(Vase vase) {
        int column = vase.getPosition().getX() - 1;
        int row = vase.getPosition().getY() - 1;

        if (column < 0 || column >= COLUMN_COUNT || row < 0 || row >= ROW_COUNT) {
            return;
        }

        PamAnimationCatalog.AnimationInfo animation =
            this.animationCatalog.find(animationName(vase.getType()));

        if (animation == null ||
            !Gdx.files.internal("IMAGES/" + animation.getPath()).exists()) {
            return;
        }

        float cellWidth = getWidth() / COLUMN_COUNT;
        float cellHeight = getHeight() / ROW_COUNT;

        Group root = new Group();
        root.setBounds(
            column * cellWidth,
            getHeight() - (row + 1f) * cellHeight,
            cellWidth,
            cellHeight
        );
        root.setTouchable(Touchable.enabled);

        float actorWidth = GameplayPamScale.actorWidth(animation.getCanvasWidth());
        float actorHeight = GameplayPamScale.actorHeight(animation.getCanvasHeight());

        PamAnimationActor body = new PamAnimationActor(
            this.assets.getPamPlayer(),
            animation.getPath(),
            "idle",
            animation.getCanvasWidth(),
            animation.getCanvasHeight()
        );
        body.setBounds(
            (cellWidth - actorWidth) / 2f,
            (cellHeight - actorHeight) / 2f,
            actorWidth,
            actorHeight
        );
        body.setTouchable(Touchable.disabled);
        root.addActor(body);

        RenderedVase rendered = new RenderedVase(root, body, animation);

        root.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                breakVase(vase, rendered);
            }
        });

        this.renderedVases.put(vase, rendered);
        addActor(root);
    }

    private void breakVase(Vase vase, RenderedVase rendered) {
        if (rendered.breaking) {
            return;
        }

        VasebreakerActionResult result =
            this.controller.onBreakVaseRequested(vase.getPosition());

        if (result == null ||
            result.getStatus() != VasebreakerActionStatus.VASE_BROKEN) {
            return;
        }

        GameplaySoundPlayer.shared().play(GameplaySoundPlayer.Effect.VASE_BREAK);

        String clip = (vase.getPosition().getX() + vase.getPosition().getY()) % 2 == 0
            ? "break"
            : "break2";

        rendered.breaking = true;
        rendered.breakRemaining =
            rendered.animation.getClipDuration(clip, DEFAULT_BREAK_SECONDS);
        rendered.root.setTouchable(Touchable.disabled);
        rendered.body.setAnimation(rendered.animation.getPath(), clip);
        rendered.body.setLooping(false);
    }

    private String animationName(VaseType type) {
        if (type == VaseType.PLANT) {
            return "VASE_EGG_GREEN";
        }

        if (type == VaseType.GARGANTUAR) {
            return "VASE_EGG_GARGANTUAR";
        }

        return "VASE_EGG_BROWN";
    }

    private static final class RenderedVase {
        private final Group root;
        private final PamAnimationActor body;
        private final PamAnimationCatalog.AnimationInfo animation;
        private boolean breaking;
        private float breakRemaining;

        private RenderedVase(
            Group root,
            PamAnimationActor body,
            PamAnimationCatalog.AnimationInfo animation
        ) {
            this.root = root;
            this.body = body;
            this.animation = animation;
        }
    }
}
