package model.mechanism;
import lombok.Getter;
@Getter
public class Loot {
    private LootType type;
    private int amount;
    private Position position;
    private boolean collected;

    public Loot(LootType type, int amount, Position position) {
        this.type = type;
        this.amount = amount;
        this.position = position;
        this.collected = false;
    }


    public void collect() {
        this.collected = true;
    }
}
