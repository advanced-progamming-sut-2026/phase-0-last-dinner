package model.zombie;

import model.plant.ProjectileType;
import model.zombie.behavior.AmphibiousBehavior;
import model.zombie.behavior.BarrelRollerBehavior;
import model.zombie.behavior.BasicZombieBehavior;
import model.zombie.behavior.BlockPusherBehavior;
import model.zombie.behavior.ChargingZombieBehavior;
import model.zombie.behavior.ColdResistantBehavior;
import model.zombie.behavior.CompositeZombieBehavior;
import model.zombie.behavior.CrystalSkullBehavior;
import model.zombie.behavior.FishermanBehavior;
import model.zombie.behavior.FlyingBehavior;
import model.zombie.behavior.GargantuarBehavior;
import model.zombie.behavior.GraveSummonerBehavior;
import model.zombie.behavior.KingBehavior;
import model.zombie.behavior.OctopusThrowerBehavior;
import model.zombie.behavior.PhaseChangeBehavior;
import model.zombie.behavior.PianoBehavior;
import model.zombie.behavior.ProjectileReflectorBehavior;
import model.zombie.behavior.ProjectileShieldBehavior;
import model.zombie.behavior.ProspectorBehavior;
import model.zombie.behavior.SnowballBehavior;
import model.zombie.behavior.SunStealerBehavior;
import model.zombie.behavior.TorchBearerBehavior;
import model.zombie.behavior.WizardBehavior;
import model.zombie.behavior.ZombieBehavior;

import java.util.ArrayList;
import java.util.Locale;

public class ZombieBehaviorFactory {
    private ZombieDefinitionRepository definitionRepository;
    private ZombieFactory zombieFactory;

    public ZombieBehaviorFactory() {
    }

    public ZombieBehaviorFactory(ZombieDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    public void setZombieFactory(ZombieFactory zombieFactory) {
        this.zombieFactory = zombieFactory;
    }

    public ZombieBehavior create(ZombieDefinition definition) {
        if (definition == null) {
            return new BasicZombieBehavior();
        }

        CompositeZombieBehavior behavior = new CompositeZombieBehavior();
        String alias = normalize(definition.getAlias());

        switch (alias) {
            case "zombiegargantuar":
                behavior.addBehavior(new GargantuarBehavior(
                        this.findImpDefinition(definition),
                        this.getZombieFactory(),
                        0.5
                ));
                break;
            case "zombiera":
                behavior.addBehavior(new SunStealerBehavior(25, 10, null));
                break;
            case "zombieexplorer":
                behavior.addBehavior(new TorchBearerBehavior());
                break;
            case "zombietombraiser":
                behavior.addBehavior(new GraveSummonerBehavior(2, 60));
                break;
            case "zombieiceagedodo":
                behavior.addBehavior(new ColdResistantBehavior());
                behavior.addBehavior(new FlyingBehavior(true));
                break;
            case "zombieiceagehunter":
                behavior.addBehavior(new ColdResistantBehavior());
                behavior.addBehavior(new SnowballBehavior(4, 10));
                break;
            case "zombieiceagetroglobite":
                behavior.addBehavior(new ColdResistantBehavior());
                behavior.addBehavior(new BlockPusherBehavior("Ice Block", 1200, 3));
                break;
            case "zombiebeachfisherman":
                behavior.addBehavior(new FishermanBehavior(25));
                break;
            case "zombiebeachoctopus":
                behavior.addBehavior(new OctopusThrowerBehavior(30));
                break;
            case "zombiebeachsnorkel":
                behavior.addBehavior(new AmphibiousBehavior(
                        definition.getSpeed(),
                        definition.getSpeed(),
                        false
                ));
                break;
            case "zombiedarkjuggler":
                behavior.addBehavior(new ProjectileReflectorBehavior(false));
                break;
            case "zombiewizard":
                behavior.addBehavior(new WizardBehavior(30));
                break;
            case "zombiedarkking":
                behavior.addBehavior(new KingBehavior(25));
                break;
            case "zombiedarkimpdragon":
                behavior.addBehavior(new ProjectileShieldBehavior(ProjectileType.FIRE));
                break;
            case "zombiemodernallstar":
                behavior.addBehavior(new ChargingZombieBehavior(2.0, 0.5));
                break;
            case "zombielostcityjane":
                behavior.addBehavior(new ProjectileShieldBehavior(ProjectileType.LOBBED));
                break;
            case "zombiecrystalskull":
                behavior.addBehavior(new CrystalSkullBehavior(4, 50, 50));
                break;
            case "zombieprospector":
                behavior.addBehavior(new ProspectorBehavior(100));
                break;
            case "zombiepiano":
                behavior.addBehavior(new PianoBehavior(30));
                break;
            case "zombienewspaper":
                behavior.addBehavior(new PhaseChangeBehavior(ArmorType.NEWSPAPER, 4.0, 4.0));
                break;
            case "zombiearcade":
                behavior.addBehavior(new BlockPusherBehavior("Arcade Machine", 1100, 1));
                break;
            case "zombiebarrelroller":
                behavior.addBehavior(new BarrelRollerBehavior(
                        this.findImpDefinition(definition),
                        this.getZombieFactory()
                ));
                break;
            default:
                break;
        }

        behavior.addBehavior(new BasicZombieBehavior(definition.getEatDamagePerSecond()));
        return behavior;
    }

    private ZombieDefinition findImpDefinition(ZombieDefinition gargantuarDefinition) {
        if (this.definitionRepository != null) {
            ZombieDefinition definition = this.definitionRepository.findByAlias("ZombieImp");
            if (definition != null) {
                return definition;
            }
        }

        ZombieChapter chapter = gargantuarDefinition == null
                ? ZombieChapter.ALL_CHAPTERS
                : gargantuarDefinition.getChapter();
        return new ZombieDefinition(
                "ZombieImp",
                "Imp",
                "Small and fast zombie thrown by Gargantuar",
                ZombieType.IMP,
                chapter == null ? ZombieChapter.ALL_CHAPTERS : chapter,
                190,
                100,
                0.22,
                100,
                1000,
                false,
                new ArrayList<ZombieArmorDefinition>(),
                new ArrayList<ConditionResistance>()
        );
    }

    private ZombieFactory getZombieFactory() {
        // factory fallback lazeme ta zombie spawn shode behavior va armor kamel begire
        if (this.zombieFactory == null) {
            this.zombieFactory = new ZombieFactory(
                    new ZombieBehaviorFactory(this.definitionRepository),
                    new ZombieArmorFactory()
            );
        }
        return this.zombieFactory;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
