package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

import java.util.Set;

public class InstantPlantDestroyerBehavior implements ZombieBehavior {
    private Set<String> affectedPlantNames;

    public InstantPlantDestroyerBehavior(Set<String> affectedPlantNames) {
        this.affectedPlantNames = affectedPlantNames;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (plant == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        if (this.affectedPlantNames == null || this.affectedPlantNames.isEmpty()
                || this.affectedPlantNames.contains(plant.getName())) {
            board.getCombatSystem().destroyPlant(plant);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }
}
