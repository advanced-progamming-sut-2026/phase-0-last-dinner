package model.level;

import model.Plant;
import model.chapters.Chapter;
import model.mechanism.GameClock;

import java.util.List;

public class LevelFactory {
    public Level create(
            LevelType levelType,
            Chapter chapter,
            List<Plant> allowedPlants,
            double baseDifficulty,
            GameClock gameClock) {
        if (levelType == null) {
            throw new IllegalArgumentException("levelType cannot be null.");
        }

        switch (levelType) {
            case NORMAL:
                return new NormalLevel(chapter, allowedPlants, baseDifficulty);
            case CONVEYOR_BELT:
                return new ConveyorBeltLevel(chapter, allowedPlants, baseDifficulty, gameClock);
            case DEADLINE:
                return new DeadlineLevel(chapter, allowedPlants, baseDifficulty);
            case NIGHT_OPS:
                return new NightOpsLevel(chapter, allowedPlants, baseDifficulty);
            case LOVE_YOUR_PLANTS:
                return new LoveYourPlantsLevel(chapter, allowedPlants, baseDifficulty);
            case BOSS:
            case MEOW_POINT:
                return new MeowPointLevel(chapter, allowedPlants, baseDifficulty, gameClock);
            default:
                throw new IllegalArgumentException("Unknown level type: " + levelType);
        }
    }
}