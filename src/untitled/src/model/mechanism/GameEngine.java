package model.mechanism;

import lombok.Getter;import lombok.Setter;import view.GameEventListener;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class GameEngine {
    private GameClock clock;
    private List<Tickable> tickables;
    private boolean gameRunning;
    private GameEventListener listener;
    public GameEngine() {
        this.clock = new GameClock();
        this.tickables = new ArrayList<>();
        this.gameRunning = true;
    }
    private void fireEvent(String message) {
        if (listener != null) listener.onGameEvent(message);
    }
    public void advanceTime(int tickCount) {
        if (!gameRunning || tickCount <= 0) return;
        for (int i = 0; i < tickCount; i++) {
            clock.advance(1);
            for (Tickable tickable : new ArrayList<>(tickables)) {
                tickable.onTick();
            }
        }
    }
    // رجیستر و آنریجستور فکر کنم قراره تو کنترلر انجام بدیم
    public void register(Tickable tickable) {
        if (tickable != null && !tickables.contains(tickable)) {
            tickables.add(tickable);
        }
    }

    public void unregister(Tickable tickable) {
        tickables.remove(tickable);
    }

    public void endGame() {
        this.gameRunning = false;
    }
}
