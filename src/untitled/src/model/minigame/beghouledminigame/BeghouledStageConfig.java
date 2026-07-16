package model.minigame.beghouledminigame;

import lombok.Getter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class BeghouledStageConfig {

    private static final int REQUIRED_PLANT_TYPE_COUNT = 5;

    private final int stageNumber;

    private final int targetMatchCount;

    private final List<String> plantNames;

    public BeghouledStageConfig(
            int stageNumber,
            int targetMatchCount,
            List<String> plantNames
    ) {
        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException(
                    "Beghouled stage must be "
                            + "between 1 and 3."
            );
        }

        if (targetMatchCount <= 0) {
            throw new IllegalArgumentException(
                    "Target match count must be positive."
            );
        }

        if (plantNames == null
                || plantNames.size()
                != REQUIRED_PLANT_TYPE_COUNT) {

            throw new IllegalArgumentException(
                    "Every Beghouled stage requires "
                            + "exactly five plant types."
            );
        }

        for (String plantName : plantNames) {
            if (plantName == null
                    || plantName.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Plant names cannot be blank."
                );
            }
        }

        this.stageNumber = stageNumber;
        this.targetMatchCount = targetMatchCount;
        this.plantNames =
                new ArrayList<>(plantNames);
    }

    public List<String> getPlantNames() {
        return Collections.unmodifiableList(
                plantNames
        );
    }
}
