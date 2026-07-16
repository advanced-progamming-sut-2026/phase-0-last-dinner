package model.minigame.beghouledminigame;

import lombok.Getter;
import model.mechanism.Position;
import model.minigame.beghouledminigame.PlantUpgradeOption;
import model.plant.PlantDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class BeghouledStateResult {

    public static final int COLUMN_COUNT = 9;
    public static final int ROW_COUNT = 5;

    private final int stageNumber;
    private final int highestUnlockedStage;

    private final int sunAmount;
    private final int completedMatchCount;
    private final int targetMatchCount;

    private final List<PlantDefinition> availablePlantTypes;
    private final List<PlantUpgradeOption> upgradeOptions;

    private final List<List<String>> plantNames;
    private final Set<Position> craters;

    private final boolean possibleMove;
    private final boolean endlessZombieWaves;

    private final boolean started;
    private final boolean completed;
    private final boolean won;
    private final boolean lost;

    public BeghouledStateResult(
            int stageNumber,
            int highestUnlockedStage,
            int sunAmount,
            int completedMatchCount,
            int targetMatchCount,
            List<PlantDefinition> availablePlantTypes,
            List<PlantUpgradeOption> upgradeOptions,
            List<List<String>> plantNames,
            Set<Position> craters,
            boolean possibleMove,
            boolean endlessZombieWaves,
            boolean started,
            boolean completed,
            boolean won,
            boolean lost
    ) {
        this.stageNumber = Math.max(1, stageNumber);
        this.highestUnlockedStage =
                Math.max(1, highestUnlockedStage);

        this.sunAmount = Math.max(0, sunAmount);
        this.completedMatchCount =
                Math.max(0, completedMatchCount);
        this.targetMatchCount =
                Math.max(0, targetMatchCount);

        this.availablePlantTypes =
                availablePlantTypes == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(
                        new ArrayList<>(availablePlantTypes)
                );

        this.upgradeOptions =
                upgradeOptions == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(
                        new ArrayList<>(upgradeOptions)
                );

        this.plantNames = copyGrid(plantNames);

        this.craters = craters == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(
                new HashSet<>(craters)
        );

        this.possibleMove = possibleMove;
        this.endlessZombieWaves = endlessZombieWaves;

        this.started = started;
        this.completed = completed;
        this.won = won;
        this.lost = lost;
    }

    private List<List<String>> copyGrid(
            List<List<String>> source
    ) {
        if (source == null) {
            return Collections.emptyList();
        }

        List<List<String>> result = new ArrayList<>();

        for (List<String> row : source) {
            if (row == null) {
                result.add(Collections.emptyList());
            } else {
                result.add(
                        Collections.unmodifiableList(
                                new ArrayList<>(row)
                        )
                );
            }
        }

        return Collections.unmodifiableList(result);
    }

    public String getPlantNameAt(Position position) {
        if (position == null) {
            return "";
        }

        int columnIndex = position.getX() - 1;
        int rowIndex = position.getY() - 1;

        if (rowIndex < 0
                || rowIndex >= plantNames.size()) {
            return "";
        }

        List<String> row = plantNames.get(rowIndex);

        if (columnIndex < 0
                || columnIndex >= row.size()) {
            return "";
        }

        String name = row.get(columnIndex);
        return name == null ? "" : name;
    }

    public boolean isCrater(Position position) {
        return position != null
                && craters.contains(position);
    }

    public int getRemainingMatchCount() {
        return Math.max(
                0,
                targetMatchCount - completedMatchCount
        );
    }
}