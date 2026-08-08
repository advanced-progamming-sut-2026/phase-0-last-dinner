package model.minigame.beghouledminigame;

import lombok.Getter;
import model.mechanism.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class PlantMatch {

    private static final int SUN_VALUE = 50;

    private final List<Position> positions;

    private final boolean cascade;

    public PlantMatch(
            List<Position> positions,
            boolean cascade
    ) {
        if (positions == null
                || positions.size() < 3) {

            throw new IllegalArgumentException(
                    "A plant match requires at least "
                            + "three positions."
            );
        }

        this.positions =
                new ArrayList<>(positions);

        this.cascade = cascade;
    }

    public int calculateSunReward() {

        int sunCount = getSize() - 2;


        if (cascade) {
            sunCount++;
        }

        return sunCount * SUN_VALUE;
    }

    public boolean contains(Position position) {
        return position != null
                && positions.contains(position);
    }

    public List<Position> getPositions() {
        return Collections.unmodifiableList(
                positions
        );
    }

    public int getSize() {
        return positions.size();
    }
}