package view.collection;

import lombok.Getter;
import lombok.Setter;
import model.collection.CollectionActionResult;
import model.collection.CollectionStateResult;
import model.collection.PlantCollectionState;
import model.collection.ZombieCollectionState;
import model.zombie.ConditionResistance;
import model.zombie.ZombieArmorDefinition;
import view.CommandHandler;

import java.util.regex.Matcher;

@Getter
@Setter
public class CollectionView implements CommandHandler {
    private CollectionViewObserver observer;

    @Override
    public void handleCommand(String input) {
        if (this.observer == null) {
            System.out.println("Collection controller is not connected.");
            return;
        }

        Matcher matcher;

        matcher = CollectionCommands.SHOW_PLANTS.getMatcher(input);

        if (matcher != null) {
            this.showUnlockedPlants();
            return;
        }

        matcher = CollectionCommands.SHOW_ALL_PLANTS.getMatcher(input);

        if (matcher != null) {
            this.showAllPlants();
            return;
        }

        matcher = CollectionCommands.SHOW_ZOMBIES.getMatcher(input);

        if (matcher != null) {
            this.showEncounteredZombies();
            return;
        }

        matcher = CollectionCommands.SHOW_ALL_ZOMBIES.getMatcher(input);

        if (matcher != null) {
            this.showAllZombies();
            return;
        }

        matcher = CollectionCommands.SHOW_PLANT.getMatcher(input);

        if (matcher != null) {
            this.showPlant(this.cleanName(matcher.group("plantName")));
            return;
        }

        matcher = CollectionCommands.SHOW_ZOMBIE.getMatcher(input);

        if (matcher != null) {
            this.showZombie(this.cleanName(matcher.group("zombieName")));
            return;
        }

        matcher = CollectionCommands.UPGRADE_PLANT.getMatcher(input);

        if (matcher != null) {
            this.upgradePlant(this.cleanName(matcher.group("plantName")));
            return;
        }

        matcher = CollectionCommands.PURCHASE_PLANT.getMatcher(input);

        if (matcher != null) {
            this.purchasePlant(this.cleanName(matcher.group("plantName")));
            return;
        }

        System.out.println("Invalid collection command.");
    }

    private void showUnlockedPlants() {
        CollectionStateResult result = this.observer.onShowUnlockedPlantsRequested();

        this.printStateResult(result);
    }

    private void showAllPlants() {
        CollectionStateResult result = this.observer.onShowAllPlantsRequested();

        this.printStateResult(result);
    }

    private void showEncounteredZombies() {
        CollectionStateResult result = this.observer.onShowEncounteredZombiesRequested();

        this.printStateResult(result);
    }

    private void showAllZombies() {
        CollectionStateResult result = this.observer.onShowAllZombiesRequested();

        this.printStateResult(result);
    }

    private void showPlant(String plantName) {
        CollectionStateResult result = this.observer.onShowPlantRequested(plantName);

        this.printStateResult(result);
    }

    private void showZombie(String zombieName) {
        CollectionStateResult result = this.observer.onShowZombieRequested(zombieName);

        this.printStateResult(result);
    }

    private void upgradePlant(String plantName) {
        CollectionActionResult result = this.observer.onUpgradePlantRequested(plantName);

        this.printActionResult(result);
    }

    private void purchasePlant(String plantName) {
        CollectionActionResult result = this.observer.onPurchasePlantRequested(plantName);

        this.printActionResult(result);
    }

    private void printStateResult(CollectionStateResult result) {
        if (result == null) {
            System.out.println("Collection information is not available.");
            return;
        }

        if (!result.isSuccessful()) {
            System.out.println(result.getMessage());
            System.out.println("Coins: " + result.getGold());
            return;
        }

        if (!result.getPlants().isEmpty()) {
            System.out.println("Plants");
            System.out.println("Coins: " + result.getGold());
            System.out.println("----------------------------------------");

            for (PlantCollectionState state : result.getPlants()) {
                this.printPlantState(state);
            }

            return;
        }

        if (!result.getZombies().isEmpty()) {
            System.out.println("Zombies");
            System.out.println("----------------------------------------");

            for (ZombieCollectionState state : result.getZombies()) {
                this.printZombieState(state);
            }
            return;
        }

        System.out.println(result.getMessage());
        System.out.println("No collection item was found.");
    }

