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
        return null;
    }

    @Override
    public List<ZombieDefinition> findByChapter(ZombieChapter chapter) {
        return null;
    }

    @Override
    public List<ZombieDefinition> findAll() {
        return null;
    }
}
