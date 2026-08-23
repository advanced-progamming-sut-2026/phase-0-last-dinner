package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Disposable;
import view.GameSettings;

final class GameplayGridOverlayLayer extends Group implements Disposable {
    private static final float LINE_THICKNESS = 2f;
    private static final Color LINE_COLOR = new Color(1f, 0f, 0f, 0.85f);

    private final Texture pixel;

    GameplayGridOverlayLayer() {
        setTouchable(Touchable.disabled);
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        this.pixel = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        setVisible(GameSettings.isShowGrid());
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!isVisible()) {
            return;
        }

        int columns = GameplayBoardInteractionLayer.COLUMN_COUNT;
        int rows = GameplayBoardInteractionLayer.ROW_COUNT;
        float cellWidth = getWidth() / columns;
        float cellHeight = getHeight() / rows;

        Color previous = batch.getColor();
        float previousR = previous.r;
        float previousG = previous.g;
        float previousB = previous.b;
        float previousA = previous.a;
        batch.setColor(LINE_COLOR.r, LINE_COLOR.g, LINE_COLOR.b, LINE_COLOR.a * parentAlpha);

        for (int column = 0; column <= columns; column++) {
            float x = getX() + column * cellWidth;
            batch.draw(this.pixel, x - LINE_THICKNESS / 2f, getY(), LINE_THICKNESS, getHeight());
        }
        for (int row = 0; row <= rows; row++) {
            float y = getY() + row * cellHeight;
            batch.draw(this.pixel, getX(), y - LINE_THICKNESS / 2f, getWidth(), LINE_THICKNESS);
        }

        batch.setColor(previousR, previousG, previousB, previousA);
        super.draw(batch, parentAlpha);
    }

    @Override
    public void dispose() {
        this.pixel.dispose();

    }
}
