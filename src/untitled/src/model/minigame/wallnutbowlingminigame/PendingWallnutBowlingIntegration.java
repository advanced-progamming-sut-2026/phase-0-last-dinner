package model.minigame.wallnutbowlingminigame;

import model.mechanism.Position;

public class PendingWallnutBowlingIntegration
        implements WallnutBowlingIntegration {

    @Override
    public boolean isReady() {
        /*
         * TODO: Return true after Board, Zombie,
         * WaveManager and GameEngine are connected.
         */

        return false;
    }

    @Override
    public void prepareStage(int stageNumber) {
        /*
         * TODO: Prepare or reset the real Board
         * for this Wallnut Bowling stage.
         */
    }

    @Override
    public void startZombieWaves(int stageNumber) {
        /*
         * TODO: Configure and start the zombie waves
         * belonging to this stage.
         */
    }

    @Override
    public int getNormalZombieHealth() {
        /*
         * TODO: Read the normal zombie's health
         * from ZombieDefinitionRepository.
         */

        return 0;
    }

    @Override
    public int getCherryBombDamage() {
        /*
         * TODO: Read Cherry Bomb's damage from
         * PlantDefinitionRepository.
         */

        return 0;
    }

    @Override
    public boolean hasZombieAt(Position position) {
        /*
         * TODO: Ask Board whether an alive zombie
         * currently collides with this position.
         */

        return false;
    }

    @Override
    public void damageFirstZombieAt(
            Position position,
            int damage
    ) {
        /*
         * TODO: Find the first zombie at this position
         * and apply the requested damage through
         * CombatSystem.
         */
    }

    @Override
    public void crushZombiesAt(
            Position position
    ) {
        /*
         * TODO: Instantly destroy the zombies colliding
         * with the Giant Wallnut at this position.
         */
    }

    @Override
    public void explodeAt(
            Position centre,
            int radius,
            int damage
    ) {
        /*
         * TODO: Apply the requested explosion damage
         * to zombies in the specified area.
         */
    }

    @Override
    public void advanceOneTick() {
        /*
         * TODO: Advance WaveManager, zombies and other
         * external systems by exactly one game tick.
         */
    }

    @Override
    public boolean areAllWavesFinished() {
        /*
         * TODO: Read the wave-completion state
         * from WaveManager.
         */

        return false;
    }

    @Override
    public boolean hasAliveZombies() {
        /*
         * TODO: Ask Board whether at least one enemy
         * zombie is still alive.
         */

        return false;
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