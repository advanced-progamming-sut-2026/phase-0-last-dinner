package model.Greenhouse;

import model.mechanism.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GreenhouseBoard {

    public static final int ROW_COUNT = 4;
    public static final int COLUMN_COUNT = 5;
    public static final int MAXIMUM_POT_COUNT =
            ROW_COUNT * COLUMN_COUNT;

    private ArrayList<Pot> pots;

    public GreenhouseBoard() {
        this.initialisePots();
    }

    public List<Pot> getPots() {
        this.ensurePotsInitialised();

        return Collections.unmodifiableList(
                this.pots
        );
    }

    public Pot getPot(Position position) {
        if (position == null) {
            return null;
        }

        return this.getPot(
                position.getX(),
                position.getY()
        );
    }

    public Pot getPot(int x, int y) {
        if (!this.isValidPosition(x, y)) {
            return null;
        }

        this.ensurePotsInitialised();

        for (Pot pot : this.pots) {
            if (pot == null || pot.getPosition() == null) {
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

    public boolean isValidPosition(int x, int y) {
        return x >= 1
                && x <= COLUMN_COUNT
                && y >= 1
                && y <= ROW_COUNT;
    }

    public int getUnlockedPotCount() {
        this.ensurePotsInitialised();

        int unlockedCount = 0;

        for (Pot pot : this.pots) {
            if (pot != null && pot.isUnlocked()) {
                unlockedCount++;
            }
        }

        return unlockedCount;
    }

    public Pot unlockNextPot() {
        this.ensurePotsInitialised();

        for (Pot pot : this.pots) {
            if (pot != null && !pot.isUnlocked()) {
                pot.unlock();
                return pot;
            }
        }

        return null;
    }

    private void initialisePots() {
        this.pots = new ArrayList<>();

        for (int y = 1; y <= ROW_COUNT; y++) {
            for (int x = 1; x <= COLUMN_COUNT; x++) {
                boolean initiallyUnlocked = y == 1;

                this.pots.add(
                        new Pot(
                                new Position(x, y),
                                initiallyUnlocked
                        )
                );
            }
        }
    }

    private void ensurePotsInitialised() {
        if (this.pots == null || this.pots.isEmpty()) {
            this.initialisePots();
        }
    }
}
