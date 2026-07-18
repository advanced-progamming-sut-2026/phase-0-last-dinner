package model.Greenhouse;

import model.Plant;
import model.mechanism.Position;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class Greenhouse {

    public static final String MARIGOLD_NAME = "Marigold";
    public static final int MARIGOLD_REWARD_COINS = 500;

    private static final long MARIGOLD_GROWTH_MILLIS =
            Duration.ofHours(2).toMillis();

    private static final long NORMAL_PLANT_GROWTH_MILLIS =
            Duration.ofHours(8).toMillis();

    private GreenhouseBoard board;
    private Set<String> storedBoostPlantNames;

    public Greenhouse() {
        this.board = new GreenhouseBoard();
        this.storedBoostPlantNames = new LinkedHashSet<>();
    }

    public GreenhouseBoard getBoard() {
        this.ensureStateInitialised();
        return this.board;
    }

    public String plantRandom(
            Position position,
            List<Plant> unlockedPlants
    ) {
        return this.plantRandom(
                position,
                unlockedPlants,
                System.currentTimeMillis(),
                new Random()
        );
    }

    public String plantRandom(
            Position position,
            List<Plant> unlockedPlants,
            long currentTimeMillis,
            Random random
    ) {
        if (position == null || random == null) {
            return null;
        }

        Pot pot = this.getBoard().getPot(position);

        if (pot == null
                || !pot.isUnlocked()
                || !pot.isEmpty()) {

            return null;
        }

        List<Plant> eligiblePlants =
                this.findEligiblePlants(
                        unlockedPlants
                );

        String selectedPlantName;

        if (random.nextBoolean()
                || eligiblePlants.isEmpty()) {

            selectedPlantName = MARIGOLD_NAME;
        } else {
            Plant selectedPlant = eligiblePlants.get(
                    random.nextInt(
                            eligiblePlants.size()
                    )
            );

            selectedPlantName =
                    selectedPlant.getName();
        }

        long growthDurationMillis =
                this.isMarigold(selectedPlantName)
                        ? MARIGOLD_GROWTH_MILLIS
                        : NORMAL_PLANT_GROWTH_MILLIS;

        boolean planted = pot.plant(
                selectedPlantName,
                currentTimeMillis,
                growthDurationMillis
        );

        return planted
                ? selectedPlantName
                : null;
    }

    public String harvest(Position position) {
        return this.harvest(
                position,
                System.currentTimeMillis()
        );
    }

    public String harvest(
            Position position,
            long currentTimeMillis
    ) {
        Pot pot = this.getBoard().getPot(position);

        if (pot == null) {
            return null;
        }

        String harvestedPlantName =
                pot.harvest(currentTimeMillis);

        if (harvestedPlantName == null) {
            return null;
        }

        if (!this.isMarigold(harvestedPlantName)) {
            this.storeBoost(harvestedPlantName);
        }

        return harvestedPlantName;
    }

    public int getSpeedUpCost(Position position) {
        return this.getSpeedUpCost(
                position,
                System.currentTimeMillis()
        );
    }

    public int getSpeedUpCost(
            Position position,
            long currentTimeMillis
    ) {
        Pot pot = this.getBoard().getPot(position);

        if (pot == null
                || pot.isEmpty()
                || pot.isReady(currentTimeMillis)) {

            return 0;
        }

        return pot.getRemainingGrowthHours(
                currentTimeMillis
        );
    }

    public boolean speedUpGrowth(Position position) {
        return this.speedUpGrowth(
                position,
                System.currentTimeMillis()
        );
    }

    public boolean speedUpGrowth(
            Position position,
            long currentTimeMillis
    ) {
        Pot pot = this.getBoard().getPot(position);

        if (pot == null) {
            return false;
        }

        return pot.accelerateGrowth(
                currentTimeMillis
        );
    }

    public boolean hasStoredBoost(
            String plantName
    ) {
        if (plantName == null) {
            return false;
        }

        this.ensureStateInitialised();

        String normalisedName =
                this.normalise(plantName);

        for (String storedPlantName
                : this.storedBoostPlantNames) {

            if (this.normalise(storedPlantName)
                    .equals(normalisedName)) {

                return true;
            }
        }

        return false;
    }

    public boolean consumeStoredBoost(
            String plantName
    ) {
        if (plantName == null) {
            return false;
        }

        this.ensureStateInitialised();

        String normalisedName =
                this.normalise(plantName);

        Iterator<String> iterator =
                this.storedBoostPlantNames.iterator();

        while (iterator.hasNext()) {
            String storedPlantName =
                    iterator.next();

            if (this.normalise(storedPlantName)
                    .equals(normalisedName)) {

                iterator.remove();
                return true;
            }
        }

        return false;
    }

    public Set<String> getStoredBoostPlantNames() {
        this.ensureStateInitialised();

        return Collections.unmodifiableSet(
                this.storedBoostPlantNames
        );
    }

    public Pot unlockNextPot() {
        return this.getBoard().unlockNextPot();
    }

    public boolean isMarigold(String plantName) {
        return MARIGOLD_NAME.equalsIgnoreCase(
                plantName
        );
    }

    private void storeBoost(String plantName) {
        if (plantName == null
                || this.hasStoredBoost(plantName)) {

            return;
        }

        this.storedBoostPlantNames.add(
                plantName.trim()
        );
    }

    private List<Plant> findEligiblePlants(
            List<Plant> unlockedPlants
    ) {
        List<Plant> eligiblePlants =
                new ArrayList<>();

        if (unlockedPlants == null) {
            return eligiblePlants;
        }

        Set<String> addedPlantNames =
                new LinkedHashSet<>();

        for (Plant plant : unlockedPlants) {
            if (plant == null
                    || plant.getName() == null
                    || plant.getName().trim().isEmpty()
                    || plant.getPlantFoodBehavior() == null) {

                continue;
            }

            String normalisedName =
                    this.normalise(plant.getName());

            if (addedPlantNames.add(normalisedName)) {
                eligiblePlants.add(plant);
            }
        }

        return eligiblePlants;
    }

    private void ensureStateInitialised() {
        if (this.board == null) {
            this.board = new GreenhouseBoard();
        }

        if (this.storedBoostPlantNames == null) {
            this.storedBoostPlantNames =
                    new LinkedHashSet<>();
        }
    }

    private String normalise(String value) {
        return value == null
                ? ""
                : value.trim()
                .toLowerCase(Locale.ROOT);
    }
}