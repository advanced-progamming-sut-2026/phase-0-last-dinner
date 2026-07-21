package model.GameMenuRelated;

import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class QuestObj {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^}]+}");

    private final Quest quest;
    private final String variableValue;
    private int completionPercentage;
    private boolean rewardClaimed;

    public QuestObj(Quest quest) {
        this(quest, defaultVariableValue(quest));
    }

    public QuestObj(Quest quest, String variableValue) {
        if (quest == null) {
            throw new IllegalArgumentException("Quest is required.");
        }

        this.quest = quest;
        this.variableValue = variableValue == null ? "" : variableValue.trim();
        this.completionPercentage = 0;
        this.rewardClaimed = false;
    }

    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = Math.max(0, Math.min(100, completionPercentage));
    }

    public void addProgress(int percentage) {
        if (percentage > 0) {
            this.setCompletionPercentage(this.completionPercentage + percentage);
        }
    }

    public boolean isCompleted() {
        return this.completionPercentage == 100;
    }

    public boolean claimReward() {
        if (!this.isCompleted() || this.rewardClaimed) {
            return false;
        }

        this.rewardClaimed = true;
        return true;
    }

    public void reset() {
        this.completionPercentage = 0;
        this.rewardClaimed = false;
    }

    public String getCompletionCondition() {
        return this.resolve(this.quest.getCompletionCondition());
    }

    public String getReward() {
        return this.resolve(this.quest.getReward());
    }

    private String resolve(String template) {
        if (template == null || this.variableValue.isEmpty()) {
            return template == null ? "" : template;
        }

        return VARIABLE_PATTERN.matcher(template).replaceAll(
                Matcher.quoteReplacement(this.variableValue)
        );
    }

    private static String defaultVariableValue(Quest quest) {
        if (quest == null) {
            return "";
        }

        switch (quest) {
            case DAILY_SUN_COLLECTOR:
                return "3000";
            case CHAPTER_HUNTER:
                return "Ancient Egypt";
            case PROFESSIONAL_PLANT_PLAYER:
                return "Peashooter";
            case ECONOMICAL_GARDENER:
                return "0";
            case FAMILY_MASSACRE:
            case BLOOMING_WITH_LIMITS:
                return "selected family";
            case EMPTY_COLUMN:
            case UNDEFENDED_ROW:
            case UNDEFENDED_CROSS:
                return "1";
            case MOWING_TIME:
                return "10";
            default:
                return "";
        }
    }
}
