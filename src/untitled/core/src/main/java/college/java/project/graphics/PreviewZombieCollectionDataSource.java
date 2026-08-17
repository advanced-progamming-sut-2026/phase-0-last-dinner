package college.java.project.graphics;

import model.collection.ZombieCollectionState;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Visual-preview data source that exposes every repository zombie as encountered. */
public final class PreviewZombieCollectionDataSource implements ZombieCollectionDataSource {
    private final ZombieDefinitionRepository definitions;

    public PreviewZombieCollectionDataSource(ZombieDefinitionRepository definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("Zombie definition repository is required");
        }
        this.definitions = definitions;
    }

    @Override
    public List<ZombieCollectionState> loadZombies() {
        List<ZombieDefinition> source = this.definitions.findAll();
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<ZombieCollectionState> states = new ArrayList<>(source.size());
        for (ZombieDefinition definition : source) {
            if (definition != null) {
                states.add(ZombieCollectionState.from(definition, true));
            }
        }
        return states;
    }
}
