package model.plant;

import model.Plant;
import model.plant.behavior.ActivationTrigger;
import model.plant.behavior.ConfiguredPlantFoodBehavior;
import model.plant.behavior.DefenderBehavior;
import model.plant.behavior.DefenderMode;
import model.plant.behavior.ExplosiveBehavior;
import model.plant.behavior.ExplosivePattern;
import model.plant.behavior.HomingBehavior;
import model.plant.behavior.HomingTargetMode;
import model.plant.behavior.LobberBehavior;
import model.plant.behavior.MeleeBehavior;
import model.plant.behavior.MeleePattern;
import model.plant.behavior.ModifierBehavior;
import model.plant.behavior.ModifierType;
import model.plant.behavior.PlantBehavior;
import model.plant.behavior.PlantFoodEffectType;
import model.plant.behavior.PlantFoodBehavior;
import model.plant.behavior.ShooterBehavior;
import model.plant.behavior.ShooterPattern;
import model.plant.behavior.SunProducerBehavior;
import model.plant.behavior.SunProductionMode;
import model.zombie.ZombieCondition;

import java.util.Locale;
import java.util.Set;

public class PlantFactory {
    private static final int TICKS_PER_SECOND = 10;
    private final PlantUpgradeEffectParser upgradeEffectParser = new PlantUpgradeEffectParser();

    public Plant create(PlantDefinition definition) {
        return this.create(
                definition,
                this.createBehavior(definition),
                this.createPlantFoodBehavior(definition)
        );
    }

    public Plant create(
            PlantDefinition definition,
            PlantBehavior behavior,
            PlantFoodBehavior plantFoodBehavior
    ) {
        long cooldownTicks = this.secondsToTicks(definition.getRechargeSeconds());
        PlantUpgradeData upgradeData = this.createUpgradeData(definition);
        long lifespanTicks = this.createLifespanTicks(definition);

        return new Plant(
                definition.getName(),
                definition.getBaseHealth(),
                1,
                definition.getCost(),
                cooldownTicks,
                definition.getActionIntervalSeconds(),
                definition.getCategories(),
                definition.getTags(),
                behavior,
                plantFoodBehavior,
                upgradeData,
                lifespanTicks
        );
    }

    private PlantUpgradeData createUpgradeData(PlantDefinition definition) {
        if (definition == null || definition.getLevelUpEffects() == null
                || definition.getLevelUpEffects().isEmpty()) {
            return null;
        }

        return new PlantUpgradeData(this.upgradeEffectParser.parseAll(definition.getLevelUpEffects()));
    }

    private long createLifespanTicks(PlantDefinition definition) {
        String name = this.normalize(definition.getName());

        if (name.contains("sea-shroom") || name.contains("puff-shroom")) {
            return this.secondsToTicks(60);
        }

        return 0;
    }

    private PlantBehavior createBehavior(PlantDefinition definition) {
        String name = this.normalize(definition.getName());
        Set<PlantCategory> categories = definition.getCategories();

        if (this.isMint(name)) {
            return new ModifierBehavior(
                    ModifierType.FAMILY_BUFF,
                    ActivationTrigger.ON_PLANTING,
                    definition.getBaseAbilityDescription(),
                    99,
                    this.secondsToTicks(10)
            );
        }

        if (this.hasCategory(categories, PlantCategory.SUN_PRODUCER)) {
            return this.createSunProducerBehavior(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.HOMING)) {
            return this.createHomingBehavior(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.LOBBER)) {
            return new LobberBehavior(
                    this.createProjectile(definition, ProjectileType.LOBBED),
                    this.secondsToTicks(definition.getActionIntervalSeconds()),
                    this.hasTag(definition.getTags(), PlantTag.AOE)
            );
        }

        if (this.hasCategory(categories, PlantCategory.STRIKE_THROUGH)) {
            return new ShooterBehavior(
                    this.createProjectile(definition, ProjectileType.PIERCING),
                    ShootingDirection.FORWARD,
                    1,
                    0,
                    1,
                    this.secondsToTicks(definition.getActionIntervalSeconds()),
                    ShooterPattern.PIERCING_FORWARD
            );
        }

        if (this.hasCategory(categories, PlantCategory.SHOOTER)) {
            return this.createShooterBehavior(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.EXPLOSIVE)) {
            return this.createExplosiveBehavior(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.MELEE_ATTACKER)) {
            return this.createMeleeBehavior(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.DEFENDER)) {
            return this.createDefenderBehavior(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.MODIFIER)) {
            return this.createModifierBehavior(definition, name);
        }

        return new DefenderBehavior();
    }

