package model.minigame.vasebreakerminigame;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.Position;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class VasebreakerMiniGame extends MiniGame {
    private static final long SEED_PACKET_LIFETIME_TICKS = 300;

    private List<Vase> vases;
    private List<DroppedSeedPacket> droppedSeedPackets;
    private boolean plantSelectionEnabled;
    private boolean skySunEnabled;
    private boolean lost;
    private long currentTick;

    public VasebreakerMiniGame() {
        super(MiniGameType.VASEBREAKER);
        this.vases = new ArrayList<>();
        this.droppedSeedPackets = new ArrayList<>();
        this.plantSelectionEnabled = false;
        this.skySunEnabled = false;
        this.lost = false;
        this.currentTick = 0;
    }

    @Override
    public void start() {
        markStarted();

        vases.clear();
        droppedSeedPackets.clear();

        plantSelectionEnabled = false;
        skySunEnabled = false;
        lost = false;
        currentTick = 0;

        VaseBreakerStageGenerator generator = new VaseBreakerStageGenerator();
        vases.addAll(generator.generateStageOne());
    }

    public VasebreakerActionResult breakVase(Position position) {
        Vase vase = findUnbrokenVase(position);

        if (vase == null) {
            return VasebreakerActionResult.noVase(position);
        }

        vase.breakVase();

        DroppedSeedPacket droppedSeedPacket = null;
        boolean zombieReleased = false;

        if (vase.getContentType() == VaseContentType.SEED_PACKET) {
            droppedSeedPacket = dropSeedPacket(vase);
        } else if (vase.getContentType() == VaseContentType.ZOMBIE) {
            releaseZombie(vase);
            zombieReleased = true;
        }

        updateCompletedIfWon();

        return VasebreakerActionResult.vaseBroken(
                position,
                vase.getContentType(),
                droppedSeedPacket,
                zombieReleased,
                isCompleted(),
                isLoseConditionMet()
        );
    }

    public VasebreakerActionResult collectSeedPacket(Position position) {
        DroppedSeedPacket seedPacket = findAvailableSeedPacket(position);

        if (seedPacket == null) {
            return VasebreakerActionResult.noSeedPacket(position);
        }

        seedPacket.collect();
        updateCompletedIfWon();

        return VasebreakerActionResult.seedPacketCollected(
                position,
                isCompleted(),
                isLoseConditionMet()
        );
    }

    public VasebreakerActionResult plantFromPacket(
            DroppedSeedPacket seedPacket,
            Position position
    ) {
        if (seedPacket == null) {
            return VasebreakerActionResult.invalidAction(position);
        }

        if (!seedPacket.isAvailable(currentTick)) {
            return VasebreakerActionResult.seedPacketNotAvailable(position);
        }

        seedPacket.collect();

        // TODO: Later, create the plant with PlantFactory and add it to the board.

        updateCompletedIfWon();

        return VasebreakerActionResult.plantFromPacket(
                position,
                isCompleted(),
                isLoseConditionMet()
        );
    }

    @Override
    public void onTick() {
        currentTick++;
        removeExpiredSeedPackets();
        updateCompletedIfWon();
    }

    @Override
    public boolean isWinConditionMet() {
        return areAllVasesBroken();
        // TODO: Later, also check no alive zombies on the board.
    }

    @Override
    public boolean isLoseConditionMet() {
        return lost;
    }

    private Vase findUnbrokenVase(Position position) {
        for (Vase vase : vases) {
            if (!vase.isBroken() && vase.isAt(position)) {
                return vase;
            }
        }
        return null;
    }

    private DroppedSeedPacket findAvailableSeedPacket(Position position) {
        for (DroppedSeedPacket seedPacket : droppedSeedPackets) {
            if (seedPacket.isAt(position) && seedPacket.isAvailable(currentTick)) {
                return seedPacket;
            }
        }
        return null;
    }

    private DroppedSeedPacket dropSeedPacket(Vase vase) {
        DroppedSeedPacket seedPacket = new DroppedSeedPacket(
                vase.getPlantDefinition(),
                vase.getPosition(),
                currentTick + SEED_PACKET_LIFETIME_TICKS
        );

        droppedSeedPackets.add(seedPacket);
        return seedPacket;
    }

    private void releaseZombie(Vase vase) {
        // TODO: Later, create the zombie with ZombieFactory and add it to the board.
    }

    private boolean areAllVasesBroken() {
        for (Vase vase : vases) {
            if (!vase.isBroken()) {
                return false;
            }
        }
        return true;
    }

    private void removeExpiredSeedPackets() {
        droppedSeedPackets.removeIf(seedPacket -> seedPacket.isExpired(currentTick));
    }

    private void updateCompletedIfWon() {
        if (isWinConditionMet()) {
            markCompleted();
        }
    }

    public void markLost() {
        lost = true;
    }

    public VasebreakerStateResult getState() {
        return new VasebreakerStateResult(
                currentTick,
                vases,
                droppedSeedPackets,
                isCompleted(),
                isLoseConditionMet()
        );
    }
}