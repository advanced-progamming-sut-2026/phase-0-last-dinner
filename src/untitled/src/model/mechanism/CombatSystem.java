package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import model.zombie.behavior.ProjectileReflectorBehavior;
import view.GameEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
@Setter
public class CombatSystem implements Tickable {
    private Board board;
    private GameEventListener listener;
    private Random random = new Random();

    public CombatSystem() {
    }

    public CombatSystem(Board board) {
        this.board = board;
    }

    @Override
    public void onTick() {
        if (this.board == null) {
            return;
        }

        this.resolveProjectiles();
        this.applyPoisonDamage();

        for (Zombie zombie : this.board.getAllZombies()) {
            if (zombie == null || zombie.isDead() || zombie.hasCondition(ZombieCondition.HYPNOTIZED)) {
                continue;
            }

            Plant target = this.getPlantInEatingRange(zombie);

            if (target != null) {
                zombie.attack(target);
            }
        }
    }

    public void attack() {
        this.onTick();
    }

    public void applyDamageToZombie(Zombie zombie, int damage) {
        if (zombie == null || damage <= 0) {
            return;
        }

        zombie.takeDamage(damage);

        if (zombie.isDead()) {
            this.fireZombieDiedEvent(zombie);
            this.removeZombieFromBoard(zombie);
        }
    }

    public void applyDamageToPlant(Plant plant, int damage) {
        if (plant == null || damage <= 0) {
            return;
        }

        plant.takeDamage(damage);

        if (plant.isDead()) {
            plant.activateUpgradeDeathEffects(this.board);
            this.firePlantDestroyedEvent(plant);
            this.removePlantFromBoard(plant);
        }
    }

    public void destroyPlant(Plant plant) {
        if (plant == null) {
            return;
        }

        this.applyDamageToPlant(plant, plant.getHealth());
    }

    public void killZombie(Zombie zombie) {
        if (zombie == null) {
            return;
        }

        zombie.die();
        this.fireZombieDiedEvent(zombie);
        this.removeZombieFromBoard(zombie);
    }

    public void applyRadioactiveSunExplosion(Position center) {
        if (this.board == null || center == null) {
            return;
        }

        for (Zombie zombie : this.board.getZombiesInRadius(center, 2)) {
            this.applyDamageToZombie(zombie, 150);
        }

        for (Plant plant : this.board.getPlantsInRadius(center, 1)) {
            this.applyDamageToPlant(plant, 80);
        }
    }

    private Plant getPlantInEatingRange(Zombie zombie) {
        if (zombie == null || zombie.getPosition() == null || this.board == null) {
            return null;
        }

        return this.board.getNearestPlantInZombieAttackRange(zombie.getPosition(), 1);
    }

    private void removeZombieFromBoard(Zombie zombie) {
        if (zombie == null || zombie.getBoard() == null) {
            return;
        }

        zombie.getBoard().removeZombie(zombie);
    }

    private void removePlantFromBoard(Plant plant) {
        if (plant == null || plant.getPosition() == null || this.board == null) {
            return;
        }

        this.board.removePlant(plant);
    }

    private void resolveProjectiles() {
        if (this.board == null || this.board.getProjectiles() == null || this.board.getProjectiles().isEmpty()) {
            return;
        }

        List<Projectile> projectiles = new ArrayList<>(this.board.getProjectiles());

        for (Projectile projectile : projectiles) {
            Zombie target = this.findProjectileTarget(projectile);

            if (target == null || target.isDead()) {
                continue;
            }

            ProjectileReflectorBehavior reflector = target.findBehavior(ProjectileReflectorBehavior.class);

            if (reflector != null && reflector.reflect(projectile, target, this.board)) {
                this.board.getProjectiles().remove(projectile);
                continue;
            }

            this.applyProjectileHit(projectile, target);

            if (projectile.shouldContinueAfterHit()) {
                projectile.setTarget(null);
            } else {
                this.board.getProjectiles().remove(projectile);
            }
        }
    }

    private void applyPoisonDamage() {
        if (this.board == null) {
            return;
        }

        for (Zombie zombie : new ArrayList<>(this.board.getAllZombies())) {
            if (zombie == null || zombie.isDead() || !zombie.hasCondition(ZombieCondition.POISONED)
                    || zombie.getPoisonDamagePerTick() <= 0) {
                continue;
            }

            this.applyDamageToZombie(zombie, zombie.getPoisonDamagePerTick());
        }
    }

