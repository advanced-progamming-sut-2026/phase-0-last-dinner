package model.plant;

import model.plant.behavior.ConfiguredPlantFoodBehavior;
import model.plant.behavior.AllLaneBarragePlantFoodBehavior;
import model.plant.behavior.AlliedGargantuarPlantFoodBehavior;
import model.plant.behavior.DefenderPlantFoodBehavior;
import model.plant.behavior.FumeKnockbackPlantFoodBehavior;
import model.plant.behavior.GiantPeaPlantFoodBehavior;
import model.plant.behavior.KernelButterPlantFoodBehavior;
import model.plant.behavior.PlantFoodBehavior;
import model.plant.behavior.PlantFoodEffectType;
import model.plant.behavior.TangleKelpPlantFoodBehavior;

import java.util.Locale;
import java.util.Set;

// asar plant food ro az name va tozih definition entekhab va config mikone
final class PlantFoodBehaviorFactory {
    private static final int TICKS_PER_SECOND = 10;
    private static final int DEFAULT_BARRAGE_SHOTS = 5;
    private static final int MAGNET_RANGE_TILES = 3;

    PlantFoodBehavior create(PlantDefinition definition) {
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

        if (name.contains("fire peashooter")) {
            return this.createPlantFood(
                    PlantFoodEffectType.REPEAT_ABILITY,
                    definition,
                    DEFAULT_BARRAGE_SHOTS,
                    0,
                    0,
                    0,
                    0,
                    null
            );
        }

        if (name.contains("citron") || name.contains("cactus")) {
            return this.createPlantFood(PlantFoodEffectType.LANE_DAMAGE, definition, 1, 0, 0, 0, 0, null);
        }

        if (name.contains("fume-shroom")) {
            return new FumeKnockbackPlantFoodBehavior(
                    DamageExpressionParser.parseTotalDamage(definition.getDamageExpression()),
                    2
            );
        }

        if (name.contains("kernel-pult")) {
            return new KernelButterPlantFoodBehavior(
                    DamageExpressionParser.parseDamageAt(definition.getDamageExpression(), 1),
                    this.secondsToTicks(3)
            );
        }

        if (name.contains("iceberg lettuce")) {
            return this.createPlantFood(
                    PlantFoodEffectType.FREEZE_BOARD, definition, 1, 0, 0, 0, 0, null
            ).withTimedCondition(this.secondsToTicks(3), 0);
        }

        if (name.contains("snow pea")) {
            return this.createPlantFood(
                    PlantFoodEffectType.FREEZE_LANE, definition, 5, 0, 0, 0, 0, null
            ).withTimedCondition(this.secondsToTicks(3), 0);
        }

        if (name.contains("goo peashooter")) {
            return this.createPlantFood(
                    PlantFoodEffectType.POISON_TARGETS, definition, 5, 5, 0, 0, 0, null
            ).withTimedCondition(this.secondsToTicks(3), 5);
        }

        if (name.contains("caulipower")) {
            return this.createPlantFood(
                    PlantFoodEffectType.HYPNOTIZE_TARGETS, definition, 1, 3, 0, 0, 0, null
            ).withRandomTargets();
        }

        if (name.contains("hypno-shroom")) {
            return new AlliedGargantuarPlantFoodBehavior();
        }

        if (name.contains("magnet-shroom")) {
            return this.createPlantFood(
                    PlantFoodEffectType.REMOVE_ARMOR, definition, 1, 5, 0, 0, 0, null
            ).withMetallicTargetsOnly().withTargetRange(MAGNET_RANGE_TILES);
        }

        if (name.contains("electric blueberry")) {
            return this.createPlantFood(
                    PlantFoodEffectType.TARGETED_DAMAGE, definition, 1, 3, 0, 0, 0, "Insta-kill"
            ).withRandomTargets();
        }

        if (name.contains("squash")) {
            return this.createPlantFood(
                    PlantFoodEffectType.TARGETED_DAMAGE, definition, 1, 2, 0, 0, 0, "Insta-kill"
            ).withRandomTargets().withGroundTargetsOnly();
        }

        if (name.contains("tangle kelp")) {
            return new TangleKelpPlantFoodBehavior(3);
        }

        if (name.contains("chomper")) {
            return this.createPlantFood(PlantFoodEffectType.TARGETED_DAMAGE, definition, 1, 3, 0, 0, 0, "Insta-kill");
        }

        if (this.hasCategory(definition.getCategories(), PlantCategory.LOBBER)) {
            return this.createPlantFood(
                    PlantFoodEffectType.PROJECTILE_BURST,
                    definition,
                    name.contains("pepper-pult") ? 3 : 4,
                    0,
                    0,
                    0,
                    0,
                    null,
                    this.createLobbedProjectile(definition)
            ).withRandomTargets();
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

            if (name.contains("endurian")) {
                return new DefenderPlantFoodBehavior(
                        DefenderPlantFoodBehavior.Boost.ENDURIAN_ARMOR,
                        this.plantFoodArmorAmount(name)
                );
            }

            if (name.contains("explode-o-nut")) {
                return new DefenderPlantFoodBehavior(
                        DefenderPlantFoodBehavior.Boost.EXPLOSIVE_ARMOR,
                        this.plantFoodArmorAmount(name)
                );
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
            Projectile largeBulb = this.createProjectile(definition, ProjectileType.NORMAL);
            largeBulb.setDamageExpression(DamageExpressionParser.selectExpressionAt(
                    definition.getDamageExpression(),
                    2
            ));
            largeBulb.setSplashRadius(1);
            return this.createPlantFood(
                    PlantFoodEffectType.PROJECTILE_BURST,
                    definition,
                    3,
                    0,
                    0,
                    0,
                    0,
                    null,
                    largeBulb
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

        if (name.contains("repeater") || name.contains("pea pod")) {
            return new GiantPeaPlantFoodBehavior(
                    DamageExpressionParser.parseDamagePerHit(definition.getDamageExpression()) * 20,
                    name.contains("repeater") ? 5 : 0,
                    name.contains("pea pod")
            );
        }

        if (name.contains("threepeater")) {
            return new AllLaneBarragePlantFoodBehavior(
                    this.createProjectile(definition, this.projectileTypeFor(definition)),
                    DEFAULT_BARRAGE_SHOTS
            );
        }

        if (name.contains("mega gatling")) {
            return new GiantPeaPlantFoodBehavior(
                    DamageExpressionParser.parseDamagePerHit(definition.getDamageExpression()) * 20,
                    8,
                    4
            );
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

    private ConfiguredPlantFoodBehavior createPlantFood(
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

    private ConfiguredPlantFoodBehavior createPlantFood(
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
        projectile.setPeaBased(this.hasTag(definition.getTags(), PlantTag.PEA));

        if (projectileType == ProjectileType.PIERCING) {
            projectile.setPierceCount(2);
        }

        if (projectileType == ProjectileType.LOBBED) {
            projectile.setLobbed(true);
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
