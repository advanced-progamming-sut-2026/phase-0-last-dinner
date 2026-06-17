package model.minigame;

public class MiniGameFactory {
    public MiniGame create(MiniGameType type) {
        switch (type) {
            case VASEBREAKER:
                return new VasebreakerMiniGame();
            case I_ZOMBIE:
                return new IZombieMiniGame();
            case BEGHOULED:
                return new BeghouledMiniGame();
            case ZOMBOTANY:
                return new ZombotanyMiniGame();
            case WALLNUT_BOWLING:
                return new WallnutBowlingMiniGame();
            default:
                return null;
        }
    }
}
