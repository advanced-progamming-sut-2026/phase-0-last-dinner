package model.minigame.vasebreakerminigame;

import model.mechanism.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VaseBreakerStageGenerator {
    //TODO : VaseBreakerMiniGame controlles the logic and this creates the 3 stages , it can be randomised too
    public List<Vase> generateStageOne() {
        List<Vase> vases = new ArrayList<>();

        vases.add(new Vase(
                new Position(3, 1),
                VaseType.NORMAL,
                VaseContentType.EMPTY,
                null,
                null
        ));

        vases.add(new Vase(
                new Position(4, 1),
                VaseType.PLANT,
                VaseContentType.SEED_PACKET,
                null,
                null
        ));

        vases.add(new Vase(
                new Position(5, 1),
                VaseType.ZOMBIE,
                VaseContentType.ZOMBIE,
                null,
                null
        ));

        vases.add(new Vase(
                new Position(3, 2),
                VaseType.NORMAL,
                VaseContentType.EMPTY,
                null,
                null
        ));

        vases.add(new Vase(
                new Position(4, 2),
                VaseType.PLANT,
                VaseContentType.SEED_PACKET,
                null,
                null
        ));

        vases.add(new Vase(
                new Position(5, 2),
                VaseType.ZOMBIE,
                VaseContentType.ZOMBIE,
                null,
                null
        ));

        Collections.shuffle(vases);
        return vases;
    }
}