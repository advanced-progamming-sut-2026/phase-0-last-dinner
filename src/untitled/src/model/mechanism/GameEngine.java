package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import view.GameEventListener;

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

    public void advanceTime() {
        this.advanceTime(1);
    }

    public void advanceTime(int tickCount) {
        if (!this.gameRunning || tickCount <= 0) {
            return;
        }

        for (int i = 0; i < tickCount; i++) {
            this.clock.advance(1);

            for (Tickable tickable : new ArrayList<>(this.tickables)) {
                if (tickable != null) {
                    tickable.onTick();
                }
            }
        }
    }

    public void register() {
    }

    public void register(Tickable tickable) {
        if (tickable != null && !this.tickables.contains(tickable)) {
            this.tickables.add(tickable);
        }
    }

    public void unregister() {
    }

    public void unregister(Tickable tickable) {
        this.tickables.remove(tickable);
    }

    public void endGame() {
        this.gameRunning = false;
        this.fireEvent("Game ended.");
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
