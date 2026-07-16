package view.greenhouse;

import model.Greenhouse.GreenhouseActionResult;
import model.Greenhouse.GreenhouseStateResult;
import model.mechanism.Position;
import view.CommandHandler;

public interface GreenhouseViewObserver {

    GreenhouseStateResult onShowGreenhouseRequested();

    GreenhouseActionResult onPlantPotRequested(
            Position position
    );

    GreenhouseActionResult onCollectRequested(
            Position position
    );

    GreenhouseActionResult onGrowRequested(
            Position position
    );
}