    private PlantBehavior createSunProducerBehavior(PlantDefinition definition, String name) {
        if (name.contains("gold bloom")) {
            return new SunProducerBehavior(375, 0, SunProductionMode.INSTANT_ON_PLANTING);
        }

        if (name.contains("twin")) {
            return new SunProducerBehavior(100, this.secondsToTicks(definition.getActionIntervalSeconds()));
        }

        if (name.contains("primal")) {
            return new SunProducerBehavior(75, this.secondsToTicks(definition.getActionIntervalSeconds()));
        }

        if (name.contains("sun-shroom")) {
            return new SunProducerBehavior(25, this.secondsToTicks(definition.getActionIntervalSeconds()), SunProductionMode.RAMPING);
        }

        return new SunProducerBehavior(50, this.secondsToTicks(definition.getActionIntervalSeconds()));
    }

    private PlantBehavior createShooterBehavior(PlantDefinition definition, String name) {
        ShootingDirection direction = ShootingDirection.FORWARD;
        int forwardShotCount = 1;
        int backwardShotCount = 0;
        int laneCount = 1;
        ShooterPattern pattern = ShooterPattern.SINGLE_FORWARD;

        if (name.contains("repeater")) {
            forwardShotCount = 2;
        } else if (name.contains("mega gatling")) {
            forwardShotCount = 4;
        } else if (name.contains("split pea")) {
            direction = ShootingDirection.BOTH;
            forwardShotCount = 1;
            backwardShotCount = 2;
            pattern = ShooterPattern.FORWARD_AND_BACKWARD;
        } else if (name.contains("threepeater")) {
            laneCount = 3;
            pattern = ShooterPattern.THREE_LANE;
        } else if (name.contains("rotobaga")) {
            laneCount = 4;
            pattern = ShooterPattern.FOUR_DIAGONAL;
        } else if (name.contains("starfruit")) {
            laneCount = 5;
            pattern = ShooterPattern.STAR_FIVE;
        } else if (name.contains("pea pod")) {
            pattern = ShooterPattern.STACKED_FORWARD;
        } else if (name.contains("citron")) {
            pattern = ShooterPattern.CHARGED_FORWARD;
        } else if (name.contains("bowling bulb")) {
            pattern = ShooterPattern.BOUNCING;
        } else if (name.contains("sea-shroom") || name.contains("puff-shroom")) {
            pattern = ShooterPattern.SHORT_RANGE;
        }

        return new ShooterBehavior(
                this.createProjectile(definition, this.projectileTypeFor(definition)),
                direction,
                forwardShotCount,
                backwardShotCount,
                laneCount,
                this.secondsToTicks(definition.getActionIntervalSeconds()),
                pattern
        );
    }

    private PlantBehavior createHomingBehavior(PlantDefinition definition, String name) {
        HomingTargetMode targetMode = HomingTargetMode.NEAREST;
        ProjectileType projectileType = ProjectileType.HOMING;

        if (name.contains("caulipower")) {
            targetMode = HomingTargetMode.HYPNOSIS;
        } else if (name.contains("electric blueberry")) {
            targetMode = HomingTargetMode.PRIORITY;
        } else if (name.contains("magnet")) {
            targetMode = HomingTargetMode.ARMOR;
            projectileType = ProjectileType.NORMAL;
        } else if (name.contains("cat-tail")) {
            targetMode = HomingTargetMode.NEAREST;
        }

        return new HomingBehavior(
                this.createProjectile(definition, projectileType),
                this.secondsToTicks(definition.getActionIntervalSeconds()),
                targetMode,
                definition.getDamageExpression()
        );
    }

