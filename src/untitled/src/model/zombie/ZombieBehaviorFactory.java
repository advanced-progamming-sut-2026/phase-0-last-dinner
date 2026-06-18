package model.zombie;

import model.zombie.behavior.AmphibiousBehavior;
import model.zombie.behavior.AuraBuffBehavior;
import model.zombie.behavior.BasicZombieBehavior;
import model.zombie.behavior.BlockPusherBehavior;
import model.zombie.behavior.BossBehavior;
import model.zombie.behavior.CompositeZombieBehavior;
import model.zombie.behavior.FlyingBehavior;
import model.zombie.behavior.GargantuarBehavior;
import model.zombie.behavior.GraveSummonerBehavior;
import model.zombie.behavior.InstantPlantDestroyerBehavior;
import model.zombie.behavior.PhaseChangeBehavior;
import model.zombie.behavior.ProjectileReflectorBehavior;
import model.zombie.behavior.RangedAbilityType;
import model.zombie.behavior.RangedZombieBehavior;
import model.zombie.behavior.SunStealerBehavior;
import model.zombie.behavior.UnitReleaserBehavior;
import model.zombie.behavior.ZombieBehavior;

import java.util.Locale;

public class ZombieBehaviorFactory {
    public ZombieBehavior create(ZombieDefinition definition) {
        if (definition == null) {
            return new BasicZombieBehavior();
        }

        CompositeZombieBehavior composite = new CompositeZombieBehavior();
        String alias = this.normalize(definition.getAlias());
        String key = this.normalize(definition.getAlias() + " " + definition.getDisplayName() + " " + definition.getDescription());

        composite.addBehavior(new BasicZombieBehavior(definition.getEatDamagePerSecond()));

        if (definition.getType() == ZombieType.BOSS || key.contains("boss") || key.contains("zombot")) {
            composite.addBehavior(new BossBehavior(new java.util.ArrayList<>()));
        }

        if (definition.getType() == ZombieType.GARGANTUAR || key.contains("gargantuar")) {
            composite.addBehavior(new GargantuarBehavior(null, null, 0.5));
        }

        if (definition.getType() == ZombieType.ANIMAL || key.contains("flying")
                || key.contains("balloon") || key.contains("seagull") || key.contains("parrot")
                || alias.contains("dodo")) {
            composite.addBehavior(new FlyingBehavior(true));
        }

        if (key.contains("water") || key.contains("aqua") || key.contains("surfer")
                || key.contains("snorkel") || key.contains("octo") || alias.contains("fastswimmer")) {
            composite.addBehavior(new AmphibiousBehavior(definition.getSpeed() * 1.2, definition.getSpeed(), false));
        }

        if (key.contains("jester") || key.contains("reflect") || alias.contains("juggler")) {
            composite.addBehavior(new ProjectileReflectorBehavior(true));
        }

        if (key.contains("wizard") || key.contains("mage") || key.contains("magic")) {
            composite.addBehavior(new RangedZombieBehavior(RangedAbilityType.MAGIC_TRANSFORM, 5, 30));
        }

        if (key.contains("fisher") || key.contains("hook")) {
            composite.addBehavior(new RangedZombieBehavior(RangedAbilityType.FISHING_HOOK, 5, 30));
        }

        if (key.contains("octo")) {
            composite.addBehavior(new RangedZombieBehavior(RangedAbilityType.OCTOPUS, 5, 30));
        }

        if (key.contains("snow") || key.contains("yeti") || alias.contains("hunter")) {
            composite.addBehavior(new RangedZombieBehavior(RangedAbilityType.SNOWBALL, 5, 30));
        }

        if (alias.contains("ra") || (key.contains("sun") && (key.contains("steal") || key.contains("thief") || key.contains("producer")))) {
            composite.addBehavior(new SunStealerBehavior(25, null));
        }

        if (key.contains("grave") || key.contains("tomb") || key.contains("summon") || key.contains("necrom")) {
            composite.addBehavior(new GraveSummonerBehavior(1, 50));
        }

        if (key.contains("king") || key.contains("queen") || key.contains("aura")) {
            composite.addBehavior(new AuraBuffBehavior(1.2, 1.0, 2));
        }

        if (key.contains("newspaper") || key.contains("phase") || alias.contains("pharaoh") || alias.contains("surfer")) {
            composite.addBehavior(new PhaseChangeBehavior(
                    new BasicZombieBehavior(definition.getEatDamagePerSecond()),
                    new BasicZombieBehavior(definition.getEatDamagePerSecond()),
                    0.5,
                    1.5
            ));
        }

        if (key.contains("block") || key.contains("push") || alias.contains("troglobite") || alias.contains("surfer")) {
            composite.addBehavior(new BlockPusherBehavior(1000));
        }

        if (key.contains("destroy") || key.contains("instant") || alias.contains("explorer")) {
            composite.addBehavior(new InstantPlantDestroyerBehavior(new java.util.HashSet<String>()));
        }

        if (alias.contains("camel")) {
            composite.addBehavior(new model.zombie.behavior.SegmentedGroupBehavior(new java.util.ArrayList<Zombie>()));
        }

        if (alias.contains("weaselhoarder")) {
            composite.addBehavior(new UnitReleaserBehavior(null, null, 4, 0.5));
        }

        return composite.isEmpty() ? new BasicZombieBehavior(definition.getEatDamagePerSecond()) : composite;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
