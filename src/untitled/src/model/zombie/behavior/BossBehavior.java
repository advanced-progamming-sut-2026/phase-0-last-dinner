package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

import java.util.List;

public class BossBehavior implements ZombieBehavior {
    private List<BossStage> stages;
    private int currentStageIndex;

    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }

    public BossStage getCurrentStage() {
        return null;
    }

    public void advanceStage() {
    }
}