    private PlantBehavior createExplosiveBehavior(PlantDefinition definition, String name) {
        ExplosivePattern pattern = ExplosivePattern.RADIUS;
        int radius = 1;
        boolean triggeredByContact = false;
        long armDelayTicks = 0;
        boolean activateOnPlanting = true;

        if (name.contains("potato mine")) {
            pattern = ExplosivePattern.CONTACT_RADIUS;
            triggeredByContact = true;
            activateOnPlanting = false;
            armDelayTicks = name.contains("primal") ? this.secondsToTicks(5) : this.secondsToTicks(15);
            radius = name.contains("primal") ? 1 : 0;
        } else if (name.contains("iceberg lettuce")) {
            pattern = ExplosivePattern.CONTACT_SINGLE;
            triggeredByContact = true;
            activateOnPlanting = false;
            radius = 0;
        } else if (name.contains("squash") || name.contains("tangle kelp")) {
            pattern = ExplosivePattern.CONTACT_SINGLE;
            triggeredByContact = true;
            activateOnPlanting = false;
            radius = 0;
        } else if (name.contains("jalapeno")) {
            pattern = ExplosivePattern.FULL_LANE;
        } else if (name.contains("doom-shroom") || name.contains("ice-shroom")) {
            pattern = ExplosivePattern.FULL_BOARD;
        } else if (name.contains("hot potato")) {
            pattern = ExplosivePattern.TERRAIN_ONLY;
        } else if (name.contains("grave buster")) {
            pattern = ExplosivePattern.GRAVE_ONLY;
        }

        ExplosiveBehavior behavior = new ExplosiveBehavior(
                definition.getDamageExpression(),
                radius,
                triggeredByContact,
                pattern,
                armDelayTicks,
                activateOnPlanting
        );

        if (name.contains("iceberg lettuce") || name.contains("ice-shroom")) {
            behavior.setConditionOnHit(ZombieCondition.FROZEN, this.secondsToTicks(3));
        }

        return behavior;
    }

    private PlantBehavior createMeleeBehavior(PlantDefinition definition, String name) {
        MeleePattern pattern = MeleePattern.FRONT_AND_BACK;
        int range = 1;
        long digestTicks = 0;

        if (name.contains("phat beet") || name.contains("kiwibeast")) {
            pattern = name.contains("kiwibeast") ? MeleePattern.RAMPING_RADIUS : MeleePattern.RADIUS;
            range = 1;
        } else if (name.contains("chomper")) {
            pattern = MeleePattern.SINGLE_TARGET;
            digestTicks = this.secondsToTicks(40);
        }

        return new MeleeBehavior(
                definition.getDamageExpression(),
                range,
                this.secondsToTicks(definition.getActionIntervalSeconds()),
                pattern,
                digestTicks
        );
    }

    private PlantBehavior createDefenderBehavior(PlantDefinition definition, String name) {
        DefenderMode mode = DefenderMode.BASIC;

        if (name.contains("tall-nut")) {
            mode = DefenderMode.TALL;
        } else if (name.contains("endurian")) {
            mode = DefenderMode.REFLECT_DAMAGE;
        } else if (name.contains("garlic")) {
            mode = DefenderMode.MOVE_ZOMBIES;
        } else if (name.contains("sweet potato")) {
            mode = DefenderMode.ATTRACT_ZOMBIES;
        } else if (name.contains("explode-o-nut")) {
            mode = DefenderMode.EXPLODE_ON_DEATH;
        } else if (name.contains("pumpkin")) {
            mode = DefenderMode.STACK_PROTECTOR;
        } else if (name.contains("sun bean")) {
            mode = DefenderMode.SUN_ON_HIT;
        }

        return new DefenderBehavior(mode, definition.getDamageExpression());
    }

