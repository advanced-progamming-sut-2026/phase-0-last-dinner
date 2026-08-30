package model.minigame.izombieminigame.multiplayer;

import model.Plant;
import model.mechanism.Board;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;
import model.zombie.ZombieCondition;
import network.izombie.protocol.IZombieEntityKind;
import network.izombie.protocol.IZombieEntitySnapshot;
import network.izombie.protocol.IZombieMatchSnapshot;
import network.izombie.protocol.IZombieMatchStatus;
import network.izombie.protocol.IZombiePlayerSnapshot;
import network.izombie.protocol.IZombieRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IZombieSnapshotBuilder {
    private final String matchId;
    private final int stageNumber;
    private final String plantUsername;
    private final String zombieUsername;
    private final IZombieMultiplayerIntegration integration;
    private final IZombieMatchLoadout loadout;

    private final Map<Object, Long> entityIds = new IdentityHashMap<>();
    private long nextEntityId = 1;

    public IZombieSnapshotBuilder(String matchId, int stageNumber, String plantUsername, String zombieUsername,
                                  IZombieMultiplayerIntegration integration, IZombieMatchLoadout loadout) {
        if (matchId == null || matchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Match identifier is required.");
        }

        if (plantUsername == null || zombieUsername == null) {
            throw new IllegalArgumentException("Both player usernames are required.");
        }

        if (integration == null || loadout == null) {
            throw new IllegalArgumentException("Snapshot dependencies are required.");
        }

        this.matchId = matchId;
        this.stageNumber = stageNumber;
        this.plantUsername = plantUsername;
        this.zombieUsername = zombieUsername;
        this.integration = integration;
        this.loadout = loadout;
    }

    public IZombieMatchSnapshot build(long serverTick, long remainingTicks, IZombieMatchStatus status) {
        Board board = this.integration.getBoard();

        if (board == null) {
            throw new IllegalStateException("IZombie multiplayer board is not ready.");
        }

        List<IZombieEntitySnapshot> entities = buildEntities(board);
        Map<Integer, Boolean> brains = buildBrainStates();

        return new IZombieMatchSnapshot(this.matchId, this.stageNumber, serverTick, remainingTicks,
            IZombieMatchRules.TICKS_PER_SECOND, status, IZombieMatchRules.FIRST_ZOMBIE_COLUMN,
            buildPlantPlayerSnapshot(), buildZombiePlayerSnapshot(), entities, brains);
    }

    private IZombiePlayerSnapshot buildPlantPlayerSnapshot() {
        IZombieMatchResources resources = this.loadout.plantResources();

        return new IZombiePlayerSnapshot(this.plantUsername, IZombieRole.PLANTS, resources.getSunAmount(),
            resources.getAvailableUnits(), resources.getUnitCosts(), resources.getRemainingCooldownTicks());
    }

    private IZombiePlayerSnapshot buildZombiePlayerSnapshot() {
        IZombieMatchResources resources = this.loadout.zombieResources();

        return new IZombiePlayerSnapshot(this.zombieUsername, IZombieRole.ZOMBIES, resources.getSunAmount(),
            resources.getAvailableUnits(), resources.getUnitCosts(), resources.getRemainingCooldownTicks());
    }

    private List<IZombieEntitySnapshot> buildEntities(Board board) {
        List<IZombieEntitySnapshot> snapshots = new ArrayList<>();

        Set<Object> liveEntities = Collections.newSetFromMap(new IdentityHashMap<>());

        addPlants(board, snapshots, liveEntities);
        addZombies(board, snapshots, liveEntities);
        addProjectiles(board, snapshots, liveEntities);

        removeUnusedEntityIds(liveEntities);
        return snapshots;
    }

    private void addPlants(Board board, List<IZombieEntitySnapshot> snapshots, Set<Object> liveEntities) {
        for (Plant plant : board.getAllPlants()) {
            if (plant == null || plant.getPosition() == null) {
                continue;
            }

            liveEntities.add(plant);

            snapshots.add(new IZombieEntitySnapshot(getEntityId(plant), IZombieEntityKind.PLANT, plant.getName(),
                plant.getPosition().getX(), plant.getPosition().getY(), plant.getHealth(), plant.getMaximumHealth(),
                false, plant.isDead(), getPlantStates(plant)));
        }
    }

    private void addZombies(Board board, List<IZombieEntitySnapshot> snapshots, Set<Object> liveEntities) {
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie.getPosition() == null) {
                continue;
            }

            liveEntities.add(zombie);

            snapshots.add(new IZombieEntitySnapshot(getEntityId(zombie), IZombieEntityKind.ZOMBIE, getZombieKey(zombie),
                zombie.getExactX(), zombie.getPosition().getY(), zombie.getHealth(), zombie.getMaximumHealth(),
                zombie.isAttacking(), zombie.isDead(), getZombieStates(zombie)));
        }
    }

    private void addProjectiles(Board board, List<IZombieEntitySnapshot> snapshots, Set<Object> liveEntities) {
        for (Projectile projectile : board.getProjectiles()) {
            if (projectile == null || projectile.getPosition() == null) {
                continue;
            }

            liveEntities.add(projectile);

            snapshots.add(new IZombieEntitySnapshot(getEntityId(projectile), IZombieEntityKind.PROJECTILE,
                getProjectileKey(projectile), projectile.getExactX(), projectile.getExactY(), 0,
                0, false, projectile.isExpired(), getProjectileStates(projectile)));
        }
    }

    private List<String> getPlantStates(Plant plant) {
        List<String> states = new ArrayList<>();

        if (plant.isFrozen()) {
            states.add("FROZEN");
        }

        if (plant.isDisabled()) {
            states.add("DISABLED");
        }

        if (plant.isTransformed()) {
            states.add("TRANSFORMED");
        }

        if (plant.isCovered()) {
            states.add("COVERED");
        }

        if (plant.isTerrainDisabled()) {
            states.add("TERRAIN_DISABLED");
        }

        return states;
    }

    private List<String> getZombieStates(Zombie zombie) {
        List<String> states = new ArrayList<>();

        if (zombie.getConditions() != null) {
            for (ZombieCondition condition : zombie.getConditions()) {
                if (condition != null) {
                    states.add(condition.name());
                }
            }
        }

        if (zombie.getArmors() != null) {
            for (ZombieArmor armor : zombie.getArmors()) {
                if (armor == null || armor.isDestroyed() || armor.isDropped() || armor.getDefinition() == null ||
                    armor.getDefinition().getType() == null) {
                    continue;
                }

                int maximumHealth = Math.max(1, armor.getDefinition().getBaseHealth());

                float healthRatio = Math.max(0, armor.getCurrentHealth()) / (float) maximumHealth;

                String variant;

                if (healthRatio > 0.66f) {
                    variant = "NORM";
                } else if (healthRatio > 0.33f) {
                    variant = "DAMAGE_01";
                } else {
                    variant = "DAMAGE_02";
                }

                states.add("ARMOR:" + armor.getDefinition().getType().name() + ":" + variant);
            }
        }

        return states;
    }

    private List<String> getProjectileStates(Projectile projectile) {
        List<String> states = new ArrayList<>();

        if (projectile.isLobbed())
            states.add("LOBBED");
        if (projectile.isPeaBased())
            states.add("PEA_BASED");
        if (projectile.isHostileToPlants())
            states.add("HOSTILE_TO_PLANTS");

        Plant sourcePlant = projectile.getSourcePlant();

        if (sourcePlant != null)
            states.add("SOURCE_PLANT:" + getEntityId(sourcePlant));

        return states;
    }

    private Map<Integer, Boolean> buildBrainStates() {
        Map<Integer, Boolean> brainStates = new LinkedHashMap<>();

        for (int row = 0; row < IZombieMatchRules.BOARD_ROW_COUNT; row++) {
            boolean eaten = this.integration.isBrainEaten(row + 1);
            brainStates.put(row, eaten);
        }

        return brainStates;
    }

    private String getZombieKey(Zombie zombie) {
        if (zombie.getDefinition() == null) {
            return "";
        }

        String alias = zombie.getDefinition().getAlias();

        if (alias != null && !alias.trim().isEmpty()) {
            return alias;
        }

        String displayName = zombie.getDefinition().getDisplayName();
        return displayName == null ? "" : displayName;
    }

    private String getProjectileKey(Projectile projectile) {
        if (projectile.getType() == null) {
            return "NORMAL";
        }

        return projectile.getType().name();
    }

    private long getEntityId(Object entity) {
        Long existingId = this.entityIds.get(entity);

        if (existingId != null) {
            return existingId;
        }

        long createdId = this.nextEntityId++;
        this.entityIds.put(entity, createdId);
        return createdId;
    }

    private void removeUnusedEntityIds(Set<Object> liveEntities) {
        this.entityIds.keySet().removeIf(entity -> !liveEntities.contains(entity));
    }
}
