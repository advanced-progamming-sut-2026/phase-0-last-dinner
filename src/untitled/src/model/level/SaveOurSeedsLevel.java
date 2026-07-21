package model.level;

import model.Plant;
public class SaveOurSeedsLevel extends Level {
    public SaveOurSeedsLevel() {
        super(LevelType.SAVE_OUR_SEEDS);
    }

    public boolean isProtectedPlant(Plant plant) {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public boolean isWinConditionMet() {
        return false;
    }

    @Override
    public boolean isLoseConditionMet() {
        return false;
    }
}