    private void printPlantState(PlantCollectionState state) {
        if (state == null)
            return;

        System.out.println("Name: " + state.getName());
        System.out.println("Status: " + (state.isUnlocked() ? "unlocked" : "locked"));

        System.out.println(
                "Level: " + state.getCurrentLevel()
                        + "/" + state.getMaximumLevel()
        );

        System.out.println("Seed packets: " + state.getSeedPackets());

        if (state.getCurrentLevel() >= state.getMaximumLevel()) {

            System.out.println("Next upgrade: maximum level reached");
        } else {
            System.out.println("Required seed packets: " + state.getRequiredSeedPackets());

            System.out.println("Required coins: " + state.getRequiredCoins());
        }

        System.out.println("Sun cost: " + state.getSunCost());

        System.out.println("Base health: " + state.getBaseHealth());

        System.out.println(
                "Damage: "
                        + this.displayText(
                        state.getDamageExpression()
                )
        );

        System.out.println(
                "Action interval: " + state.getActionIntervalSeconds() + " second(s)"
        );

        System.out.println(
                "Recharge: " + state.getRechargeSeconds() + " second(s)"
        );

        System.out.println(
                "Categories: " + state.getCategories()
        );

        System.out.println(
                "Tags: " + state.getTags()
        );

        System.out.println(
                "Ability: " + this.displayText(state.getBaseAbilityDescription())
        );

        System.out.println(
                "Plant Food: " + this.displayText(state.getPlantFoodEffectDescription()
                )
        );

        if (state.getLevelUpEffects().isEmpty())
            System.out.println("Level-up effects: none");
        else {
            System.out.println("Level-up effects:");

            for (int i = 0; i < state.getLevelUpEffects().size(); i++) {
                System.out.println(
                        "- Level "
                                + (i + 2)
                                + ": "
                                + state.getLevelUpEffects()
                                .get(i)
                );
            }
        }

        System.out.println("----------------------------------------");
    }

    private void printZombieState(ZombieCollectionState state) {
        if (state == null)
            return;


        System.out.println(
                "Name: " + this.displayText(state.getDisplayName())
        );

        System.out.println(
                "Alias: " + this.displayText(state.getAlias())
        );

        System.out.println(
                "Status: " + (state.isEncountered() ? "encountered" : "not encountered")
        );

        System.out.println(
                "Description: " + this.displayText(state.getDescription())
        );

        System.out.println(
                "Type: " + state.getType()
        );

        System.out.println(
                "Chapter: " + state.getChapter()
        );

        System.out.println(
                "Hitpoints: " + state.getHitpoints()
        );

        System.out.println(
                "Eat damage per second: " + state.getEatDamagePerSecond()
        );

        System.out.println(
                "Speed: " + state.getSpeed()
        );

        System.out.println(
                "Wave point cost: " + state.getWavePointCost()
        );

        System.out.println(
                "Weight: " + state.getWeight()
        );

        System.out.println(
                "Can spawn Plant Food: " + state.isCanSpawnPlantFood()
        );

        this.printZombieArmor(state);
        this.printConditionResistances(state);

        System.out.println("----------------------------------------");
    }

    private void printZombieArmor(ZombieCollectionState state) {
        if (state.getArmorDefinitions().isEmpty()) {
            System.out.println("Armor: none");
            return;
        }

        System.out.println("Armor:");

        for (ZombieArmorDefinition armor : state.getArmorDefinitions()) {

            if (armor == null)
                continue;


            System.out.println(
                    "- "
                            + armor.getAlias()
                            + " | type: "
                            + armor.getType()
                            + " | health: "
                            + armor.getBaseHealth()
                            + " | flags: "
                            + armor.getFlags()
            );
        }
    }

    private void printConditionResistances(ZombieCollectionState state) {
        if (state.getConditionResistances().isEmpty()) {
            System.out.println("Condition resistances: none");
            return;
        }

        System.out.println("Condition resistances:");

        for (ConditionResistance resistance : state.getConditionResistances()) {

            if (resistance == null)
                continue;


            System.out.println(
                    "- "
                            + resistance.getCondition()
                            + " | resistance: "
                            + resistance.getResistancePercent()
                            + "%"
                            + " | immune: "
                            + resistance.isImmune()
            );
        }
    }

    private void printActionResult(CollectionActionResult result) {
        if (result == null) {
            System.out.println("Collection action failed.");
            return;
        }

        System.out.println(result.getMessage());

        if (result.getPlantName() != null) {
            System.out.println(
                    "Plant: " + result.getPlantName()
            );
        }

        if (result.isSuccessful()) {
            System.out.println(
                    "Level: "
                            + result.getPreviousLevel()
                            + " -> "
                            + result.getCurrentLevel()
            );

            System.out.println(
                    "Remaining seed packets: " + result.getRemainingSeedPackets()
            );

            System.out.println(
                    "Spent coins: " + result.getSpentCoins()
            );
        }

        System.out.println(
                "Coins: " + result.getRemainingGold()
        );
    }

    private String cleanName(String value) {
        if (value == null) {
            return null;
        }

        String cleanValue = value.trim();

        if (cleanValue.length() >= 2) {
            boolean doubleQuoted = cleanValue.startsWith("\"") && cleanValue.endsWith("\"");

            boolean singleQuoted = cleanValue.startsWith("'") && cleanValue.endsWith("'");

            if (doubleQuoted || singleQuoted) {
                cleanValue = cleanValue.substring(
                        1,
                        cleanValue.length() - 1
                );
            }
        }

        return cleanValue.trim();
    }

    private String displayText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "none";
        }

        return value;
    }
}