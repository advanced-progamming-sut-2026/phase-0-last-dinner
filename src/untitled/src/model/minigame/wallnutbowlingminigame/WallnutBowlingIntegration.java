package model.minigame.wallnutbowlingminigame;

import model.mechanism.Position;

public interface WallnutBowlingIntegration {
    /*
     * Returns true after Board, Zombie, WaveManager
     * and GameEngine have been connected.
     */
    boolean isReady();

    /*
     * Prepare or reset the Board for the requested stage.
     */
    void prepareStage(int stageNumber);

    /*
     * Start the zombie waves configured for this stage.
     */
    void startZombieWaves(int stageNumber);

    /*
     * Wallnut Bowling's normal wallnut must deal damage
     * equal to the health of a normal zombie.
     */
    int getNormalZombieHealth();

    /*
     * Explode-O-Nut must deal the same damage as
     * Cherry Bomb.
     */
    int getCherryBombDamage();

    /*
     * Return true if at least one alive zombie currently
     * collides with this position.
     */
    boolean hasZombieAt(Position position);

    /*
     * Damage the first zombie found at this position.
     */
    void damageFirstZombieAt(
            Position position,
            int damage
    );

    /*
     * Giant Wallnut crushes the zombies at this position
     * and continues moving.
     */
    void crushZombiesAt(Position position);

    /*
     * Explode-O-Nut damages zombies in a 3x3 area.
     *
     * A radius of 1 represents the required 3x3 area.
     */
    void explodeAt(
            Position centre,
            int radius,
            int damage
    );

    /*
     * Advance zombies, waves and other external systems
     * by exactly one tick.
     */
    void advanceOneTick();

    /*
     * Return true when every zombie wave has been generated.
     */
    boolean areAllWavesFinished();

    /*
     * Return true if at least one enemy zombie is alive.
     */
    boolean hasAliveZombies();

    /*
     * Return true if a zombie has reached the house
     * and eaten the player's brain.
     */
    boolean isBrainEaten();
}