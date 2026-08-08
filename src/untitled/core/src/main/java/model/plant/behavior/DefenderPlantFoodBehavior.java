package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;

public class DefenderPlantFoodBehavior implements PlantFoodBehavior {
    public enum Boost {
        ENDURIAN_ARMOR,
        EXPLOSIVE_ARMOR
    }

    private Boost boost;
    private int armorHealth;

    public DefenderPlantFoodBehavior(Boost boost, int armorHealth) {
        this.boost = boost;
        this.armorHealth = Math.max(0, armorHealth);
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || !(plant.getBehavior() instanceof DefenderBehavior)) {
            return;
        }

        DefenderBehavior defender = (DefenderBehavior) plant.getBehavior();

        if (this.boost == Boost.ENDURIAN_ARMOR) {
            defender.grantPlantFoodArmor(plant, this.armorHealth, false);
            defender.multiplyReflectionDamage(2.0);
        } else if (this.boost == Boost.EXPLOSIVE_ARMOR) {
            defender.grantPlantFoodArmor(plant, this.armorHealth, true);
        }
    }

    @Override
    public PlantFoodBehavior copy() {
        return new DefenderPlantFoodBehavior(this.boost, this.armorHealth);
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect != null) {
            this.armorHealth += Math.max(0, effect.getHealthBonus());
        }
    }
}
