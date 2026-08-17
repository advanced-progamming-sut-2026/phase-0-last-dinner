package college.java.project.graphics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Original PvZ2 Almanac packet artwork used by the zombie Collection grid. */
public final class ZombiePacketCatalog {
    private static final Map<String, PacketVisual> PACKETS = createPackets();

    private ZombiePacketCatalog() {
    }

    public static PacketVisual findPacket(String zombieAlias) {
        return PACKETS.get(normalize(zombieAlias));
    }

    public static PacketVisual findGameplayPacket(String zombieAlias, boolean hasLiveArmor) {
        PacketVisual original = findPacket(zombieAlias);
        if (hasLiveArmor) {
            return original;
        }
        String baseAlias = unarmoredAlias(zombieAlias);
        if (baseAlias == null) {
            return original;
        }
        PacketVisual base = findPacket(baseAlias);
        return base == null ? original : base;
    }

    public static Map<String, PacketVisual> allPackets() {
        return PACKETS;
    }

    private static Map<String, PacketVisual> createPackets() {
        Map<String, PacketVisual> packets = new LinkedHashMap<>();
        addCorePackets(packets);
        addEgyptPackets(packets);
        addIceAgePackets(packets);
        addBeachPackets(packets);
        addDarkPackets(packets);
        addZombossPackets(packets);
        addProjectPackets(packets);
        return Collections.unmodifiableMap(packets);
    }

