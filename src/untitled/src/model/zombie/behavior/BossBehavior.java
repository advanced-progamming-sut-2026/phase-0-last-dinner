package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.TerrainType;
import model.zombie.Zombie;

import java.util.List;

public class BossBehavior implements ZombieBehavior {
    private List<BossStage> stages;
    private int currentStageIndex;
    private long ticksSinceLastAction;
    private long actionIntervalTicks = 50;

    public BossBehavior(List<BossStage> stages) {
        this.stages = stages;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.ticksSinceLastAction++;

        if (this.ticksSinceLastAction >= this.actionIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceLastAction = 0;
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (zombie == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        Plant target = board.getNearestPlant(zombie.getPosition());

        if (target != null) {
            board.getCombatSystem().applyDamageToPlant(target, 250);
        }

        board.placeTerrainNear(zombie.getPosition(), TerrainType.GRAVE, 1);
        this.advanceStage();
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
