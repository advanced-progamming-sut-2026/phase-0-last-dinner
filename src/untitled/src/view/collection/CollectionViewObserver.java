package view.collection;

import model.collection.CollectionActionResult;
import model.collection.CollectionStateResult;

public interface CollectionViewObserver {
    CollectionStateResult onShowUnlockedPlantsRequested();

    CollectionStateResult onShowAllPlantsRequested();

    CollectionStateResult onShowEncounteredZombiesRequested();

    CollectionStateResult onShowAllZombiesRequested();

    CollectionStateResult onShowPlantRequested(String plantName);

    CollectionStateResult onShowZombieRequested(String zombieName);

    CollectionActionResult onUpgradePlantRequested(String plantName);

    CollectionActionResult onPurchasePlantRequested(String plantName);
}