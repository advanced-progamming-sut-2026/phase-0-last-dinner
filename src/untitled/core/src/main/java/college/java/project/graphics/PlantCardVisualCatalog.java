package college.java.project.graphics;

/** Keeps legacy constructor compatibility; packet artwork is the preferred collection visual. */
public final class PlantCardVisualCatalog {
    public PlantCardVisualProfile find(String plantName) {
        return PlantCardVisualProfile.DEFAULT;
    }
}
