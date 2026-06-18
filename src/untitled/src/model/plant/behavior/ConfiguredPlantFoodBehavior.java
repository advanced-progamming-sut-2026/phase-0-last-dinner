package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Tile;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.ArrayList;
import java.util.List;

public class ConfiguredPlantFoodBehavior implements PlantFoodBehavior {
    private PlantFoodEffectType effectType;
    private String effectDescription;
    private String damageExpression;
    private int activationCount;
    private int targetCount;
    private int radius;
    private int sunAmount;
    private int bonusHealth;
    private Projectile projectileTemplate;

    public ConfiguredPlantFoodBehavior(String effectDescription, int activationCount) {
        this(PlantFoodEffectType.REPEAT_ABILITY, effectDescription, "0", activationCount, activationCount, 1, 0, 0, null);
    }

    public ConfiguredPlantFoodBehavior(
            PlantFoodEffectType effectType,
            String effectDescription,
            String damageExpression,
            int activationCount,
            int targetCount,
            int radius,
            int sunAmount,
            int bonusHealth,
            Projectile projectileTemplate
    ) {
        this.effectType = effectType;
        this.effectDescription = effectDescription;
        this.damageExpression = damageExpression;
        this.activationCount = activationCount;
        this.targetCount = targetCount;
        this.radius = radius;
        this.sunAmount = sunAmount;
        this.bonusHealth = bonusHealth;
        this.projectileTemplate = projectileTemplate;
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || this.effectType == null || this.effectType == PlantFoodEffectType.NONE) {
            return;
        }