    private PlantBehavior createModifierBehavior(PlantDefinition definition, String name) {
        ModifierType modifierType = ModifierType.PROJECTILE_TRANSFORM;
        ActivationTrigger trigger = ActivationTrigger.PASSIVE;

        if (name.contains("hypno")) {
            modifierType = ModifierType.CONTACT_HYPNOSIS;
            trigger = ActivationTrigger.ON_CONTACT;
        } else if (name.contains("imitater")) {
            modifierType = ModifierType.COPY_PLANT;
            trigger = ActivationTrigger.ON_SELECTION;
        } else if (name.contains("lily pad")) {
            modifierType = ModifierType.PLANTING_SURFACE;
        } else if (name.contains("mint")) {
            modifierType = ModifierType.FAMILY_BUFF;
            trigger = ActivationTrigger.ON_PLANTING;
        }

        return new ModifierBehavior(
                modifierType,
                trigger,
                definition.getBaseAbilityDescription(),
                1,
                trigger == ActivationTrigger.ON_PLANTING ? this.secondsToTicks(10) : 0
        );
    }

    private PlantFoodBehavior createPlantFoodBehavior(PlantDefinition definition) {
        String name = this.normalize(definition.getName());
        String effect = definition.getPlantFoodEffectDescription();

        if (this.hasNoPlantFood(effect) || this.isMint(name)) {
            return this.createPlantFood(
                    PlantFoodEffectType.NONE,
                    definition,
                    0,
                    0,
                    0,
                    0,
                    0,
                    null
            );
        }

        if (this.hasCategory(definition.getCategories(), PlantCategory.SUN_PRODUCER)) {
            return this.createPlantFood(
                    PlantFoodEffectType.SUN_BURST,
                    definition,
                    1,
                    0,
                    0,
                    this.plantFoodSunAmount(name),
                    0,
                    null
            );
        }

        if (name.contains("potato mine")) {
            return this.createPlantFood(PlantFoodEffectType.ARM_AND_CLONE, definition, 2, 0, 1, 0, 0, null);
        }

        if (name.contains("citron") || name.contains("fire peashooter") || name.contains("cactus")
                || name.contains("fume-shroom")) {
            return this.createPlantFood(PlantFoodEffectType.LANE_DAMAGE, definition, 1, 0, 0, 0, 0, null);
        }

        if (name.contains("kernel-pult")) {
            return this.createPlantFood(PlantFoodEffectType.BOARD_DAMAGE, definition, 1, 0, 0, 0, 0, null);
        }

        if (name.contains("iceberg lettuce")) {
            return this.createPlantFood(PlantFoodEffectType.FREEZE_BOARD, definition, 1, 0, 0, 0, 0, null);
        }

        if (name.contains("snow pea")) {
            return this.createPlantFood(PlantFoodEffectType.FREEZE_LANE, definition, 5, 0, 0, 0, 0, null);
        }

        if (name.contains("goo peashooter")) {
            return this.createPlantFood(PlantFoodEffectType.POISON_TARGETS, definition, 5, 5, 0, 0, 0, null);
        }

        if (name.contains("caulipower")) {
            return this.createPlantFood(PlantFoodEffectType.HYPNOTIZE_TARGETS, definition, 1, 3, 0, 0, 0, null);
        }

        if (name.contains("hypno-shroom")) {
            return this.createPlantFood(PlantFoodEffectType.HYPNOTIZE_TARGETS, definition, 1, 1, 0, 0, 0, null);
        }

        if (name.contains("magnet-shroom")) {
            return this.createPlantFood(PlantFoodEffectType.REMOVE_ARMOR, definition, 1, 5, 0, 0, 0, null);
        }

        if (name.contains("electric blueberry")) {
            return this.createPlantFood(PlantFoodEffectType.TARGETED_DAMAGE, definition, 1, 3, 0, 0, 0, "Insta-kill");
        }

        if (name.contains("squash")) {
            return this.createPlantFood(PlantFoodEffectType.TARGETED_DAMAGE, definition, 1, 2, 0, 0, 0, "Insta-kill");
        }

        if (name.contains("tangle kelp")) {
            return this.createPlantFood(PlantFoodEffectType.TARGETED_DAMAGE, definition, 1, 3, 0, 0, 0, "Insta-kill");
        }

        if (name.contains("chomper")) {
            return this.createPlantFood(PlantFoodEffectType.TARGETED_DAMAGE, definition, 1, 3, 0, 0, 0, "Insta-kill");
        }

        if (this.hasCategory(definition.getCategories(), PlantCategory.LOBBER)) {
            return this.createPlantFood(
                    PlantFoodEffectType.TARGETED_DAMAGE,
                    definition,
                    1,
                    name.contains("pepper-pult") ? 3 : 4,
                    0,
                    0,
                    0,
                    null
            );
        }

        if (name.contains("bonk choy") || name.contains("wasabi whip")
                || name.contains("phat beet") || name.contains("kiwibeast")) {
            return this.createPlantFood(PlantFoodEffectType.AREA_DAMAGE, definition, 1, 0, 1, 0, 0, null);
        }

        if (this.hasCategory(definition.getCategories(), PlantCategory.DEFENDER)) {
            if (name.contains("garlic")) {
                return this.createPlantFood(PlantFoodEffectType.LANE_SHIFT, definition, 1, 0, 0, 0, 0, null);
            }

            if (name.contains("sweet potato")) {
                return this.createPlantFood(PlantFoodEffectType.HEAL_TO_FULL, definition, 1, 0, 1, 0, 0, null);
            }

            return this.createPlantFood(
                    PlantFoodEffectType.ARMOR_BOOST,
                    definition,
                    1,
                    0,
                    0,
                    0,
                    this.plantFoodArmorAmount(name),
                    null
            );
        }

        if (name.contains("sea-shroom") || name.contains("puff-shroom")) {
            return this.createPlantFood(PlantFoodEffectType.RESET_SAME_PLANTS, definition, 5, 0, 0, 0, 0, null);
        }

        if (name.contains("bowling bulb")) {
            return this.createPlantFood(
                    PlantFoodEffectType.PROJECTILE_BURST,
                    definition,
                    3,
                    0,
                    0,
                    0,
                    0,
                    null,
                    this.createProjectile(definition, ProjectileType.NORMAL)
            );
        }

        if (name.contains("cat-tail")) {
            return this.createPlantFood(
                    PlantFoodEffectType.PROJECTILE_BURST,
                    definition,
                    5,
                    0,
                    0,
                    0,
                    0,
                    null,
                    this.createProjectile(definition, ProjectileType.HOMING)
            );
        }

        if (name.contains("lily pad")) {
            return this.createPlantFood(PlantFoodEffectType.CLONE_NEARBY, definition, 3, 0, 1, 0, 0, null);
        }

        if (name.contains("torchwood")) {
            return this.createPlantFood(PlantFoodEffectType.PROJECTILE_BUFF, definition, 1, 0, 0, 0, 0, null);
        }

        if (this.hasCategory(definition.getCategories(), PlantCategory.SHOOTER)
                || this.hasCategory(definition.getCategories(), PlantCategory.STRIKE_THROUGH)) {
            return this.createPlantFood(
                    PlantFoodEffectType.REPEAT_ABILITY,
                    definition,
                    this.plantFoodShotCount(name, effect),
                    0,
                    0,
                    0,
                    0,
                    null
            );
        }

        return this.createPlantFood(
                PlantFoodEffectType.REPEAT_ABILITY,
                definition,
                this.activationCountFromEffect(effect),
                0,
                0,
                0,
                0,
                null
        );
    }

