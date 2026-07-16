package model.Greenhouse;

import lombok.Getter;
import model.mechanism.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class GreenhouseStateResult {

    private final List<GreenhousePotState> pots;
    private final Set<String> storedBoostPlantNames;
    private final int coins;
    private final int diamonds;

    private GreenhouseStateResult(
            List<GreenhousePotState> pots,
            Set<String> storedBoostPlantNames,
            int coins,
            int diamonds
    ) {
        this.pots = Collections.unmodifiableList(
                new ArrayList<>(pots)
        );

        this.storedBoostPlantNames =
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(
                                storedBoostPlantNames
                        )
                );

        this.coins = Math.max(0, coins);
        this.diamonds = Math.max(0, diamonds);
    }

    public static GreenhouseStateResult from(
            Greenhouse greenhouse,
            int coins,
            int diamonds
    ) {
        return from(
                greenhouse,
                coins,
                diamonds,
                System.currentTimeMillis()
        );
    }

    public static GreenhouseStateResult from(
            Greenhouse greenhouse,
            int coins,
            int diamonds,
            long currentTimeMillis
    ) {
        if (greenhouse == null) {
            return new GreenhouseStateResult(
                    Collections.emptyList(),
                    Collections.emptySet(),
                    coins,
                    diamonds
            );
        }

        List<GreenhousePotState> potStates =
                new ArrayList<>();

        for (Pot pot
                : greenhouse.getBoard().getPots()) {

            if (pot == null) {
                continue;
            }

            GreenhousePotState state =
                    new GreenhousePotState(
                            pot.getPosition(),
                            pot.isUnlocked(),
                            pot.getPlantName(),
                            pot.isReady(currentTimeMillis),
                            pot.getRemainingGrowthHours(
                                    currentTimeMillis
                            )
                    );

            potStates.add(state);
        }

        return new GreenhouseStateResult(
                potStates,
                greenhouse.getStoredBoostPlantNames(),
                coins,
                diamonds
        );
    }

    public GreenhousePotState getPot(
            Position position
    ) {
        if (position == null) {
            return null;
        }

        return this.getPot(
                position.getX(),
                position.getY()
        );
    }

    public GreenhousePotState getPot(
            int x,
            int y
    ) {
        for (GreenhousePotState pot : this.pots) {
            if (pot == null
                    || pot.getPosition() == null) {

                continue;
            }

            Position position = pot.getPosition();

            if (position.getX() == x
                    && position.getY() == y) {

                return pot;
            }
        }

        return null;
    }
}