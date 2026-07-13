package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.PlantingSystem;
import model.mechanism.Position;
import model.plant.DamageExpressionParser;
import model.plant.PlantUpgradeEffect;
import model.plant.Projectile;
import model.zombie.ArmorFlag;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;
import model.zombie.ZombieCondition;
import model.zombie.behavior.FlyingBehavior;

import java.util.Collections;
import java.util.List;
import java.util.Random;

// effect haye moshtarak plant food ro ba yek config ejra mikone
public class ConfiguredPlantFoodBehavior implements PlantFoodBehavior {
    private PlantFoodEffectType effectType;
    private String damageExpression;
    private int activationCount;
    private int targetCount;
    private int radius;
    private int sunAmount;
    private int bonusHealth;
    private long conditionDurationTicks;
    private int poisonDamagePerTick;
    private Projectile projectileTemplate;
    private boolean randomTargets;
    private boolean groundTargetsOnly;
    private boolean metallicTargetsOnly;
    private int maxTargetRange;
    private Random random = new Random();

    public ConfiguredPlantFoodBehavior(String effectDescription, int activationCount) {
        this(PlantFoodEffectType.REPEAT_ABILITY, effectDescription, "0", activationCount, activationCount, 1, 0, 0, null);
    }

    public ConfiguredPlantFoodBehavior(
            PlantFoodEffectType effectType,
            String effectDescription,
            String damageExpression,
            int activationCount,
            int targetCount,
            int radius,
            int sunAmount,
            int bonusHealth,
            Projectile projectileTemplate
    ) {
        this.effectType = effectType;
        this.damageExpression = damageExpression;
        this.activationCount = activationCount;
        this.targetCount = targetCount;
        this.radius = radius;
        this.sunAmount = sunAmount;
        this.bonusHealth = bonusHealth;
        this.conditionDurationTicks = 0;
        this.poisonDamagePerTick = 0;
        this.projectileTemplate = projectileTemplate;
    }

    public ConfiguredPlantFoodBehavior withTimedCondition(long durationTicks, int damagePerTick) {
        this.conditionDurationTicks = Math.max(1, durationTicks);
        this.poisonDamagePerTick = Math.max(0, damagePerTick);
        return this;
    }

    public ConfiguredPlantFoodBehavior withRandomTargets() {
        this.randomTargets = true;
        return this;
    }

    public ConfiguredPlantFoodBehavior withGroundTargetsOnly() {
        this.groundTargetsOnly = true;
        return this;
    }

    public ConfiguredPlantFoodBehavior withMetallicTargetsOnly() {
        this.metallicTargetsOnly = true;
        return this;
    }

    public ConfiguredPlantFoodBehavior withTargetRange(int range) {
        this.maxTargetRange = Math.max(0, range);
        return this;
    }

    public void setRandom(Random random) {
        this.random = random == null ? new Random() : random;
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || this.effectType == null || this.effectType == PlantFoodEffectType.NONE) {
            return;
        }

