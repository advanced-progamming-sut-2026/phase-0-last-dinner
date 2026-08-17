package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import model.collection.PlantCollectionState;
import model.plant.PlantCategory;
import model.plant.PlantTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps project plant names to the original PvZ2 seed-packet resources present in
 * RESOURCES.json. Names are normalized so spelling punctuation/case differences in
 * the model do not leak into the graphical layer.
 */
public final class PlantPacketCatalog {
    private static final String EMPTY_PACKET = "IMAGE_UI_PACKETS_EMPTY_PACKET";

    private static final String FAMILY_SUN = "IMAGE_UI_PACKETS_MINTFAM_SUN";
    private static final String FAMILY_PEA = "IMAGE_UI_PACKETS_MINTFAM_PEASHOOTER";
    private static final String FAMILY_LOBBER = "IMAGE_UI_PACKETS_MINTFAM_LOBBER";
    private static final String FAMILY_EXPLOSIVE = "IMAGE_UI_PACKETS_MINTFAM_EXPLOSIVE";
    private static final String FAMILY_MELEE = "IMAGE_UI_PACKETS_MINTFAM_MELEE";
    private static final String FAMILY_DEFENSE = "IMAGE_UI_PACKETS_MINTFAM_DEFENSE";
    private static final String FAMILY_SHARP = "IMAGE_UI_PACKETS_MINTFAM_SHARP";
    private static final String FAMILY_TRAP = "IMAGE_UI_PACKETS_MINTFAM_TRAP";
    private static final String FAMILY_FIRE = "IMAGE_UI_PACKETS_MINTFAM_FIRE";
    private static final String FAMILY_COLD = "IMAGE_UI_PACKETS_MINTFAM_COLD";
    private static final String FAMILY_POISON = "IMAGE_UI_PACKETS_MINTFAM_POISON";
    private static final String FAMILY_MAGIC = "IMAGE_UI_PACKETS_MINTFAM_MAGIC";

    private static final Map<String, PacketVisual> PACKETS = createPackets();

    private PlantPacketCatalog() {
    }

    public static PacketVisual findPacket(String plantName) {
        return PACKETS.get(normalize(plantName));
    }

    public static FamilyVisual findFamily(PlantCollectionState state) {
        if (state == null) {
            return family(FAMILY_MAGIC);
        }

        FamilyVisual namedMintFamily = namedMintFamily(state.getName());
        if (namedMintFamily != null) {
            return namedMintFamily;
        }

        Set<PlantCategory> categories = safeCategories(state);
        Set<PlantTag> tags = safeTags(state);

        // elemental va special tag ha vaghti mint family asli hast olaviat daran
        if (tags.contains(PlantTag.FIRE)) {
            return family(FAMILY_FIRE);
        }
        if (tags.contains(PlantTag.ICE)) {
            return family(FAMILY_COLD);
        }
        if (tags.contains(PlantTag.POISON)) {
            return family(FAMILY_POISON);
        }
        if (tags.contains(PlantTag.TRAP)) {
            return family(FAMILY_TRAP);
        }
        if (categories.contains(PlantCategory.SUN_PRODUCER) || tags.contains(PlantTag.SUN)) {
            return family(FAMILY_SUN);
        }
        if (categories.contains(PlantCategory.LOBBER)) {
            return family(FAMILY_LOBBER);
        }
        if (categories.contains(PlantCategory.EXPLOSIVE)) {
            return family(FAMILY_EXPLOSIVE);
        }
        if (categories.contains(PlantCategory.MELEE_ATTACKER)) {
            return family(FAMILY_MELEE);
        }
        if (categories.contains(PlantCategory.DEFENDER)) {
            return family(FAMILY_DEFENSE);
        }
        if (categories.contains(PlantCategory.STRIKE_THROUGH)) {
            return family(FAMILY_SHARP);
        }
        if (categories.contains(PlantCategory.SHOOTER) && tags.contains(PlantTag.PEA)) {
            return family(FAMILY_PEA);
        }
        return family(FAMILY_MAGIC);
    }


    private static FamilyVisual namedMintFamily(String plantName) {
        String name = normalize(plantName);
        if (name.equals("enlightenmint")) return family(FAMILY_SUN);
        if (name.equals("appeasemint")) return family(FAMILY_PEA);
        if (name.equals("armamint")) return family(FAMILY_LOBBER);
        if (name.equals("bombardmint")) return family(FAMILY_EXPLOSIVE);
        if (name.equals("enforcemint")) return family(FAMILY_MELEE);
        if (name.equals("reinforcemint")) return family(FAMILY_DEFENSE);
        if (name.equals("piercemint") || name.equals("cattailmint")) return family(FAMILY_SHARP);
        if (name.equals("enchantmint")) return family(FAMILY_MAGIC);
        return null;
    }

    private static Map<String, PacketVisual> createPackets() {
        Map<String, PacketVisual> packets = new HashMap<>();
        addProducerAndShooterPackets(packets);
        addAttackAndExplosivePackets(packets);
        addDefenseAndUtilityPackets(packets);
        addMintPackets(packets);
        return Collections.unmodifiableMap(packets);
    }

