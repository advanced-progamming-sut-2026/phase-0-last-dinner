package network.izombie.client;

import network.izombie.protocol.IZombieEntityKind;
import network.izombie.protocol.IZombieEntitySnapshot;
import network.izombie.protocol.IZombieMatchSnapshot;
import network.izombie.protocol.IZombieMatchStatus;
import network.izombie.protocol.IZombiePlayerSnapshot;
import network.izombie.protocol.IZombieRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class IZombieClientGameData {

    private static final int DEFAULT_TICKS_PER_SECOND = 10;
    private static final int DEFAULT_RED_LINE_COLUMN = 5;

    private final IZombieClientMatchState state;

    public IZombieClientGameData(IZombieClientMatchState state) {
        if (state == null) {
            throw new IllegalArgumentException("Client match state is required.");
        }

        this.state = state;
    }

    public IZombieMatchSnapshot getSnapshot() {
        return state.getSnapshot();
    }

    public boolean hasSnapshot() {
        return getSnapshot() != null;
    }

    public String getMatchId() {
        IZombieMatchSnapshot snapshot = getSnapshot();

        return snapshot == null ? state.getMatchId() : snapshot.matchId();
    }

    public int getStageNumber() {
        IZombieMatchSnapshot snapshot = getSnapshot();

        return snapshot == null ? state.getStageNumber() : snapshot.stageNumber();
    }

    public IZombieRole getRole() {
        return state.getRole();
    }

    public IZombieMatchStatus getMatchStatus() {
        IZombieMatchSnapshot snapshot = getSnapshot();

        return snapshot == null ? null : snapshot.status();
    }

    public long getServerTick() {
        IZombieMatchSnapshot snapshot = getSnapshot();

        return snapshot == null ? 0 : snapshot.serverTick();
    }

    public long getRemainingTicks() {
        IZombieMatchSnapshot snapshot = getSnapshot();

        return snapshot == null ? 0 : Math.max(0, snapshot.remainingTicks());
    }

    public int getTicksPerSecond() {
        IZombieMatchSnapshot snapshot = getSnapshot();

        if (snapshot == null || snapshot.ticksPerSecond() <= 0) {

            return DEFAULT_TICKS_PER_SECOND;
        }

        return snapshot.ticksPerSecond();
    }

    public int getRemainingSeconds() {
        long remainingTicks = getRemainingTicks();
        int ticksPerSecond = getTicksPerSecond();

        return (int) ((remainingTicks + ticksPerSecond - 1) / ticksPerSecond);
    }

    public int getRedLineColumn() {
        IZombieMatchSnapshot snapshot = getSnapshot();

        if (snapshot == null)
            return DEFAULT_RED_LINE_COLUMN;

        int column = snapshot.redLineColumn();

        return column >= 0 && column < 9 ? column : DEFAULT_RED_LINE_COLUMN;
    }

    public IZombiePlayerSnapshot getCurrentPlayer() {
        IZombieMatchSnapshot snapshot = getSnapshot();
        IZombieRole role = getRole();

        if (snapshot == null || role == null) {
            return null;
        }

        return role == IZombieRole.PLANTS ? snapshot.plantPlayer() : snapshot.zombiePlayer();
    }

    public IZombiePlayerSnapshot getOpponentPlayer() {
        IZombieMatchSnapshot snapshot = getSnapshot();
        IZombieRole role = getRole();

        if (snapshot == null || role == null) {
            return null;
        }

        return role == IZombieRole.PLANTS ? snapshot.zombiePlayer() : snapshot.plantPlayer();
    }

    public int getSunAmount() {
        IZombiePlayerSnapshot player = getCurrentPlayer();

        return player == null ? 0 : Math.max(0, player.sunAmount());
    }

    public List<String> getAvailableUnits() {
        IZombiePlayerSnapshot player = getCurrentPlayer();

        if (player == null || player.availableUnits() == null) {
            return Collections.emptyList();
        }

        return List.copyOf(player.availableUnits());
    }

    public int getUnitCost(String unitKey) {
        IZombiePlayerSnapshot player = getCurrentPlayer();

        if (player == null || player.unitCosts() == null) {
            return 0;
        }

        String resolvedKey = resolveUnitKey(unitKey);

        if (resolvedKey == null) {
            return 0;
        }

        return Math.max(0, player.unitCosts().getOrDefault(resolvedKey, 0));
    }

    public int getCooldownTicks(String unitKey) {
        IZombiePlayerSnapshot player = getCurrentPlayer();

        if (player == null || player.cooldownTicks() == null) {
            return 0;
        }

        String resolvedKey = resolveUnitKey(unitKey);

        if (resolvedKey == null) {
            return 0;
        }

        return Math.max(0, player.cooldownTicks().getOrDefault(resolvedKey, 0));
    }

    public float getCooldownSeconds(String unitKey) {
        return (float) getCooldownTicks(unitKey) / getTicksPerSecond();
    }

    public boolean canAfford(String unitKey) {
        String resolvedKey = resolveUnitKey(unitKey);

        return resolvedKey != null && getSunAmount() >= getUnitCost(resolvedKey);
    }

    public boolean isUnitReady(String unitKey) {
        String resolvedKey = resolveUnitKey(unitKey);

        return resolvedKey != null && getCooldownTicks(resolvedKey) == 0;
    }

    public boolean canUseUnit(String unitKey) {
        return canAfford(unitKey) && isUnitReady(unitKey);
    }

    public String resolveUnitKey(String unitKey) {
        if (unitKey == null || unitKey.isBlank()) {
            return null;
        }

        for (String availableKey : getAvailableUnits()) {
            if (availableKey != null && availableKey.equalsIgnoreCase(unitKey.trim())) {
                return availableKey;
            }
        }

        return null;
    }

    public List<IZombieEntitySnapshot> getEntities() {
        IZombieMatchSnapshot snapshot = getSnapshot();

        if (snapshot == null || snapshot.entities() == null) {
            return Collections.emptyList();
        }

        return List.copyOf(snapshot.entities());
    }

    public List<IZombieEntitySnapshot> getPlants() {
        return getEntitiesByKind(IZombieEntityKind.PLANT);
    }

    public List<IZombieEntitySnapshot> getZombies() {
        return getEntitiesByKind(IZombieEntityKind.ZOMBIE);
    }

    public List<IZombieEntitySnapshot> getProjectiles() {
        return getEntitiesByKind(IZombieEntityKind.PROJECTILE);
    }

    private List<IZombieEntitySnapshot> getEntitiesByKind(IZombieEntityKind kind) {
        List<IZombieEntitySnapshot> result = new ArrayList<>();

        for (IZombieEntitySnapshot entity : getEntities()) {
            if (entity != null && entity.kind() == kind) {
                result.add(entity);
            }
        }

        return List.copyOf(result);
    }

    public IZombieEntitySnapshot findEntity(long entityId) {
        for (IZombieEntitySnapshot entity : getEntities()) {
            if (entity != null && entity.entityId() == entityId) {
                return entity;
            }
        }

        return null;
    }

    public List<Boolean> getBrainStates() {
        IZombieMatchSnapshot snapshot = getSnapshot();

        if (snapshot == null || snapshot.brainStates() == null) {
            return List.of(false, false, false, false, false);
        }

        return List.copyOf(snapshot.brainStates().values());
    }

    public boolean isBrainEaten(int zeroBasedRow) {
        List<Boolean> brainStates = getBrainStates();

        if (zeroBasedRow < 0 || zeroBasedRow >= brainStates.size()) {
            return false;
        }

        return Boolean.TRUE.equals(brainStates.get(zeroBasedRow));
    }

    public boolean canPlaceAt(int column, int row) {
        if (column < 0 || column >= 9 || row < 0 || row >= 5) {
            return false;
        }

        int redLineColumn = getRedLineColumn();

        if (getRole() == IZombieRole.PLANTS) {
            return column < redLineColumn;
        }

        if (getRole() == IZombieRole.ZOMBIES) {
            return column >= redLineColumn;
        }

        return false;
    }

    public boolean isFinished() {
        IZombieMatchStatus status = getMatchStatus();

        return status == IZombieMatchStatus.PLANTS_WON || status == IZombieMatchStatus.ZOMBIES_WON || status == IZombieMatchStatus.CANCELLED;
    }

    public boolean didCurrentPlayerWin() {
        IZombieMatchStatus status = getMatchStatus();
        IZombieRole role = getRole();

        return (role == IZombieRole.PLANTS && status == IZombieMatchStatus.PLANTS_WON) || (role == IZombieRole.ZOMBIES && status == IZombieMatchStatus.ZOMBIES_WON);
    }

    public Map<String, Integer> getUnitCosts() {
        IZombiePlayerSnapshot player = getCurrentPlayer();

        if (player == null || player.unitCosts() == null) {
            return Collections.emptyMap();
        }

        return Map.copyOf(player.unitCosts());
    }

    public Map<String, Integer> getCooldowns() {
        IZombiePlayerSnapshot player = getCurrentPlayer();

        if (player == null || player.cooldownTicks() == null) {
            return Collections.emptyMap();
        }

        return Map.copyOf(player.cooldownTicks());
    }
}
