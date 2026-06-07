package model.plant;

import java.util.List;

public interface PlantDefinitionRepository {

    PlantDefinition findByName(String name);

    List<PlantDefinition> findAll();
}
