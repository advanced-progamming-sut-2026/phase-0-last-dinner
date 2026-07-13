package model.mechanism;

import lombok.Getter;
import model.Plant;
import model.zombie.Zombie;
import view.GameEventListener;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GameEngine {
    private GameClock clock;
    private List<Tickable> tickables;
    private boolean gameRunning;
    private GameEventListener listener;
    private Board board;

    public GameEngine() {
        this(null);
    }

    public GameEngine(Board board) {
        this.clock = new GameClock();
        this.tickables = new ArrayList<>();
        this.gameRunning = true;
        this.board = board;
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

            for (Tickable tickable : this.getScheduledTickables()) {
                if (tickable != null) {
                    tickable.onTick();
                }
            }

            if (this.board != null && this.board.isBrainEaten()) {
                this.endGame();
                break;
            }
        }
    }

    public void register(Tickable tickable) {
        if (tickable != null && !this.tickables.contains(tickable)) {
            this.tickables.add(tickable);
        }
    }

    public void unregister(Tickable tickable) {
        this.tickables.remove(tickable);
    }

    public void endGame() {
        this.gameRunning = false;
        this.fireEvent("Game ended.");
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public void setListener(GameEventListener listener) {
        this.listener = listener;
    }

    private List<Tickable> getScheduledTickables() {
        List<Tickable> scheduled = new ArrayList<>(this.tickables);

        if (this.board == null) {
            return scheduled;
        }

        for (Plant plant : this.board.getAllPlants()) {
            if (plant != null && !scheduled.contains(plant)) {
                scheduled.add(plant);
            }
        }

        for (Zombie zombie : this.board.getAllZombies()) {
            if (zombie != null && !scheduled.contains(zombie)) {
                scheduled.add(zombie);
            }
        }

        return scheduled;
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
