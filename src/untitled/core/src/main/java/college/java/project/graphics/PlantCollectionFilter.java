package college.java.project.graphics;

import model.collection.PlantCollectionState;

/** Availability and family filters required by Phase 2. */
public enum PlantCollectionFilter {
    ALL("Show All Plants", null), UNLOCKED("Unlocked", null), LOCKED("Locked", null),
    UPGRADEABLE("Upgradeable", null), SUN("Sun Family", "IMAGE_UI_PACKETS_MINTFAM_SUN"),
    PEA("Pea Family", "IMAGE_UI_PACKETS_MINTFAM_PEASHOOTER"),
    LOBBER("Lobber Family", "IMAGE_UI_PACKETS_MINTFAM_LOBBER"),
    EXPLOSIVE("Explosive Family", "IMAGE_UI_PACKETS_MINTFAM_EXPLOSIVE"),
    MELEE("Melee Family", "IMAGE_UI_PACKETS_MINTFAM_MELEE"),
    DEFENSE("Defense Family", "IMAGE_UI_PACKETS_MINTFAM_DEFENSE"),
    SHARP("Sharp Family", "IMAGE_UI_PACKETS_MINTFAM_SHARP"),
    TRAP("Trap Family", "IMAGE_UI_PACKETS_MINTFAM_TRAP"), FIRE("Fire Family", "IMAGE_UI_PACKETS_MINTFAM_FIRE"),
    COLD("Cold Family", "IMAGE_UI_PACKETS_MINTFAM_COLD"), POISON("Poison Family", "IMAGE_UI_PACKETS_MINTFAM_POISON"),
    MAGIC("Magic Family", "IMAGE_UI_PACKETS_MINTFAM_MAGIC");

    private final String displayName;
    private final String familyResource;
    PlantCollectionFilter(String displayName, String familyResource) {
        this.displayName = displayName; this.familyResource = familyResource;
    }
    public String getDisplayName() { return this.displayName; }
    public String getFamilyResourceId() { return this.familyResource; }
    public boolean matches(PlantCollectionState state) {
        return matches(state, Integer.MAX_VALUE);
    }

    /**
     * Wallet-aware variant used by graphical collection/pick screens.
     * A plant is only "Upgradeable" when the user can actually pay both
     * the seed-packet and coin requirements defined by Phase 1.
     */
    public boolean matches(PlantCollectionState state, int availableCoins) {
        if (state == null) return false;
        switch (this) {
            case ALL: return true;
            case UNLOCKED: return state.isUnlocked();
            case LOCKED: return !state.isUnlocked();
            case UPGRADEABLE: return state.isUnlocked()
                    && state.getCurrentLevel() < state.getMaximumLevel()
                    && state.getRequiredSeedPackets() > 0
                    && state.getSeedPackets() >= state.getRequiredSeedPackets()
                    && Math.max(0, availableCoins) >= Math.max(0, state.getRequiredCoins());
            default:
                PlantPacketCatalog.FamilyVisual family = PlantPacketCatalog.findFamily(state);
                return family != null && this.familyResource.equals(family.getGlyphResourceId());
        }
    }
}
