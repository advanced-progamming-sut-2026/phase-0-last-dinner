package model.minigame;

import model.mechanism.Position;

import java.util.List;

public class VasebreakerMiniGame extends MiniGame {
    private List<Vase> vases;
    private List<DroppedSeedPacket> droppedSeedPackets;
    private boolean plantSelectionEnabled;
    private boolean skySunEnabled;

    public VasebreakerMiniGame() {
        super(MiniGameType.VASEBREAKER);
    }

    public void breakVase(Position position) {
    }

    public void collectSeedPacket(Position position) {
    }

    public void plantFromPacket(
            DroppedSeedPacket seedPacket,
            Position position
    ) {
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
