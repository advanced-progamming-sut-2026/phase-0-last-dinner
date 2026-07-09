package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.plant.DamageExpressionParser;
import model.plant.PlantUpgradeEffect;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

public class DefenderBehavior implements PlantBehavior {
    private DefenderMode defenderMode;
    private String damageExpression;
    private boolean deathEffectUsed;
    private int contactSunAmount = 25;

    public DefenderBehavior() {
        this(DefenderMode.BASIC, "0");
    }

    public DefenderBehavior(DefenderMode defenderMode, String damageExpression) {
        this.defenderMode = defenderMode;
        this.damageExpression = damageExpression;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        if (plant == null || board == null) {
            return;
        }

        if (plant.isDead()) {
            this.activateDeathEffect(plant, board);
            return;
        }

        if (this.defenderMode == DefenderMode.REFLECT_DAMAGE) {
            this.damageNearbyZombies(plant, board);
        } else if (this.defenderMode == DefenderMode.MOVE_ZOMBIES) {
            this.moveContactZombiesToAnotherLane(plant, board);
        } else if (this.defenderMode == DefenderMode.ATTRACT_ZOMBIES) {
            this.pullNearbyZombiesIntoLane(plant, board);
        } else if (this.defenderMode == DefenderMode.SUN_ON_HIT) {
            this.rewardSunWhenContacted(plant, board);
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (this.defenderMode == DefenderMode.EXPLODE_ON_DEATH) {
            this.activateDeathEffect(plant, board);
        }
    }

    private void damageNearbyZombies(Plant plant, Board board) {
        if (board.getCombatSystem() == null) {
            return;
        }

        int damage = Math.max(10, DamageExpressionParser.parseTotalDamage(this.damageExpression));

        for (Zombie zombie : board.getZombiesAt(plant.getPosition())) {
            board.getCombatSystem().applyDamageToZombie(zombie, damage);
        }
    }

    private void moveContactZombiesToAnotherLane(Plant plant, Board board) {
        List<Zombie> zombies = new ArrayList<>(board.getZombiesAt(plant.getPosition()));

        for (Zombie zombie : zombies) {
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

    private void pullNearbyZombiesIntoLane(Plant plant, Board board) {
        if (plant.getPosition() == null) {
            return;
        }

        for (Zombie zombie : board.getZombiesInRadius(plant.getPosition(), 1)) {
            if (zombie == null || zombie.getPosition() == null) {
                continue;
            }

            if (zombie.getPosition().getY() == plant.getPosition().getY()) {
                continue;
            }

            board.moveZombie(zombie, new Position(zombie.getPosition().getX(), plant.getPosition().getY()));
        }
    }

    private void rewardSunWhenContacted(Plant plant, Board board) {
        if (board.getSunSystem() == null || board.getZombiesAt(plant.getPosition()).isEmpty()) {
            return;
        }

        board.getSunSystem().addSun(this.contactSunAmount);
    }

    private void activateDeathEffect(Plant plant, Board board) {
        if (this.deathEffectUsed || plant == null || board == null || board.getCombatSystem() == null
                || this.defenderMode != DefenderMode.EXPLODE_ON_DEATH) {
            return;
        }

        this.deathEffectUsed = true;

        for (Zombie zombie : board.getZombiesInRadius(plant.getPosition(), 1)) {
            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                board.getCombatSystem().killZombie(zombie);
            } else {
                int damage = Math.max(180, DamageExpressionParser.parseTotalDamage(this.damageExpression));
                board.getCombatSystem().applyDamageToZombie(zombie, damage);
            }
        }
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        this.damageExpression = DamageExpressionParser.addFlatDamage(this.damageExpression, effect.getDamageBonus());
        this.contactSunAmount += effect.getSunDropBonus();
    }
}
