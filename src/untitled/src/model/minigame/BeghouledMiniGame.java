package model.minigame;

import model.mechanism.Position;
import model.plant.PlantDefinition;

import java.util.List;
import java.util.Set;

public class BeghouledMiniGame extends MiniGame {
    private List<PlantDefinition> availablePlantTypes;
    private List<PlantUpgradeOption> upgradeOptions;
    private Set<Position> craters;
    private int sunAmount;
    private int targetMatchCount;
    private int completedMatchCount;
    private boolean endlessZombieWaves;

    public BeghouledMiniGame() {
        super(MiniGameType.BEGHOULED);
    }

    public boolean swap(Position first, Position second) {
        return false;
    }

    public List<PlantMatch> findMatches() {
        return null;
    }

    public void resolveMatches(List<PlantMatch> matches) {
    }

    public void refillBoard() {
    }

    public void resetBoard() {
    }

    public void createCrater(Position position) {
    }

    public void upgradePlants(PlantUpgradeOption upgradeOption) {
    }

    @Override
    public void start() {
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean isWinConditionMet() {
        return false;
    }

    @Override
    public boolean isLoseConditionMet() {
        return false;
    }
}
