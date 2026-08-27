package college.java.project.graphics.minigame.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.PamAnimationCatalog;
import college.java.project.graphics.ZombieAnimationCatalog;
import network.izombie.client.IZombieClientGameData;
import network.izombie.protocol.IZombieEntityKind;
import network.izombie.protocol.IZombieEntitySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IZombieMultiplayerEntityLayer extends Group {

    private final IZombieClientGameData data;
    private final GameAssetManager assets;

    private final PamAnimationCatalog plantAnimations;
    private final ZombieAnimationCatalog zombieAnimations;

    private final Map<Long, IZombieSnapshotEntityActor> actors = new HashMap<>();

    private final Set<Long> terminalEntityIds = new HashSet<>();

    private final Set<Long> failedEntityIds = new HashSet<>();

    public IZombieMultiplayerEntityLayer(IZombieClientGameData data, GameAssetManager assets) {
        if (data == null || assets == null) {
            throw new IllegalArgumentException("Multiplayer entity layer dependencies are required.");
        }

        this.data = data;
        this.assets = assets;

        plantAnimations = new PamAnimationCatalog();
        zombieAnimations = new ZombieAnimationCatalog();

        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        assets.update();
        synchroniseEntities();

        super.act(Math.max(0f, delta));
        sortByBoardDepth();
    }

    public int getRenderedEntityCount() {
        return actors.size();
    }

    public int getRenderedPlantCount() {
        return countByKind(IZombieEntityKind.PLANT);
    }

    public int getRenderedZombieCount() {
        return countByKind(IZombieEntityKind.ZOMBIE);
    }

    public int getRenderedProjectileCount() {
        return countByKind(IZombieEntityKind.PROJECTILE);
    }

    public void clearEntities() {
        for (IZombieSnapshotEntityActor actor : actors.values()) {
            if (actor != null) {
                actor.remove();
            }
        }

        actors.clear();
        terminalEntityIds.clear();
        failedEntityIds.clear();
        clearChildren();
    }

    private void synchroniseEntities() {
        if (!data.hasSnapshot()) {
            removeAllMissingEntities();
            return;
        }

        List<IZombieEntitySnapshot> snapshots = data.getEntities();

        Set<Long> seenEntityIds = new HashSet<>();

        for (IZombieEntitySnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.kind() == null) {
                continue;
            }

            long entityId = snapshot.entityId();
            seenEntityIds.add(entityId);

            if (snapshot.dead()) {
                handleDeadEntity(snapshot);
                continue;
            }

            if (terminalEntityIds.contains(entityId) || failedEntityIds.contains(entityId)) {
                continue;
            }

            IZombieSnapshotEntityActor actor = actors.get(entityId);

            if (actor == null) {
                actor = createActor(snapshot);

                if (actor == null) {
                    failedEntityIds.add(entityId);
                    continue;
                }

                actors.put(entityId, actor);
                addActor(actor);
            } else {
                actor.applySnapshot(snapshot, false);
            }
        }

        removeMissingEntities(seenEntityIds);

        terminalEntityIds.retainAll(seenEntityIds);
        failedEntityIds.retainAll(seenEntityIds);
    }

    private IZombieSnapshotEntityActor createActor(IZombieEntitySnapshot snapshot) {
        try {
            return new IZombieSnapshotEntityActor(snapshot, assets, plantAnimations, zombieAnimations);
        } catch (RuntimeException exception) {
            logVisualError(snapshot, exception);
            return null;
        }
    }

    private void handleDeadEntity(IZombieEntitySnapshot snapshot) {
        long entityId = snapshot.entityId();

        terminalEntityIds.add(entityId);

        IZombieSnapshotEntityActor actor = actors.get(entityId);

        if (actor == null) {
            return;
        }

        actor.applySnapshot(snapshot, false);
    }

    private void removeMissingEntities(Set<Long> seenEntityIds) {
        List<Long> removedIds = new ArrayList<>();

        for (Map.Entry<Long, IZombieSnapshotEntityActor> entry : actors.entrySet()) {
            if (!seenEntityIds.contains(entry.getKey())) {
                removedIds.add(entry.getKey());
            }
        }

        for (Long entityId : removedIds) {
            IZombieSnapshotEntityActor actor = actors.remove(entityId);

            if (actor != null) {
                actor.beginRemoval();
            }

            terminalEntityIds.remove(entityId);
            failedEntityIds.remove(entityId);
        }
    }

    private void removeAllMissingEntities() {
        if (actors.isEmpty()) {
            return;
        }

        for (IZombieSnapshotEntityActor actor : actors.values()) {
            if (actor != null) {
                actor.beginRemoval();
            }
        }

        actors.clear();
        terminalEntityIds.clear();
        failedEntityIds.clear();
    }

    private void sortByBoardDepth() {
        getChildren().sort((first, second) -> {
            if (!(first instanceof IZombieSnapshotEntityActor firstEntity) ||
                !(second instanceof IZombieSnapshotEntityActor secondEntity)) {
                return 0;
            }

            int rowComparison = Integer.compare(firstEntity.getTargetRow(), secondEntity.getTargetRow());

            if (rowComparison != 0) {
                return rowComparison;
            }

            int kindComparison = Integer.compare(kindPriority(firstEntity.getKind()), kindPriority(secondEntity.getKind()));

            if (kindComparison != 0) {
                return kindComparison;
            }

            return Float.compare(firstEntity.getX(), secondEntity.getX());
        });
    }

    private int kindPriority(IZombieEntityKind kind) {
        if (kind == null) {
            return 0;
        }

        return switch (kind) {
            case PLANT -> 0;
            case ZOMBIE -> 1;
            case PROJECTILE -> 2;
        };
    }

    private int countByKind(IZombieEntityKind kind) {
        int count = 0;

        for (IZombieSnapshotEntityActor actor : actors.values()) {
            if (actor != null && actor.getKind() == kind && !actor.isRemovalStarted()) {
                count++;
            }
        }

        return count;
    }

    private void logVisualError(IZombieEntitySnapshot snapshot, RuntimeException exception) {
        if (Gdx.app == null) {
            return;
        }

        Gdx.app.error("IZombieMultiplayer", "Could not render entity " + snapshot.entityId() +
            " with definition " + snapshot.definitionKey(), exception);
    }
}
