package model.mechanism;

import model.Plant;

import java.util.HashMap;
import java.util.Map;

public class PlantCooldownManager implements Tickable {
    private Map<String , Long> availableAtTick; //اینجا توی فاز 0 یه اشتباهی کردیم و خود گیاه رو نگه میداشتیم در صورتی که هر گیاه یکتاست و باید اسم گیاه رو نگه داریم من درستش کردم
    private boolean cooldownDisabled;
    private GameClock gameClock;

    public PlantCooldownManager(GameClock gameClock) {
        this.gameClock = gameClock; //توی گیم انجین مقداردهی میشه و ازش استفاده میکنیم
        this.cooldownDisabled = false;
        this.availableAtTick = new HashMap<>();
    }



    @Override
    public void onTick() {
    }

    public boolean isAvailable(Plant plant) {
        if (cooldownDisabled) return true;
        Long availableAt = availableAtTick.get(plant.getName());
        if (availableAt == null) return true; //یعنی گیاه تا حالا کاشته نشده
        return gameClock.getCurrentTick() >= availableAt;
    }

    public long getRemainingTicks(Plant plant) {
        if (cooldownDisabled) return 0;
        Long availableAt = availableAtTick.get(plant.getName());
        if (availableAt == null) return 0;
        return Math.max(0, availableAt - gameClock.getCurrentTick());
    }

    public void startCooldown(Plant plant) {
        Long availableAt = gameClock.getCurrentTick() + plant.getCooldownTicks();
        availableAtTick.put(plant.getName(), availableAt);
    }

    public void removeAllCooldowns() {
        cooldownDisabled = true;
        availableAtTick.clear();
    }
}
