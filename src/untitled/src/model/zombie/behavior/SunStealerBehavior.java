package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.SunSystem;
import model.zombie.Zombie;

public class SunStealerBehavior implements ZombieBehavior {
    private int stealAmount;
    private SunSystem sunSystem;

    public SunStealerBehavior(int stealAmount, SunSystem sunSystem) {
        this.stealAmount = stealAmount;
        this.sunSystem = sunSystem;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        this.activate(zombie, board);
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        SunSystem system = this.sunSystem;

        if (system == null && board != null) {
            system = board.getSunSystem();
        }

        if (system == null) {
            return;
        }

        system.addSun(-this.stealAmount);
    }
}
