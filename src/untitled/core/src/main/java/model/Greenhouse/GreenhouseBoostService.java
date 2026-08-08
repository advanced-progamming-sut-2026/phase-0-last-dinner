package model.Greenhouse;

import lombok.Getter;
import model.Plant;

@Getter
public class GreenhouseBoostService {
    private final Greenhouse greenhouse;

    public GreenhouseBoostService(Greenhouse greenhouse) {
        if(greenhouse == null)
            throw new IllegalArgumentException("Greenhouse must not be null");
        this.greenhouse = greenhouse;
    }

    public boolean castBoost(Plant plant){
        if(plant == null)
            return false;
        else if(plant.getName().isEmpty())
            return false;
        else if(plant.getPosition() == null)
            return false;
        else if(greenhouse.getBoard() == null)
            return false;
        else if(!plant.canReceivePlantFood())
            return false;
        else if(!greenhouse.consumeStoredBoost(plant.getName()))
            return false;

        plant.receivePlantFood();
        return true;
    }
}
