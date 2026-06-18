package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

import java.util.List;

public class BossBehavior implements ZombieBehavior {
    private List<BossStage> stages;
    private int currentStageIndex;

    public BossBehavior(List<BossStage> stages) {
        this.stages = stages;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.activate(zombie, board);
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }

    public BossStage getCurrentStage() {
        if (this.stages == null || this.stages.isEmpty()
                || this.currentStageIndex < 0 || this.currentStageIndex >= this.stages.size()) {
            return null;
        }

        return this.stages.get(this.currentStageIndex);
    }

    public void advanceStage() {
        if (this.stages == null) {
            return;
        }

        if (this.currentStageIndex < this.stages.size() - 1) {
            this.currentStageIndex++;
        }
    }
}
