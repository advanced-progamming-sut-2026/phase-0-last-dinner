package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.Projectile;

public class LobberBehavior implements PlantBehavior {
    private Projectile projectileTemplate;
    private long lobIntervalTicks;
    private long ticksSinceLastLob;
    private boolean areaDamage;

    public LobberBehavior(Projectile projectileTemplate, long lobIntervalTicks, boolean areaDamage) {
        this.projectileTemplate = projectileTemplate;
        this.lobIntervalTicks = lobIntervalTicks;
        this.areaDamage = areaDamage;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        this.ticksSinceLastLob++;

        if (this.ticksSinceLastLob >= this.lobIntervalTicks) {
            this.activate(plant, board);
            this.ticksSinceLastLob = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || board == null || this.projectileTemplate == null) {
            return;
        }

        board.addProjectile(this.projectileTemplate.copyAt(plant.getPosition()));
    }
}
