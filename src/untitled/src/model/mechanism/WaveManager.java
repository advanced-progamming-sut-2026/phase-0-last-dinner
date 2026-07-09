package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import view.GameEventListener;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WaveManager implements Tickable {
    private List<Wave> waves;
    private int currentWaveIndex;
    private ZombieSpawner zombieSpawner;
    private GameEventListener listener;
    private boolean started;

    public WaveManager() {
        this(new ArrayList<Wave>(), null);
    }

    public WaveManager(List<Wave> waves, ZombieSpawner zombieSpawner) {
        this.waves = waves == null ? new ArrayList<Wave>() : waves;
        this.zombieSpawner = zombieSpawner;
        this.currentWaveIndex = -1;
    }

    @Override
    public void onTick() {
        if (!this.started) {
            this.startNextWave();
            this.started = true;
            return;
        }

        Wave currentWave = this.getCurrentWave();

        if (currentWave != null && currentWave.canStartNextWave() && this.hasNextWave()) {
            this.startNextWave();
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

        if (this.zombieSpawner != null) {
            this.zombieSpawner.spawnWave(wave);
        }

        this.fireEvent(wave.isFinalWave()
                ? "The final wave has started."
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

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
