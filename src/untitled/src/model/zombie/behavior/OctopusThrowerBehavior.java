package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.zombie.Zombie;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public class OctopusThrowerBehavior implements ZombieBehavior {
    private static final int OCTOPUS_HEALTH = 300;

    private long throwIntervalTicks;
    private long ticksSinceThrow;
    private Map<Plant, Integer> coveredPlants = new IdentityHashMap<>();

    public OctopusThrowerBehavior(long throwIntervalTicks) {
        this.throwIntervalTicks = Math.max(1, throwIntervalTicks);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.blockProjectiles(board);
        this.ticksSinceThrow++;
        if (this.ticksSinceThrow >= this.throwIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceThrow = 0;
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        Plant target = this.findTarget(zombie, board);
        if (target != null) {
            target.disable();
            this.coveredPlants.put(target, OCTOPUS_HEALTH);
        }
    }

    public void removeOctopus(Plant plant) {
        if (plant != null && this.coveredPlants.remove(plant) != null) {
            plant.enable();
        }
    }

    private void blockProjectiles(Board board) {
        if (board == null) {
            return;
        }

        Iterator<Map.Entry<Plant, Integer>> coveredIterator = this.coveredPlants.entrySet().iterator();

        while (coveredIterator.hasNext()) {
            Map.Entry<Plant, Integer> covered = coveredIterator.next();
            Plant plant = covered.getKey();

            if (plant == null || plant.isDead() || plant.getPosition() == null) {
                coveredIterator.remove();
                continue;
            }

            for (Projectile projectile : board.getProjectiles()) {
                if (projectile != null && !projectile.isLobbed()
                        && projectile.getPosition() != null
                        && projectile.getPosition().getY() == plant.getPosition().getY()
                        && Math.abs(projectile.getExactX() - plant.getPosition().getX()) <= 0.5) {
                    projectile.expire();
                    int damage = DamageExpressionParser.isInstantKill(projectile.getDamageExpression())
                            ? covered.getValue()
                            : DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
                    int health = covered.getValue() - Math.max(0, damage);

                    if (health <= 0) {
                        plant.enable();
                        coveredIterator.remove();
                        break;
                    }

                    covered.setValue(health);
                }
            }
        }
    }

    private Plant findTarget(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return null;
        }

        Plant nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (Plant plant : board.getPlantsInLane(zombie.getPosition())) {
            if (plant == null || plant.isDead() || plant.isDisabled() || plant.getPosition() == null) {
                continue;
            }

            int distance = zombie.getPosition().getX() - plant.getPosition().getX();
            if (distance >= 0 && distance < nearestDistance) {
                nearest = plant;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
