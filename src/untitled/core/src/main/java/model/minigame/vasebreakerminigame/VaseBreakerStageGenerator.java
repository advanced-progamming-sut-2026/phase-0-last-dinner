package model.minigame.vasebreakerminigame;

import model.mechanism.Position;
import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class VaseBreakerStageGenerator {
    private final VasebreakerIntegration integration;

    private final Random random;

    public VaseBreakerStageGenerator() {
        this(
                new PlantZombieVasebreakerIntegration(),
                new Random()
        );
    }

    public VaseBreakerStageGenerator(
            VasebreakerIntegration integration
    ) {
        this(
                integration,
                new Random()
        );
    }

    public VaseBreakerStageGenerator(
            VasebreakerIntegration integration,
            Random random
    ) {
        if (integration == null) {
            this.integration =
                    new PlantZombieVasebreakerIntegration();
        } else {
            this.integration = integration;
        }

        if (random == null) {
            this.random = new Random();
        } else {
            this.random = random;
        }
    }

    public List<Vase> generateStage(int stageNumber) {
        switch (stageNumber) {
            case 1:
                return this.generateStageOne();
            case 2:
                return this.generateStageTwo();
            case 3:
                return this.generateStageThree();
            default:
                throw new IllegalArgumentException(
                        "Vasebreaker stage must be between 1 and 3."
                );
        }
    }

    public List<Vase> generateStageOne() {
        return buildStage(
            1,
            7,
            9,
            5,
            4,
            2,
            5,
            4,
            0
        );
    }

    public List<Vase> generateStageTwo() {
        return buildStage(
                2,
                6,
                9,
                5,
                4,
                3,
                8,
                4,
                1
        );
    }

    public List<Vase> generateStageThree() {
        return buildStage(
                3,
                4,
                9,
                5,
                5,
                5,
                14,
                4,
                2
        );
    }

    private List<Vase> buildStage(
            int stageNumber,
            int firstColumn,
            int lastColumn,
            int rowCount,
            int visiblePlantVaseCount,
            int hiddenSeedPacketCount,
            int regularZombieCount,
            int emptyVaseCount,
            int gargantuarVaseCount
    ) {
        List<Position> positions = createPositions(firstColumn, lastColumn, rowCount);
        List<VaseTemplate> templates = createTemplates(
                visiblePlantVaseCount,
                hiddenSeedPacketCount,
                regularZombieCount,
                emptyVaseCount,
                gargantuarVaseCount
        );
        if (positions.size() != templates.size()) {
            throw new IllegalStateException(
                    "The number of vase contents does not match the number of positions."
            );
        }

        Collections.shuffle(templates, random);
        List<Vase> vases = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            vases.add(createVase(stageNumber, positions.get(i), templates.get(i)));
        }
        return vases;
    }

    private List<VaseTemplate> createTemplates(
            int visiblePlantVaseCount,
            int hiddenSeedPacketCount,
            int regularZombieCount,
            int emptyVaseCount,
            int gargantuarVaseCount
    ) {
        List<VaseTemplate> templates = new ArrayList<>();
        addVisiblePlantVases(templates, visiblePlantVaseCount);
        addHiddenSeedPacketVases(templates, hiddenSeedPacketCount);
        addRegularZombieVases(templates, regularZombieCount);
        addEmptyVases(templates, emptyVaseCount);
        addGargantuarVases(templates, gargantuarVaseCount);
        return templates;
    }

    private Vase createVase(int stageNumber, Position position, VaseTemplate template) {
        PlantDefinition plantDefinition = choosePlantDefinition(stageNumber, template);
        ZombieDefinition zombieDefinition = chooseZombieDefinition(stageNumber, template);
        return new Vase(
                position,
                template.vaseType,
                template.contentType,
                plantDefinition,
                zombieDefinition
        );
    }

    private List<Position> createPositions(
            int firstColumn,
            int lastColumn,
            int rowCount
    ) {
        List<Position> positions =
                new ArrayList<>();

        for (int y = 1; y <= rowCount; y++) {
            for (int x = firstColumn;
                 x <= lastColumn;
                 x++) {

                positions.add(
                        new Position(x, y)
                );
            }
        }

        return positions;
    }

    private PlantDefinition choosePlantDefinition(
            int stageNumber,
            VaseTemplate template
    ) {
        if (template.contentType
                != VaseContentType.SEED_PACKET) {

            return null;
        }

        return integration.choosePlantDefinition(
                stageNumber
        );
    }

    private ZombieDefinition chooseZombieDefinition(
            int stageNumber,
            VaseTemplate template
    ) {
        if (template.contentType
                != VaseContentType.ZOMBIE) {

            return null;
        }

        if (template.vaseType
                == VaseType.GARGANTUAR) {

            return integration
                    .chooseGargantuarDefinition(
                            stageNumber
                    );
        }

        return integration
                .chooseRegularZombieDefinition(
                        stageNumber
                );
    }

    private void addVisiblePlantVases(
            List<VaseTemplate> templates,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            templates.add(
                    new VaseTemplate(
                            VaseType.PLANT,
                            VaseContentType.SEED_PACKET
                    )
            );
        }
    }

    private void addHiddenSeedPacketVases(
            List<VaseTemplate> templates,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            templates.add(
                    new VaseTemplate(
                            VaseType.NORMAL,
                            VaseContentType.SEED_PACKET
                    )
            );
        }
    }

    private void addRegularZombieVases(
            List<VaseTemplate> templates,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            templates.add(
                    new VaseTemplate(
                            VaseType.NORMAL,
                            VaseContentType.ZOMBIE
                    )
            );
        }
    }

    private void addEmptyVases(
            List<VaseTemplate> templates,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            templates.add(
                    new VaseTemplate(
                            VaseType.NORMAL,
                            VaseContentType.EMPTY
                    )
            );
        }
    }

    private void addGargantuarVases(
            List<VaseTemplate> templates,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            templates.add(
                    new VaseTemplate(
                            VaseType.GARGANTUAR,
                            VaseContentType.ZOMBIE
                    )
            );
        }
    }

    private static class VaseTemplate {
        private final VaseType vaseType;

        private final VaseContentType contentType;

        private VaseTemplate(
                VaseType vaseType,
                VaseContentType contentType
        ) {
            this.vaseType = vaseType;
            this.contentType = contentType;
        }
    }
}
