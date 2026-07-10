package model.minigame.vasebreakerminigame;

import model.mechanism.Position;
import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;

public class PendingVasebreakerIntegration
        implements VasebreakerIntegration {

    @Override
    public boolean isReady() {
        /*
         * TODO: Return true in the real integration class
         * after Plant, Zombie, Board and GameEngine are ready.
         */

        return false;
    }

    @Override
    public void prepareStage(int stageNumber) {
        /*
         * TODO: Prepare or reset the real game Board
         * for the requested Vasebreaker stage.
         */
    }

    @Override
    public PlantDefinition choosePlantDefinition(
            int stageNumber
    ) {
        /*
         * TODO: Ask the Plant repository for a random plant
         * that is allowed in this Vasebreaker stage.
         */

        return null;
    }

    @Override
    public ZombieDefinition chooseRegularZombieDefinition(
            int stageNumber
    ) {
        /*
         * TODO: Ask the Zombie repository for a random
         * regular zombie for this stage.
         */

        return null;
    }

    @Override
    public ZombieDefinition chooseGargantuarDefinition(
            int stageNumber
    ) {
        /*
         * TODO: Ask the Zombie repository for the
         * Gargantuar definition used in this stage.
         */

        return null;
    }

    @Override
    public boolean releaseZombie(
            ZombieDefinition zombieDefinition,
            Position position
    ) {
        /*
         * TODO: Create the zombie with ZombieFactory
         * and add it to Board.
         */

        return false;
    }

    @Override
    public boolean plantFromSeedPacket(
            PlantDefinition plantDefinition,
            Position position
    ) {
        /*
         * TODO: Create the plant with PlantFactory
         * and add it to Board without consuming sun.
         */

        return false;
    }

    @Override
    public boolean isPlantingPositionOccupied(
            Position position
    ) {
        /*
         * TODO: Ask Board or PlantingSystem whether
         * planting is possible at this position.
         */

        return false;
    }

    @Override
    public boolean hasAliveVasebreakerZombies() {
        /*
         * TODO: Check the zombies that were released
         * from Vasebreaker vases.
         */

        return false;
    }

    @Override
    public void advanceOneTick() {
        /*
         * TODO: Advance the real GameEngine by one tick.
         */
    }

    @Override
    public boolean isBrainEaten() {
        /*
         * TODO: Read the lose condition from Board
         * or GameEngine.
         */

        return false;
    }
}