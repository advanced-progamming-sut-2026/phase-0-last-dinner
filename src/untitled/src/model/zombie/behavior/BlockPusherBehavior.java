package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.zombie.PushedObstacle;
import model.zombie.Zombie;
import model.zombie.ZombieChapter;

import java.util.ArrayList;
import java.util.List;

public class BlockPusherBehavior implements ZombieBehavior {
    private final String obstacleName;
    private final int obstacleHealth;
    private final int obstacleCount;
    private final List<PushedObstacle> obstacles = new ArrayList<>();
    // obstacle ha yek bar sakhte mishan va ba marge owner hazf nemishan
    private boolean deployed;

    public BlockPusherBehavior(int blockHealth) {
        this("Pushed Obstacle", blockHealth, 1);
    }

    public BlockPusherBehavior(int blockHealth, int blockCount) {
        this("Pushed Obstacle", blockHealth, blockCount);
    }

    public BlockPusherBehavior(String obstacleName, int obstacleHealth, int obstacleCount) {
        this.obstacleName = obstacleName == null ? "Pushed Obstacle" : obstacleName;
        this.obstacleHealth = Math.max(1, obstacleHealth);
        this.obstacleCount = Math.max(0, obstacleCount);
    }

    @Override
    public void onTick(Zombie owner, Board board) {
        this.ensureObstacles(owner, board);
        this.pushObstacles(owner, board);
        this.resolveCollisions(board);
    }

    @Override
    public void attack(Zombie owner, Plant plant, Board board) {
        if (plant != null && this.hasLivingObstacle() && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().destroyPlant(plant);
        }
    }

    @Override
    public void activate(Zombie owner, Board board) {
        this.ensureObstacles(owner, board);
    }

    @Override
    public void onDeath(Zombie owner, Board board) {
        if (owner == null || !owner.isHypnotized()) {
            this.ensureObstacles(owner, board);
        }
    }

    public List<PushedObstacle> getObstacles() {
        return this.obstacles;
    }

    private void ensureObstacles(Zombie owner, Board board) {
        if (this.deployed || owner == null || owner.getPosition() == null || board == null) {
            return;
        }

        ZombieChapter chapter = owner.getDefinition() == null
                ? ZombieChapter.ALL_CHAPTERS
                : owner.getDefinition().getChapter();

        for (int index = 0; index < this.obstacleCount; index++) {
            Position position = this.positionAhead(owner, index + 1);
            PushedObstacle obstacle = new PushedObstacle(
                    this.obstacleName,
                    this.obstacleHealth,
                    chapter,
                    position
            );
            board.addZombie(obstacle, position);

            if (obstacle.getBoard() == board) {
                this.obstacles.add(obstacle);

                if (owner.getWave() != null) {
                    // obstacle ham ozve wave ast ta wave zudtar tamam nashe
                    owner.getWave().addZombie(obstacle);
                }
            }
        }

        this.deployed = true;
    }

    private void pushObstacles(Zombie owner, Board board) {
        if (owner == null || owner.isDead() || board == null) {
            return;
        }

        for (int index = this.obstacles.size() - 1; index >= 0; index--) {
            PushedObstacle obstacle = this.obstacles.get(index);

            if (obstacle == null || obstacle.isDead() || obstacle.getBoard() != board) {
                continue;
            }

            Position destination = this.positionAhead(owner, index + 1);

            if (!destination.equals(obstacle.getPosition()) && board.removeZombie(obstacle)) {
                board.addZombie(obstacle, destination);
            }
        }
    }

    private void resolveCollisions(Board board) {
        if (board == null || board.getCombatSystem() == null) {
            return;
        }

        for (PushedObstacle obstacle : this.obstacles) {
            if (obstacle == null || obstacle.isDead() || obstacle.getPosition() == null
                    || obstacle.getBoard() != board) {
                continue;
            }

            for (Plant plant : board.getPlantsAt(obstacle.getPosition())) {
                board.getCombatSystem().destroyPlant(plant);
            }

            for (Zombie zombie : board.getZombiesAt(obstacle.getPosition())) {
                if (zombie != null && zombie != obstacle && !zombie.isDead() && zombie.isHypnotized()) {
                    board.getCombatSystem().killZombieIgnoringAllegiance(zombie);
                }
            }
        }
    }

    private boolean hasLivingObstacle() {
        for (PushedObstacle obstacle : this.obstacles) {
            if (obstacle != null && !obstacle.isDead()) {
                return true;
            }
        }

        return false;
    }

    private Position positionAhead(Zombie owner, int distance) {
        int x = owner.getPosition().getX() - Math.max(1, distance);
        return new Position(Math.max(0, x), owner.getPosition().getY());
    }
}
