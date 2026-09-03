package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import model.Plant;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.plant.ProjectileType;

import java.util.Locale;

/** Maps live Phase 1 projectiles to the closest authored PvZ2 visual available in the asset pack. */
final class GameplayProjectileVisualCatalog {
    private static final String PEA = "IMAGE_PROJECTILEPEA";
    private static final String FIRE = "IMAGE_EFFECTS_T_FIRE_PEA_T_FIRE_PEA_43X43";
    private static final String PEPPER =
            "IMAGE_EFFECTS_PEPPERPULT_PROJECTILE_PEPPERPULT_PROJECTILE_55X61";
    private static final String MELON =
            "IMAGE_EFFECTS_T_MELON_PROJECTILE_T_MELON_PROJECTILE_122X83";
    private static final String WINTER_MELON = "IMAGE_WINTERMELON_PROJECTILE";
    private static final String FUME =
            "IMAGE_EFFECTS_FUMESHROOM_BUBBLES_FUMESHROOM_BUBBLES_52X52";
    private static final String CACTUS =
            "IMAGE_EFFECTS_CACTUS_PROJECTILE_CACTUS_PROJECTILE_52X35";
    private static final String BUTTER = "IMAGE_EFFECTS_KERNELPULT_PROJECTILE_BUTTER";
    private static final String KERNEL =
            "IMAGE_EFFECTS_T_KERNALPULT_PROJECTILE_T_KERNALPULT_PROJECTILE_83X79";
    private static final String CABBAGE =
            "IMAGE_EFFECTS_T_CABBAGEPULT_PROJECTILE_T_CABBAGEPULT_PROJECTILE_125X117";
    private static final String CITRON =
            "IMAGE_EFFECTS_CITRON_CITRUS_ORB_CITRON_CITRUS_ORB_91X91";
    private static final String STARFRUIT =
            "IMAGE_EFFECTS_T_STARFRUIT_PROJECTILE_T_STARFRUIT_PROJECTILE_102X104";
    private static final String ROTO =
            "IMAGE_EFFECTS_T_ROTORUTABAGA_PROJECTILE1_T_ROTORUTABAGA_PROJECTILE1_44X108";
    private static final String PUFF =
            "IMAGE_EFFECTS_T_PUFFSHROOM_PROJECTILE_T_PUFFSHROOM_PROJECTILE_23X22";
    private static final String SEA_SHROOM =
            "IMAGE_EFFECTS_SEASHROOM_PROJECTILE_SEASHROOM_PROJECTILE_45X45";
    private static final String GOO =
            "IMAGE_EFFECTS_GOOPEASHOOTER_PROJECTILES_GOOPEASHOOTER_PROJECTILES_86X86";
    private static final String SNOW_PEA =
            "IMAGE_EFFECTS_T_SNOW_PEA_T_SNOW_PEA_64X44";
    private static final String GRAPE =
            "IMAGE_EFFECTS_GRAPESHOT_PROJECTILE_GRAPESHOT_PROJECTILE_55X55";
    private static final String GIANT_PEA =
            "IMAGE_EFFECTS_REPEATER_PLANTFOOD_GIANTPEA_REPEATER_PLANTFOOD_GIANTPEA_72X72";
    private static final String BULB_ONE =
            "IMAGE_EFFECTS_BOWLINGBULB_PROJECTILE1_BOWLINGBULB_PROJECTILE1_66X66";
    private static final String BULB_TWO =
            "IMAGE_EFFECTS_BOWLINGBULB_PROJECTILE2_BOWLINGBULB_PROJECTILE2_81X81";
    private static final String BULB_THREE =
            "IMAGE_EFFECTS_BOWLINGBULB_PROJECTILE3_BOWLINGBULB_PROJECTILE3_95X95";
    private static final String CAULIPOWER =
            "IMAGE_EFFECTS_CAULIPOWER_PROJECTILE_CAULIPOWER_PROJECTILE_128X128";
    private static final String ELECTRIC_BLUEBERRY =
            "IMAGE_EFFECTS_ELECTRICBLUEBERRY_CLOUD_PROJECTILE_ELECTRICBLUEBERRY_CLOUD_PROJECTILE_94X63";

