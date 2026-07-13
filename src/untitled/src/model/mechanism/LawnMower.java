package model.mechanism;

import lombok.Getter;import lombok.Setter;import model.zombie.Zombie;import model.zombie.ZombieType;import view.GameEventListener;

import java.util.ArrayList;import java.util.List;
@Getter
@Setter
public class LawnMower {
    private int row;
    private boolean active;
    private boolean used;
    private GameEventListener listener;
    public LawnMower(int row) {
        this.row = row;
        this.active = true;
        this.used = false;
    }
    private void fireEvent(String message) {
        if (listener != null) listener.onGameEvent(message);
    }
    public List<Zombie> trigger(List<Zombie> zombiesInRow) {
        List<Zombie> killed = new ArrayList<>();

        if (!active || zombiesInRow == null) return killed;

        if (!used) {
            StringBuilder killedNames = new StringBuilder();
            for (Zombie zombie : zombiesInRow) {
                if (zombie == null || zombie.isDead()) continue;
                if (zombie.getDefinition().getType() == ZombieType.BOSS) continue;
                zombie.die();
                killed.add(zombie);
                if (killedNames.length() > 0) killedNames.append(", ");
                killedNames.append(zombie.getDefinition().getDisplayName());
            }
            fireEvent("The lawn mower in the row " + row
                    + " is triggered and killed these zombies: " + killedNames);
            used = true;
            active = false;

        } else {
            fireEvent("The zombie ate your brain; LOSER!!!");
        }
        return killed;
    }

    public boolean canTrigger() {
        return active && !used;
    }
}
