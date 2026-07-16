package model.minigame;

import model.minigame.izombieminigame.IZombieMiniGame;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.minigame.wallnutbowlingminigame.WallnutBowlingMiniGame;

public class MiniGameFactory {

    public MiniGame create(MiniGameType type) {
        if (type == null) {
            return null;
        }

        switch (type) {
            case VASEBREAKER:
                return new VasebreakerMiniGame();
            case WALLNUT_BOWLING:
                return new WallnutBowlingMiniGame();
            case I_ZOMBIE:
                return new IZombieMiniGame();
            case BEGHOULED:
                return new BeghouledMiniGame();
            case ZOMBOTANY:
                return new ZombotanyMiniGame();
            default:
                return null;
        }
    }
}
