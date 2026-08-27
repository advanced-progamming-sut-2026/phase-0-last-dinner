package model.minigame.izombieminigame.multiplayer;

import lombok.Getter;
import network.izombie.protocol.IZombieRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IZombieMatchResources {
    @Getter
    private final IZombieRole role;

    @Getter
    private int sunAmount;

    private final List<String> availableUnits;
    private final Map<String, Integer> unitCosts;
    private final Map<String, Integer> baseCooldownTicks;
    private final Map<String, Integer> remainingCooldownTicks;

    public IZombieMatchResources(IZombieRole role, int startingSun, Map<String, Integer> unitCosts, Map<String,
        Integer> baseCooldownTicks) {

        if (role == null) {
            throw new IllegalArgumentException("Player role is required.");
        }

        if (startingSun < 0) {
            throw new IllegalArgumentException("Starting sun cannot be negative.");
        }

        if (unitCosts == null || unitCosts.isEmpty()) {
            throw new IllegalArgumentException("At least one available unit is required.");
        }

        this.role = role;
        this.sunAmount = startingSun;
        this.unitCosts = copyAndValidateCosts(unitCosts);
        this.availableUnits = new ArrayList<>(this.unitCosts.keySet());
        this.baseCooldownTicks = copyCooldowns(baseCooldownTicks);
        this.remainingCooldownTicks = createEmptyCooldowns();
    }

    public boolean canUse(String unitKey) {
        String resolvedKey = resolveUnitKey(unitKey);

        return resolvedKey != null && canAfford(resolvedKey) && isReady(resolvedKey);
    }

    public boolean canAfford(String unitKey) {
        int cost = getUnitCost(unitKey);

        return cost >= 0 && this.sunAmount >= cost;
    }

    public boolean isReady(String unitKey) {
        return getRemainingCooldownTicks(unitKey) == 0;
    }

    public boolean commitUse(String unitKey) {
        String resolvedKey = resolveUnitKey(unitKey);

        if (resolvedKey == null || !canUse(resolvedKey)) {
            return false;
        }

        int cost = this.unitCosts.get(resolvedKey);
        this.sunAmount -= cost;

        int cooldown = this.baseCooldownTicks.getOrDefault(resolvedKey, 0);
        this.remainingCooldownTicks.put(resolvedKey, cooldown);
        return true;
    }

    public void advanceOneTick() {
        for (Map.Entry<String, Integer> entry : this.remainingCooldownTicks.entrySet()) {
            int updatedCooldown = Math.max(0, entry.getValue() - 1);
            entry.setValue(updatedCooldown);
        }
    }

    public void addSun(int amount) {
        if (amount <= 0) {
            return;
        }

        long updatedAmount = (long) this.sunAmount + amount;
        this.sunAmount = updatedAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) updatedAmount;
    }

    public int getUnitCost(String unitKey) {
        String resolvedKey = resolveUnitKey(unitKey);

        if (resolvedKey == null) {
            return -1;
        }

        return this.unitCosts.get(resolvedKey);
    }

    public int getRemainingCooldownTicks(String unitKey) {
        String resolvedKey = resolveUnitKey(unitKey);

        if (resolvedKey == null) {
            return 0;
        }

        return Math.max(0, this.remainingCooldownTicks.getOrDefault(resolvedKey, 0));
    }

    public List<String> getAvailableUnits() {
        return Collections.unmodifiableList(new ArrayList<>(this.availableUnits));
    }

    public Map<String, Integer> getUnitCosts() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.unitCosts));
    }

    public Map<String, Integer> getRemainingCooldownTicks() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.remainingCooldownTicks));
    }

    public String resolveUnitKey(String unitKey) {
        if (unitKey == null || unitKey.trim().isEmpty()) {
            return null;
        }

        String searchedKey = unitKey.trim();

        for (String availableUnit : this.availableUnits) {
            if (availableUnit.equalsIgnoreCase(searchedKey)) {
                return availableUnit;
            }
        }

        return null;
    }

    private Map<String, Integer> copyAndValidateCosts(Map<String, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String unitKey = cleanUnitKey(entry.getKey());
            Integer cost = entry.getValue();

            if (unitKey == null || cost == null || cost < 0) {
                throw new IllegalArgumentException("Every unit must have a valid non-negative cost.");
            }

            if (containsIgnoringCase(result, unitKey)) {
                throw new IllegalArgumentException("Available unit keys must be unique.");
            }

            result.put(unitKey, cost);
        }

        return result;
    }

    private Map<String, Integer> copyCooldowns(Map<String, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (String unitKey : this.unitCosts.keySet()) {
            int cooldown = findCooldown(source, unitKey);

            if (cooldown < 0) {
                throw new IllegalArgumentException("Cooldown ticks cannot be negative.");
            }

            result.put(unitKey, cooldown);
        }

        return result;
    }

    private int findCooldown(Map<String, Integer> source, String unitKey) {
        if (source == null) {
            return 0;
        }

        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(unitKey)) {
                return entry.getValue() == null ? 0 : entry.getValue();
            }
        }

        return 0;
    }

    private Map<String, Integer> createEmptyCooldowns() {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (String unitKey : this.availableUnits) {
            result.put(unitKey, 0);
        }

        return result;
    }

    private boolean containsIgnoringCase(Map<String, Integer> values, String searchedKey) {
        for (String existingKey : values.keySet()) {
            if (existingKey.equalsIgnoreCase(searchedKey)) {
                return true;
            }
        }

        return false;
    }

    private String cleanUnitKey(String unitKey) {
        if (unitKey == null || unitKey.trim().isEmpty()) {
            return null;
        }

        return unitKey.trim();
    }
}
