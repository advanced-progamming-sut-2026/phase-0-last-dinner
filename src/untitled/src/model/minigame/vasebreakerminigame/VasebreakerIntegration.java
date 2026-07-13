package model.minigame.vasebreakerminigame;

import model.mechanism.Position;
import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;

public interface VasebreakerIntegration {
    /*
     * Returns true when the Plant, Zombie, Board and
     * GameEngine connections are ready to be used.
     */
    boolean isReady();

    /*
     * Called whenever a Vasebreaker stage starts.
     *
     * The external game systems should prepare or reset
     * the Board for the requested stage.
     */
    void prepareStage(int stageNumber);

    /*
     * Plant module responsibility:
     *
     * Return a random plant definition that can appear
     * inside a Vasebreaker seed-packet vase.
     */
    PlantDefinition choosePlantDefinition(
            int stageNumber
    );

    /*
     * Zombie module responsibility:
     *
     * Return a random regular zombie definition for
     * a normal vase.
     */
    ZombieDefinition chooseRegularZombieDefinition(
            int stageNumber
    );

    /*
     * Zombie module responsibility:
     *
     * Return the Gargantuar definition that must be used
     * inside a special Gargantuar vase.
     */
    ZombieDefinition chooseGargantuarDefinition(
            int stageNumber
    );

    /*
     * Zombie and Board responsibility:
     *
     * Create the requested zombie and place it on Board
     * at the specified position.
     *
     * Return true only if the zombie is actually placed.
     */
    boolean releaseZombie(
            ZombieDefinition zombieDefinition,
            Position position
    );

    /*
     * Plant and Board responsibility:
     *
     * Create the requested plant and place it on Board
     * at the specified position.
     *
     * This planting must not consume sun because the plant
     * comes from a one-use Vasebreaker seed packet.
     *
     * Return true only if planting succeeds.
     */
    boolean plantFromSeedPacket(
            PlantDefinition plantDefinition,
            Position position
    );

    /*
     * Board or planting-system responsibility:
     *
     * Return true if a plant cannot be placed at the
     * specified position because the tile is occupied.
     */
    boolean isPlantingPositionOccupied(
            Position position
    );

    /*
     * Zombie and Board responsibility:
     *
     * Return true if at least one zombie released from
     * a Vasebreaker vase is still alive.
     */
    boolean hasAliveVasebreakerZombies();

    /*
     * GameEngine responsibility:
     *
     * Advance plants, zombies, projectiles and combat
     * by exactly one game tick.
     */
    void advanceOneTick();

    /*
     * GameEngine or Board responsibility:
     *
     * Return true when a zombie has reached and eaten
     * the player's brain.
     */
    boolean isBrainEaten();
}