package model.minigame.izombieminigame;

import model.mechanism.Board;
import model.zombie.Zombie;

public class IZombieBoard extends Board {

    private static final int ROW_COUNT = 5;

    private final boolean[] eatenBrains;

    public IZombieBoard() {
        super();

        eatenBrains = new boolean[ROW_COUNT];

        getLawnMowers().clear();
    }

    @Override
    public boolean handleZombieAtHouse(Zombie zombie) {
        if (zombie == null
                || zombie.isDead()
                || zombie.getPosition() == null) {

            return false;
        }

        int row = zombie.getPosition().getY();

        if (row < 0 || row >= ROW_COUNT) {
            return false;
        }

        eatenBrains[row] = true;

        zombie.die();
        removeZombie(zombie);

        return true;
    }

    @Override
    public boolean isBrainEaten() {
        return false;
    }

    public boolean isBrainEatenInRow(int userRow) {
        int boardRow = userRow - 1;

        if (boardRow < 0 || boardRow >= ROW_COUNT) {
            return false;
        }

        return eatenBrains[boardRow];
    }

    public int getEatenBrainCount() {
        int count = 0;

        for (boolean eaten : eatenBrains) {
            if (eaten) {
                count++;
            }
        }

        return count;
    }

    public boolean areAllBrainsEaten() {
        return getEatenBrainCount() == ROW_COUNT;
    }
}