package model.zombie;

import model.mechanism.Position;
import model.zombie.behavior.ZombieBehavior;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ZombieFactory {
    private ZombieBehaviorFactory behaviorFactory;
    private ZombieArmorFactory armorFactory;

    public ZombieFactory() {
        this(new ZombieBehaviorFactory(), new ZombieArmorFactory());
    }

    public ZombieFactory(ZombieDefinitionRepository definitionRepository) {
        this(new ZombieBehaviorFactory(definitionRepository), new ZombieArmorFactory());
    }

    public ZombieFactory(ZombieBehaviorFactory behaviorFactory, ZombieArmorFactory armorFactory) {
        this.behaviorFactory = behaviorFactory;
        this.armorFactory = armorFactory;

        if (this.behaviorFactory != null) {
            this.behaviorFactory.setZombieFactory(this);
        }
    }

    public Zombie create(ZombieDefinition definition, Position spawnPosition) {
        if (definition == null) {
            return null;
        }

        ZombieBehavior behavior = this.behaviorFactory == null
                ? new ZombieBehaviorFactory().create(definition)
                : this.behaviorFactory.create(definition);

        List<ZombieArmor> armors = new ArrayList<>();

        List<ZombieArmorDefinition> armorDefinitions = definition.getArmorDefinitions();

        if (armorDefinitions == null || armorDefinitions.isEmpty()) {
            armorDefinitions = this.inferArmorDefinitions(definition);
        }

        if (armorDefinitions != null) {
            ZombieArmorFactory factory = this.armorFactory == null ? new ZombieArmorFactory() : this.armorFactory;

            for (ZombieArmorDefinition armorDefinition : armorDefinitions) {
                armors.add(factory.create(armorDefinition));
            }
        }

        return new Zombie(
                definition,
                spawnPosition,
                definition.getHitpoints(),
                definition.getSpeed(),
                armors,
                new ArrayList<ZombieCondition>(),
                behavior
        );
    }

    private List<ZombieArmorDefinition> inferArmorDefinitions(ZombieDefinition definition) {
        List<ZombieArmorDefinition> armorDefinitions = new ArrayList<>();

        if (definition == null || definition.getAlias() == null) {
            return armorDefinitions;
        }

        String alias = definition.getAlias().toLowerCase(java.util.Locale.ROOT);

        if (alias.contains("armor1")) {
            armorDefinitions.add(new ZombieArmorDefinition("ConeDefault", ArmorType.CONE, 370,
                    EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.DROPPABLE, ArmorFlag.HELMET)));
        } else if (alias.contains("armor2")) {
            armorDefinitions.add(new ZombieArmorDefinition("BucketDefault", ArmorType.BUCKET, 1100,
                    EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.DROPPABLE, ArmorFlag.METALLIC, ArmorFlag.HELMET)));
        } else if (alias.contains("armor4")) {
            armorDefinitions.add(new ZombieArmorDefinition("BrickDefault", ArmorType.BRICK, 2200,
                    EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.DROPPABLE, ArmorFlag.HELMET)));
        } else if (alias.contains("iceagearmor3")) {
            armorDefinitions.add(new ZombieArmorDefinition("IceBlockDefault", ArmorType.ICE_BLOCK, 1200,
                    EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.DROPPABLE)));
        } else if (alias.contains("darkarmor3")) {
            armorDefinitions.add(new ZombieArmorDefinition("ShoulderArmorDefault", ArmorType.SHOULDER_ARMOR, 1600,
                    EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.PASS_DAMAGE)));
            armorDefinitions.add(new ZombieArmorDefinition("CrownDefault", ArmorType.CROWN, 1600,
                    EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.DROPPABLE, ArmorFlag.METALLIC, ArmorFlag.HELMET)));
        } else if (alias.contains("pharaoh")) {
            armorDefinitions.add(new ZombieArmorDefinition("SarcophagusDefault", ArmorType.SARCOPHAGUS, 2200,
                    EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.DROPPABLE)));
        } else if (alias.contains("surfer")) {
            armorDefinitions.add(new ZombieArmorDefinition("SurfboardDefault", ArmorType.SURFBOARD, 1200,
                    EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.DROPPABLE)));
        }

        return armorDefinitions;
    }

}
