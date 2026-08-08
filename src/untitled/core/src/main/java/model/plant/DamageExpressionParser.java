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

    public static int parseDamagePerHit(String damageExpression) {
        String selectedExpression = selectExpressionAt(damageExpression, 0);

        if (selectedExpression == null || selectedExpression.isEmpty()) {
            return 0;
        }

        if (isInstantKill(selectedExpression)) {
            return Integer.MAX_VALUE;
        }

        String[] multiplication = splitMultiplication(selectedExpression);

        try {
            return Integer.parseInt(multiplication[0].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int parseHitCount(String damageExpression) {
        String selectedExpression = selectExpressionAt(damageExpression, 0);
        String[] multiplication = splitMultiplication(selectedExpression);

        if (multiplication.length != 2) {
            return 1;
        }

        try {
            return Math.max(1, Integer.parseInt(multiplication[1].trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static String multiplyDamage(String damageExpression, double multiplier) {
        if (damageExpression == null || multiplier <= 0 || isInstantKill(damageExpression)) {
            return damageExpression;
        }

        int damage = parseTotalDamage(damageExpression);
        return String.valueOf(Math.max(0, Math.round(damage * multiplier)));
    }

    public static String selectExpressionAt(String damageExpression, int index) {
        if (damageExpression == null || damageExpression.trim().isEmpty()) {
            return "0";
        }

        String[] expressions = damageExpression.trim().split("/");
        int selectedIndex = Math.max(0, Math.min(index, expressions.length - 1));
        return expressions[selectedIndex].trim();
    }

    public static int parseDamageAt(String damageExpression, int index) {
        if (damageExpression == null || damageExpression.trim().isEmpty()) {
            return 0;
        }

        String expression = damageExpression.trim();

        if (isInstantKill(expression)) {
            return Integer.MAX_VALUE;
        }

        String[] alternatives = expression.split("/");

        if (index < 0 || index >= alternatives.length) {
            return 0;
        }

        expression = alternatives[index].trim();
        String[] multiplication = splitMultiplication(expression);

        try {
            int damage = Integer.parseInt(multiplication[0].trim());

            if (multiplication.length == 2) {
                damage *= Integer.parseInt(multiplication[1].trim());
            }

            return damage;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String[] splitMultiplication(String expression) {
        if (expression == null) {
            return new String[]{"0"};
        }

        return expression.toLowerCase().split("x", -1);
    }
}
