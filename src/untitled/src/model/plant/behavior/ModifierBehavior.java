package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;

public class ModifierBehavior implements PlantBehavior, OnPlantingBehavior {
    private ModifierType modifierType;
    private ActivationTrigger activationTrigger;
    private String effectDescription;
    private int effectRadius;
    private boolean active;
    private long durationTicks;
    private long activeTicks;

    public ModifierBehavior(
            ModifierType modifierType,
            ActivationTrigger activationTrigger,
            String effectDescription,
            int effectRadius,
            long durationTicks
    ) {
        this.modifierType = modifierType;
        this.activationTrigger = activationTrigger;
        this.effectDescription = effectDescription;
        this.effectRadius = effectRadius;
        this.durationTicks = durationTicks;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        if (this.activationTrigger == ActivationTrigger.PASSIVE && !this.active) {
            this.activate(plant, board);
        }

        if (!this.active) {
            return;
        }

        this.activeTicks++;

        if (this.durationTicks > 0 && this.activeTicks >= this.durationTicks) {
            this.active = false;
            this.activeTicks = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        this.active = true;
        this.activeTicks = 0;
    }

    @Override
    public boolean shouldActivateOnPlanting() {
        return this.activationTrigger == ActivationTrigger.ON_PLANTING;
    }
}
