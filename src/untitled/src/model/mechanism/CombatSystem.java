package model.mechanism;

import model.Plant;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import model.zombie.behavior.ZombieBehavior;
import view.GameEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CombatSystem implements Tickable {
    private Board board;
    private GameEventListener listener;
    private Random random;

    public CombatSystem() {
        this(null);
    }

    public CombatSystem(Board board) {
        this.board = board;
        this.random = new Random();

        if (board != null) {
            board.setCombatSystem(this);
        }
    }

    @Override
    public void onTick() {
        if (this.board == null) {
            return;
        }

        this.resolveProjectiles();
        this.applyPoisonDamage();
        this.removeDeadZombies();
    }

    public void attack() {
        this.onTick();
    }

    public void attack(Zombie zombie) {
        if (zombie == null || zombie.isDead() || zombie.getPosition() == null || this.board == null) {
            return;
        }

        Plant target = this.board.getNearestPlantInZombieAttackRange(zombie.getPosition(), 1);

        if (target != null) {
            zombie.attack(target);
        }
    }

    public void applyDamageToZombie(Zombie zombie, int damage) {
        if (zombie == null || damage <= 0 || zombie.isDead()) {
            return;
        }

        zombie.takeDamage(damage);
        this.finishZombieDeath(zombie);
    }

    public void applyDirectDamageToZombie(Zombie zombie, int damage) {
        if (zombie == null || damage <= 0 || zombie.isDead()) {
            return;
        }

        zombie.takeDirectDamage(damage);
        this.finishZombieDeath(zombie);
    }

    public void applyDamageToPlant(Plant plant, int damage) {
        if (plant == null || damage <= 0 || plant.isDead()) {
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
        if (plant != null) {
            this.applyDamageToPlant(plant, Math.max(1, plant.getHealth()));
        }
    }

    public void killZombie(Zombie zombie) {
        if (zombie == null) {
            return;
        }

        if (!zombie.isDead()) {
            zombie.die();
        }

        this.finishZombieDeath(zombie);
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

    public Board getBoard() {
        return this.board;
    }

    public void setBoard(Board board) {
        this.board = board;

        if (board != null && board.getCombatSystem() != this) {
            board.setCombatSystem(this);
        }
    }

    public GameEventListener getListener() {
        return this.listener;
    }

    public void setListener(GameEventListener listener) {
        this.listener = listener;
    }

    public void setRandom(Random random) {
        this.random = random == null ? new Random() : random;
    }

    private void resolveProjectiles() {
        Iterator<Projectile> iterator = this.board.getProjectiles().iterator();

        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();

            if (projectile == null) {
                iterator.remove();
                continue;
            }

            projectile.move();

            if (projectile.getType() == ProjectileType.FIRE && projectile.getPosition() != null) {
                Tile tile = this.board.getTile(projectile.getPosition());

                if (tile != null && tile.getTerrainType() == TerrainType.FROZEN) {
                    this.board.setTerrain(projectile.getPosition(), TerrainType.CLASSIC);
                }
            }

            if (projectile.isExpired()) {
                iterator.remove();
                continue;
            }

            Zombie target = this.findCollidingZombie(projectile);

            if (target == null) {
                continue;
            }

            ZombieBehavior behavior = target.getBehavior();

            if (behavior != null && behavior.onProjectileHit(target, projectile, this.board)) {
                iterator.remove();
                continue;
            }

            if (behavior != null && !behavior.canBeHitBy(target, projectile)) {
                continue;
            }

            this.applyProjectileHit(projectile, target);

            if (projectile.shouldContinueAfterHit()) {
                projectile.setTarget(
                        projectile.getBounceCount() > 0
                                ? this.findNearestUnhitZombie(projectile)
                                : null
                );
            } else {
                iterator.remove();
            }
        }
    }

    private void applyPoisonDamage() {
        for (Zombie zombie : new ArrayList<>(this.board.getAllZombies())) {
            if (zombie == null || zombie.isDead() || !zombie.hasCondition(ZombieCondition.POISONED)
                    || zombie.getPoisonDamagePerTick() <= 0) {
                continue;
            }

            this.applyDirectDamageToZombie(zombie, zombie.getPoisonDamagePerTick());
        }
    }

    private void removeDeadZombies() {
        for (Zombie zombie : new ArrayList<>(this.board.getAllZombies())) {
            this.finishZombieDeath(zombie);
        }
    }

    private void applyProjectileHit(Projectile projectile, Zombie directTarget) {
        for (Zombie target : this.getProjectileAffectedTargets(projectile, directTarget)) {
            if (!this.canProjectileHit(projectile, target)) {
                continue;
            }

            projectile.markHit(target);
            this.applyProjectileConditions(projectile, target);

            if (projectile.getType() == ProjectileType.FIRE) {
                target.removeCondition(ZombieCondition.CHILLED);
                target.removeCondition(ZombieCondition.FROZEN);
            }

            if (DamageExpressionParser.isInstantKill(projectile.getDamageExpression())) {
                this.killZombie(target);
            } else {
                int damage = DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());

                if (projectile.getType() == ProjectileType.POISON) {
                    this.applyDirectDamageToZombie(target, damage);
                } else {
                    this.applyDamageToZombie(target, damage);
                }
            }

            this.tryAwardPlantFood(projectile, target);
        }
    }

    private List<Zombie> getProjectileAffectedTargets(Projectile projectile, Zombie directTarget) {
        List<Zombie> targets = new ArrayList<>();
        targets.add(directTarget);

        if (projectile.getSplashRadius() <= 0 || directTarget.getPosition() == null) {
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
            zombie.addCondition(condition, durationTicks, projectile);
        }

        if (projectile.getPoisonDamagePerTick() > 0) {
            zombie.addCondition(
                    ZombieCondition.POISONED,
                    Math.max(30, projectile.getConditionDurationTicks()),
                    projectile
            );

            if (zombie.hasCondition(ZombieCondition.POISONED)) {
                zombie.addPoisonDamagePerTick(projectile.getPoisonDamagePerTick());
            }
        }

        if (projectile.getStunChancePercent() > 0
                && this.random.nextInt(100) < projectile.getStunChancePercent()) {
            zombie.addCondition(ZombieCondition.STUNNED, 20, projectile);
        }
    }

    private Zombie findCollidingZombie(Projectile projectile) {
        if (projectile.getPosition() == null) {
            return null;
        }

        List<Zombie> candidates = projectile.getTarget() == null
                ? this.board.getZombiesInLane(projectile.getPosition())
                : this.board.getAllZombies();
        Zombie nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Zombie zombie : candidates) {
            if (!this.canPhysicallyCollide(projectile, zombie) || zombie.getPosition() == null) {
                continue;
            }

            double deltaX = zombie.getExactX() - projectile.getExactX();
            double deltaY = zombie.getPosition().getY() - projectile.getExactY();
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (distance <= Math.max(0.55, projectile.getSpeed() * 0.6) && distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private Zombie findNearestUnhitZombie(Projectile projectile) {
        Zombie nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Zombie zombie : this.board.getAllZombies()) {
            if (!this.canProjectileHit(projectile, zombie) || zombie.getPosition() == null) {
                continue;
            }

            double deltaX = zombie.getExactX() - projectile.getExactX();
            double deltaY = zombie.getPosition().getY() - projectile.getExactY();
            double distance = deltaX * deltaX + deltaY * deltaY;

            if (distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private boolean canProjectileHit(Projectile projectile, Zombie zombie) {
        if (!this.canPhysicallyCollide(projectile, zombie)) {
            return false;
        }

        ZombieBehavior behavior = zombie.getBehavior();
        return behavior == null || behavior.canBeHitBy(zombie, projectile);
    }

    private boolean canPhysicallyCollide(Projectile projectile, Zombie zombie) {
        return projectile != null && zombie != null && !zombie.isDead() && projectile.canHit(zombie);
    }

    private void tryAwardPlantFood(Projectile projectile, Zombie zombie) {
        if (projectile.getPlantFoodChancePercent() <= 0 || zombie == null || !zombie.isDead()
                || this.board.getPlantFoodSystem() == null) {
            return;
        }

        if (zombie.isGlowing()
                || zombie.getDefinition() != null && zombie.getDefinition().isCanSpawnPlantFood()) {
            return;
        }

        if (this.random.nextInt(100) < projectile.getPlantFoodChancePercent()) {
            this.board.getPlantFoodSystem().addPlantFood();
        }
    }

    private void finishZombieDeath(Zombie zombie) {
        if (zombie == null || !zombie.isDead() || !zombie.markDeathProcessed()) {
            return;
        }

        if (this.board.getPlantFoodSystem() != null
                && (zombie.isGlowing()
                || zombie.getDefinition() != null && zombie.getDefinition().isCanSpawnPlantFood())) {
            this.board.getPlantFoodSystem().addPlantFood();
        }

        this.fireZombieDiedEvent(zombie);
        this.removeZombieFromBoard(zombie);
    }

    private void removeZombieFromBoard(Zombie zombie) {
        Board zombieBoard = zombie == null ? null : zombie.getBoard();

        if (zombieBoard != null) {
            zombieBoard.removeZombie(zombie);
        }
    }

    private void removePlantFromBoard(Plant plant) {
        if (plant != null && plant.getPosition() != null && this.board != null) {
            this.board.removePlant(plant);
        }
    }

    private void fireZombieDiedEvent(Zombie zombie) {
        String name = zombie == null || zombie.getDefinition() == null
                ? "Zombie"
                : "Zombie " + zombie.getDefinition().getDisplayName();
        this.fireEvent(name + " died.");
    }

    private void firePlantDestroyedEvent(Plant plant) {
        if (plant != null) {
            this.fireEvent("Plant " + plant.getName() + " was destroyed.");
        }
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
