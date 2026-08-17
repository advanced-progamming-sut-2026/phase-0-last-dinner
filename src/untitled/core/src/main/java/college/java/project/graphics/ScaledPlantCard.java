package college.java.project.graphics;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;

final class ScaledPlantCard extends WidgetGroup {
    private final PlantCard card;
    private final Group scaleRoot;
    private final float cardScale;

    ScaledPlantCard(PlantCard card, float cardScale) {
        if (card == null) {
            throw new IllegalArgumentException("Plant card is required");
        }
        if (cardScale <= 0f) {
            throw new IllegalArgumentException("Plant card scale must be positive");
        }
        this.card = card;
        this.cardScale = cardScale;
        this.scaleRoot = new Group();
        this.scaleRoot.setTransform(true);
        this.scaleRoot.addActor(card);
        addActor(this.scaleRoot);
        setSize(getPrefWidth(), getPrefHeight());
    }

    PlantCard getCard() {
        return this.card;
    }

    @Override
    public void layout() {
        float cardWidth = this.card.getPrefWidth();
        float cardHeight = this.card.getPrefHeight();
        this.card.setBounds(0f, 0f, cardWidth, cardHeight);
        this.card.validate();
        this.card.setOrigin(cardWidth / 2f, cardHeight / 2f);
        this.card.setScale(1f);
        this.scaleRoot.setBounds(0f, 0f, cardWidth, cardHeight);
        this.scaleRoot.setOrigin(0f, 0f);
        this.scaleRoot.setScale(this.cardScale);
    }

    @Override
    public float getPrefWidth() {
        return this.card.getPrefWidth() * this.cardScale;
    }

    @Override
    public float getPrefHeight() {
        return this.card.getPrefHeight() * this.cardScale;
    }
}
