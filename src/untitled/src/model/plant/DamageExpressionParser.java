package model.plant;

public class DamageExpressionParser {
    public static boolean isInstantKill(String damageExpression) {
        return damageExpression != null
                && damageExpression.trim().equalsIgnoreCase("Insta-kill");
    }

    public static String addFlatDamage(String damageExpression, int bonusDamage) {
        if (bonusDamage <= 0 || damageExpression == null || damageExpression.trim().isEmpty()) {
            return damageExpression;
        }

        String expression = damageExpression.trim();

        if (isInstantKill(expression)) {
            return expression;
        }

        if (expression.contains("/")) {
            String[] parts = expression.split("/");
            StringBuilder upgradedExpression = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    upgradedExpression.append("/");
                }

                upgradedExpression.append(addFlatDamage(parts[i], bonusDamage));
            }

            return upgradedExpression.toString();
        }

        if (expression.toLowerCase().contains("x")) {
            String[] parts = expression.toLowerCase().split("x");

            if (parts.length != 2) {
                return expression;
            }

            try {
                int damagePerHit = Integer.parseInt(parts[0].trim());
                int hitCount = Integer.parseInt(parts[1].trim());
                return (damagePerHit + bonusDamage) + "x" + hitCount;
            } catch (NumberFormatException e) {
                return expression;
            }
        }

        try {
            return String.valueOf(Integer.parseInt(expression) + bonusDamage);
        } catch (NumberFormatException e) {
            return expression;
        }
    }

    public static int parseTotalDamage(String damageExpression) {
        return parseDamageAt(damageExpression, 0);
    }

    public static int parseDamageAt(String damageExpression, int index) {
        if (damageExpression == null || damageExpression.trim().isEmpty()) {
            return 0;
        }

        String expression = damageExpression.trim();

        if (isInstantKill(expression)) {
            return Integer.MAX_VALUE;
        }

        if (expression.contains("/")) {
            String[] parts = expression.split("/");

            if (index < 0 || index >= parts.length) {
                return 0;
            }

            expression = parts[index].trim();
        }

        if (expression.contains("x")) {
            String[] parts = expression.split("x");

            if (parts.length != 2) {
                return 0;
            }

            return Integer.parseInt(parts[0].trim()) * Integer.parseInt(parts[1].trim());
        }

        return Integer.parseInt(expression);
    }
}
