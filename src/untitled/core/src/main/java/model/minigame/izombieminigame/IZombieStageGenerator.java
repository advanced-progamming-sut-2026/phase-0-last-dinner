package model.minigame.izombieminigame;

public class IZombieStageGenerator {

    private static final int STAGE_COUNT = 3;

    private static final int STARTING_SUN = 150;
    private static final int RED_LINE_COLUMN = 5;
    private static final int SUN_PRODUCTION_AMOUNT = 25;

    public IZombieStageConfig generateStage(int stageNumber) {
        switch (stageNumber) {
            case 1:
                return createStageOne();
            case 2:
                return createStageTwo();
            case 3:
                return createStageThree();
            default:
                throw new IllegalArgumentException(
                        "Stage number must be between 1 and " + STAGE_COUNT + "."
                );
        }
    }

    private IZombieStageConfig createStageOne() {
        return new IZombieStageConfig(
            1,
            STARTING_SUN,
            RED_LINE_COLUMN,
            SUN_PRODUCTION_AMOUNT,
            80,
            35,
            5
        );
    }

    private IZombieStageConfig createStageTwo() {
        return new IZombieStageConfig(
            2,
            STARTING_SUN,
            RED_LINE_COLUMN,
            SUN_PRODUCTION_AMOUNT,
            100,
            40,
            5
        );
    }

    private IZombieStageConfig createStageThree() {
        return new IZombieStageConfig(
            3,
            STARTING_SUN,
            RED_LINE_COLUMN,
            SUN_PRODUCTION_AMOUNT,
            120,
            45,
            4
        );
    }

    public int getStageCount() {
        return STAGE_COUNT;
    }
}