    private static void addCorePackets(Map<String, PacketVisual> packets) {
        add(packets, "ZombieTutorialDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL");
        add(packets, "ZombieTutorialArmor1Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_ARMOR1");
        add(packets, "ZombieTutorialArmor2Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_ARMOR2");
        add(packets, "ZombieTutorialArmor4Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_ARMOR4");
        add(packets, "ZombieGargantuarBasic", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_GARGANTUAR");
        add(packets, "ZombieTutorialImpDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_IMP");
        add(packets, "ZombieTutorialFlagDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_FLAG");
    }

    private static void addEgyptPackets(Map<String, PacketVisual> packets) {
        add(packets, "ZombieMummyDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY");
        add(packets, "ZombieMummyArmor1Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY_ARMOR1");
        add(packets, "ZombieMummyArmor2Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY_ARMOR2");
        add(packets, "ZombieMummyArmor4Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY_ARMOR4");
        add(packets, "ZombiePharaohDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_PHARAOH");
        add(packets, "ZombieRaDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_RA");
        add(packets, "ZombieExplorerDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EXPLORER");
        add(packets, "ZombieTombRaiserDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TOMB_RAISER");
        add(packets, "ZombieCamelDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_CAMEL_ALMANAC");
        add(packets, "ZombieEgyptGargantuar", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EGYPT_GARGANTUAR");
        add(packets, "ZombieEgyptImpDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EGYPT_IMP");
    }

    private static void addIceAgePackets(Map<String, PacketVisual> packets) {
        add(packets, "ZombieIceageDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE");
        add(packets, "ZombieIceageArmor1Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_ARMOR1");
        add(packets, "ZombieIceageArmor2Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_ARMOR2");
        add(packets, "ZombieIceageArmor3Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_ARMOR3");
        add(packets, "ZombieIceAgeHunter", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_HUNTER");
        add(packets, "ZombieIceAgeTroglobite", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_TROGLOBITE");
        add(packets, "ZombieIceAgeDodo", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_DODO");
        add(packets, "ZombieWeaselHoarderDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_WEASELHOARDER");
        add(packets, "ZombieWeaselDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_WEASEL");
        add(packets, "ZombieIceAgeGargantuar", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_GARGANTUAR");
        add(packets, "ZombieIceageImpDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_IMP");
    }

    private static void addBeachPackets(Map<String, PacketVisual> packets) {
        add(packets, "ZombieBeachDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH");
        add(packets, "ZombieBeachArmor1Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_ARMOR1");
        add(packets, "ZombieBeachArmor2Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_ARMOR2");
        add(packets, "ZombieBeachSnorkel", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_SNORKEL");
        add(packets, "ZombieBeachSurfer", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_SURFER");
        add(packets, "ZombieBeachFisherman", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_FISHERMAN");
        add(packets, "ZombieBeachOctopus", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_OCTOPUS");
        add(packets, "ZombieBeachGargantuar", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_GARGANTUAR");
        add(packets, "ZombieBeachImpDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_IMP");
        // The original atlas names this swimmer visual Beach Fem rather than Fast Swimmer.
        add(packets, "ZombieBeachFastSwimmer", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_FEM");
    }

    private static void addDarkPackets(Map<String, PacketVisual> packets) {
        add(packets, "ZombieDarkDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK");
        add(packets, "ZombieDarkArmor1Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_ARMOR1");
        add(packets, "ZombieDarkArmor2Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_ARMOR2");
        add(packets, "ZombieDarkArmor3Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_ARMOR3");
        add(packets, "ZombieDarkArmor4Default", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_ARMOR4");
        add(packets, "ZombieWizardDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_WIZARD");
        add(packets, "ZombieDarkJugglerDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_JUGGLER");
        add(packets, "ZombieDarkKing", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_KING");
        add(packets, "ZombieDarkGargantuar", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_GARGANTUAR");
        add(packets, "ZombieDarkImpDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_IMP");
    }

    private static void addZombossPackets(Map<String, PacketVisual> packets) {
        add(packets, "ZombieZombossMechEgypt", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ZOMBOSSMECH_EGYPT");
        add(packets, "ZombieZombossMechPirate", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ZOMBOSSMECH_PIRATE");
        add(packets, "ZombieZombossMechCowboy", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ZOMBOSSMECH_COWBOY");
        add(packets, "ZombieZombossMechDark", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ZOMBOSSMECH_DARK");
    }


    private static void addProjectPackets(Map<String, PacketVisual> packets) {
        add(packets, "ZombieDefault", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL");
        add(packets, "ZombieArmor1", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_ARMOR1");
        add(packets, "ZombieArmor2", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_ARMOR2");
        add(packets, "ZombieArmor4", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_ARMOR4");
        add(packets, "ZombieDarkArmor3", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_ARMOR3");
        add(packets, "ZombieGargantuar", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_GARGANTUAR");
        add(packets, "ZombieImp", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_IMP");
        add(packets, "ZombieRa", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_RA");
        add(packets, "ZombieExplorer", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EXPLORER");
        add(packets, "ZombieTombRaiser", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TOMB_RAISER");
        add(packets, "ZombieIceAgeDodo", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_DODO");
        add(packets, "ZombieIceAgeHunter", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_HUNTER");
        add(packets, "ZombieIceAgeTroglobite", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_TROGLOBITE");
        add(packets, "ZombieBeachFisherman", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_FISHERMAN");
        add(packets, "ZombieBeachOctopus", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_OCTOPUS");
        add(packets, "ZombieBeachSnorkel", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_SNORKEL");
        add(packets, "ZombieDarkJuggler", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_JUGGLER");
        add(packets, "ZombieWizard", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_WIZARD");
        add(packets, "ZombieDarkKing", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_KING");
        add(packets, "ZombieDarkImpDragon", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_IMP_DRAGON");
        add(packets, "ZombieModernAllStar", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MODERN_ALLSTAR");
        add(packets, "ZombieLostCityJane", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_LOSTCITY_JANE");
        add(packets, "ZombieCrystalSkull", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_LOSTCITY_CRYSTALSKULL");
        add(packets, "ZombieProspector", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_PROSPECTOR");
        add(packets, "ZombiePiano", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_PIANO");
        add(packets, "ZombieNewspaper", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MODERN_NEWSPAPER");
        add(packets, "ZombieArcade", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EIGHTIES_ARCADE");
        add(packets, "ZombieBarrelRoller", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BARRELROLLER");
    }

    private static void add(Map<String, PacketVisual> packets, String alias, String resourceId) {
        packets.put(normalize(alias), new PacketVisual(alias, resourceId));
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private static String unarmoredAlias(String alias) {
        return switch (normalize(alias)) {
            case "zombietutorialarmor1default", "zombietutorialarmor2default",
                    "zombietutorialarmor4default" -> "ZombieTutorialDefault";
            case "zombiearmor1", "zombiearmor2", "zombiearmor4" -> "ZombieDefault";
            case "zombiemummyarmor1default", "zombiemummyarmor2default",
                    "zombiemummyarmor4default" -> "ZombieMummyDefault";
            case "zombieiceagearmor1default", "zombieiceagearmor2default",
                    "zombieiceagearmor3default" -> "ZombieIceageDefault";
            case "zombiebeacharmor1default", "zombiebeacharmor2default" -> "ZombieBeachDefault";
            case "zombiedarkarmor1default", "zombiedarkarmor2default",
                    "zombiedarkarmor3default", "zombiedarkarmor4default",
                    "zombiedarkarmor3" -> "ZombieDarkDefault";
            default -> null;
        };
    }

    public static final class PacketVisual {
        private final String alias;
        private final String resourceId;

        private PacketVisual(String alias, String resourceId) {
            this.alias = alias;
            this.resourceId = resourceId;
        }

        public String getAlias() {
            return this.alias;
        }

        public String getResourceId() {
            return this.resourceId;
        }

        public boolean isFallback() {
            return false;
        }
    }
}
