package model.minigame.beghouledminigame;

import java.util.List;

public class BeghouledStageGenerator {

    private static final int STAGE_COUNT = 3;

    public BeghouledStageConfig generateStage(
            int stageNumber
    ) {
        return switch (stageNumber) {
            case 1 -> createStageOne();
            case 2 -> createStageTwo();
            case 3 -> createStageThree();

            default -> throw new IllegalArgumentException(
                    "Beghouled stage must be "
                            + "between 1 and 3."
            );
        };
    }

    public int getStageCount() {
        return STAGE_COUNT;
    }

    private BeghouledStageConfig createStageOne() {
        return new BeghouledStageConfig(
                1,
                10,
                List.of(
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
                List.of(
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
                List.of(
                        "Repeater",
                        "Wall-nut",
                        "Fume-shroom",
                        "Cabbage-pult",
                        "Melon-pult"
                )
        );
    }
}