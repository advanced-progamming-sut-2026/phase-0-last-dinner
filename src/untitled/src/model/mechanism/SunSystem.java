package model.mechanism;

import view.GameEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    @Override
    public void onTick() {
        if (this.clock == null) {
            return;
        }

        long currentTick = this.clock.getCurrentTick();
        double elapsedSeconds = this.clock.getElapsedSeconds();
        double intervalSeconds = Math.max(6 + 0.05 * elapsedSeconds, 12);
        long intervalTicks = (long) (intervalSeconds * this.clock.getTicksPerSecond());

        if (currentTick - this.lastSunSpawnTick >= intervalTicks) {
            this.spawnFallingSun();
            this.lastSunSpawnTick = currentTick;
        }

        for (Sun sun : this.suns) {
            if (sun.isFalling() && currentTick >= sun.getLandingTick()) {
                sun.reachGround();
                this.fireEvent("Sun reached ground at (" + sun.getPosition().getX()
                        + ", " + sun.getPosition().getY() + ").");
            }
        }
    }

    public Sun spawnFallingSun() {
        if (this.clock == null) {
            return null;
        }

        int x = this.random.nextInt(9);
        int y = this.random.nextInt(5);
        Position position = new Position(x, y);
        SunType type = this.chooseSunType();
        Sun sun = new Sun(type, position, this.clock.getCurrentTick());
        this.suns.add(sun);
        this.fireEvent("New " + type + " sun is dropping at (" + x + ", " + y + ").");
        return sun;
    }

    public Sun addPlantSun(Position position) {
        if (this.clock == null || position == null) {
            return null;
        }

        Sun sun = new Sun(SunType.PLANT_PRODUCED, position, this.clock.getCurrentTick());
        this.suns.add(sun);
        return sun;
    }

    public int collectSun(Position position) {
        if (position == null) {
            return 0;
        }

        for (Sun sun : this.suns) {
            if (!sun.isCollected()
                    && sun.getPosition().getX() == position.getX()
                    && sun.getPosition().getY() == position.getY()) {
                sun.collect();
                int value = sun.getType().getValue();
                this.sunAmount += value;
                return value;
            }
        }

        return 0;
    }

    public void addSun(int amount) {
        this.sunAmount += amount;
    }

    public int getSunAmount() {
        return this.sunAmount;
    }

    public void cheatCode(int amount) {
        this.addSun(amount);
    }

    private SunType chooseSunType() {
        int roll = this.random.nextInt(100);

        if (roll < 80) {
            return SunType.NORMAL;
        }

        if (roll < 95) {
            return SunType.SPECIAL;
        }

        return SunType.RADIOACTIVE;
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