    private PlantFoodBehavior createPlantFood(
            PlantFoodEffectType effectType,
            PlantDefinition definition,
            int activationCount,
            int targetCount,
            int radius,
            int sunAmount,
            int bonusHealth,
            String damageOverride
    ) {
        return this.createPlantFood(
                effectType,
                definition,
                activationCount,
                targetCount,
                radius,
                sunAmount,
                bonusHealth,
                damageOverride,
                null
        );
    }

    private PlantFoodBehavior createPlantFood(
            PlantFoodEffectType effectType,
            PlantDefinition definition,
            int activationCount,
            int targetCount,
            int radius,
            int sunAmount,
            int bonusHealth,
            String damageOverride,
            Projectile projectile
    ) {
        return new ConfiguredPlantFoodBehavior(
                effectType,
                definition.getPlantFoodEffectDescription(),
                damageOverride == null ? definition.getDamageExpression() : damageOverride,
                Math.max(1, activationCount),
                targetCount,
                radius,
                sunAmount,
                bonusHealth,
                projectile
        );
    }

    private boolean hasNoPlantFood(String effect) {
        return effect == null
                || effect.trim().isEmpty()
                || effect.contains("\u0646\u062F\u0627\u0631\u062F");
    }

    private int activationCountFromEffect(String effect) {
        if (effect != null && effect.contains("\u06F4")) {
            return 4;
        }

        if (effect != null && effect.contains("\u06F3")) {
            return 3;
        }

        if (effect != null && effect.contains("\u06F2")) {
            return 2;
        }

        return 1;
    }

