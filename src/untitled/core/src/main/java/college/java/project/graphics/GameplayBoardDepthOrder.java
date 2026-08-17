package college.java.project.graphics;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class GameplayBoardDepthOrder {
    static final int BASE_PLANT = 0;
    static final int PLANT = 1;
    static final int ZOMBIE = 2;
    static final int COVER_PLANT = 3;
    static final int PROJECTILE = 4;

    private GameplayBoardDepthOrder() {
    }

    static void mark(Actor actor, int row, int priority) {
        if (actor != null) {
            actor.setUserObject(new DepthKey(row, priority));
        }
    }

    static void sort(Group group) {
        if (group == null || group.getChildren().size < 2) {
            return;
        }
        List<Actor> actors = new ArrayList<>();
        for (Actor actor : group.getChildren()) {
            actors.add(actor);
        }
        actors.sort(Comparator
                .comparingInt(GameplayBoardDepthOrder::row)
                .thenComparingInt(GameplayBoardDepthOrder::priority)
                .thenComparingDouble(Actor::getX));
        for (int index = 0; index < actors.size(); index++) {
            actors.get(index).setZIndex(index);
        }
    }

    private static int row(Actor actor) {
        DepthKey key = key(actor);
        return key == null ? 0 : key.row;
    }

    private static int priority(Actor actor) {
        DepthKey key = key(actor);
        return key == null ? PLANT : key.priority;
    }

    private static DepthKey key(Actor actor) {
        return actor != null && actor.getUserObject() instanceof DepthKey
                ? (DepthKey) actor.getUserObject()
                : null;
    }

    private static final class DepthKey {
        private final int row;
        private final int priority;

        private DepthKey(int row, int priority) {
            this.row = row;
            this.priority = priority;
        }
    }
}
