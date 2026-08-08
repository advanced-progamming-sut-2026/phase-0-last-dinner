package model.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlantUpgradeEffectParser {
    private static final int TICKS_PER_SECOND = 10;
    private static final Pattern FIRST_INTEGER = Pattern.compile("-?\\d+");

    public List<PlantUpgradeEffect> parseAll(List<String> descriptions) {
        List<PlantUpgradeEffect> effects = new ArrayList<>();

        if (descriptions == null) {
            return effects;
        }

        for (String description : descriptions) {
            if (description == null || description.trim().isEmpty()) {
                continue;
            }

            effects.add(this.parse(description));
        }

        return effects;
    }

    public PlantUpgradeEffect parse(String description) {
        String text = description == null ? "" : description.trim();
        String normalized = text.toLowerCase(Locale.ROOT);
        int amount = this.firstInteger(normalized);
        PlantUpgradeEffect.Builder builder = PlantUpgradeEffect.builder(text);
        this.addNumericEffect(normalized, amount, builder);
        this.addSpecialEffects(normalized, amount, builder);
        return builder.build();
    }

    private void addNumericEffect(String normalized, int amount, PlantUpgradeEffect.Builder builder) {
        if (this.addTimingOrCostEffect(normalized, amount, builder)) {
            return;
        }

        if (normalized.startsWith("range +")) {
            builder.addRangeBonus(amount);
        } else if (normalized.startsWith("lifespan +")) {
            builder.addLifespanBonusTicks(this.secondsToTicks(amount));
        } else if (normalized.startsWith("duration +")
                || normalized.startsWith("chill time +")
                || normalized.startsWith("freeze time +")) {
            builder.addDurationBonusTicks(this.secondsToTicks(amount));
        } else if (normalized.startsWith("sun drop +")) {
            builder.addSunDropBonus(amount);
        } else if (normalized.startsWith("sun +")) {
            builder.addSunProductionBonus(amount);
        } else if (normalized.startsWith("targets +")) {
            builder.addTargetCountBonus(amount);
        } else if (normalized.startsWith("pierce +")) {
            builder.addPierceBonus(amount);
        } else if (normalized.startsWith("bounces +")) {
            builder.addBounceBonus(amount);
        } else if (normalized.startsWith("plant food chance +")) {
            builder.addPlantFoodChanceBonusPercent(amount);
        } else if (normalized.startsWith("dmg/tick +")) {
            builder.addPoisonDamageBonusPerTick(amount);
        } else if (normalized.contains("dmg")) {
            builder.addDamageBonus(amount);
        }
    }

    private boolean addTimingOrCostEffect(
            String normalized,
            int amount,
            PlantUpgradeEffect.Builder builder
    ) {
        if (normalized.startsWith("hp +")) {
            builder.addHealthBonus(amount);
        } else if (normalized.startsWith("cost -")) {
            builder.addSunCostReduction(amount);
        } else if (normalized.startsWith("cooldown -")) {
            builder.addCooldownReductionTicks(this.secondsToTicks(amount));
        } else if (normalized.startsWith("prod. time -")
                || normalized.startsWith("grow time -")
                || normalized.startsWith("charge time -")
                || normalized.startsWith("regen -")
                || normalized.startsWith("eat time -")) {
            builder.addActionIntervalReductionTicks(this.secondsToTicks(amount));
        } else if (normalized.startsWith("atk speed +")) {
            builder.addAttackSpeedBonusPercent(amount);
        } else if (normalized.startsWith("arm time -")) {
            builder.addArmDelayReductionTicks(this.secondsToTicks(amount));
        } else if (normalized.startsWith("digest -")) {
            builder.addDigestReductionTicks(this.secondsToTicks(amount));
        } else {
            return false;
        }
        return true;
    }

    private void addSpecialEffects(String normalized, int amount, PlantUpgradeEffect.Builder builder) {
        if (normalized.contains("double sun")) {
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.DOUBLE_SUN_CHANCE);
        }

        if (normalized.contains("target priority")) {
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.TARGET_PRIORITY_UP);
        }

        if (normalized.startsWith("butter +")) {
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.BUTTER_CHANCE_UP);
        }

        if (normalized.contains("can crush")) {
            builder.addTargetCountBonus(Math.max(1, amount - 1));
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.CAN_CRUSH_EXTRA_TARGET);
        }

        if (normalized.contains("max size")) {
            builder.addRangeBonus(Math.max(1, amount));
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.MAX_SIZE_UP);
        }

        if (normalized.contains("warmth radius")) {
            builder.addRangeBonus(Math.max(1, amount));
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.WARMTH_RADIUS_UP);
        }

        this.addRemainingSpecialEffects(normalized, builder);
    }

    private void addRemainingSpecialEffects(String normalized, PlantUpgradeEffect.Builder builder) {
        if (normalized.contains("melt area")) {
            builder.addRangeBonus(1);
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.MELT_AREA);
        }

        if (normalized.contains("explode on finish")) {
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.EXPLODE_ON_FINISH);
        }

        if (normalized.contains("aoe on death")) {
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.AOE_ON_DEATH);
        }

        if (normalized.contains("zombie hp buff")) {
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.ZOMBIE_HEALTH_BUFF);
        }

        if (normalized.contains("zombie dmg buff")) {
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.ZOMBIE_DAMAGE_BUFF);
        }

        if (normalized.contains("plant food on enterance")
                || normalized.contains("plant food on entrance")) {
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.PLANT_FOOD_ON_PLANTING);
        }

        if (normalized.contains("reset family cooldown")) {
            builder.addSpecialEffect(PlantUpgradeSpecialEffect.RESET_FAMILY_COOLDOWNS);
        }
    }

    private int firstInteger(String value) {
        if (value == null) {
            return 0;
        }

        Matcher matcher = FIRST_INTEGER.matcher(value);

        if (!matcher.find()) {
            return 0;
        }

        return Math.abs(Integer.parseInt(matcher.group()));
    }

    private long secondsToTicks(int seconds) {
        return Math.max(0, seconds) * TICKS_PER_SECOND;
    }
}
