package model.zombie;

import java.util.List;

public interface ZombieDefinitionRepository {
    ZombieDefinition findByAlias(String alias);

    List<ZombieDefinition> findByChapter(ZombieChapter chapter);

    List<ZombieDefinition> findAll();
}
