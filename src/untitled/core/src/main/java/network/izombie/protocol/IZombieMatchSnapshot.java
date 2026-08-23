package network.izombie.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record IZombieMatchSnapshot(String matchId, int stageNumber, long serverTick, long remainingTicks,
                                   int ticksPerSecond, IZombieMatchStatus status, int redLineColumn,
                                   IZombiePlayerSnapshot plantPlayer, IZombiePlayerSnapshot zombiePlayer,
                                   List<IZombieEntitySnapshot> entities, Map<Integer, Boolean> brainStates) {
    public IZombieMatchSnapshot(
        String matchId,
        int stageNumber,
        long serverTick,
        long remainingTicks,
        int ticksPerSecond,
        IZombieMatchStatus status,
        int redLineColumn,
        IZombiePlayerSnapshot plantPlayer,
        IZombiePlayerSnapshot zombiePlayer,
        List<IZombieEntitySnapshot> entities,
        Map<Integer, Boolean> brainStates
    ) {
        this.matchId = matchId;
        this.stageNumber = stageNumber;
        this.serverTick = serverTick;
        this.remainingTicks = remainingTicks;
        this.ticksPerSecond = ticksPerSecond;
        this.status = status;
        this.redLineColumn = redLineColumn;
        this.plantPlayer = plantPlayer;
        this.zombiePlayer = zombiePlayer;
        this.entities = copyEntities(entities);
        this.brainStates = copyBrainStates(brainStates);
    }

    private List<IZombieEntitySnapshot> copyEntities(
        List<IZombieEntitySnapshot> source
    ) {
        if (source == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
            new ArrayList<>(source)
        );
    }

    private Map<Integer, Boolean> copyBrainStates(
        Map<Integer, Boolean> source
    ) {
        if (source == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(
            new LinkedHashMap<>(source)
        );
    }

    public IZombiePlayerSnapshot getPlayer(
        IZombieRole role
    ) {
        if (role == IZombieRole.PLANTS) {
            return this.plantPlayer;
        }

        if (role == IZombieRole.ZOMBIES) {
            return this.zombiePlayer;
        }

        return null;
    }

    public boolean isBrainEaten(int row) {
        return this.brainStates.getOrDefault(row, false);
    }

    public boolean isFinished() {
        return this.status == IZombieMatchStatus.PLANTS_WON
            || this.status == IZombieMatchStatus.ZOMBIES_WON
            || this.status == IZombieMatchStatus.CANCELLED;
    }
}
