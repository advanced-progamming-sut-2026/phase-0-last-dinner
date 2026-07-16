package model.minigame.wallnutbowlingminigame;

import java.util.Random;

public class WallnutBowlingStageGenerator {
    private final Random random;

    public WallnutBowlingStageGenerator() {
        this(new Random());
    }

    public WallnutBowlingStageGenerator(
            Random random
    ) {
        if (random == null) {
            this.random = new Random();
        } else {
            this.random = random;
        }
    }

    public WallnutBowlingStageConfig generateStage(
            int stageNumber
    ) {
        return switch (stageNumber) {
            case 1 -> generateStageOne();
            case 2 -> generateStageTwo();
            case 3 -> generateStageThree();

            default -> throw new IllegalArgumentException(
                    "Wallnut Bowling stage must "
                            + "be between 1 and 3."
            );
        };
    }

    public WallnutBowlingStageConfig generateStageOne() {
        return new WallnutBowlingStageConfig(
                1,

                3,

                5,

                3,

                40,

                2,

                80,

                20,

                0
        );
    }

    public WallnutBowlingStageConfig generateStageTwo() {
        return new WallnutBowlingStageConfig(
                2,

                3,

                5,

                3,

                35,

                2,

                65,

                30,

                5
        );
    }

    public WallnutBowlingStageConfig generateStageThree() {
        return new WallnutBowlingStageConfig(
                3,

                3,

                5,

                3,

                30,

                1,

                50,

                30,

                20
        );
    }

    public BowlingWallnutType chooseRandomWallnutType(
            WallnutBowlingStageConfig config
    ) {
        if (config == null) {
            throw new IllegalArgumentException(
                    "Stage config cannot be null."
            );
        }

        int roll = random.nextInt(100);

        int normalLimit =
                config.getNormalWallnutChance();

        if (roll < normalLimit) {
            return BowlingWallnutType
                    .BOWLING_WALLNUT;
        }

        int explosiveLimit =
                normalLimit
                        + config.getExplodeONutChance();

        if (roll < explosiveLimit) {
            return BowlingWallnutType
                    .EXPLODE_O_NUT;
        }

        return BowlingWallnutType
                .GIANT_WALLNUT;
    }
}