        switch (this.effectType) {
            case SUN_BURST:
                this.addSun(board);
                break;
            case PROJECTILE_BURST:
                this.fireProjectiles(plant, board);
                break;
            case TARGETED_DAMAGE:
                this.affectZombies(board, this.getNearestZombies(plant, board), null, false);
                break;
            case LANE_DAMAGE:
                this.affectZombies(board, board == null ? null : board.getZombiesInLane(plant.getPosition()), null, false);
                break;
            case BOARD_DAMAGE:
                this.affectZombies(board, board == null ? null : board.getAllZombies(), null, false);
                break;
            case AREA_DAMAGE:
                this.affectZombies(board, board == null ? null : board.getZombiesInRadius(plant.getPosition(), this.radius), null, false);
                break;
            case FREEZE_LANE:
                this.affectZombies(board, board == null ? null : board.getZombiesInLane(plant.getPosition()), ZombieCondition.FROZEN, false);
                this.repeatAbility(plant);
                break;
            case FREEZE_BOARD:
                this.affectZombies(board, board == null ? null : board.getAllZombies(), ZombieCondition.FROZEN, false);
                break;
            case POISON_TARGETS:
                this.affectZombies(board, this.getNearestZombies(plant, board), ZombieCondition.POISONED, false);
                this.repeatAbility(plant);
                break;
            case HYPNOTIZE_TARGETS:
                this.affectZombies(board, this.getNearestZombies(plant, board), ZombieCondition.HYPNOTIZED, false);
                break;
            case REMOVE_ARMOR:
                this.affectZombies(board, this.getNearestZombies(plant, board), null, true);
                break;
            case ARM_AND_CLONE:
                this.armExplosiveBehavior(plant);
                this.cloneNearby(plant, board);
                break;
            case ARMOR_BOOST:
                plant.addBonusHealth(this.bonusHealth);
                break;
            case HEAL_TO_FULL:
                plant.healToFull();
                this.repeatAbility(plant);
                break;
            case LANE_SHIFT:
                this.shiftLane(plant, board);
                break;
            case RESET_SAME_PLANTS:
                this.resetSamePlants(plant, board);
                this.repeatAbility(plant);
                break;
            case CLONE_NEARBY:
                this.cloneNearby(plant, board);
                break;
            case PROJECTILE_BUFF:
            case REPEAT_ABILITY:
            default:
                this.repeatAbility(plant);
                break;
        }
    }

    private void repeatAbility(Plant plant) {
        int count = Math.max(1, this.activationCount);

        for (int i = 0; i < count; i++) {
            plant.useAbility();
        }
    }

    private void addSun(Board board) {
        if (board == null || board.getSunSystem() == null || this.sunAmount <= 0) {
            return;
        }

        board.getSunSystem().addSun(this.sunAmount);
    }

    private void fireProjectiles(Plant plant, Board board) {
        if (board == null || this.projectileTemplate == null) {
            this.repeatAbility(plant);
            return;
        }

        int count = Math.max(1, this.activationCount);
        List<Zombie> targets = board.getNearestZombies(plant.getPosition(), count);

        for (int i = 0; i < count; i++) {
            if (targets.isEmpty()) {
                board.addProjectile(this.projectileTemplate.copyAt(plant.getPosition()));
            } else {
                board.addProjectile(this.projectileTemplate.copyAtTarget(
                        plant.getPosition(),
                        targets.get(i % targets.size())
                ));
            }
        }
    }

    private List<Zombie> getNearestZombies(Plant plant, Board board) {
        if (plant == null || board == null) {
            return new ArrayList<>();
        }

        return board.getNearestZombies(plant.getPosition(), Math.max(1, this.targetCount));
    }

    private void affectZombies(
            Board board,
            List<Zombie> zombies,
            ZombieCondition condition,
            boolean removeArmor
    ) {
        if (board == null || zombies == null || zombies.isEmpty()) {
            return;
        }

        List<Zombie> targets = new ArrayList<>(zombies);
        int remainingTargets = this.targetCount <= 0 ? targets.size() : Math.min(this.targetCount, targets.size());

        for (int i = 0; i < remainingTargets; i++) {
            Zombie zombie = targets.get(i);

            if (zombie == null || zombie.isDead()) {
                continue;
            }

            if (condition != null) {
                zombie.addCondition(condition);
            }

            if (removeArmor) {
                zombie.dropActiveArmor();
            }

            if (board.getCombatSystem() == null || this.damageExpression == null) {
                continue;
            }

            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                board.getCombatSystem().killZombie(zombie);
            } else {
                int damage = DamageExpressionParser.parseTotalDamage(this.damageExpression);

                if (damage > 0) {
                    board.getCombatSystem().applyDamageToZombie(zombie, damage);
                }
            }
        }
    }

    private void armExplosiveBehavior(Plant plant) {
        if (plant.getBehavior() instanceof ExplosiveBehavior) {
            ((ExplosiveBehavior) plant.getBehavior()).armNow();
        }
    }

    private void resetSamePlants(Plant plant, Board board) {
        if (board == null || plant.getName() == null) {
            return;
        }

        for (Plant samePlant : board.getPlantsByName(plant.getName())) {
            if (samePlant != null) {
                samePlant.healToFull();
            }
        }
    }

    private void shiftLane(Plant plant, Board board) {
        if (plant == null || board == null || plant.getPosition() == null) {
            return;
        }

        List<Zombie> laneZombies = new ArrayList<>(board.getZombiesInLane(plant.getPosition()));

        for (Zombie zombie : laneZombies) {
            if (zombie == null || zombie.getPosition() == null) {
                continue;
            }

            Position upperLane = new Position(zombie.getPosition().getX(), zombie.getPosition().getY() - 1);

            if (board.moveZombie(zombie, upperLane)) {
                continue;
            }

            Position lowerLane = new Position(zombie.getPosition().getX(), zombie.getPosition().getY() + 1);
            board.moveZombie(zombie, lowerLane);
        }
    }

    private void cloneNearby(Plant plant, Board board) {
        if (plant == null || board == null || plant.getPosition() == null) {
            return;
        }

        int cloneCount = Math.max(1, this.activationCount);
        int placed = 0;

        for (int distance = 1; distance <= 2 && placed < cloneCount; distance++) {
            for (int deltaY = -distance; deltaY <= distance && placed < cloneCount; deltaY++) {
                for (int deltaX = -distance; deltaX <= distance && placed < cloneCount; deltaX++) {
                    if (Math.abs(deltaX) != distance && Math.abs(deltaY) != distance) {
                        continue;
                    }

                    Position clonePosition = new Position(
                            plant.getPosition().getX() + deltaX,
                            plant.getPosition().getY() + deltaY
                    );
                    Tile tile = board.getTile(clonePosition);

                    if (tile == null || !tile.canPlacePlant(plant)) {
                        continue;
                    }

                    Plant clone = plant.copyForPlantFood(clonePosition);
                    tile.addPlant(clone);
                    placed++;
                }
            }
        }
    }
}
