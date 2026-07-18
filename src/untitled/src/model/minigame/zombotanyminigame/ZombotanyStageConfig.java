package model.minigame.zombotanyminigame;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public class ZombotanyStageConfig {

    private final int stageNumber;
    private final List<Integer> waveDifficulties;
    private final List<ZombotanyTrait> availableTraits;
    private final List<String> availablePlantNames;

    public ZombotanyStageConfig(
            int stageNumber,
            List<Integer> waveDifficulties,
            List<ZombotanyTrait> availableTraits,
            List<String> availablePlantNames
    ) {
        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException(
                    "Zombotany stage must be between 1 and 3."
            );
        }

        this.stageNumber = stageNumber;

        this.waveDifficulties =
                waveDifficulties == null
                        ? Collections.emptyList()
                        : List.copyOf(waveDifficulties);

        this.availableTraits =
                availableTraits == null
                        ? Collections.emptyList()
                        : List.copyOf(availableTraits);

        this.availablePlantNames =
                availablePlantNames == null
                        ? Collections.emptyList()
                        : List.copyOf(availablePlantNames);
    }

    public static ZombotanyStageConfig forStage(
            int stageNumber
    ) {
        return switch (stageNumber) {
            case 1 -> new ZombotanyStageConfig(
                    1,
                    Arrays.asList(
                            4,
                            6,
                            8
                    ),
                    Arrays.asList(
                            ZombotanyTrait.PEASHOOTER,
                            ZombotanyTrait.WALLNUT
                    ),
                    Arrays.asList(
                            "Sunflower",
                            "Peashooter",
                            "Wall-nut",
                            "Potato Mine",
                            "Cabbage-pult",
                            "Cherry Bomb"
                    )
            );
            case 2 -> new ZombotanyStageConfig(
                    2,
                    Arrays.asList(
                            6,
                            8,
                            10,
                            12
                    ),
                    Arrays.asList(
                            ZombotanyTrait.PEASHOOTER,
                            ZombotanyTrait.WALLNUT,
                            ZombotanyTrait.JALAPENO
                    ),
                    Arrays.asList(
                            "Sunflower",
                            "Repeater",
                            "Snow Pea",
                            "Wall-nut",
                            "Bonk Choy",
                            "Kernel-pult",
                            "Cherry Bomb"
                    )
            );
            case 3 -> new ZombotanyStageConfig(
                    3,
                    Arrays.asList(
                            8,
                            11,
                            14,
                            17,
                            20
                    ),
                    Arrays.asList(
                            ZombotanyTrait.PEASHOOTER,
                            ZombotanyTrait.WALLNUT,
                            ZombotanyTrait.JALAPENO,
                            ZombotanyTrait.SQUASH
                    ),
                    Arrays.asList(
                            "Twin Sunflower",
                            "Repeater",
                            "Snow Pea",
                            "Tall-nut",
                            "Bonk Choy",
                            "Melon-pult",
                            "Cherry Bomb",
                            "Jalapeno"
                    )
            );
            default -> throw new IllegalArgumentException(
                    "Zombotany stage must be between 1 and 3."
            );
        };
    }

    public int getWaveCount() {
        return waveDifficulties.size();
    }

}
