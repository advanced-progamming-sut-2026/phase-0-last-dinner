package model.minigame.izombieminigame;

import model.mechanism.Position;
import model.zombie.ZombieDefinition;

import java.util.Collections;
import java.util.List;

public class PendingIZombieIntegration
        implements IZombieIntegration {

    @Override
    public boolean isReady() {
        /*
         * TODO: Return true after Plant, Zombie,
         * Board, CombatSystem and GameEngine
         * are connected.
         */

        return false;
    }

    @Override
    public void prepareStage(int stageNumber) {
        /*
         * TODO: Clear the previous Board and place
         * random defensive plants on the left side.
         */
    }

    @Override
    public List<ZombieDefinition> chooseAvailableZombies(
            int stageNumber
    ) {
        /*
         * TODO: Return exactly five selectable zombie
         * definitions for this stage.
         *
         * The three stages must collectively contain
         * at least ten different zombie types.
         */

        return Collections.emptyList();
    }

    @Override
    public int getZombieSunCost(
            ZombieDefinition zombieDefinition,
            int stageNumber
    ) {
        /*
         * TODO: Return the configured sun cost of
         * this zombie in the requested stage.
         */

        return Integer.MAX_VALUE;
    }

    @Override
    public void spawnInitialSunProducerZombies(
            int stageNumber,
            IZombieMiniGame miniGame
    ) {
        /*
         * TODO: Create one special sun-producing zombie
         * in every row.
         *
         * Attach IZombieSunProducerBehavior to these
         * zombies together with their normal behaviour.
         */
    }

    @Override
    public boolean isZombiePlacementBlocked(
            Position position
    ) {
        /*
         * TODO: Ask Board whether a zombie can be
         * placed at this position.
         */

        return false;
    }

    @Override
    public boolean placeZombie(
            ZombieDefinition zombieDefinition,
            Position position
    ) {
        /*
         * TODO: Create the zombie using ZombieFactory
         * and add it to Board.
         */

        return false;
    }

    @Override
    public void advanceOneTick() {
        /*
         * TODO: Advance plants, zombies, projectiles
         * and combat by exactly one game tick.
         */
    }

    @Override
    public boolean hasAlivePlayerZombies() {
        /*
         * TODO: Return true when at least one zombie
         * belonging to the player is alive.
         */

        return false;
    }

    @Override
    public boolean isBrainEaten(int row) {
        /*
         * TODO: Read the brain state of this row
         * from Board or GameEngine.
         */

        return false;
    }
}