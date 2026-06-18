package model.zombie;

import java.nio.file.Path;
import java.util.List;

public class JsonZombieDefinitionRepository
        implements ZombieDefinitionRepository {
    private Path sourceFile;
    private List<ZombieDefinition> definitions;

    public void load() {
    }

    @Override
    public ZombieDefinition findByAlias(String alias) {
        if (alias == null || this.definitions == null) {
            return null;
        }

        for (ZombieDefinition definition : this.definitions) {
            if (definition != null && alias.equals(definition.getAlias())) {
                return definition;
            }
        }

        return null;
    }

    @Override
    public List<ZombieDefinition> findByChapter(ZombieChapter chapter) {
        List<ZombieDefinition> result = new java.util.ArrayList<>();

        if (chapter == null || this.definitions == null) {
            return result;
        }

        for (ZombieDefinition definition : this.definitions) {
            if (definition != null && definition.getChapter() == chapter) {
                result.add(definition);
            }
        }

        return result;
    }

    @Override
    public List<ZombieDefinition> findAll() {
        return this.definitions == null ? new java.util.ArrayList<ZombieDefinition>() : this.definitions;
    }
}
