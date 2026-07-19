package model.mechanism;

import lombok.Getter;
import lombok.Setter;
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
@Getter
@Setter
public class CombatSystem implements Tickable {
    private Board board;
    private GameEventListener listener;
    private Random random;
    private LootSystem lootSystem;

    // این سه تارو برای محاسبه امتیاز میوپوینت اضافه میکنم
    private GameClock gameClock;
    private ZombieKillObserver killObserver;
    private Projectile currentKillProjectile;

    public CombatSystem() {
        this(null, new LootSystem());
    }

    public CombatSystem(Board board) {
        this(board, new LootSystem());
    }

    public CombatSystem(Board board, LootSystem lootSystem) {
        this.board = board;
        this.random = new Random();
        this.lootSystem = lootSystem == null ? new LootSystem() : lootSystem;

        if (board != null) {
            board.setCombatSystem(this);
        }
    }

    public CombatSystem(Board board, GameEngine gameEngine, LootSystem lootSystem, PlantFoodSystem plantFoodSystem) {
        this(board, lootSystem);

        if (board != null && plantFoodSystem != null) {
            board.setPlantFoodSystem(plantFoodSystem);
        }
    }

    @Override
    public void onTick() {
        if (this.board == null) {
            return;
        }

        this.updateEnvironment();
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
        if (!this.canTakePlantDamage(zombie, damage) || this.isSubmerged(zombie)) {
            return;
        }

        zombie.takeDamage(damage);
        this.finishZombieDeath(zombie);
    }

