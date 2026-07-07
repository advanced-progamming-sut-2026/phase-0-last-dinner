package model.mechanism;

import view.GameEventListener;

import java.util.*;

public class SunSystem implements Tickable {
    private List<Sun> suns;
    private int sunAmount;
    private Board board;
    private Random random;
    private long lastSunSpawnTick;
    private GameEventListener listener;
    private GameClock clock;

    public SunSystem(Board board, GameClock clock) {
        this.board = board;
        this.clock = clock;
        this.suns = new ArrayList<>();
        this.sunAmount = 50;
        this.lastSunSpawnTick = 0;
        this.random = new Random();
    }
    public void setListener(GameEventListener listener) {
        this.listener = listener;
    }
    private void fireEvent(String message) {
        if (listener != null) {
            listener.onGameEvent(message);
        }
    }

    @Override
    public void onTick() {
        long currentTick = clock.getCurrentTick();
        double elapsedSeconds = clock.getElapsedSeconds();

        double intervalSeconds = Math.max(6 + 0.05 * elapsedSeconds, 12);
        long intervalTicks = (long)(intervalSeconds * 10);
        if (currentTick - lastSunSpawnTick >= intervalTicks) {
            spawnFallingSun(); //سقوط خورشید جدید
            lastSunSpawnTick = currentTick;
        }
        for (Sun sun : suns) {
            if (sun.isFalling() && currentTick >= sun.getLandingTick()) {
                sun.reachGround();
                fireEvent("Sun reached the ground at position ("
                        + sun.getPosition().getX() + ", "
                        + sun.getPosition().getY() + ")");
            }
        }
    }
    private SunType chooseSunType() {  // انتخاب نوع خورشید به صورت رندم
        int roll = random.nextInt(100);
        if (roll < 80) return SunType.NORMAL;
        if (roll < 95) return SunType.SPECIAL;
        return SunType.RADIOACTIVE;
    }


    public Sun spawnFallingSun() {
        int x = random.nextInt(9);
        int y = random.nextInt(5); //انتخاب یک سطر و ستون رندم برای فرود امدن
        Position position = new Position(x, y);
        SunType type = chooseSunType();

        Sun sun = new Sun(type, position, clock.getCurrentTick());
        suns.add(sun);

        fireEvent("New " + type + " sun is dropping at position ("
                + x + ", " + y + ")");

        return sun;
    }

    public Sun addPlantSun(Position position) {
        Sun sun = new Sun(SunType.PLANT_PRODUCED, position, clock.getCurrentTick());
        suns.add(sun); //اینجا چک نمیکنم که خورشید قبلی برداشته شده یا نه بهتره توی بخش گیاه ها هندل شه
        return sun;
    }

    public int collectSun(Position position) {
        for (Sun sun : suns) {
            if (!sun.isCollected()
                    && sun.getPosition().getX() == position.getX()
                    && sun.getPosition().getY() == position.getY()) {
                if (sun.getType() == SunType.RADIOACTIVE && sun.isFalling()) {
                    board.getCombatSystem().applyRadioactiveSunExplosion(position);
                    sun.collect();
                    return 0; // خورشیدی نمیده
                }
                sun.collect();
                int value = sun.getType().getValue();
                sunAmount += value;
                return value;
            }
        }
        return 0;
    }

    public void addSun(int amount) {
        this.sunAmount += amount;
    }

    public int getSunAmount() {
        return sunAmount;
    }
    public void cheatCode(int amount){
        addSun(amount);
    }
}
