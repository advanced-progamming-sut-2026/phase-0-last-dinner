package model.mechanism;

import lombok.Getter;import lombok.Setter;import view.GameEventListener;

import java.util.List;
@Getter
@Setter
public class WaveManager implements Tickable {
    private List<Wave> waves;
    private int currentWaveIndex;
    private ZombieSpawner zombieSpawner;
    private GameEventListener listener;
    private boolean started;
    private GameEngine gameEngine;

    public WaveManager(List<Wave> waves, ZombieSpawner zombieSpawner, GameEngine gameEngine) {
        this.waves = waves;
        this.zombieSpawner = zombieSpawner;
        this.currentWaveIndex = -1;
        this.started = false;
        this.gameEngine = gameEngine;
    }


    private void fireEvent(String message) {
        if (listener != null) listener.onGameEvent(message);
    }

    @Override
    public void onTick() {
        if (!started) {
            startNextWave();
            started = true;
            return;
        }
        Wave current = getCurrentWave();
        if (current == null) return;

        if (current.canStartNextWave() && hasNextWave()) {
            startNextWave();
        }
        if (!hasNextWave() && current.getRemainingHealthPercentage() == 0) {
            fireEvent("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
            gameEngine.endGame();
        }
    }

    public void startNextWave() {
        currentWaveIndex++;
        Wave wave = waves.get(currentWaveIndex);
        wave.start();
        zombieSpawner.spawnWave(wave);
        if (wave.isFinalWave()) {
            fireEvent("The final wave has come.");
        } else {
            fireEvent("Wave " + wave.getNumber() + " started.");
        }
    }

    public Wave getCurrentWave() {
        if (currentWaveIndex < 0 || currentWaveIndex >= waves.size()) return null;
        return waves.get(currentWaveIndex);
    }

    public boolean hasNextWave() {
        return currentWaveIndex + 1 < waves.size();
    }

    public double calculateWaveDifficulty(int waveNumber) {
        return waves.get(waveNumber - 1).getDifficulty();
    }
}
