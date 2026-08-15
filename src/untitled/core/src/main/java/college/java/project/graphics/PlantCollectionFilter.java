package college.java.project.graphics;

import model.collection.PlantCollectionState;


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
    public boolean matches(PlantCollectionState state, int availableGold) {
        if (state == null) return false;
        switch (this) {
            case ALL: return true;
            case UNLOCKED: return state.isUnlocked();
            case LOCKED: return !state.isUnlocked();
            case UPGRADEABLE: return state.isUnlocked() && state.getCurrentLevel() < state.getMaximumLevel()
                    && state.getRequiredSeedPackets() > 0 && state.getSeedPackets() >= state.getRequiredSeedPackets()
                    && Math.max(0, availableGold) >= state.getRequiredCoins();
            default:
                PlantPacketCatalog.FamilyVisual family = PlantPacketCatalog.findFamily(state);
                return family != null && this.familyResource.equals(family.getGlyphResourceId());
        }
    }
}
