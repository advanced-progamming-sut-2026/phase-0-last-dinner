package model.level;
import lombok.Getter;
@Getter

public class MeowPointLevel extends Level{
    protected MeowPointLevel(LevelType levelType) {
        super(levelType);
    }

    @Override
    public void start() {

    }

    @Override
    public boolean isWinConditionMet() {
        return false;
    }

    @Override
    public boolean isLoseConditionMet() {
        return false;
    }
    private int point;
    public void calculatePoint(){}

}
