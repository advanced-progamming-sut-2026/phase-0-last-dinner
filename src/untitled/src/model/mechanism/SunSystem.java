package model.mechanism;

import model.Plant;
import view.GameEventListener;

import java.util.ArrayList;
import java.util.Iterator;
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
    private boolean automaticSunEnabled;
    private double spawnRateMultiplier;

    public SunSystem(Board board, GameClock clock) {
        this.board = board;
        this.clock = clock;
        this.suns = new ArrayList<>();
        this.sunAmount = 50;
        this.lastSunSpawnTick = 0;
        this.random = new Random();
        this.automaticSunEnabled = true;
        this.spawnRateMultiplier = 1.0;

        if (board != null) {
            board.setSunSystem(this);
        }
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
        double intervalSeconds = Math.max(6 + 0.05 * elapsedSeconds, 12)
                / this.spawnRateMultiplier;
        long intervalTicks = (long) (intervalSeconds * this.clock.getTicksPerSecond());

        if (this.automaticSunEnabled && currentTick - this.lastSunSpawnTick >= intervalTicks) {
            this.spawnFallingSun();
            this.lastSunSpawnTick = currentTick;
        }

        for (Sun sun : this.suns) {
            if (sun.isFalling() && currentTick >= sun.getLandingTick()) {
                sun.reachGround();
                this.fireEvent("Sun reached the ground at position (" + (sun.getPosition().getX() + 1)
                        + ", " + (sun.getPosition().getY() + 1) + ").");
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
        this.fireEvent("New " + type + " sun is dropping at position ("
                + (x + 1) + ", " + (y + 1) + ").");
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

    public Sun addPlantSun(Plant producer, int amount) {
        if (this.clock == null || producer == null || producer.getPosition() == null || amount <= 0) {
            return null;
        }

        Sun sun = new Sun(
                SunType.PLANT_PRODUCED,
                producer.getPosition(),
                this.clock.getCurrentTick(),
                amount,
                producer
        );
        this.suns.add(sun);
        return sun;
    }

    public boolean hasUncollectedSunFrom(Plant producer) {
        if (producer == null) {
            return false;
        }

        for (Sun sun : this.suns) {
            if (sun != null && !sun.isCollected() && sun.getProducer() == producer) {
                return true;
            }
        }

        return false;
    }

    public int collectSun(Position position) {
        if (position == null) {
            return 0;
        }

        for (Sun sun : this.suns) {
            if (!sun.isCollected()
                    && sun.getPosition().getX() == position.getX()
                    && sun.getPosition().getY() == position.getY()) {
                return this.collectSun(sun);
            }
        }

        return 0;
    }

    public int collectSun(Sun sun) {
        if (sun == null || sun.isCollected() || !this.suns.contains(sun)) {
            return 0;
        }

        sun.collect();
        this.suns.remove(sun);

        if (sun.getType() == SunType.RADIOACTIVE && sun.isFalling()) {
            if (this.board != null && this.board.getCombatSystem() != null) {
                this.board.getCombatSystem().applyRadioactiveSunExplosion(sun.getPosition());
            }

            this.fireEvent("Radioactive sun exploded before reaching the ground.");
            return 0;
        }

        this.sunAmount += sun.getValue();
        return sun.getValue();
    }

    public void addSun(int amount) {
        this.sunAmount = Math.max(0, this.sunAmount + amount);
    }

    public int getSunAmount() {
        return this.sunAmount;
    }

    public boolean isAutomaticSunEnabled() {
        return this.automaticSunEnabled;
    }

    public void setAutomaticSunEnabled(boolean automaticSunEnabled) {
        this.automaticSunEnabled = automaticSunEnabled;
    }

    public boolean isSkySunEnabled() {
        return this.automaticSunEnabled;
    }

    public void setSkySunEnabled(boolean skySunEnabled) {
        this.automaticSunEnabled = skySunEnabled;
    }

    public void setSpawnRateMultiplier(double spawnRateMultiplier) {
        this.spawnRateMultiplier = spawnRateMultiplier > 0 ? spawnRateMultiplier : 1.0;
    }

    public List<Sun> getSuns() {
        return this.suns;
    }

    public int stealGroundSun(int maximumAmount) {
        return this.stealGroundSun(maximumAmount, maximumAmount);
    }

    // ta meghdar hadaf midozde vali az hadaksar bishtar nemigire
    public int stealGroundSun(int targetAmount, int maximumAmount) {
        if (targetAmount <= 0 || maximumAmount <= 0) {
            return 0;
        }

        int stolen = 0;
        Iterator<Sun> iterator = this.suns.iterator();

        while (iterator.hasNext()) {
            Sun sun = iterator.next();
            if (sun == null || sun.isCollected() || sun.isFalling()) {
                continue;
            }

            int value = sun.getValue();

            if (stolen + value > maximumAmount) {
                continue;
            }

            sun.collect();
            iterator.remove();
            stolen += value;

            if (stolen >= targetAmount) {
                break;
            }
        }

        return stolen;
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