        switch (this.effectType) {
            case SUN_BURST:
                this.growSunProducer(plant);
                this.addSun(board);
                break;
            case PROJECTILE_BURST:
                this.fireProjectiles(plant, board);
                break;
            case TARGETED_DAMAGE:
                this.affectZombies(board, this.getTargetZombies(plant, board, this.targetCount), null, false);
                break;
            case LANE_DAMAGE:
                this.affectZombies(board, board == null ? null : board.getZombiesInLane(plant.getPosition()), null, false);
                break;
            case BOARD_DAMAGE:
                this.affectZombies(board, board == null ? null : board.getAllZombies(), null, false);
                break;
            case AREA_DAMAGE:
                this.affectZombies(board, board == null ? null : board.getZombiesInRadius(plant.getPosition(), this.radius), null, false);
                break;
            case FREEZE_LANE:
                this.affectZombies(board, board == null ? null : board.getZombiesInLane(plant.getPosition()), ZombieCondition.FROZEN, false);
                this.repeatAbility(plant);
                break;
            case FREEZE_BOARD:
                this.affectZombies(board, board == null ? null : board.getAllZombies(), ZombieCondition.FROZEN, false);
                break;
            case POISON_TARGETS:
                this.affectZombies(board, this.getTargetZombies(plant, board, this.targetCount), ZombieCondition.POISONED, false);
                this.repeatAbility(plant);
                break;
            case HYPNOTIZE_TARGETS:
                this.affectZombies(board, this.getTargetZombies(plant, board, this.targetCount), ZombieCondition.HYPNOTIZED, false);
                break;
            case REMOVE_ARMOR:
                this.affectZombies(board, this.getTargetZombies(plant, board, this.targetCount), null, true);
                break;
            case ARM_AND_CLONE:
                this.armExplosiveBehavior(plant);
                this.cloneNearby(plant, board);
                break;
            case ARMOR_BOOST:
                plant.addBonusHealth(this.bonusHealth);
                break;
            case HEAL_TO_FULL:
                plant.healToFull();
                this.repeatAbility(plant);
                break;
            case LANE_SHIFT:
                this.shiftLane(plant, board);
                break;
            case RESET_SAME_PLANTS:
                this.resetSamePlants(plant, board);
                this.repeatAbility(plant);
                break;
            case CLONE_NEARBY:
                this.cloneNearby(plant, board);
                break;
            case PROJECTILE_BUFF:
                this.boostProjectileModifier(plant);
                break;
            case REPEAT_ABILITY:
            default:
                this.repeatAbility(plant);
                break;
        }
    }

    private void repeatAbility(Plant plant) {
        int count = Math.max(1, this.activationCount);

        for (int i = 0; i < count; i++) {
            plant.useAbility();
        }
    }

    private void addSun(Board board) {
        if (board == null || board.getSunSystem() == null || this.sunAmount <= 0) {
            return;
        }

        board.getSunSystem().addSun(this.sunAmount);
    }

    private void fireProjectiles(Plant plant, Board board) {
        if (board == null || this.projectileTemplate == null) {
            this.repeatAbility(plant);
            return;
        }

        int count = Math.max(1, this.activationCount);
        List<Zombie> targets = this.getTargetZombies(plant, board, count);

        for (int i = 0; i < count; i++) {
            if (targets.isEmpty()) {
                board.addProjectile(this.projectileTemplate.copyAt(plant.getPosition()));
            } else {
                board.addProjectile(this.projectileTemplate.copyAtTarget(
                        plant.getPosition(),
                        targets.get(i % targets.size())
                ));
            }
        }
    }

    private List<Zombie> getTargetZombies(Plant plant, Board board, int limit) {
        if (plant == null || board == null) {
            return Collections.emptyList();
        }

        List<Zombie> targets = board.getAllZombies();
        targets.removeIf(zombie -> !this.isEligibleTarget(plant, zombie));

        if (this.randomTargets) {
            Collections.shuffle(targets, this.random);
        } else {
            targets.sort((first, second) -> Long.compare(
                    this.distanceSquared(plant, first),
                    this.distanceSquared(plant, second)
            ));
        }

        int targetLimit = Math.max(1, limit);

        if (targets.size() > targetLimit) {
            targets.subList(targetLimit, targets.size()).clear();
        }

        return targets;
    }

    private boolean isEligibleTarget(Plant plant, Zombie zombie) {
        if (zombie == null || zombie.isDead() || zombie.isHypnotized() || zombie.getPosition() == null) {
            return false;
        }

        boolean lobbedProjectile = this.projectileTemplate != null && this.projectileTemplate.isLobbed();

        if (zombie.hasCondition(ZombieCondition.SUBMERGED) && !lobbedProjectile) {
            return false;
        }

        if (this.groundTargetsOnly && (zombie.findBehavior(FlyingBehavior.class) != null
                || zombie.hasCondition(ZombieCondition.FLYING)
                || zombie.hasCondition(ZombieCondition.SUBMERGED))) {
            return false;
        }

        if (this.metallicTargetsOnly && this.findMetallicArmor(zombie) == null) {
            return false;
        }

        if (this.maxTargetRange <= 0 || plant.getPosition() == null) {
            return true;
        }

        int deltaX = Math.abs(zombie.getPosition().getX() - plant.getPosition().getX());
        int deltaY = Math.abs(zombie.getPosition().getY() - plant.getPosition().getY());
        return Math.max(deltaX, deltaY) <= this.maxTargetRange;
    }

    private long distanceSquared(Plant plant, Zombie zombie) {
        if (plant == null || plant.getPosition() == null || zombie == null || zombie.getPosition() == null) {
            return Long.MAX_VALUE;
        }

        long deltaX = zombie.getPosition().getX() - plant.getPosition().getX();
        long deltaY = zombie.getPosition().getY() - plant.getPosition().getY();
        return deltaX * deltaX + deltaY * deltaY;
    }

    private void affectZombies(
            Board board,
            List<Zombie> zombies,
            ZombieCondition condition,
            boolean removeArmor
    ) {
        if (board == null || zombies == null || zombies.isEmpty()) {
            return;
        }

        int remainingTargets = this.targetCount <= 0 ? zombies.size() : Math.min(this.targetCount, zombies.size());

        for (int i = 0; i < remainingTargets; i++) {
            Zombie zombie = zombies.get(i);

            if (zombie == null || zombie.isDead() || zombie.isHypnotized()
                    || zombie.hasCondition(ZombieCondition.SUBMERGED)) {
                continue;
            }

            if (condition != null) {
                if (this.conditionDurationTicks > 0) {
                    zombie.addCondition(condition, this.conditionDurationTicks);
                } else {
                    zombie.addCondition(condition);
                }

                if (condition == ZombieCondition.POISONED && this.poisonDamagePerTick > 0) {
                    zombie.addPoisonDamagePerTick(this.poisonDamagePerTick);
                }
            }

            if (removeArmor) {
                this.dropMetallicArmor(zombie);
            }

            if (board.getCombatSystem() == null || this.damageExpression == null) {
                continue;
            }

            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                board.getCombatSystem().killZombie(zombie);
            } else {
                int damage = DamageExpressionParser.parseTotalDamage(this.damageExpression);

                if (damage > 0) {
                    if (condition == ZombieCondition.POISONED) {
                        board.getCombatSystem().applyDirectDamageToZombie(zombie, damage);
                    } else {
                        board.getCombatSystem().applyDamageToZombie(zombie, damage);
                    }
                }
            }
        }
    }

    private void armExplosiveBehavior(Plant plant) {
        if (plant.getBehavior() instanceof ExplosiveBehavior) {
            ((ExplosiveBehavior) plant.getBehavior()).armNow();
        }
    }

    private void resetSamePlants(Plant plant, Board board) {
        if (board == null || plant.getName() == null) {
            return;
        }

        for (Plant samePlant : board.getPlantsByName(plant.getName())) {
            if (samePlant != null) {
                samePlant.healToFull();
                samePlant.resetLifespan();
            }
        }
    }

    private void shiftLane(Plant plant, Board board) {
        if (plant == null || board == null || plant.getPosition() == null) {
            return;
        }

        List<Zombie> laneZombies = board.getZombiesInLane(plant.getPosition());

        for (Zombie zombie : laneZombies) {
            if (zombie == null || zombie.isDead() || zombie.isHypnotized()
                    || zombie.getPosition() == null
                    || zombie.findBehavior(FlyingBehavior.class) != null
                    || zombie.hasCondition(ZombieCondition.FLYING)
                    || zombie.hasCondition(ZombieCondition.SUBMERGED)) {
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

    private void cloneNearby(Plant plant, Board board) {
        if (plant == null || board == null || plant.getPosition() == null) {
            return;
        }

        int cloneCount = Math.max(1, this.activationCount);
        int placed = 0;
        PlantingSystem placement = new PlantingSystem(board, null, null);

        for (int distance = 1; distance <= 2 && placed < cloneCount; distance++) {
            for (int deltaY = -distance; deltaY <= distance && placed < cloneCount; deltaY++) {
                for (int deltaX = -distance; deltaX <= distance && placed < cloneCount; deltaX++) {
                    if (Math.abs(deltaX) != distance && Math.abs(deltaY) != distance) {
                        continue;
                    }

                    Position clonePosition = new Position(
                            plant.getPosition().getX() + deltaX,
                            plant.getPosition().getY() + deltaY
                    );
                    Plant clone = plant.copyForPlantFood(clonePosition);

                    if (!placement.canPlant(clone, clonePosition)) {
                        continue;
                    }

                    placement.plant(clone, clonePosition);
                    placed++;
                }
            }
        }
    }

    @Override
    public boolean canActivate() {
        return this.effectType != null && this.effectType != PlantFoodEffectType.NONE;
    }

    private void growSunProducer(Plant plant) {
        if (plant != null && plant.getBehavior() instanceof SunProducerBehavior) {
            ((SunProducerBehavior) plant.getBehavior()).growToMaximum();
        }
    }

    private void boostProjectileModifier(Plant plant) {
        if (plant != null && plant.getBehavior() instanceof ModifierBehavior) {
            ((ModifierBehavior) plant.getBehavior()).boostProjectilesFor(100);
        }
    }

    @Override
    public PlantFoodBehavior copy() {
        Projectile projectileCopy = this.projectileTemplate == null
                ? null
                : this.projectileTemplate.copyAt(null);
        ConfiguredPlantFoodBehavior copy = new ConfiguredPlantFoodBehavior(
                this.effectType,
                null,
                this.damageExpression,
                this.activationCount,
                this.targetCount,
                this.radius,
                this.sunAmount,
                this.bonusHealth,
                projectileCopy
        );
        copy.conditionDurationTicks = this.conditionDurationTicks;
        copy.poisonDamagePerTick = this.poisonDamagePerTick;
        copy.randomTargets = this.randomTargets;
        copy.groundTargetsOnly = this.groundTargetsOnly;
        copy.metallicTargetsOnly = this.metallicTargetsOnly;
        copy.maxTargetRange = this.maxTargetRange;
        return copy;
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        this.damageExpression = DamageExpressionParser.addFlatDamage(this.damageExpression, effect.getDamageBonus());
        this.targetCount += effect.getTargetCountBonus();
        this.radius += effect.getRangeBonus();
        this.sunAmount += effect.getSunProductionBonus();
        this.bonusHealth += effect.getHealthBonus();
        this.conditionDurationTicks += effect.getDurationBonusTicks();
        this.poisonDamagePerTick += effect.getPoisonDamageBonusPerTick();

        if (this.maxTargetRange > 0) {
            this.maxTargetRange += effect.getRangeBonus();
        }

        if (this.projectileTemplate != null) {
            this.projectileTemplate.addDamageBonus(effect.getDamageBonus());
            this.projectileTemplate.addPierceBonus(effect.getPierceBonus());
            this.projectileTemplate.addBounceBonus(effect.getBounceBonus());
            this.projectileTemplate.addRangeBonus(effect.getRangeBonus());
            this.projectileTemplate.addConditionDuration(effect.getDurationBonusTicks());
            this.projectileTemplate.addPoisonDamagePerTick(effect.getPoisonDamageBonusPerTick());
            this.projectileTemplate.addPlantFoodChanceBonus(effect.getPlantFoodChanceBonusPercent());
        }
    }

    private void dropMetallicArmor(Zombie zombie) {
        ZombieArmor armor = this.findMetallicArmor(zombie);

        if (armor != null) {
            armor.drop();
        }
    }

    private ZombieArmor findMetallicArmor(Zombie zombie) {
        if (zombie == null || zombie.getArmors() == null) {
            return null;
        }

        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor != null && !armor.isDestroyed() && !armor.isDropped()
                    && armor.getDefinition() != null && armor.getDefinition().getFlags() != null
                    && armor.getDefinition().getFlags().contains(ArmorFlag.METALLIC)) {
                return armor;
            }
        }

        return null;
    }
}
