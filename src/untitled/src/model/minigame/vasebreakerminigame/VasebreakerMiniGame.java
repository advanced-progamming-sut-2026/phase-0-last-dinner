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

        System.out.println("Stage one : Go Go Go.");
    }

    public void breakVase(Position position) {
        Vase vase = findUnbrokenVase(position);

        if (vase == null) {
            System.out.println("There is no unbroken vase at " + position + ".");
            return;
        }

        vase.breakVase();
        System.out.println("Vase at " + position + " shekast.");

        if (vase.getContentType() == VaseContentType.EMPTY) {
            System.out.println("The vase was empty.");
            return;
        }

        if (vase.getContentType() == VaseContentType.SEED_PACKET) {
            dropSeedPacket(vase);
            return;
        }

        if (vase.getContentType() == VaseContentType.ZOMBIE) {
            releaseZombie(vase);
        }
    }

    public void collectSeedPacket(Position position) {
        DroppedSeedPacket seedPacket = findAvailableSeedPacket(position);

        if (seedPacket == null) {
            System.out.println("There is no available seed packet at " + position + ".");
            return;
        }

        seedPacket.collect();
        System.out.println("Seed packet collected at " + position + ".");
    }

    public void plantFromPacket(
            DroppedSeedPacket seedPacket,
            Position position
    ) {
        if (seedPacket == null) {
            System.out.println("No seed packet selected.");
            return;
        }

        if (!seedPacket.isAvailable(currentTick)) {
            System.out.println("This seed packet is not available.");
            return;
        }

        seedPacket.collect();

        System.out.println("Plant from seed packet was planted at " + position + ".");

        // TODO: Later, create the plant with PlantFactory and add it to the board.
    }

    @Override
    public void onTick() {
        currentTick++;
        removeExpiredSeedPackets();
    }

    @Override
    public boolean isWinConditionMet() {
        if (!areAllVasesBroken()) {
            return false;
        }

        // TODO: Later, also check no alive zombies on the board.
        markCompleted();
        return true;
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

    private void dropSeedPacket(Vase vase) {
        DroppedSeedPacket seedPacket = new DroppedSeedPacket(
                vase.getPlantDefinition(),
                vase.getPosition(),
                currentTick + SEED_PACKET_LIFETIME_TICKS
        );

        droppedSeedPackets.add(seedPacket);

        System.out.println("A seed packet dropped at " + vase.getPosition() + ".");
    }

    private void releaseZombie(Vase vase) {
        System.out.println("A zombie was released at " + vase.getPosition() + ".");

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

    public void markLost() {
        lost = true;
    }
}