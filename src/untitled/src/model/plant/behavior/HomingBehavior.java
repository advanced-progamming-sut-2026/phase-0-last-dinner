package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;

public class HomingBehavior implements PlantBehavior {
    private Projectile projectileTemplate;
    private long homingIntervalTicks;
    private long ticksSinceLastHoming;
    private HomingTargetMode targetMode;
    private String damageExpression;

    public HomingBehavior(
            Projectile projectileTemplate,
            long homingIntervalTicks,
            HomingTargetMode targetMode,
            String damageExpression
    ) {
        this.projectileTemplate = projectileTemplate;
        this.homingIntervalTicks = homingIntervalTicks;
        this.targetMode = targetMode;
        this.damageExpression = damageExpression;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        this.ticksSinceLastHoming++;

        if (this.ticksSinceLastHoming >= this.homingIntervalTicks) {
            this.activate(plant, board);
            this.ticksSinceLastHoming = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || board == null) {
            return;
        }

        if (this.targetMode == HomingTargetMode.ARMOR) {
            return;
        }

        if (this.targetMode == HomingTargetMode.HYPNOSIS && board.getCombatSystem() != null) {
            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                board.getCombatSystem().killZombie(board.getNearestZombie(plant.getPosition()));
            }
            return;
        }

        if (this.projectileTemplate == null) {
            return;
        }

        board.addProjectile(this.projectileTemplate.copyAtTarget(
                plant.getPosition(),
                board.getNearestZombie(plant.getPosition())
        ));
    }
}