    private static void addProducerAndShooterPackets(Map<String, PacketVisual> packets) {
        packet(packets, "Sunflower", "IMAGE_UI_PACKETS_SUNFLOWER");
        packet(packets, "Twin Sunflower", "IMAGE_UI_PACKETS_TWINSUNFLOWER");
        packet(packets, "Sun-shroom", "IMAGE_UI_PACKETS_SUNSHROOM");
        packet(packets, "Primal Sunflower", "IMAGE_UI_PACKETS_PRIMALSUNFLOWER");
        packet(packets, "Gold Bloom", "IMAGE_UI_PACKETS_GOLDBLOOM");
        packet(packets, "Peashooter", "IMAGE_UI_PACKETS_PEASHOOTER");
        packet(packets, "Repeater", "IMAGE_UI_PACKETS_REPEATER");
        packet(packets, "Threepeater", "IMAGE_UI_PACKETS_THREEPEATER");
        packet(packets, "Snow Pea", "IMAGE_UI_PACKETS_SNOWPEA");
        // rotobaga to resource haye asli pvz2 ba name xshot zakhire shode
        packet(packets, "Rotobaga", "IMAGE_UI_PACKETS_XSHOT");
        packet(packets, "Pea Pod", "IMAGE_UI_PACKETS_PEAPOD");
        packet(packets, "Split Pea", "IMAGE_UI_PACKETS_SPLITPEA");
        packet(packets, "Citron", "IMAGE_UI_PACKETS_CITRON");
        packet(packets, "Caulipower", "IMAGE_UI_PACKETS_CAULIPOWER");
        packet(packets, "Electric Blueberry", "IMAGE_UI_PACKETS_ELECTRICBLUEBERRY");
        packet(packets, "Bowling Bulb", "IMAGE_UI_PACKETS_BOWLINGBULB");
        packet(packets, "Cactus", "IMAGE_UI_PACKETS_CACTUS");
        packet(packets, "Fire Peashooter", "IMAGE_UI_PACKETS_FIREPEASHOOTER");
        packet(packets, "Starfruit", "IMAGE_UI_PACKETS_STARFRUIT");
        packet(packets, "Goo Peashooter", "IMAGE_UI_PACKETS_POISONPEASHOOTER");
        packet(packets, "Mega Gatling Pea", "IMAGE_UI_PACKETS_MEGAGATLING");
        packet(packets, "Sea-shroom", "IMAGE_UI_PACKETS_SEASHROOM");
        packet(packets, "Puff-shroom", "IMAGE_UI_PACKETS_PUFFSHROOM");
        packet(packets, "Fume-shroom", "IMAGE_UI_PACKETS_FUMESHROOM");
    }

    private static void addAttackAndExplosivePackets(Map<String, PacketVisual> packets) {
        packet(packets, "Cabbage-pult", "IMAGE_UI_PACKETS_CABBAGEPULT");
        packet(packets, "Kernel-pult", "IMAGE_UI_PACKETS_KERNELPULT");
        packet(packets, "Melon-pult", "IMAGE_UI_PACKETS_MELONPULT");
        packet(packets, "Winter Melon", "IMAGE_UI_PACKETS_WINTERMELON");
        packet(packets, "Pepper-pult", "IMAGE_UI_PACKETS_PEPPERPULT");
        packet(packets, "Potato Mine", "IMAGE_UI_PACKETS_POTATOMINE");
        packet(packets, "Primal Potato Mine", "IMAGE_UI_PACKETS_PRIMALPOTATOMINE");
        packet(packets, "Cherry Bomb", "IMAGE_UI_PACKETS_CHERRY_BOMB");
        packet(packets, "Squash", "IMAGE_UI_PACKETS_SQUASH");
        packet(packets, "Grapeshot", "IMAGE_UI_PACKETS_GRAPESHOT");
        packet(packets, "Jalapeno", "IMAGE_UI_PACKETS_JALAPENO");
        packet(packets, "Doom-shroom", "IMAGE_UI_PACKETS_DOOMSHROOM");
        packet(packets, "Tangle Kelp", "IMAGE_UI_PACKETS_TANGLEKELP");
        packet(packets, "Iceberg Lettuce", "IMAGE_UI_PACKETS_ICEBURG");
        packet(packets, "Bonk Choy", "IMAGE_UI_PACKETS_BONKCHOY");
        packet(packets, "Phat Beet", "IMAGE_UI_PACKETS_PHATBEET");
        packet(packets, "Chomper", "IMAGE_UI_PACKETS_CHOMPER");
        packet(packets, "Wasabi Whip", "IMAGE_UI_PACKETS_WASABIWHIP");
        packet(packets, "Kiwibeast", "IMAGE_UI_PACKETS_KIWIBEAST");
    }

