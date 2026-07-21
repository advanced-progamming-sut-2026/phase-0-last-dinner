package view;

import java.util.List;

public interface PlantPickViewObserver {
    List<String> onShowAllPlantsRequested();

    List<String> onShowAvailablePlantsRequested();

    String onAddPlantRequested(String plantName);

    String onRemovePlantRequested(String plantName);

    String onBoostPlantRequested(String plantName);

    String onStartGameRequested();
}
