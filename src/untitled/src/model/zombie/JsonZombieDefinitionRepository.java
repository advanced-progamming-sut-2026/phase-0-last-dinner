package model.zombie;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonZombieDefinitionRepository implements ZombieDefinitionRepository {
    private final Path sourceFile;
    private final Path armorSourceFile;
    private final String sourceDescription;
    private List<ZombieDefinition> definitions;

    public JsonZombieDefinitionRepository() {
        this(null, null, "no source", new ArrayList<ZombieDefinition>());
    }

    public JsonZombieDefinitionRepository(Path sourceFile) {
        this(sourceFile, null);
    }

    public JsonZombieDefinitionRepository(Path sourceFile, Path armorSourceFile) {
        this(
                sourceFile,
                armorSourceFile,
                sourceFile == null ? "no source" : sourceFile.toString(),
                new ArrayList<ZombieDefinition>()
        );
    }

    public JsonZombieDefinitionRepository(Reader sourceReader, Reader armorSourceReader) throws IOException {
        this(null, null, "JSON readers", loadDefinitions(sourceReader, armorSourceReader));
    }

    private JsonZombieDefinitionRepository(
            Path sourceFile,
            Path armorSourceFile,
            String sourceDescription,
            List<ZombieDefinition> definitions
    ) {
        this.sourceFile = sourceFile;
        this.armorSourceFile = armorSourceFile;
        this.sourceDescription = sourceDescription;
        this.definitions = definitions;
    }

    public static JsonZombieDefinitionRepository fromClasspath(
            String zombieResourceName,
            String armorResourceName
    ) throws IOException {
        String zombiePath = normalizeResourcePath(zombieResourceName, "Zombie definitions");
        String armorPath = normalizeResourcePath(armorResourceName, "Zombie armor definitions");

        try (Reader zombieReader = openClasspathReader(zombiePath, "Zombie definitions");
             Reader armorReader = openClasspathReader(armorPath, "Zombie armor definitions")) {
            return new JsonZombieDefinitionRepository(
                    null,
                    null,
                    "classpath:" + zombiePath + " + classpath:" + armorPath,
                    loadDefinitions(zombieReader, armorReader)
            );
        }
    }

    public void load() {
        if (this.sourceFile == null) {
            return;
        }

        try {
            this.definitions = loadDefinitions(this.sourceFile, this.armorSourceFile);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load zombie definitions from " + this.sourceDescription, e);
        }
    }

    public static List<ZombieDefinition> loadDefinitions(Path sourceFile) throws IOException {
        return loadDefinitions(sourceFile, null);
    }

    public static List<ZombieDefinition> loadDefinitions(Path sourceFile, Path armorSourceFile) throws IOException {
        if (sourceFile == null) {
            throw new IllegalArgumentException("Zombie definitions path must not be null");
        }

        try (Reader sourceReader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
            if (armorSourceFile == null) {
                return loadDefinitions(sourceReader, null);
            }

            try (Reader armorReader = Files.newBufferedReader(armorSourceFile, StandardCharsets.UTF_8)) {
                return loadDefinitions(sourceReader, armorReader);
            }
        }
    }

    public static List<ZombieDefinition> loadDefinitions(
            Reader sourceReader,
            Reader armorSourceReader
    ) throws IOException {
        return ZombieJsonDefinitionParser.parse(sourceReader, armorSourceReader);
    }

    @Override
    public ZombieDefinition findByAlias(String alias) {
        if (alias == null) {
            return null;
        }

        for (ZombieDefinition definition : this.definitions) {
            if (alias.equalsIgnoreCase(definition.getAlias())) {
                return definition;
            }
        }

        return null;
    }

    @Override
    public List<ZombieDefinition> findByChapter(ZombieChapter chapter) {
        List<ZombieDefinition> result = new ArrayList<>();

        if (chapter == null) {
            return result;
        }

        for (ZombieDefinition definition : this.definitions) {
            if (definition.getChapter() == chapter) {
                result.add(definition);
            }
        }

        return result;
    }

    @Override
    public List<ZombieDefinition> findAll() {
        return this.definitions;
    }

    private static Reader openClasspathReader(String resourcePath, String label) throws IOException {
        InputStream input = JsonZombieDefinitionRepository.class.getClassLoader()
                .getResourceAsStream(resourcePath);

        if (input == null) {
            throw new IOException(label + " resource not found on classpath: " + resourcePath);
        }

        return new InputStreamReader(input, StandardCharsets.UTF_8);
    }

    private static String normalizeResourcePath(String resourceName, String label) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " resource name must not be blank");
        }

        String path = resourceName.trim().replace('\\', '/');

        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }
}
