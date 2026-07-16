package model.minigame.zombotanyminigame;

import lombok.Getter;
import model.mechanism.Position;
import model.minigame.zombotanyminigame.ZombotanyTrait;
import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ZombotanyStateResult {

    public static final int COLUMN_COUNT = 9;
    public static final int ROW_COUNT = 5;

    private final int stageNumber;
    private final int highestUnlockedStage;

    private final int currentWave;
    private final int waveCount;

    private final int sunAmount;
    private final int plantFoodAmount;

    private final List<PlantDefinition> availablePlants;

    private final Map<ZombieDefinition, ZombotanyTrait>
            zombieTraits;

    private final List<List<String>> plantGrid;

    private final List<ZombotanyZombieState> zombies;
    private final List<Position> collectibleSunPositions;

    private final boolean started;
    private final boolean completed;
    private final boolean won;
    private final boolean lost;

    public ZombotanyStateResult(
            int stageNumber,
            int highestUnlockedStage,
            int currentWave,
            int waveCount,
            int sunAmount,
            int plantFoodAmount,
            List<PlantDefinition> availablePlants,
            Map<ZombieDefinition, ZombotanyTrait> zombieTraits,
            List<List<String>> plantGrid,
            List<ZombotanyZombieState> zombies,
            List<Position> collectibleSunPositions,
            boolean started,
            boolean completed,
            boolean won,
            boolean lost
    ) {
        this.stageNumber = Math.max(1, stageNumber);
        this.highestUnlockedStage =
                Math.max(1, highestUnlockedStage);

        this.currentWave = Math.max(0, currentWave);
        this.waveCount = Math.max(0, waveCount);

        this.sunAmount = Math.max(0, sunAmount);
        this.plantFoodAmount =
                Math.max(0, plantFoodAmount);

        this.availablePlants =
                availablePlants == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(
                        new ArrayList<>(availablePlants)
                );

        this.zombieTraits =
                zombieTraits == null
                        ? Collections.emptyMap()
                        : Collections.unmodifiableMap(
                        new LinkedHashMap<>(zombieTraits)
                );

        this.plantGrid = copyGrid(plantGrid);

        this.zombies =
                zombies == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(
                        new ArrayList<>(zombies)
                );

        this.collectibleSunPositions =
                collectibleSunPositions == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(
                        new ArrayList<>(
                                collectibleSunPositions
                        )
                );

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

        List<List<String>> result =
                new ArrayList<>();

        for (List<String> row : source) {
            if (row == null) {
                result.add(
                        Collections.emptyList()
                );
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
                || rowIndex >= plantGrid.size()) {
            return "";
        }

        List<String> row =
                plantGrid.get(rowIndex);

        if (columnIndex < 0
                || columnIndex >= row.size()) {
            return "";
        }

        String name = row.get(columnIndex);

        return name == null ? "" : name;
    }
}