    private static final String PEA_IMPACT = "IMAGE_EFFECTS_SPLAT_PEA_SPLAT_PEA_150X88";
    private static final String FIRE_IMPACT = "IMAGE_EFFECTS_T_SPLAT_FIRE_PEA_T_SPLAT_FIRE_PEA_148X146";
    private static final String PEPPER_IMPACT =
            "IMAGE_EFFECTS_T_PEPPERPULT_PROJECTILE_SPLAT_T_PEPPERPULT_PROJECTILE_SPLAT_143X141";
    private static final String MELON_IMPACT = "IMAGE_EFFECTS_T_SPLAT_MELONPULT_T_SPLAT_MELONPULT_117X113";
    private static final String WINTER_MELON_IMPACT =
            "IMAGE_EFFECTS_T_SPLAT_WINTERMELON_T_SPLAT_WINTERMELON_140X140";
    private static final String FUME_IMPACT =
            "IMAGE_EFFECTS_FUMESHROOM_BUBBLES_HIT_FUMESHROOM_BUBBLES_HIT_29X29";
    private static final String CACTUS_IMPACT =
            "IMAGE_EFFECTS_CACTUS_PROJECTILE_HIT_CACTUS_PROJECTILE_HIT_41X49";
    private static final String BUTTER_IMPACT =
            "IMAGE_EFFECTS_SPLAT_KERNALPULT_BUTTER_SPLAT_KERNALPULT_BUTTER_90X50";
    private static final String KERNEL_IMPACT =
            "IMAGE_EFFECTS_SPLAT_KERNALPULT_KERNAL_SPLAT_KERNALPULT_KERNAL_10X10";
    private static final String CABBAGE_IMPACT =
            "IMAGE_EFFECTS_SPLAT_CABBAGEPULT_CABBAGEPULT_PARTICLE_BITS";
    private static final String CITRON_IMPACT =
            "IMAGE_EFFECTS_CITRON_CITRUS_ORB_HIT_CITRON_CITRUS_ORB_HIT_54X54";
    private static final String STARFRUIT_IMPACT =
            "IMAGE_EFFECTS_T_STARFRUIT_PROJECTILE_HIT_T_STARFRUIT_PROJECTILE_HIT_89X89";
    private static final String ROTO_IMPACT =
            "IMAGE_EFFECTS_T_ROTORUTABAGA_PROJECTILE_HIT_T_ROTORUTABAGA_PROJECTILE_HIT_47X47";
    private static final String CAULIPOWER_IMPACT =
            "IMAGE_EFFECTS_CAULIPOWER_PROJECTILE_CAULIPOWER_PROJECTILE_15X15";
    private static final String ELECTRIC_BLUEBERRY_IMPACT =
            "IMAGE_EFFECTS_ELECTRICBLUEBERRY_CLOUD_PROJECTILE_ELECTRICBLUEBERRY_CLOUD_PROJECTILE_67X155";
    private static final String GRAPE_IMPACT =
            "IMAGE_EFFECTS_GRAPESHOT_HIT_GRAPESHOT_HIT_128X124";
    private static final String GIANT_PEA_IMPACT =
            "IMAGE_EFFECTS_SPLAT_GIANTPEA_SPLAT_GIANTPEA_110X60";

    private GameplayProjectileVisualCatalog() {
    }

    static Visual forProjectile(Projectile projectile) {
        return forProjectile(projectile, false);
    }

