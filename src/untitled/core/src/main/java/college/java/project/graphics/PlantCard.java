package college.java.project.graphics;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import pvz.libpvz.pam.PamPlayer;

public final class PlantCard extends Table {
    private static final float PREVIEW_SIZE = 120f;

    private final String plantName;
    private final PamAnimationActor animationActor;

    public PlantCard(
            Skin skin,
            PamPlayer pamPlayer,
            PamAnimationCatalog.AnimationInfo animationInfo,
            String plantName
    ) {
        super(skin);

        if (plantName == null || plantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Plant name is required");
        }

        this.plantName = plantName;
        this.animationActor = this.createAnimation(
                pamPlayer,
                animationInfo
        );

        if (this.animationActor != null) {
            this.add(this.animationActor)
                    .size(PREVIEW_SIZE);
            this.row();
        }

        this.add(new Label(this.plantName, skin));
        this.pack();
    }

    public String getPlantName() {
        return this.plantName;
    }

    public PamAnimationActor getAnimationActor() {
        return this.animationActor;
    }

    private PamAnimationActor createAnimation(
            PamPlayer pamPlayer,
            PamAnimationCatalog.AnimationInfo animationInfo
    ) {
        if (pamPlayer == null || animationInfo == null
                || !animationInfo.hasClip("idle")) {
            return null;
        }

        return new PamAnimationActor(
                pamPlayer,
                animationInfo.getPath(),
                "idle",
                animationInfo.getCanvasWidth(),
                animationInfo.getCanvasHeight()
        );
    }
}
