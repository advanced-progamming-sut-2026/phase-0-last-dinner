package model.plant;

import model.Plant;
import model.User.User;

import java.util.ArrayList;

public final class PlantUnlockService {
    private PlantUnlockService() {
    }

    public static boolean unlock(User user, Plant plant) {
        if (user == null || plant == null || plant.getName() == null
                || plant.getName().trim().isEmpty()) {
            return false;
        }
        if (user.getUnlockedPlants() == null) {
            user.setUnlockedPlants(new ArrayList<Plant>());
        }
        for (Plant unlockedPlant : user.getUnlockedPlants()) {
            if (unlockedPlant != null && unlockedPlant.getName() != null
                    && unlockedPlant.getName().equalsIgnoreCase(plant.getName().trim())) {
                return false;
            }
        }
        user.getUnlockedPlants().add(plant);
        return true;
    }
}
