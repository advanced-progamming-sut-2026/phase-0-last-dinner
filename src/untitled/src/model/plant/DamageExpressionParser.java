package model.plant;

public class DamageExpressionParser {
    public static boolean isInstantKill(String damageExpression) {
        return damageExpression != null
                && damageExpression.trim().equalsIgnoreCase("Insta-kill");
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