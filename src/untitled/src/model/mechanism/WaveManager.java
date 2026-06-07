package model.mechanism;

import java.util.List;

public class WaveManager implements Tickable {
    private List<Wave> waves;
    private int currentWaveIndex;
    private ZombieSpawner zombieSpawner;

    @Override
    public void onTick() {
    }

    public void startNextWave() {
    }

    public Wave getCurrentWave() {
        return null;
    }

    public boolean hasNextWave() {
        return false;
    }

    public double calculateWaveDifficulty(int waveNumber) {
        return 0;
    }
}
