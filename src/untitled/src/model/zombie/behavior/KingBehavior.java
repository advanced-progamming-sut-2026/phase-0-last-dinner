package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.ArmorFlag;
import model.zombie.ArmorType;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;
import model.zombie.ZombieArmorDefinition;
import model.zombie.ZombieType;

import java.util.EnumSet;

public class KingBehavior implements ZombieBehavior {
    private long knightIntervalTicks;
    private long ticksSinceKnighting;

    public KingBehavior(long knightIntervalTicks) {
        this.knightIntervalTicks = Math.max(1, knightIntervalTicks);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.ticksSinceKnighting++;
        if (this.ticksSinceKnighting >= this.knightIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceKnighting = 0;
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        Zombie target = this.findTarget(zombie, board);
        if (target == null) {
            return;
        }

        target.addArmor(new ZombieArmor(new ZombieArmorDefinition(
                "ShoulderArmorDefault",
                ArmorType.SHOULDER_ARMOR,
                1600,
                EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.PASS_DAMAGE)
        )));
        target.addArmor(new ZombieArmor(new ZombieArmorDefinition(
                "CrownDefault",
                ArmorType.CROWN,
                1600,
                EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.DROPPABLE, ArmorFlag.METALLIC, ArmorFlag.HELMET)
        )));
    }

    @Override
    public boolean canMove(Zombie zombie, Board board) {
        return false;
    }

    private Zombie findTarget(Zombie king, Board board) {
        if (king == null || king.getPosition() == null || board == null) {
            return null;
        }

        for (Zombie zombie : board.getZombiesInRadius(king.getPosition(), 4)) {
            if (zombie == null || zombie == king || zombie.isDead() || zombie.getDefinition() == null
                    || zombie.getDefinition().getType() != ZombieType.BASIC || zombie.getPosition() == null) {
                continue;
            }

            int verticalDistance = Math.abs(zombie.getPosition().getY() - king.getPosition().getY());
            if (verticalDistance <= 1 && !this.hasKnightArmor(zombie)) {
                return zombie;
            }
        }
        return null;
    }

    private boolean hasKnightArmor(Zombie zombie) {
        if (zombie.getArmors() == null) {
            return false;
        }
        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor != null && armor.getDefinition() != null
                    && (armor.getDefinition().getType() == ArmorType.CROWN
                    || armor.getDefinition().getType() == ArmorType.SHOULDER_ARMOR)) {
                return true;
            }
        }
        return false;
    }
}
