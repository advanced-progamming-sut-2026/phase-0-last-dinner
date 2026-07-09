package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.ArrayList;
import java.util.List;

public class ModifierBehavior implements PlantBehavior, OnPlantingBehavior {
    private ModifierType modifierType;
    private ActivationTrigger activationTrigger;
    private String effectDescription;
    private int effectRadius;
    private boolean active;
    private long durationTicks;
    private long activeTicks;
    private List<PlantUpgradeEffect> hypnotizedZombieUpgradeEffects = new ArrayList<>();

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

        if (plant == null || board == null) {
            return;
        }

        if (this.modifierType == ModifierType.CONTACT_HYPNOSIS) {
            this.hypnotizeContactZombies(plant, board);
        } else if (this.modifierType == ModifierType.FAMILY_BUFF) {
            this.buffNearbyPlants(plant, board);
        }
    }

    @Override
    public boolean shouldActivateOnPlanting() {
        return this.activationTrigger == ActivationTrigger.ON_PLANTING;
    }

    private void hypnotizeContactZombies(Plant plant, Board board) {
        List<Zombie> zombies = board.getZombiesAt(plant.getPosition());

        for (Zombie zombie : zombies) {
            if (zombie != null) {
                zombie.addCondition(ZombieCondition.HYPNOTIZED);
                this.applyHypnotizedZombieUpgrades(zombie);
            }
        }

        if (!zombies.isEmpty()) {
            board.removePlant(plant);
        }
    }

    private void buffNearbyPlants(Plant plant, Board board) {
        if (plant.getPosition() == null) {
            return;
        }

        for (Plant nearbyPlant : board.getPlantsInRadius(plant.getPosition(), this.effectRadius)) {
            if (nearbyPlant != null && nearbyPlant != plant) {
                nearbyPlant.addBonusHealth(50);
            }
        }
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        this.effectRadius += effect.getRangeBonus();
        this.durationTicks += effect.getDurationBonusTicks();

        if (effect.hasSpecialEffect(PlantUpgradeSpecialEffect.ZOMBIE_HEALTH_BUFF)
                || effect.hasSpecialEffect(PlantUpgradeSpecialEffect.ZOMBIE_DAMAGE_BUFF)) {
            this.hypnotizedZombieUpgradeEffects.add(effect);
        }
    }

    private void applyHypnotizedZombieUpgrades(Zombie zombie) {
        if (zombie == null || this.hypnotizedZombieUpgradeEffects.isEmpty()) {
            return;
        }

        for (PlantUpgradeEffect effect : this.hypnotizedZombieUpgradeEffects) {
            if (effect.hasSpecialEffect(PlantUpgradeSpecialEffect.ZOMBIE_HEALTH_BUFF)) {
                zombie.addHealth(200);
            }

            if (zombie.getBehavior() != null) {
                zombie.getBehavior().applyPlantUpgrade(effect);
            }
        }
    }
}
