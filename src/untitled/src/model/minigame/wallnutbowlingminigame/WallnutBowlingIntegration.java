package model.minigame.wallnutbowlingminigame;

import model.mechanism.Position;

public interface WallnutBowlingIntegration {

    boolean isReady();

    void prepareStage(int stageNumber);

    void startZombieWaves(int stageNumber);

    int getNormalZombieHealth();

    int getCherryBombDamage();

    boolean hasZombieAt(Position position);

    void damageFirstZombieAt(
            Position position,
            int damage
    );

    void crushZombiesAt(Position position);

    void explodeAt(
            Position centre,
            int radius,
            int damage
    );

    void advanceOneTick();

    boolean areAllWavesFinished();

    boolean hasAliveZombies();

    boolean isBrainEaten();
}