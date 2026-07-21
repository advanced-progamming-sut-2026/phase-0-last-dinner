package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.plant.DamageExpressionParser;
import model.plant.PlantUpgradeEffect;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import model.zombie.behavior.FlyingBehavior;

import java.util.List;

// halat haye khas defender mesl reflect va avaz kardan lane ro ejra mikone
public class DefenderBehavior implements PlantBehavior {
    private DefenderMode defenderMode;
    private String damageExpression;
    private boolean deathEffectUsed;
    private int contactSunAmount = 5;
    private double reflectionDamageMultiplier = 1.0;
    // hp giah ghabl az armor plant food marz shekast armor hesab mishe
    private int plantFoodArmorBreakHealth = -1;
    private boolean armorBreakExplosionArmed;
    private boolean armorBreakExplosionUsed;

    public DefenderBehavior() {
        this(DefenderMode.BASIC, "0");
    }

    public DefenderBehavior(DefenderMode defenderMode, String damageExpression) {
        this.defenderMode = defenderMode;
        this.damageExpression = damageExpression;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        if (plant == null || board == null) {
            return;
        }

        if (plant.isDead()) {
            this.activateDeathEffect(plant, board);
            return;
        }

        if (this.defenderMode == DefenderMode.ATTRACT_ZOMBIES) {
            this.pullNearbyZombiesIntoLane(plant, board);
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (this.defenderMode == DefenderMode.EXPLODE_ON_DEATH) {
            this.activateDeathEffect(plant, board);
        } else if (this.defenderMode == DefenderMode.ATTRACT_ZOMBIES
                && plant != null && plant.getPosition() != null && board != null) {
            for (Zombie zombie : board.getAllZombies()) {
                if (zombie != null && !zombie.isDead() && !zombie.isHypnotized()
                        && zombie.findBehavior(FlyingBehavior.class) == null
                        && !zombie.hasCondition(ZombieCondition.SUBMERGED)
                        && zombie.getPosition() != null) {
                    board.moveZombie(
                            zombie,
                            new Position(zombie.getPosition().getX(), plant.getPosition().getY())
                    );
                }
            }
        }
    }

    private void damageNearbyZombies(Plant plant, Board board) {
        if (board.getCombatSystem() == null) {
            return;
        }

        int baseDamage = Math.max(10, DamageExpressionParser.parseTotalDamage(this.damageExpression));
        int damage = Math.max(1, (int) Math.round(baseDamage * this.reflectionDamageMultiplier));

        for (Zombie zombie : this.getContactZombies(plant, board)) {
            board.getCombatSystem().applyDamageToZombie(zombie, damage, plant);
        }
    }

    private void moveContactZombiesToAnotherLane(Plant plant, Board board) {
        List<Zombie> zombies = this.getContactZombies(plant, board);

        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.isHypnotized() || zombie.getPosition() == null) {
                continue;
            }

            Position upperLane = new Position(zombie.getPosition().getX(), zombie.getPosition().getY() - 1);

            if (board.moveZombie(zombie, upperLane)) {
                continue;
            }

            Position lowerLane = new Position(zombie.getPosition().getX(), zombie.getPosition().getY() + 1);
            board.moveZombie(zombie, lowerLane);
        }
    }

    private void pullNearbyZombiesIntoLane(Plant plant, Board board) {
        if (plant.getPosition() == null) {
            return;
        }

        for (Zombie zombie : board.getZombiesInRadius(plant.getPosition(), 1)) {
            if (zombie == null || zombie.isDead() || zombie.isHypnotized()
                    || zombie.getPosition() == null
                    || zombie.findBehavior(FlyingBehavior.class) != null
                    || zombie.hasCondition(ZombieCondition.SUBMERGED)) {
                continue;
            }

            if (zombie.getPosition().getY() == plant.getPosition().getY()) {
                continue;
            }

            board.moveZombie(zombie, new Position(zombie.getPosition().getX(), plant.getPosition().getY()));
        }
    }

    @Override
    public void onDamaged(Plant plant, Board board, int damage) {
        if (damage <= 0 || board == null) {
            return;
        }

        if (this.defenderMode == DefenderMode.SUN_ON_HIT && board.getSunSystem() != null) {
            board.getSunSystem().addSun(this.contactSunAmount);
        } else if (this.defenderMode == DefenderMode.REFLECT_DAMAGE) {
            this.damageNearbyZombies(plant, board);
        } else if (this.defenderMode == DefenderMode.MOVE_ZOMBIES) {
            this.moveContactZombiesToAnotherLane(plant, board);
        }

        if (this.armorBreakExplosionArmed && !this.armorBreakExplosionUsed
                && plant != null && plant.getHealth() <= this.plantFoodArmorBreakHealth) {
            this.armorBreakExplosionUsed = true;
            this.explodeNearby(plant, board);
        }
    }

    @Override
    public void onDeath(Plant plant, Board board) {
        this.activateDeathEffect(plant, board);
    }

    @Override
    public PlantBehavior copy() {
        DefenderBehavior copy = new DefenderBehavior(this.defenderMode, this.damageExpression);
        copy.contactSunAmount = this.contactSunAmount;
        copy.reflectionDamageMultiplier = this.reflectionDamageMultiplier;
        copy.plantFoodArmorBreakHealth = this.plantFoodArmorBreakHealth;
        copy.armorBreakExplosionArmed = this.armorBreakExplosionArmed;
        copy.armorBreakExplosionUsed = this.armorBreakExplosionUsed;
        return copy;
    }

    public void grantPlantFoodArmor(Plant plant, int armorHealth, boolean explodeOnBreak) {
        if (plant == null || armorHealth <= 0) {
            return;
        }

        this.plantFoodArmorBreakHealth = plant.getHealth();
        plant.addBonusHealth(armorHealth);
        this.armorBreakExplosionArmed = explodeOnBreak;
        this.armorBreakExplosionUsed = false;
    }

    public void multiplyReflectionDamage(double multiplier) {
        if (multiplier > 1.0) {
            this.reflectionDamageMultiplier *= multiplier;
        }
    }

    private void activateDeathEffect(Plant plant, Board board) {
        if (this.deathEffectUsed || plant == null || board == null || board.getCombatSystem() == null
                || this.defenderMode != DefenderMode.EXPLODE_ON_DEATH) {
            return;
        }

        this.deathEffectUsed = true;

        this.explodeNearby(plant, board);
    }

    private void explodeNearby(Plant plant, Board board) {
        if (plant == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        for (Zombie zombie : board.getZombiesInRadius(plant.getPosition(), 1)) {
            if (zombie == null || zombie.isHypnotized()
                    || zombie.hasCondition(ZombieCondition.SUBMERGED)) {
                continue;
            }

            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                board.getCombatSystem().killZombie(zombie, plant);
            } else {
                int damage = Math.max(180, DamageExpressionParser.parseTotalDamage(this.damageExpression));
                board.getCombatSystem().applyDamageToZombie(zombie, damage, plant);
            }
        }
    }

    private List<Zombie> getContactZombies(Plant plant, Board board) {
        List<Zombie> contactZombies = new java.util.ArrayList<>();

        if (plant == null || plant.getPosition() == null || board == null) {
            return contactZombies;
        }

        for (Zombie zombie : board.getZombiesInLane(plant.getPosition())) {
            if (zombie != null && !zombie.isDead() && !zombie.isHypnotized()
                    && zombie.getPosition() != null
                    && zombie.findBehavior(FlyingBehavior.class) == null
                    && !zombie.hasCondition(ZombieCondition.SUBMERGED)
                    && Math.abs(zombie.getPosition().getX() - plant.getPosition().getX()) <= 1) {
                contactZombies.add(zombie);
            }
        }

        return contactZombies;
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        this.damageExpression = DamageExpressionParser.addFlatDamage(this.damageExpression, effect.getDamageBonus());
        this.contactSunAmount += effect.getSunDropBonus();
    }
}
