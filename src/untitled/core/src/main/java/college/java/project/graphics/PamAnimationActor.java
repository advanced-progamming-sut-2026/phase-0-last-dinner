package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import pvz.libpvz.pam.PamPlayer;

import java.util.Map;

public final class PamAnimationActor extends Actor {
    private final PamPlayer pamPlayer;
    private final float canvasWidth;
    private final float canvasHeight;
    private String pamPath;
    private String clipName;
    private boolean looping;
    private float stateTime;
    private Map<String, Boolean> partsVisibility = Map.of();

    public PamAnimationActor(
            PamPlayer pamPlayer,
            String pamPath,
            String clipName,
            float canvasWidth,
            float canvasHeight
    ) {
        if (pamPlayer == null) {
            throw new IllegalArgumentException("PAM player is required");
        }
        if (pamPath == null || pamPath.trim().isEmpty()) {
            throw new IllegalArgumentException("PAM path is required");
        }
        if (clipName == null || clipName.trim().isEmpty()) {
            throw new IllegalArgumentException("Clip name is required");
        }
        if (canvasWidth <= 0f || canvasHeight <= 0f) {
            throw new IllegalArgumentException("Canvas size must be positive");
        }

        this.pamPlayer = pamPlayer;
        this.pamPath = pamPath;
        this.clipName = clipName;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.looping = true;
        this.setSize(canvasWidth, canvasHeight);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.stateTime += Math.max(0f, delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color batchColor = batch.getColor();
        float batchRed = batchColor.r;
        float batchGreen = batchColor.g;
        float batchBlue = batchColor.b;
        float batchAlpha = batchColor.a;
        Color color = this.getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

        float baseScale = Math.min(
                this.getWidth() / this.canvasWidth,
                this.getHeight() / this.canvasHeight
        );
        float centerX = this.getX() + this.getWidth() / 2f;
        float centerY = this.getY() + this.getHeight() / 2f;
        Matrix4 previousTransform = new Matrix4(batch.getTransformMatrix());
        Matrix4 scaledTransform = new Matrix4(previousTransform)
                .translate(centerX, centerY, 0f)
                .rotate(0f, 0f, 1f, this.getRotation())
                .scale(
                        baseScale * this.getScaleX(),
                        baseScale * this.getScaleY(),
                        1f
                )
                .translate(-centerX, -centerY, 0f);

        try {
            batch.flush();
            batch.setTransformMatrix(scaledTransform);
            if (this.partsVisibility.isEmpty()) {
                this.pamPlayer.draw(
                        batch,
                        this.pamPath,
                        this.clipName,
                        this.stateTime,
                        centerX,
                        centerY,
                        this.looping
                );
            } else {
                this.pamPlayer.draw(
                        batch,
                        this.pamPath,
                        this.clipName,
                        this.stateTime,
                        centerX,
                        centerY,
                        this.looping,
                        this.partsVisibility
                );
            }
            batch.flush();
        } catch (RuntimeException ignored) {
            // frame kharab faghat hamin frame ro skip mikone; frame badi dobare test mishe.
        } finally {
            batch.setTransformMatrix(previousTransform);
            batch.setColor(batchRed, batchGreen, batchBlue, batchAlpha);
        }
    }

    public void applyVisualProfile(PlantCardVisualProfile visualProfile) {
        // Packet artwork is preferred for collection cards; keep PAM fallback neutral.
    }

    public void setAnimation(String pamPath, String clipName) {
        if (pamPath == null || pamPath.trim().isEmpty()
                || clipName == null || clipName.trim().isEmpty()) {
            return;
        }

        this.pamPath = pamPath;
        this.clipName = clipName;
        this.stateTime = 0f;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void setPartsVisibility(Map<String, Boolean> partsVisibility) {
        this.partsVisibility = partsVisibility == null ? Map.of() : Map.copyOf(partsVisibility);
    }

    public float getStateTime() {
        return this.stateTime;
    }

    /** Seek within the current clip without changing its path or looping mode. */
    public void setStateTime(float stateTime) {
        this.stateTime = Math.max(0f, stateTime);
    }

    public String getPamPath() {
        return this.pamPath;
    }

    public String getClipName() {
        return this.clipName;
    }

    public float getCanvasWidth() {
        return this.canvasWidth;
    }

    public float getCanvasHeight() {
        return this.canvasHeight;
    }
}
