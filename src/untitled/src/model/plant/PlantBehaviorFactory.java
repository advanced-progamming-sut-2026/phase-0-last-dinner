package model.plant;

import model.plant.behavior.ActivationTrigger;
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
import model.plant.behavior.ShooterBehavior;
import model.plant.behavior.ShooterPattern;
import model.plant.behavior.SunProducerBehavior;
import model.plant.behavior.SunProductionMode;
import model.zombie.ZombieCondition;

import java.util.Locale;
import java.util.Set;

// behavior mamooli har giah ro az category va name definition misaze
class PlantBehaviorFactory {
    private static final int TICKS_PER_SECOND = 10;
    private static final int MAGNET_RANGE_TILES = 3;
    private static final int GRAVE_BUSTER_EAT_SECONDS = 5;

    PlantBehavior create(PlantDefinition definition) {
        String name = this.normalize(definition.getName());
        Set<PlantCategory> categories = definition.getCategories();

        if (this.isMint(name)) {
            return new ModifierBehavior(
                    ModifierType.FAMILY_BUFF,
                    ActivationTrigger.ON_PLANTING,
                    definition.getBaseAbilityDescription(),
                    99,
                    this.secondsToTicks(10),
                    this.mintFamilyCategory(name),
                    this.mintFamilyTag(name)
            );
        }

        if (this.hasCategory(categories, PlantCategory.SUN_PRODUCER)) {
            return this.createSunProducer(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.HOMING)) {
            return this.createHoming(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.LOBBER)) {
            return new LobberBehavior(
                    this.createLobbedProjectile(definition),
                    this.secondsToTicks(definition.getActionIntervalSeconds()),
                    this.hasTag(definition.getTags(), PlantTag.AOE),
                    name.contains("kernel-pult")
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
            return this.createShooter(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.EXPLOSIVE)) {
            return this.createExplosive(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.MELEE_ATTACKER)) {
            return this.createMelee(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.DEFENDER)) {
            return this.createDefender(definition, name);
        }

        if (this.hasCategory(categories, PlantCategory.MODIFIER)) {
            return this.createModifier(definition, name);
        }

        return new DefenderBehavior();
    }

    private PlantBehavior createSunProducer(PlantDefinition definition, String name) {
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
            return new SunProducerBehavior(
                    25,
                    this.secondsToTicks(definition.getActionIntervalSeconds()),
                    SunProductionMode.RAMPING
            );
        }

        return new SunProducerBehavior(50, this.secondsToTicks(definition.getActionIntervalSeconds()));
    }

    private PlantBehavior createShooter(PlantDefinition definition, String name) {
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

    private PlantBehavior createHoming(PlantDefinition definition, String name) {
        HomingTargetMode targetMode = HomingTargetMode.NEAREST;
        ProjectileType projectileType = ProjectileType.HOMING;

        if (name.contains("caulipower")) {
            targetMode = HomingTargetMode.HYPNOSIS;
        } else if (name.contains("electric blueberry")) {
            targetMode = HomingTargetMode.PRIORITY;
        } else if (name.contains("magnet")) {
            targetMode = HomingTargetMode.ARMOR;
            projectileType = ProjectileType.NORMAL;
        }

        return new HomingBehavior(
                this.createProjectile(definition, projectileType),
                this.secondsToTicks(definition.getActionIntervalSeconds()),
                targetMode,
                definition.getDamageExpression(),
                targetMode == HomingTargetMode.ARMOR ? MAGNET_RANGE_TILES : 0
        );
    }

    private PlantBehavior createExplosive(PlantDefinition definition, String name) {
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
            radius = 0;
        } else if (name.contains("grave buster")) {
            pattern = ExplosivePattern.GRAVE_ONLY;
            radius = 0;
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

        if (name.contains("doom-shroom")) {
            behavior.setCreatesCrater(true);
        }

        if (name.contains("jalapeno")) {
            behavior.setMeltsLane(true);
        }

        if (name.contains("tangle kelp")) {
            behavior.setWaterTargetsOnly(true);
        }

        if (name.contains("grave buster")) {
            behavior.setTerrainRemovalDelayTicks(this.secondsToTicks(GRAVE_BUSTER_EAT_SECONDS));
        }

        if (name.contains("grapeshot")) {
            Projectile grape = new Projectile("100", null, 1.0, ProjectileType.NORMAL, null);
            grape.setBounceCount(1);
            grape.setRemainingTicks(this.secondsToTicks(5));
            behavior.setSecondaryProjectileBurst(grape, 8);
        }

        return behavior;
    }

    private PlantBehavior createMelee(PlantDefinition definition, String name) {
        MeleePattern pattern = MeleePattern.FRONT_AND_BACK;
        int range = 1;
        long digestTicks = 0;
        long attackIntervalTicks = this.secondsToTicks(definition.getActionIntervalSeconds());

        if (name.contains("phat beet") || name.contains("kiwibeast")) {
            pattern = name.contains("kiwibeast") ? MeleePattern.RAMPING_RADIUS : MeleePattern.RADIUS;
        } else if (name.contains("chomper")) {
            pattern = MeleePattern.SINGLE_TARGET;
            digestTicks = this.secondsToTicks(40);
            attackIntervalTicks = 1;
        }

        return new MeleeBehavior(
                definition.getDamageExpression(),
                range,
                attackIntervalTicks,
                pattern,
                digestTicks
        );
    }

    private PlantBehavior createDefender(PlantDefinition definition, String name) {
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

    private PlantBehavior createModifier(PlantDefinition definition, String name) {
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

    private Projectile createProjectile(PlantDefinition definition, ProjectileType projectileType) {
        Projectile projectile = new Projectile(
                definition.getDamageExpression(),
                null,
                1.0,
                projectileType,
                null
        );
        projectile.setPeaBased(this.hasTag(definition.getTags(), PlantTag.PEA));

        if (projectileType == ProjectileType.PIERCING) {
            projectile.setPierceCount(2);
        }

        if (this.normalize(definition.getName()).contains("bowling bulb")) {
            projectile.setBounceCount(2);
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

        if (projectileType == ProjectileType.POISON) {
            projectile.addPoisonDamagePerTick(5);
        }

        return projectile;
    }

    private Projectile createLobbedProjectile(PlantDefinition definition) {
        Projectile projectile = this.createProjectile(definition, this.projectileTypeFor(definition));
        projectile.setLobbed(true);
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

    private PlantCategory mintFamilyCategory(String name) {
        if (name.contains("enlighten")) {
            return PlantCategory.SUN_PRODUCER;
        }

        if (name.contains("arma")) {
            return PlantCategory.LOBBER;
        }

        if (name.contains("bombard")) {
            return PlantCategory.EXPLOSIVE;
        }

        if (name.contains("enforce")) {
            return PlantCategory.MELEE_ATTACKER;
        }

        if (name.contains("reinforce")) {
            return PlantCategory.DEFENDER;
        }

        if (name.contains("pierce")) {
            return PlantCategory.STRIKE_THROUGH;
        }

        if (name.contains("cattail")) {
            return PlantCategory.HOMING;
        }

        return null;
    }

    private PlantTag mintFamilyTag(String name) {
        if (name.contains("appease")) {
            return PlantTag.PEA;
        }

        if (name.contains("enchant")) {
            return PlantTag.MAGIC;
        }

        return null;
    }

    private String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private long secondsToTicks(double seconds) {
        return seconds <= 0 ? 1 : Math.round(seconds * TICKS_PER_SECOND);
    }
}
