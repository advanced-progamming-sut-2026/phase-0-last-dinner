package model.mechanism;

import java.util.List;

public class SunSystem implements Tickable {
    private List<Sun> suns;
    private int sunAmount;
    private Board board;

    @Override
    public void onTick() {
    }

    public Sun spawnFallingSun() {
        return null;
    }

    public Sun addPlantSun(Position position) {
        return null;
    }

    public int collectSun(Position position) {
        return 0;
    }

    public void addSun(int amount) {
    }

    public int getSunAmount() {
        return 0;
    }
    public void cheatCode(){}
}
