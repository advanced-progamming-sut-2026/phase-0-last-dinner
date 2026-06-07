package model.GameMenuRelated;

public enum Quest {
    DAILY_SUN_COLLECTOR(
            "Daily Sun Collector",
            QuestCategory.DAILY,
            QuestPriority.MEDIUM,
            "Collect {sunAmount} sun during one day.",
            "{sunAmount} / 100 coins",
            "sunAmount: 3000, 4000, 5000"
    ),
    CHAPTER_HUNTER(
            "Chapter Hunter",
            QuestCategory.MAIN,
            QuestPriority.HIGH,
            "Defeat 50 zombies from {chapter}.",
            "10 seed packets",
            "chapter: any game chapter"
    ),
    PROFESSIONAL_PLANT_PLAYER(
            "Professional Plant Player",
            QuestCategory.DAILY,
            QuestPriority.HIGH,
            "Defeat 10 zombies using only {plant}.",
            "One random new plant",
            "plant: any plant capable of defeating zombies"
    ),
    ONLY_CACTUS(
            "Only Cactus",
            QuestCategory.DAILY,
            QuestPriority.HIGH,
            "Defeat 10 zombies using only Cactus.",
            "20 diamonds",
            ""
    ),
    ECONOMICAL_GARDENER(
            "Economical Gardener",
            QuestCategory.MAIN,
            QuestPriority.HIGH,
            "Win a level without losing more than {plantCount} plants.",
            "20 - {plantCount} seed packets",
            "plantCount: 0, 1, 2, 3, 4, 5"
    ),
    DEFENSE_MASTER(
            "Defense Master",
            QuestCategory.EPIC,
            QuestPriority.CRITICAL,
            "Finish a level with exactly zero sun.",
            "200 diamonds",
            ""
    ),
    QUICK_ACTION(
            "Quick Action",
            QuestCategory.MAIN,
            QuestPriority.MEDIUM,
            "Defeat 10 zombies within 30 seconds after the first wave starts.",
            "500 coins",
            ""
    ),
    PROFESSIONAL_DEMOLITION(
            "Professional Demolition",
            QuestCategory.DAILY,
            QuestPriority.LOW,
            "Use 3 explosive plants in one level.",
            "100 coins",
            ""
    ),
    SYMMETRY(
            "Symmetry",
            QuestCategory.DAILY,
            QuestPriority.HIGH,
            "Finish the level with a symmetrical garden.",
            "500 coins",
            ""
    ),
    FAMILY_MASSACRE(
            "Family Massacre",
            QuestCategory.DAILY,
            QuestPriority.MEDIUM,
            "Use only plants from {familyType} to defeat zombies.",
            "1000 coins",
            "familyType: any plant family"
    ),
    BLOOMING_WITH_LIMITS(
            "Blooming With Limits",
            QuestCategory.DAILY,
            QuestPriority.HIGH,
            "Win without using plants from {familyType}.",
            "100 diamonds",
            "familyType: any plant family"
    ),
    NIGHT_OR_MORNING(
            "Night Or Morning",
            QuestCategory.EPIC,
            QuestPriority.HIGH,
            "Finish a daytime level using night plants.",
            "20 diamonds",
            ""
    ),
    WINNING_STREAK(
            "Winning Streak",
            QuestCategory.DAILY,
            QuestPriority.MEDIUM,
            "Win 5 levels in a row at maximum difficulty.",
            "5000 coins",
            ""
    ),
    ALMOST_WINNER(
            "Almost Winner",
            QuestCategory.DAILY,
            QuestPriority.MEDIUM,
            "Defeat 10 zombies in the first column of a row without a lawn mower.",
            "300 coins",
            ""
    ),
    ASYMMETRIC_GARDEN(
            "No OCD",
            QuestCategory.DAILY,
            QuestPriority.MEDIUM,
            "Win with no garden symmetry except for the middle row.",
            "800 coins",
            ""
    ),
    CLOUDY_DAY(
            "Cloudy Day",
            QuestCategory.DAILY,
            QuestPriority.HIGH,
            "Win a level using only 3 sun-producing plants.",
            "10 diamonds",
            ""
    ),
    EMPTY_COLUMN(
            "One Less Column",
            QuestCategory.DAILY,
            QuestPriority.HIGH,
            "Win without planting anything in column {column}.",
            "10 diamonds",
            "column: any board column"
    ),
    UNDEFENDED_ROW(
            "Undefended Row",
            QuestCategory.DAILY,
            QuestPriority.HIGH,
            "Win without planting anything in row {row}.",
            "20 diamonds",
            "row: any board row"
    ),
    UNDEFENDED_CROSS(
            "Undefended Cross",
            QuestCategory.DAILY,
            QuestPriority.HIGH,
            "Win while row {index} and column {index} are empty.",
            "25 diamonds",
            "index: minimum of board row and column counts"
    ),
    MOWING_TIME(
            "Mowing Time",
            QuestCategory.EPIC,
            QuestPriority.MEDIUM,
            "Defeat at least {zombieCount} zombies using lawn mowers.",
            "{zombieCount} diamonds",
            "zombieCount: 10, 20, 30, 40, 50"
    );

    private final String displayName;
    private final QuestCategory category;
    private final QuestPriority priority;
    private final String completionCondition;
    private final String reward;
    private final String variables;

    Quest(
            String displayName,
            QuestCategory category,
            QuestPriority priority,
            String completionCondition,
            String reward,
            String variables
    ) {
        this.displayName = displayName;
        this.category = category;
        this.priority = priority;
        this.completionCondition = completionCondition;
        this.reward = reward;
        this.variables = variables;
    }

    public String getDisplayName() {
        return displayName;
    }

    public QuestCategory getCategory() {
        return category;
    }

    public QuestPriority getPriority() {
        return priority;
    }

    public String getCompletionCondition() {
        return completionCondition;
    }

    public String getReward() {
        return reward;
    }

    public String getVariables() {
        return variables;
    }
}
