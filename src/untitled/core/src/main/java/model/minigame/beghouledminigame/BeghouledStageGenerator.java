package model.minigame.beghouledminigame;

import java.util.Arrays;

public class BeghouledStageGenerator {

    private static final int STAGE_COUNT = 3;

    public BeghouledStageConfig generateStage(
            int stageNumber
    ) {
        switch (stageNumber) {
            case 1:
                return createStageOne();
            case 2:
                return createStageTwo();
            case 3:
                return createStageThree();
            default:
                throw new IllegalArgumentException(
                        "Beghouled stage must be "
                                + "between 1 and 3."
                );
        }
    }

    public int getStageCount() {
        return STAGE_COUNT;
    }

    private BeghouledStageConfig createStageOne() {
        return new BeghouledStageConfig(
                1,
                10,
                Arrays.asList(
                        "Peashooter",
                        "Wall-nut",
                        "Puff-shroom",
                        "Cabbage-pult",
                        "Melon-pult"
                )
        );
    }

    private BeghouledStageConfig createStageTwo() {
        return new BeghouledStageConfig(
                2,
                18,
                Arrays.asList(
                        "Peashooter",
                        "Repeater",
                        "Wall-nut",
                        "Puff-shroom",
                        "Cabbage-pult"
                )
        );
    }

    private BeghouledStageConfig createStageThree() {
        return new BeghouledStageConfig(
                3,
                25,
                Arrays.asList(
                        "Repeater",
                        "Wall-nut",
                        "Fume-shroom",
                        "Cabbage-pult",
                        "Melon-pult"
                )
        );
    }
}
