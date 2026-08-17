package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import model.Plant;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.plant.ProjectileType;

import java.util.Locale;

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
        String sourceName = sourceName(projectile);
        if (sourceName.contains("winter melon")) {
            return new Visual(WINTER_MELON, WINTER_MELON_IMPACT, Color.WHITE, 0.42f, 0.82f);
        }
        if (sourceName.contains("melon-pult")) {
            return new Visual(MELON, MELON_IMPACT, Color.WHITE, 0.42f, 0.76f);
        }
        if (sourceName.contains("pepper-pult")) {
            return new Visual(PEPPER, PEPPER_IMPACT, Color.WHITE, 0.38f, 0.76f);
        }
        if (sourceName.contains("cabbage-pult")) {
            return new Visual(CABBAGE, CABBAGE_IMPACT, Color.WHITE, 0.37f, 0.50f);
        }
        if (sourceName.contains("kernel")) {
            if (projectile != null && projectile.getStunChancePercent() >= 100) {
                return new Visual(BUTTER, BUTTER_IMPACT, Color.WHITE, 0.32f, 0.58f);
            }
            return new Visual(KERNEL, KERNEL_IMPACT, Color.WHITE, 0.29f, 0.42f);
        }
        if (sourceName.contains("citron")) {
            return new Visual(CITRON, CITRON_IMPACT, Color.WHITE, 0.36f, 0.66f);
        }
        if (sourceName.contains("bowling bulb")) {
            return bowlingBulbVisual(projectile);
        }
        if (sourceName.contains("starfruit")) {
            return new Visual(STARFRUIT, STARFRUIT_IMPACT, Color.WHITE, 0.29f, 0.56f);
        }
        if (sourceName.contains("rotobaga")) {
            return new Visual(ROTO, ROTO_IMPACT, Color.WHITE, 0.27f, 0.50f);
        }
        if (sourceName.contains("puff-shroom")) {
            return new Visual(PUFF, FUME_IMPACT, Color.WHITE, 0.21f, 0.40f);
        }
        if (sourceName.contains("sea-shroom")) {
            return new Visual(SEA_SHROOM, FUME_IMPACT, Color.WHITE, 0.23f, 0.42f);
        }
        if (sourceName.contains("goo peashooter")) {
            return new Visual(GOO, PEA_IMPACT, Color.WHITE, 0.27f, 0.50f);
        }
        if (sourceName.contains("grapeshot")) {
            return new Visual(GRAPE, GRAPE_IMPACT, Color.WHITE, 0.24f, 0.54f);
        }
        if (isGiantPea(projectile, sourceName)) {
            return new Visual(GIANT_PEA, GIANT_PEA_IMPACT, Color.WHITE, 0.43f, 0.76f);
        }
        if (sourceName.contains("caulipower")) {
            return new Visual(CAULIPOWER, CAULIPOWER_IMPACT, Color.WHITE, 0.31f, 0.46f);
        }
        if (sourceName.contains("electric blueberry")) {
            return new Visual(
                    ELECTRIC_BLUEBERRY,
                    ELECTRIC_BLUEBERRY_IMPACT,
                    Color.WHITE,
                    0.34f,
                    0.72f
            );
        }
        if (sourceName.contains("fume-shroom")) {
            return new Visual(FUME, FUME_IMPACT, Color.WHITE, 0.38f, 0.52f);
        }
        if (sourceName.contains("cactus")) {
            return new Visual(CACTUS, CACTUS_IMPACT, Color.WHITE, 0.31f, 0.52f);
        }
        ProjectileType type = projectile == null ? ProjectileType.NORMAL : projectile.getType();
        if (type == ProjectileType.FIRE || sourceName.contains("fire peashooter")) {
            return new Visual(FIRE, FIRE_IMPACT, Color.WHITE, 0.30f, 0.62f);
        }
        if (type == ProjectileType.ICE || sourceName.contains("snow pea")) {
            return new Visual(SNOW_PEA, PEA_IMPACT, Color.WHITE, 0.27f, 0.50f);
        }
        if (type == ProjectileType.POISON) {
            return new Visual(
                    PEA,
                    PEA_IMPACT,
                    new Color(0.55f, 1f, 0.44f, 1f),
                    0.24f,
                    0.50f
            );
        }
        if (type == ProjectileType.PIERCING) {
            return new Visual(CACTUS, CACTUS_IMPACT, Color.WHITE, 0.31f, 0.52f);
        }
        return new Visual(
                PEA,
                PEA_IMPACT,
                Color.WHITE,
                projectile != null && projectile.isLobbed() ? 0.34f : 0.24f,
                0.50f
        );
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

    private static Visual bowlingBulbVisual(Projectile projectile) {
        int damage = projectile == null
                ? 0
                : DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
        if (damage >= 160) {
            return new Visual(BULB_THREE, PEA_IMPACT, Color.WHITE, 0.39f, 0.58f);
        }
        if (damage >= 100) {
            return new Visual(BULB_TWO, PEA_IMPACT, Color.WHITE, 0.35f, 0.54f);
        }
        return new Visual(BULB_ONE, PEA_IMPACT, Color.WHITE, 0.31f, 0.50f);
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
        private final Color tint;
        private final float sizeFactor;
        private final float impactSizeFactor;

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

        String getResourceId() {
            return this.resourceId;
        }

        String getImpactResourceId() {
            return this.impactResourceId;
        }

        Color getTint() {
            return this.tint;
        }

        float getSizeFactor() {
            return this.sizeFactor;
        }

        float getImpactSizeFactor() {
            return this.impactSizeFactor;
        }
    }
}
