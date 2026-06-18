package model.plant;

import java.util.List;

public class PlantDefinitionRepository {
    private final List<PlantDefinition> definitions;

    public PlantDefinitionRepository(List<PlantDefinition> definitions) {
        this.definitions = definitions;
    }

    public PlantDefinition findByName(String name) {
        for (PlantDefinition definition : this.definitions) {
            if (definition.getName().equals(name)) {
                return definition;
            }
        }

        return null;
    }

    public List<PlantDefinition> findAll() {
        return this.definitions;
    }
}