    private static void addDefenseAndUtilityPackets(Map<String, PacketVisual> packets) {
        packet(packets, "Wall-nut", "IMAGE_UI_PACKETS_WALLNUT");
        packet(packets, "Tall-nut", "IMAGE_UI_PACKETS_TALLNUT");
        packet(packets, "Endurian", "IMAGE_UI_PACKETS_ENDURIAN");
        packet(packets, "Garlic", "IMAGE_UI_PACKETS_GARLIC");
        packet(packets, "Sweet Potato", "IMAGE_UI_PACKETS_SWEETPOTATO");
        packet(packets, "Explode-o-nut", "IMAGE_UI_PACKETS_EXPLODEONUT");
        packet(packets, "Pumpkin", "IMAGE_UI_PACKETS_PUMPKIN");
        packet(packets, "Sun Bean", "IMAGE_UI_PACKETS_SUNBEAN");
        packet(packets, "Torchwood", "IMAGE_UI_PACKETS_TORCHWOOD");
        packet(packets, "Magnet-shroom", "IMAGE_UI_PACKETS_MAGNETSHROOM");
        packet(packets, "Hypno-shroom", "IMAGE_UI_PACKETS_HYPNOSHROOM");
        // cattail/nekotail packet to resource haye feli peyda nashod; fallback amn mimone
        packet(packets, "Cat-tail", EMPTY_PACKET);
        packet(packets, "Imitater", "IMAGE_UI_PACKETS_IMITATER");
        packet(packets, "Ice-shroom", "IMAGE_UI_PACKETS_ICESHROOM");
        packet(packets, "Lily Pad", "IMAGE_UI_PACKETS_LILYPAD");
        packet(packets, "Hot Potato", "IMAGE_UI_PACKETS_HOTPOTATO");
        packet(packets, "Grave Buster", "IMAGE_UI_PACKETS_GRAVEBUSTER");
    }

    private static void addMintPackets(Map<String, PacketVisual> packets) {
        packet(packets, "Enlighten-mint", "IMAGE_UI_PACKETS_ENLIGHTENMINT");
        packet(packets, "Appease-mint", "IMAGE_UI_PACKETS_APPEASEMINT");
        packet(packets, "Arma-mint", "IMAGE_UI_PACKETS_ARMAMINT");
        packet(packets, "Bombard-mint", "IMAGE_UI_PACKETS_BOMBARDMINT");
        packet(packets, "Enforce-mint", "IMAGE_UI_PACKETS_ENFORCEMINT");
        packet(packets, "Reinforce-mint", "IMAGE_UI_PACKETS_REINFORCEMINT");
        packet(packets, "Enchant-mint", "IMAGE_UI_PACKETS_ENCHANTMINT");
        // alias haye project az spear-mint packet asli mojood estefade mikonan
        packet(packets, "Pierce-mint", "IMAGE_UI_PACKETS_SPEARMINT");
        packet(packets, "catTail-mint", "IMAGE_UI_PACKETS_SPEARMINT");
    }

    private static void packet(
            Map<String, PacketVisual> packets,
            String plantName,
            String resourceId
    ) {
        packets.put(normalize(plantName), new PacketVisual(resourceId));
    }

    private static FamilyVisual family(String glyphResourceId) {
        return new FamilyVisual(glyphResourceId, familyColor(glyphResourceId));
    }

    private static Color familyColor(String glyphResourceId) {
        if (FAMILY_SUN.equals(glyphResourceId)) return Color.valueOf("E7C63AFF");
        if (FAMILY_PEA.equals(glyphResourceId)) return Color.valueOf("8BCB35FF");
        if (FAMILY_LOBBER.equals(glyphResourceId)) return Color.valueOf("8C5A32FF");
        if (FAMILY_EXPLOSIVE.equals(glyphResourceId)) return Color.valueOf("E47C28FF");
        if (FAMILY_MELEE.equals(glyphResourceId)) return Color.valueOf("469A48FF");
        if (FAMILY_DEFENSE.equals(glyphResourceId)) return Color.valueOf("C39D68FF");
        if (FAMILY_SHARP.equals(glyphResourceId)) return Color.valueOf("555A61FF");
        if (FAMILY_TRAP.equals(glyphResourceId)) return Color.valueOf("B8C0C8FF");
        if (FAMILY_FIRE.equals(glyphResourceId)) return Color.valueOf("D94B36FF");
        if (FAMILY_COLD.equals(glyphResourceId)) return Color.valueOf("61B8E8FF");
        if (FAMILY_POISON.equals(glyphResourceId)) return Color.valueOf("8C57A8FF");
        return Color.valueOf("C04C9BFF");
    }

    private static Set<PlantCategory> safeCategories(PlantCollectionState state) {
        return state.getCategories() == null
                ? Collections.emptySet()
                : state.getCategories();
    }

    private static Set<PlantTag> safeTags(PlantCollectionState state) {
        return state.getTags() == null
                ? Collections.emptySet()
                : state.getTags();
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    public static final class PacketVisual {
        private final String resourceId;

        private PacketVisual(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getResourceId() {
            return this.resourceId;
        }
    }

    public static final class FamilyVisual {
        private final String glyphResourceId;
        private final Color color;

        private FamilyVisual(String glyphResourceId, Color color) {
            this.glyphResourceId = glyphResourceId;
            this.color = color;
        }

        public String getGlyphResourceId() {
            return this.glyphResourceId;
        }

        public Color getColor() {
            return this.color;
        }
    }
}
