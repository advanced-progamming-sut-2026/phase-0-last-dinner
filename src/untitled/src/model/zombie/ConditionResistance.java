package model.zombie;

public class ConditionResistance {
    private final ZombieCondition condition;
    private final double resistancePercent;
    private final boolean immune;

    public ConditionResistance(ZombieCondition condition, double resistancePercent, boolean immune) {
        this.condition = condition;
        this.resistancePercent = Math.max(0, Math.min(100, resistancePercent));
        this.immune = immune;
    }

    public ZombieCondition getCondition() {
        return this.condition;
    }

    public double getResistancePercent() {
        return this.resistancePercent;
    }

    public boolean isImmune() {
        return this.immune;
    }
}