    private void applyProjectileHit(Projectile projectile, Zombie directTarget) {
        if (projectile == null || directTarget == null || this.board == null) {
            return;
        }

        List<Zombie> targets = this.getProjectileAffectedTargets(projectile, directTarget);

        for (Zombie target : targets) {
            if (target == null || target.isDead() || !projectile.canHit(target)) {
                continue;
            }

            projectile.markHit(target);
            this.applyProjectileConditions(projectile, target);

            if (DamageExpressionParser.isInstantKill(projectile.getDamageExpression())) {
                this.killZombie(target);
            } else {
                int damage = DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
                this.applyDamageToZombie(target, damage);
            }

            this.tryAwardPlantFood(projectile, target);
        }
    }

    private List<Zombie> getProjectileAffectedTargets(Projectile projectile, Zombie directTarget) {
        List<Zombie> targets = new ArrayList<>();
        targets.add(directTarget);

        if (projectile.getSplashRadius() <= 0 || directTarget.getPosition() == null || this.board == null) {
            return targets;
        }

        for (Zombie zombie : this.board.getZombiesInRadius(directTarget.getPosition(), projectile.getSplashRadius())) {
            if (zombie != null && !targets.contains(zombie)) {
                targets.add(zombie);
            }
        }

        return targets;
    }

    private void applyProjectileConditions(Projectile projectile, Zombie zombie) {
        ZombieCondition condition = projectile.getConditionFromType();

        if (condition != null) {
            long durationTicks = projectile.getConditionDurationTicks() > 0
                    ? projectile.getConditionDurationTicks()
                    : 30;
            zombie.addCondition(condition, durationTicks);
        }

        if (projectile.getPoisonDamagePerTick() > 0) {
            zombie.addCondition(ZombieCondition.POISONED, Math.max(30, projectile.getConditionDurationTicks()));
            zombie.addPoisonDamagePerTick(projectile.getPoisonDamagePerTick());
        }

        if (projectile.getStunChancePercent() > 0
                && this.random.nextInt(100) < projectile.getStunChancePercent()) {
            zombie.addCondition(ZombieCondition.STUNNED, 20);
        }
    }

    private void tryAwardPlantFood(Projectile projectile, Zombie zombie) {
        if (projectile.getPlantFoodChancePercent() <= 0 || zombie == null || !zombie.isDead()
                || this.board == null || this.board.getPlantFoodSystem() == null) {
            return;
        }

        if (this.random.nextInt(100) < projectile.getPlantFoodChancePercent()) {
            this.board.getPlantFoodSystem().addPlantFood();
        }
    }

    private Zombie findProjectileTarget(Projectile projectile) {
        if (projectile == null || this.board == null) {
            return null;
        }

        if (projectile.getTarget() != null && !projectile.getTarget().isDead()
                && projectile.canHit(projectile.getTarget())
                && projectile.isInRangeOf(projectile.getTarget().getPosition())) {
            return projectile.getTarget();
        }

        if (projectile.getPosition() == null) {
            return this.board.getNearestZombie(null);
        }

        Zombie nearestTarget = null;
        int nearestDistance = Integer.MAX_VALUE;

        List<Zombie> candidateZombies = projectile.getBounceCount() > 0
                ? this.board.getAllZombies()
                : this.board.getZombiesInLane(projectile.getPosition());

        for (Zombie zombie : candidateZombies) {
            if (zombie == null || zombie.isDead() || zombie.getPosition() == null) {
                continue;
            }

            if (!projectile.canHit(zombie) || !projectile.isInRangeOf(zombie.getPosition())) {
                continue;
            }

            int deltaX = Math.abs(zombie.getPosition().getX() - projectile.getPosition().getX());

            if (projectile.getBounceCount() <= 0
                    && zombie.getPosition().getX() - projectile.getPosition().getX() < 0) {
                continue;
            }

            if (deltaX < nearestDistance) {
                nearestDistance = deltaX;
                nearestTarget = zombie;
            }
        }

        return nearestTarget;
    }

    private void fireZombieDiedEvent(Zombie zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            this.fireEvent("Zombie died.");
            return;
        }

        this.fireEvent("Zombie " + zombie.getDefinition().getDisplayName() + " died.");
    }

    private void firePlantDestroyedEvent(Plant plant) {
        if (plant == null) {
            return;
        }

        this.fireEvent("Plant " + plant.getName() + " was destroyed.");
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