    static Visual forProjectile(Projectile projectile, boolean plantFoodActive) {
        String sourceName = sourceName(projectile);
        int tier = sourceTier(projectile);
        if (sourceName.contains("winter melon")) {
            Visual visual = Visual.staticVisual(WINTER_MELON, WINTER_MELON_IMPACT, 0.42f, 0.82f)
                    .withPam("T_WINTERMELON_PROJECTILE", numberedClip("animation", tier),
                            numberedClip("animation", tier), true);
            return plantFoodActive
                    ? visual.withImpactPam("WINTERMELON_EXPLODE", "plantfood_WintermelonExplode", null)
                    : visual.withImpactPam("T_SPLAT_WINTERMELON", numberedClip("animation", tier), null);
        }
        if (sourceName.contains("melon-pult")) {
            Visual visual = Visual.staticVisual(MELON, MELON_IMPACT, 0.42f, 0.76f)
                    .withPam("T_MELON_PROJECTILE", numberedClip("animation", tier),
                            numberedClip("animation", tier), true);
            return plantFoodActive
                    ? visual.withImpactPam("MELON_EXPLODE", "plantfood_MelonExplode", null)
                    : visual.withImpactPam("T_SPLAT_MELONPULT", numberedClip("animation", tier), null);
        }
        if (sourceName.contains("pepper-pult")) {
            Visual visual = Visual.staticVisual(PEPPER, PEPPER_IMPACT, 0.38f, 0.76f)
                    .withPam("T_PEPPERPULT_PROJECTILE", numberedClip("animation", tier),
                            numberedClip("animation", tier), true);
            return plantFoodActive
                    ? visual.withImpactPam("PEPPERPULT_PROJECTILE_PF_SPLAT",
                            numberedClip("animation", Math.min(2, tier)), null)
                    : visual.withImpactPam("T_PEPPERPULT_PROJECTILE_SPLAT",
                            numberedClip("animation", tier), null);
        }
        if (sourceName.contains("cabbage-pult")) {
            if (plantFoodActive) {
                return Visual.staticVisual(CABBAGE, CABBAGE_IMPACT, 0.44f, 0.66f)
                        .withPam("CABBAGEPULT_PLANTFOOD_PROJECTILE", "plantfood_cabbage",
                                "plantfood_cabbage", true)
                        .withImpactPam("CABBAGEPULT_PLANTFOOD_PROJECTILE",
                                "plantfood_cabbageExplode", null);
            }
            return Visual.staticVisual(CABBAGE, CABBAGE_IMPACT, 0.37f, 0.50f)
                    .withPam("T_CABBAGEPULT_PROJECTILE", numberedClip("animation", tier),
                            numberedClip("animation", tier), true)
                    .withImpactPam("SPLAT_CABBAGEPULT",
                            numberedClip("animation", Math.min(2, tier)), null);
        }
        if (sourceName.contains("kernel")) {
            if (projectile != null && projectile.getStunChancePercent() >= 100) {
                return Visual.staticVisual(BUTTER, BUTTER_IMPACT, 0.32f, 0.58f)
                        .withPam("T_KERNALPULT_PROJECTILE", numberedClip("animation", tier),
                                numberedClip("animation", tier), true)
                        .withImpactPam("SPLAT_KERNALPULT_BUTTER",
                                numberedClip("animation", Math.min(2, tier)), null);
            }
            return Visual.staticVisual(KERNEL, KERNEL_IMPACT, 0.29f, 0.42f)
                    .withPam("T_KERNALPULT_PROJECTILE", numberedClip("animation", tier),
                            numberedClip("animation", tier), true)
                    .withImpactPam("SPLAT_KERNALPULT_KERNAL",
                            numberedClip("animation", Math.min(2, tier)), null);
        }
        if (sourceName.contains("citron")) {
            if (plantFoodActive) {
                return Visual.staticVisual(CITRON, CITRON_IMPACT, 0.48f, 0.86f)
                        .withPam("CITRON_PLANTFOOD_ORB", "Plantfood_Citron_Plasma_Orb",
                                "Plantfood_Citron_Plasma_Orb", true)
                        .withImpactPam("CITRON_PLANTFOOD_SHOCK",
                                numberedClip("animation", tier), null);
            }
            return Visual.staticVisual(CITRON, CITRON_IMPACT, 0.36f, 0.66f)
                    .withPam("T_CITRON_CITRUS_ORB", citronOrbClip(tier), citronOrbClip(tier), true)
                    .withImpactPam("T_CITRON_CITRUS_ORB_HIT",
                            numberedClip("animation", tier), null);
        }
        if (sourceName.contains("bowling bulb")) {
            return bowlingBulbVisual(projectile, plantFoodActive);
        }
        if (sourceName.contains("starfruit")) {
            return Visual.staticVisual(STARFRUIT, STARFRUIT_IMPACT,
                            plantFoodActive ? 0.36f : 0.29f, 0.56f)
                    .withPam(plantFoodActive ? "STARFRUIT_PROJECTILE_PLANTFOOD" : "T_STARFRUIT_PROJECTILE",
                            plantFoodActive ? "animation" : numberedClip("animation", tier),
                            plantFoodActive ? "animation" : numberedClip("animation", tier), true)
                    .withImpactPam("T_STARFRUIT_PROJECTILE_HIT",
                            numberedClip("idle", tier), null);
        }
        if (sourceName.contains("rotobaga")) {
            return Visual.staticVisual(ROTO, ROTO_IMPACT, 0.27f, 0.50f)
                    .withPam("T_ROTORUTABAGA_PROJECTILE1", numberedClip("animation", tier),
                            numberedClip("animation", tier), true)
                    .withOverlayPam("ROTORUTABAGA_PROJECTILE2", "animation", 0.62f)
                    .withImpactPam("T_ROTORUTABAGA_PROJECTILE_HIT",
                            numberedClip("animation", tier), null)
                    .rotateToDirection();
        }
        if (sourceName.contains("puff-shroom")) {
            return Visual.staticVisual(PUFF, FUME_IMPACT, 0.21f, 0.40f);
        }
        if (sourceName.contains("sea-shroom")) {
            return Visual.staticVisual(SEA_SHROOM, FUME_IMPACT, 0.23f, 0.42f)
                    .withPam("SEASHROOM_PROJECTILE",
                            numberedClip("animation", Math.min(2, tier)),
                            numberedClip("animation", Math.min(2, tier)), true)
                    .withImpactPam("FUMESHROOM_BUBBLES_HIT", "animation", null);
        }
        if (sourceName.contains("goo peashooter")) {
            return Visual.staticVisual(GOO, PEA_IMPACT, 0.27f, 0.50f)
                    .withPam("GOOPEASHOOTER_PROJECTILES", "projectile_t" + tier,
                            "projectile_t" + tier, true)
                    .withImpactPam(plantFoodActive ? "GOOPEASHOOTER_PLANTFOOD_TILE"
                                    : "GOOPEASHOOTER_PROJECTILES",
                            plantFoodActive ? numberedClip("animation", Math.min(2, tier))
                                    : "hit_t" + tier, null);
        }
        if (sourceName.contains("grapeshot")) {
            String clip = grapeshotDirectionClip(projectile);
            return Visual.staticVisual(GRAPE, GRAPE_IMPACT, 0.24f, 0.54f)
                    .withPam("GRAPESHOT_PROJECTILE", clip, clip, true)
                    .withImpactPam("GRAPESHOT_HIT", numberedClip("animation", tier), null)
                    .rotateToDirection();
        }
        if (sourceName.contains("mega gatling")) {
            boolean giant = isGiantPea(projectile, sourceName);
            return Visual.staticVisual(giant ? GIANT_PEA : PEA, giant ? GIANT_PEA_IMPACT : PEA_IMPACT,
                            giant ? 0.48f : 0.25f, giant ? 0.78f : 0.52f)
                    .withPam("MEGAGATLING_PROJECTILE", giant ? "animation3" : "animation",
                            giant ? "animation3" : "animation", true);
        }
        if (sourceName.contains("pea pod") && isGiantPea(projectile, sourceName)) {
            return Visual.staticVisual(GIANT_PEA, GIANT_PEA_IMPACT, 0.47f, 0.78f)
                    .withPam("PEAPOD_PLANTFOOD_GIANTPEA", "animation", "animation", true);
        }
        if (isGiantPea(projectile, sourceName)) {
            Visual giantPea = Visual.staticVisual(GIANT_PEA, GIANT_PEA_IMPACT, 0.43f, 0.76f);
            if (sourceName.contains("repeater")) {
                giantPea.withPam("REPEATER_PLANTFOOD_GIANTPEA", "animation", "animation", true);
            }
            return giantPea;
        }
        if (sourceName.contains("caulipower")) {
            return Visual.staticVisual(CAULIPOWER, CAULIPOWER_IMPACT, 0.31f, 0.46f)
                    .withPam("CAULIPOWER_PROJECTILE", numberedClip("animation", tier),
                            numberedClip("animation", tier), true);
        }
        if (sourceName.contains("electric blueberry")) {
            return Visual.staticVisual(ELECTRIC_BLUEBERRY, ELECTRIC_BLUEBERRY_IMPACT, 0.34f, 0.72f)
                    .withPam("ELECTRICBLUEBERRY_CLOUD_PROJECTILE", "start", "idle", false)
                    .withImpactPam("ELECTRICBLUEBERRY_CLOUD_PROJECTILE", "attack", "death");
        }
        if (sourceName.contains("fume-shroom")) {
            String clip = plantFoodActive ? "plantfood" : "special";
            return Visual.staticVisual(FUME, FUME_IMPACT, 0.38f, 0.52f)
                    .withPam("FUMESHROOM_BUBBLES", clip, clip, true)
                    .withImpactPam("FUMESHROOM_BUBBLES_HIT", "animation", null);
        }
        if (sourceName.contains("cactus")) {
            boolean airAttack = projectile != null && (projectile.getVerticalDirection() != 0
                    || projectile.getTarget() != null && projectile.getTarget().isAirborne());
            String animation = plantFoodActive ? "CACTUS_PROJECTILE_PLANTFOOD"
                    : airAttack ? "CACTUS_AIRATTACK" : "T_CACTUS_PROJECTILE";
            String clip = plantFoodActive ? "idle" : numberedClip("idle", tier);
            return Visual.staticVisual(CACTUS, CACTUS_IMPACT, 0.31f, 0.52f)
                    .withPam(animation, clip, clip, true)
                    .withImpactPam("CACTUS_PROJECTILE_HIT",
                            numberedClip("animation", tier), null)
                    .rotateToDirection();
        }
        if (sourceName.contains("ice-shroom")) {
            return Visual.staticVisual(SNOW_PEA, WINTER_MELON_IMPACT, 0.34f, 0.74f)
                    .withPam("ICESHROOM_PROJECTILE", "animation", "animation", true)
                    .withImpactPam("ICESHROOM_FX", "animation", null);
        }
        ProjectileType type = projectile == null ? ProjectileType.NORMAL : projectile.getType();
        if (type == ProjectileType.FIRE || sourceName.contains("fire peashooter")) {
            return Visual.staticVisual(FIRE, FIRE_IMPACT, 0.30f, 0.62f);
        }
        if (type == ProjectileType.ICE || sourceName.contains("snow pea")) {
            return Visual.staticVisual(SNOW_PEA, PEA_IMPACT, 0.27f, 0.50f)
                    .withImpactPamSequence(
                            plantFoodActive ? "SNOWPEA_PLANTFOOD_SLOW" : "SPLAT_SNOW_PEA",
                            plantFoodActive ? "plantfood_on" : "animation",
                            plantFoodActive ? "plantfood_idle" : null,
                            plantFoodActive ? "plantfood_off" : null);
        }
        if (type == ProjectileType.POISON) {
            return Visual.staticVisual(PEA, PEA_IMPACT, 0.24f, 0.50f)
                    .withTint(new Color(0.55f, 1f, 0.44f, 1f));
        }
        if (type == ProjectileType.PIERCING) {
            return Visual.staticVisual(CACTUS, CACTUS_IMPACT, 0.31f, 0.52f);
        }
        return Visual.staticVisual(
                PEA,
                PEA_IMPACT,
                projectile != null && projectile.isLobbed() ? 0.34f : 0.24f,
                0.50f
        );
    }

