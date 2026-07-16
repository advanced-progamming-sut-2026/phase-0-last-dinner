package model.minigame;

import model.minigame.izombieminigame.IZombieMiniGame;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.minigame.wallnutbowlingminigame.WallnutBowlingMiniGame;

public class MiniGameFactory {

    public MiniGame create(MiniGameType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case VASEBREAKER ->
                    new VasebreakerMiniGame();

            case WALLNUT_BOWLING ->
                    new WallnutBowlingMiniGame();

            case I_ZOMBIE ->
                    new IZombieMiniGame();

            case BEGHOULED ->
                    new BeghouledMiniGame();

            case ZOMBOTANY ->
                    new ZombotanyMiniGame();
        };
    }
}