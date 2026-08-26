package network.izombie.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record IZombiePlayerSnapshot(String username, IZombieRole role, int sunAmount, List<String> availableUnits,
                                    Map<String, Integer> unitCosts, Map<String, Integer> cooldownTicks) {
    public IZombiePlayerSnapshot(String username, IZombieRole role, int sunAmount, List<String> availableUnits,
                                 Map<String, Integer> unitCosts, Map<String, Integer> cooldownTicks) {
        this.username = username;
        this.role = role;
        this.sunAmount = sunAmount;
        this.availableUnits = copyUnits(availableUnits);
        this.unitCosts = copyValues(unitCosts);
        this.cooldownTicks = copyValues(cooldownTicks);
    }

    private List<String> copyUnits(List<String> source) {
        if (source == null) {
            return Collections.emptyList();
        }

        return List.copyOf(source);
    }

    private Map<String, Integer> copyValues(Map<String, Integer> source) {
        if (source == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    public int getUnitCost(String unitKey) {
        if (unitKey == null) {
            return -1;
        }

        return this.unitCosts.getOrDefault(unitKey, -1);
    }

    public int getCooldownTicks(String unitKey) {
        if (unitKey == null) {
            return 0;
        }

        return Math.max(0, this.cooldownTicks.getOrDefault(unitKey, 0));
    }

    public boolean canAfford(String unitKey) {
        int cost = getUnitCost(unitKey);

        return cost >= 0 && this.sunAmount >= cost;
    }

    public boolean isReady(String unitKey) {
        return getCooldownTicks(unitKey) == 0;
    }
}