    private int plantFoodSunAmount(String name) {
        if (name.contains("twin")) {
            return 250;
        }

        if (name.contains("sun-shroom") || name.contains("primal")) {
            return 225;
        }

        return 150;
    }

    private int plantFoodArmorAmount(String name) {
        if (name.contains("tall-nut")) {
            return 8000;
        }

        if (name.contains("wall-nut")) {
            return 4000;
        }

        if (name.contains("endurian") || name.contains("explode-o-nut")
                || name.contains("pumpkin") || name.contains("sun bean")) {
            return 3000;
        }

        return 2000;
    }

    private int plantFoodShotCount(String name, String effect) {
        if (name.contains("mega gatling")) {
            return 8;
        }

        if (name.contains("repeater") || name.contains("pea pod")) {
            return 5;
        }

        int explicitCount = this.activationCountFromEffect(effect);

        return Math.max(5, explicitCount);
    }

    private Projectile createProjectile(PlantDefinition definition, ProjectileType projectileType) {
        Projectile projectile = new Projectile(
                definition.getDamageExpression(),
                null,
                1.0,
                projectileType,
                null
        );

        if (projectileType == ProjectileType.PIERCING) {
            projectile.setPierceCount(2);
        }

        if (this.hasTag(definition.getTags(), PlantTag.AOE)) {
            projectile.setSplashRadius(1);
        }

        if (this.normalize(definition.getName()).contains("fume-shroom")) {
            projectile.setMaxRange(4);
        }

        if (projectileType == ProjectileType.ICE || projectileType == ProjectileType.POISON) {
            projectile.setConditionDurationTicks(this.secondsToTicks(3));
        }

        return projectile;
    }

    private ProjectileType projectileTypeFor(PlantDefinition definition) {
        if (this.hasTag(definition.getTags(), PlantTag.FIRE)) {
            return ProjectileType.FIRE;
        }

        if (this.hasTag(definition.getTags(), PlantTag.ICE)) {
            return ProjectileType.ICE;
        }

        if (this.hasTag(definition.getTags(), PlantTag.POISON)) {
            return ProjectileType.POISON;
        }

        return ProjectileType.NORMAL;
    }

    private boolean hasCategory(Set<PlantCategory> categories, PlantCategory category) {
        return categories != null && categories.contains(category);
    }

    private boolean hasTag(Set<PlantTag> tags, PlantTag tag) {
        return tags != null && tags.contains(tag);
    }

    private boolean isMint(String name) {
        return name != null && name.contains("mint");
    }

    private String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private long secondsToTicks(double seconds) {
        if (seconds <= 0) {
            return 1;
        }

        return Math.round(seconds * TICKS_PER_SECOND);
    }
}
