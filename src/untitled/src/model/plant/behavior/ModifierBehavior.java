package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantCategory;
import model.plant.PlantTag;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.plant.ProjectileType;
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
    private PlantCategory familyCategory;
    private PlantTag familyTag;
    private long projectileBoostTicks;

    public ModifierBehavior(
            ModifierType modifierType,
            ActivationTrigger activationTrigger,
            String effectDescription,
            int effectRadius,
            long durationTicks
    ) {
        this(modifierType, activationTrigger, effectDescription, effectRadius, durationTicks, null, null);
    }

    public ModifierBehavior(
            ModifierType modifierType,
            ActivationTrigger activationTrigger,
            String effectDescription,
            int effectRadius,
            long durationTicks,
            PlantCategory familyCategory,
            PlantTag familyTag
    ) {
        this.modifierType = modifierType;
        this.activationTrigger = activationTrigger;
        this.effectDescription = effectDescription;
        this.effectRadius = effectRadius;
        this.durationTicks = durationTicks;
        this.familyCategory = familyCategory;
        this.familyTag = familyTag;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        if (this.activationTrigger == ActivationTrigger.PASSIVE && !this.active) {
            this.activate(plant, board);
        }

        if (this.activationTrigger == ActivationTrigger.ON_CONTACT && plant != null && board != null
                && !board.getZombiesAt(plant.getPosition()).isEmpty()) {
            this.activate(plant, board);
        }

        if (!this.active) {
            return;
        }

        if (this.modifierType == ModifierType.PROJECTILE_TRANSFORM) {
            this.transformPassingProjectiles(plant, board);

            if (this.projectileBoostTicks > 0) {
                this.projectileBoostTicks--;
            }
        }

        this.activeTicks++;

        if (this.durationTicks > 0 && this.activeTicks >= this.durationTicks) {
            this.active = false;
            this.activeTicks = 0;

            if (this.modifierType == ModifierType.FAMILY_BUFF && board != null) {
                board.removePlant(plant);
            }
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

    @Override
    public void onDamaged(Plant plant, Board board, int damage) {
        if (damage > 0 && this.activationTrigger == ActivationTrigger.ON_CONTACT) {
            this.activate(plant, board);
        }
    }

    private void hypnotizeContactZombies(Plant plant, Board board) {
        List<Zombie> zombies = board.getZombiesAt(plant.getPosition());

        if (zombies.isEmpty() && plant.getPosition() != null) {
            for (Zombie zombie : board.getZombiesInLane(plant.getPosition())) {
                if (zombie != null && !zombie.isDead() && zombie.getPosition() != null
                        && Math.abs(zombie.getPosition().getX() - plant.getPosition().getX()) <= 1) {
                    zombies.add(zombie);
                    break;
                }
            }
        }

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
            if (nearbyPlant != null && nearbyPlant != plant && this.matchesFamily(nearbyPlant)) {
                nearbyPlant.receivePlantFood();
            }
        }
    }

    @Override
    public PlantBehavior copy() {
        ModifierBehavior copy = new ModifierBehavior(
                this.modifierType,
                this.activationTrigger,
                this.effectDescription,
                this.effectRadius,
                this.durationTicks,
                this.familyCategory,
                this.familyTag
        );
        copy.hypnotizedZombieUpgradeEffects.addAll(this.hypnotizedZombieUpgradeEffects);
        copy.projectileBoostTicks = this.projectileBoostTicks;
        return copy;
    }

    public void boostProjectilesFor(long ticks) {
        this.projectileBoostTicks = Math.max(this.projectileBoostTicks, ticks);
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

    private boolean matchesFamily(Plant plant) {
        if (this.familyCategory != null
                && (plant.getCategories() == null || !plant.getCategories().contains(this.familyCategory))) {
            return false;
        }

        return this.familyTag == null
                || (plant.getTags() != null && plant.getTags().contains(this.familyTag));
    }

    public boolean isSameFamily(Plant plant) {
        return plant != null && this.matchesFamily(plant);
    }

    private void transformPassingProjectiles(Plant plant, Board board) {
        if (plant == null || plant.getPosition() == null || board == null) {
            return;
        }

        for (Projectile projectile : board.getProjectiles()) {
            if (projectile == null || projectile.getPosition() == null
                    || projectile.getType() != ProjectileType.NORMAL
                    || projectile.isLobbed() || projectile.getHorizontalDirection() <= 0) {
                continue;
            }

            if (projectile.getPosition().getY() == plant.getPosition().getY()
                    && Math.abs(projectile.getPosition().getX() - plant.getPosition().getX()) <= 0.5) {
                projectile.setDamageExpression(DamageExpressionParser.multiplyDamage(
                        projectile.getDamageExpression(),
                        this.projectileBoostTicks > 0 ? 3.0 : 2.0
                ));

                projectile.setType(ProjectileType.FIRE);
            }
        }
    }
}
