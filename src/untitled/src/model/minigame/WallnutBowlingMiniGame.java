package model.minigame;

import model.mechanism.Position;

import java.util.List;

public class WallnutBowlingMiniGame extends MiniGame {
    private List<BowlingWallnutType> conveyorBelt;
    private List<RollingWallnut> rollingWallnuts;
    private double plantingBoundaryX;
    private long generationIntervalTicks;
    private boolean skySunEnabled;

    public WallnutBowlingMiniGame() {
        super(MiniGameType.WALLNUT_BOWLING);
    }

    public BowlingWallnutType generateWallnut() {
        return null;
    }

    public RollingWallnut placeWallnut(
            BowlingWallnutType type,
            Position position
    ) {
        return null;
    }

    public boolean canPlaceAt(Position position) {
        return false;
    }

    @Override
    public void start() {
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
