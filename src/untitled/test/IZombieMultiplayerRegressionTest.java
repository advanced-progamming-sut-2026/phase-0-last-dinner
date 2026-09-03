import model.minigame.izombieminigame.multiplayer.PlantZombieIZombieMultiplayerIntegration;
import model.zombie.ArmorType;
import model.zombie.ZombieDefinition;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IZombieMultiplayerRegressionTest {
    @Test
    public void firstStageUsesTheBundledZombieAliasesIncludingBrickArmor() {
        PlantZombieIZombieMultiplayerIntegration integration = new PlantZombieIZombieMultiplayerIntegration();
        List<ZombieDefinition> zombies = integration.chooseAvailableZombies(1);

        assertEquals(List.of("ZombieDefault", "ZombieArmor1", "ZombieArmor2", "ZombieImp", "ZombieArmor4"),
            zombies.stream().map(ZombieDefinition::getAlias).toList());
        assertTrue(zombies.get(4).getArmorDefinitions().stream()
            .anyMatch(armor -> armor.getType() == ArmorType.BRICK));
    }
}