    private static int sourceTier(Projectile projectile) {
        Plant source = projectile == null ? null : projectile.getSourcePlant();
        return source == null ? 1 : Math.max(1, Math.min(3, source.getLevel()));
    }

    private static boolean isGiantPea(Projectile projectile, String sourceName) {
        if (projectile == null || sourceName == null) {
            return false;
        }
        boolean giantPeaSource = sourceName.contains("repeater")
                || sourceName.contains("pea pod")
                || sourceName.contains("mega gatling");
        if (!giantPeaSource) {
            return false;
        }
        return DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression()) >= 300;
    }

    private static Visual bowlingBulbVisual(Projectile projectile, boolean plantFoodActive) {
        if (plantFoodActive) {
            return Visual.staticVisual(BULB_THREE, PEA_IMPACT, 0.44f, 0.82f)
                    .withPam("BOWLINGBULB_PLANTFOOD_PROJECTILE", "animation", "animation", true)
                    .withImpactPam("BOWLINGBULB_PLANTFOOD_PROJECTILE", "explosion", null);
        }
        int damage = projectile == null
                ? 0
                : DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
        if (damage >= 160) {
            return Visual.staticVisual(BULB_THREE, PEA_IMPACT, 0.39f, 0.58f)
                    .withPam("BOWLINGBULB_PROJECTILE3", "animation", "animation", true);
        }
        if (damage >= 100) {
            return Visual.staticVisual(BULB_TWO, PEA_IMPACT, 0.35f, 0.54f)
                    .withPam("BOWLINGBULB_PROJECTILE2", "animation", "animation", true);
        }
        return Visual.staticVisual(BULB_ONE, PEA_IMPACT, 0.31f, 0.50f)
                .withPam("BOWLINGBULB_PROJECTILE1", "animation", "animation", true);
    }

    private static String numberedClip(String base, int tier) {
        return tier <= 1 ? base : base + tier;
    }

    private static String citronOrbClip(int tier) {
        return tier <= 1 ? "Citron_Citrus_Orb" : "Citron_Citrus_Orb" + tier;
    }

    private static String grapeshotDirectionClip(Projectile projectile) {
        if (projectile == null) {
            return "animation_forward";
        }
        if (projectile.getVerticalDirection() < 0) {
            return "animation_verticle_up";
        }
        if (projectile.getVerticalDirection() > 0) {
            return "animation_verticle_down";
        }
        return projectile.getHorizontalDirection() < 0
                ? "animation_backward"
                : "animation_forward";
    }

    private static String sourceName(Projectile projectile) {
        if (projectile == null) {
            return "";
        }
        Plant source = projectile.getSourcePlant();
        if (source == null || source.getName() == null) {
            return "";
        }
        return source.getName().toLowerCase(Locale.ROOT).trim();
    }

    static final class Visual {
        private final String resourceId;
        private final String impactResourceId;
        private Color tint;
        private final float sizeFactor;
        private final float impactSizeFactor;
        private String pamAnimationName;
        private String pamStartClip;
        private String pamLoopClip;
        private boolean pamStartLoops;
        private String overlayPamAnimationName;
        private String overlayPamClip;
        private float overlaySizeMultiplier = 1f;
        private String impactPamAnimationName;
        private String impactPamClip;
        private String impactPamFollowupClip;
        private String impactPamEndClip;
        private boolean rotateToDirection;

        private Visual(
                String resourceId,
                String impactResourceId,
                Color tint,
                float sizeFactor,
                float impactSizeFactor
        ) {
            this.resourceId = resourceId;
            this.impactResourceId = impactResourceId;
            this.tint = tint;
            this.sizeFactor = sizeFactor;
            this.impactSizeFactor = impactSizeFactor;
        }

        static Visual staticVisual(String resourceId, String impactResourceId, float size, float impactSize) {
            return new Visual(resourceId, impactResourceId, Color.WHITE, size, impactSize);
        }

        Visual withTint(Color tint) {
            this.tint = tint == null ? Color.WHITE : tint;
            return this;
        }

        Visual withPam(String animation, String startClip, String loopClip, boolean startLoops) {
            this.pamAnimationName = animation;
            this.pamStartClip = startClip;
            this.pamLoopClip = loopClip;
            this.pamStartLoops = startLoops;
            return this;
        }

        Visual withOverlayPam(String animation, String clip, float sizeMultiplier) {
            this.overlayPamAnimationName = animation;
            this.overlayPamClip = clip;
            this.overlaySizeMultiplier = Math.max(0.1f, sizeMultiplier);
            return this;
        }

        Visual withImpactPam(String animation, String clip, String followupClip) {
            return withImpactPamSequence(animation, clip, followupClip, null);
        }

        Visual withImpactPamSequence(
                String animation,
                String clip,
                String followupClip,
                String endClip
        ) {
            this.impactPamAnimationName = animation;
            this.impactPamClip = clip;
            this.impactPamFollowupClip = followupClip;
            this.impactPamEndClip = endClip;
            return this;
        }

        Visual rotateToDirection() {
            this.rotateToDirection = true;
            return this;
        }

        String getResourceId() { return this.resourceId; }
        String getImpactResourceId() { return this.impactResourceId; }
        Color getTint() { return this.tint; }
        float getSizeFactor() { return this.sizeFactor; }
        float getImpactSizeFactor() { return this.impactSizeFactor; }
        String getPamAnimationName() { return this.pamAnimationName; }
        String getPamStartClip() { return this.pamStartClip; }
        String getPamLoopClip() { return this.pamLoopClip; }
        boolean isPamStartLoops() { return this.pamStartLoops; }
        String getOverlayPamAnimationName() { return this.overlayPamAnimationName; }
        String getOverlayPamClip() { return this.overlayPamClip; }
        float getOverlaySizeMultiplier() { return this.overlaySizeMultiplier; }
        String getImpactPamAnimationName() { return this.impactPamAnimationName; }
        String getImpactPamClip() { return this.impactPamClip; }
        String getImpactPamFollowupClip() { return this.impactPamFollowupClip; }
        String getImpactPamEndClip() { return this.impactPamEndClip; }
        boolean shouldRotateToDirection() { return this.rotateToDirection; }
        boolean usesPam() { return this.pamAnimationName != null; }
        boolean usesImpactPam() { return this.impactPamAnimationName != null; }
    }
}
