package controller;

import model.Menu.MenuType;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantUpgradeResult;
import model.plant.PlantUpgradeService;

public class CollectionController implements MenuController {
    private final PlantDefinitionRepository plantDefinitions;
    // hamin service dar game ham estefade mishe ta upgrade daemi bemune
    private final PlantUpgradeService plantUpgrades;

    public CollectionController() {
        this(null, new PlantUpgradeService());
    }

    public CollectionController(
            PlantDefinitionRepository plantDefinitions,
            PlantUpgradeService plantUpgrades
    ) {
        if (plantUpgrades == null) {
            throw new IllegalArgumentException("Plant upgrade service is required");
        }

        this.plantDefinitions = plantDefinitions;
        this.plantUpgrades = plantUpgrades;
    }

    @Override
    public MenuType getCurrentMenu() {
        return MenuType.COLLECTION_MENU;
    }

    @Override
    public void changeMenu() {
    }

    public void showUnlockedPlants() {
    }

    public void showAllPlants() {
    }

    public void showUnlockedZombies() {
    }

    public void showAllZombies() {
    }

    public void showSpecificPlant() {
    }

    public void showSpecificZombie() {
    }

    public PlantUpgradeResult upgradePlant(String plantName) {
        if (this.plantDefinitions == null) {
            return PlantUpgradeResult.PLANT_NOT_FOUND;
        }

        PlantDefinition definition = this.plantDefinitions.findByName(plantName);
        return this.plantUpgrades.upgrade(definition);
    }

    public void upgradePlant() {
    }

    public PlantUpgradeService getPlantUpgradeService() {
        return this.plantUpgrades;
    }

    public void buyPlant() {
    }
}
