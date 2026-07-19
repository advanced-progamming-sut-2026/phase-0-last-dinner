package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.chapters.Chapter;
import view.GameEventListener;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WaveManager implements Tickable {
    private List<Wave> waves;
    private int currentWaveIndex;
    private ZombieSpawner zombieSpawner;
    private GameEngine gameEngine;
    private GameEventListener listener;
    private boolean started;
    private Chapter chapter;

    public WaveManager() {
        this(new ArrayList<Wave>(), null, null);
    }

    public WaveManager(List<Wave> waves, ZombieSpawner zombieSpawner) {
        this(waves, zombieSpawner, null);
    }

    public WaveManager(List<Wave> waves, ZombieSpawner zombieSpawner, GameEngine gameEngine) {
        this.zombieSpawner = zombieSpawner;
        this.gameEngine = gameEngine;
        this.configureWaves(waves);
    }

    @Override
    public void onTick() {
        if (!this.started) {
            if (this.hasNextWave()) {
                this.startNextWave();
                this.started = true;
            }
            return;
        }

        Wave currentWave = this.getCurrentWave();

        if (currentWave != null && currentWave.canStartNextWave() && this.hasNextWave()) {
            this.startNextWave();
        }

        if (currentWave != null
                && !this.hasNextWave()
                && currentWave.getRemainingHealthPercentage() <= 0
                && this.gameEngine != null
                && this.gameEngine.isGameRunning()) {
            this.fireEvent("All waves completed.");
            this.gameEngine.endGame();
        }
    }

    public void startNextWave() {
        if (!this.hasNextWave()) {
            return;
        }

        this.currentWaveIndex++;
        Wave wave = this.getCurrentWave();

        if (wave == null) {
            return;
        }

        wave.start();

        if (this.chapter != null) {
            Board board = this.zombieSpawner != null ? this.zombieSpawner.getBoard() : null;
            this.chapter.onWaveStart(board, wave);
        }

        if (this.zombieSpawner != null) {
            this.zombieSpawner.spawnWave(wave);
        }

        this.fireEvent(wave.isFinalWave()
                ? "The final wave has come."
                : "Wave " + wave.getNumber() + " started.");
    }

    public Wave getCurrentWave() {
        if (this.currentWaveIndex < 0 || this.currentWaveIndex >= this.waves.size()) {
            return null;
        }

        return this.waves.get(this.currentWaveIndex);
    }

    public boolean hasNextWave() {
        return this.waves != null && this.currentWaveIndex + 1 < this.waves.size();
    }

    public double calculateWaveDifficulty(int waveNumber) {
        if (waveNumber <= 0 || this.waves == null || waveNumber > this.waves.size()) {
            return 0;
        }

        return this.waves.get(waveNumber - 1).getDifficulty();
    }

    // list jadid ro migozare va shorue wave ha ro az aval reset mikone
    public void configureWaves(List<Wave> waves) {
        this.waves = waves == null ? new ArrayList<Wave>() : waves;
        this.currentWaveIndex = -1;
        this.started = false;
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
