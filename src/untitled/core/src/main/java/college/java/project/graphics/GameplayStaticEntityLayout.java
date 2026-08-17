package college.java.project.graphics;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Aspect-preserving placement for static entity fallbacks on a gameplay tile. */
final class GameplayStaticEntityLayout {
    private GameplayStaticEntityLayout() {
    }

    static void groundedFit(
            Actor actor,
            float containerWidth,
            float containerHeight,
            float maxWidthFactor,
            float maxHeightFactor,
            float bottomFactor
    ) {
        float maxWidth = Math.max(1f, containerWidth * maxWidthFactor);
        float maxHeight = Math.max(1f, containerHeight * maxHeightFactor);
        float sourceWidth = maxWidth;
        float sourceHeight = maxHeight;

        if (actor instanceof Image image) {
            Drawable drawable = image.getDrawable();
            if (drawable != null && drawable.getMinWidth() > 0f && drawable.getMinHeight() > 0f) {
                sourceWidth = drawable.getMinWidth();
                sourceHeight = drawable.getMinHeight();
            }
        }

        float scale = Math.min(maxWidth / sourceWidth, maxHeight / sourceHeight);
        float width = Math.max(1f, sourceWidth * scale);
        float height = Math.max(1f, sourceHeight * scale);
        actor.setBounds(
                (containerWidth - width) * 0.5f,
                containerHeight * bottomFactor,
                width,
                height
        );
    }
}