    public void applyDirectDamageToZombie(Zombie zombie, int damage) {
        if (!this.canTakePlantDamage(zombie, damage) || this.isSubmerged(zombie)) {
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
        if (zombie == null || zombie.isHypnotized() || this.isSubmerged(zombie)) {
            return;
        }

        this.killZombieIgnoringAllegiance(zombie);
    }

    // baraye marg mostaghim ke ally ya submerged boodan nabayad jelosh ro begire
    public void killZombieIgnoringAllegiance(Zombie zombie) {
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

    public LootSystem getLootSystem() {
        return this.lootSystem;
    }

    public void setLootSystem(LootSystem lootSystem) {
        this.lootSystem = lootSystem == null ? new LootSystem() : lootSystem;
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

            if (projectile.isExpired()) {
                iterator.remove();
                continue;
            }

            Tile tile = this.board.getTile(projectile.getPosition());

            if (tile != null && tile.intercept(projectile)) {
                iterator.remove();
                continue;
            }

            if (this.board.getPlantCoverSystem().intercept(projectile)) {
                iterator.remove();
                continue;
            }

            if (projectile.isHostileToPlants()) {
                Plant plant = this.findCollidingPlant(projectile);

                if (plant != null) {
                    this.applyReflectedProjectileHit(projectile, plant);
                    iterator.remove();
                }
                continue;
            }

            Zombie target = this.findCollidingZombie(projectile);

            if (target == null) {
                continue;
            }

            ZombieBehavior behavior = target.getBehavior();

            if (behavior != null && behavior.onProjectileHit(target, projectile, this.board)) {
                if (!projectile.isHostileToPlants()) {
                    iterator.remove();
                }
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
        for (Zombie zombie : this.board.getAllZombies()) {
            if (zombie == null || zombie.isDead() || !zombie.hasCondition(ZombieCondition.POISONED)
                    || zombie.getPoisonDamagePerTick() <= 0) {
                continue;
            }

            this.applyDirectDamageFromProjectile(zombie, zombie.getPoisonDamagePerTick());
        }
    }

    private void removeDeadZombies() {
        for (Zombie zombie : this.board.getAllZombies()) {
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
                this.killZombieIgnoringAllegiance(target);
            } else {
                int damage = DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());

                if (projectile.getType() == ProjectileType.POISON) {
                    this.applyDirectDamageFromProjectile(target, damage);
                } else {
                    this.applyDamageFromProjectile(target, damage);
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
            if (zombie != null && !zombie.isHypnotized() && !targets.contains(zombie)) {
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

    private void applyReflectedProjectileHit(Projectile projectile, Plant plant) {
        if (projectile == null || plant == null || plant.isDead()) {
            return;
        }
        if (plant.isFrozen()) {
            if (projectile.getType() == ProjectileType.FIRE) {
                plant.meltIceInstantly();
            } else if (DamageExpressionParser.isInstantKill(projectile.getDamageExpression())) {
                plant.damageIce(Integer.MAX_VALUE);
            } else {
                int iceDamage = DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
                plant.damageIce(iceDamage);
            }
            return;
        }

        if (DamageExpressionParser.isInstantKill(projectile.getDamageExpression())) {
            this.destroyPlant(plant);
        } else {
            int damage = DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
            this.applyDamageToPlant(plant, damage);
        }

        if (projectile.getType() == ProjectileType.ICE && !plant.isDead()) {
            this.board.getPlantCoverSystem().hitWithSnowball(plant);
        }
    }

    private Plant findCollidingPlant(Projectile projectile) {
        if (projectile == null || projectile.getPosition() == null) {
            return null;
        }

        Plant nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Plant plant : this.board.getPlantsInLane(projectile.getPosition())) {
            if (plant == null || plant.isDead() || plant.getPosition() == null) {
                continue;
            }

            double distance = Math.abs(plant.getPosition().getX() - projectile.getExactX());

            if (distance <= Math.max(0.55, projectile.getSpeed() * 0.6)
                    && distance <= nearestDistance) {
                nearest = plant;
                nearestDistance = distance;
            }
        }

        return nearest;
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
            if (zombie.isHypnotized() || !this.canPhysicallyCollide(projectile, zombie)
                    || zombie.getPosition() == null) {
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
            if (zombie.isHypnotized() || !this.canProjectileHit(projectile, zombie)
                    || zombie.getPosition() == null) {
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

        if (zombie.isGlowing()) {
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

        if (this.board.getPlantFoodSystem() != null && zombie.isGlowing()
                && this.board.getPlantFoodSystem().addPlantFood()) {
            this.fireEvent("The glowing zombie dropeed a plant food; you have "
                    + this.board.getPlantFoodSystem().getPlantFoodAmount()
                    + " plant foods now.");
        }

        this.lootSystem.generateZombieDrop(zombie);
        this.fireZombieDiedEvent(zombie);
        this.removeZombieFromBoard(zombie);
    }

    private void updateEnvironment() {
        this.board.getPlantCoverSystem().onTick(this.board);

        for (Tile tile : this.board.getTiles()) {
            if (tile != null) {
                tile.onEnvironmentTick(this.board);
            }
        }
    }

    private boolean canTakePlantDamage(Zombie zombie, int damage) {
        return zombie != null && damage > 0 && !zombie.isDead() && !zombie.isHypnotized();
    }

    private boolean isSubmerged(Zombie zombie) {
        return zombie != null && zombie.hasCondition(ZombieCondition.SUBMERGED);
    }

    private void applyDamageFromProjectile(Zombie zombie, int damage) {
        if (!this.canTakePlantDamage(zombie, damage)) {
            return;
        }

        zombie.takeDamage(damage);
        this.finishZombieDeath(zombie);
    }

    private void applyDirectDamageFromProjectile(Zombie zombie, int damage) {
        if (!this.canTakePlantDamage(zombie, damage)) {
            return;
        }

        zombie.takeDirectDamage(damage);
        this.finishZombieDeath(zombie);
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
        String type = "Zombie";

        if (zombie != null && zombie.getDefinition() != null) {
            String displayName = zombie.getDefinition().getDisplayName();
            String alias = zombie.getDefinition().getAlias();

            if (displayName != null && !displayName.trim().isEmpty()) {
                type = displayName.trim();
            } else if (alias != null && !alias.trim().isEmpty()) {
                type = alias.trim();
            }
        }

        Position position = zombie == null ? null : zombie.getPosition();
        int x = position == null ? 0 : position.getX() + 1;
        int y = position == null ? 0 : position.getY() + 1;
        this.fireEvent("Zombie of type " + type + " is dead at (" + x + ", " + y + ")");
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
