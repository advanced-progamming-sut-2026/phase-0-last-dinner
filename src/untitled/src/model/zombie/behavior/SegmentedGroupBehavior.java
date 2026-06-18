package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

import java.util.List;

public class SegmentedGroupBehavior implements ZombieBehavior {
    private List<Zombie> segments;
    private int attackingSegmentIndex;

    public SegmentedGroupBehavior(List<Zombie> segments) {
        this.segments = segments;
        this.attackingSegmentIndex = 0;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (this.segments == null || this.segments.isEmpty()) {
            return;
        }

        for (Zombie segment : this.segments) {
            if (segment != null) {
                segment.move();
            }
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (this.segments == null || this.segments.isEmpty()) {
            return;
        }

        Zombie attacker = this.segments.get(this.attackingSegmentIndex);

        if (attacker != null) {
            attacker.attack(plant);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }
}
