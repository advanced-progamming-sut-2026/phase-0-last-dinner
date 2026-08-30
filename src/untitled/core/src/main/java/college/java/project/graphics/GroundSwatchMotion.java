package college.java.project.graphics;

import com.badlogic.gdx.math.Rectangle;
import pvz.libpvz.pam.PamPlayer;

/** View-only gait correction derived from the PAM ground_swatch track. */
public final class GroundSwatchMotion {
    private static final String GROUND_SWATCH = "ground_swatch";

    private final float[] centers;
    private final float duration;

    private GroundSwatchMotion(float[] centers, float duration) {
        this.centers = centers;
        this.duration = duration;
    }

    public static GroundSwatchMotion create(PamPlayer player, String pamPath, String walkClip) {
        Rectangle[] frames = PamPartGeometry.partBoundsByFrame(player, pamPath, walkClip, GROUND_SWATCH);
        if (frames.length < 2) {
            return empty();
        }
        float[] centers = new float[frames.length];
        float last = Float.NaN;
        for (int index = 0; index < frames.length; index++) {
            Rectangle frame = frames[index];
            if (frame != null) {
                last = frame.x + frame.width / 2f;
            }
            centers[index] = last;
        }
        fillMissingFromRight(centers);
        float duration = safeDuration(player, pamPath, walkClip);
        if (!Float.isFinite(centers[0]) || !Float.isFinite(centers[centers.length - 1])
                || duration <= 0f) {
            return empty();
        }
        return new GroundSwatchMotion(centers, duration);
    }

    public float offsetX(float stateTime) {
        if (this.centers.length < 2 || this.duration <= 0f) {
            return 0f;
        }
        float wrapped = stateTime % this.duration;
        if (wrapped < 0f) {
            wrapped += this.duration;
        }
        float phase = wrapped / this.duration;
        int index = Math.min(this.centers.length - 1, (int) (phase * this.centers.length));
        float linearPhase = index / (float) (this.centers.length - 1);
        float expected = lerp(this.centers[0], this.centers[this.centers.length - 1], linearPhase);
        return (expected - this.centers[index]) * GameplayPamScale.WORLD_SCALE;
    }

    int frameCount() {
        return this.centers.length;
    }

    float rawTravel() {
        if (this.centers.length < 2) {
            return 0f;
        }
        return this.centers[this.centers.length - 1] - this.centers[0];
    }

    float maxCorrection() {
        float max = 0f;
        if (this.centers.length < 2) {
            return max;
        }
        for (int index = 0; index < this.centers.length; index++) {
            float phase = index / (float) (this.centers.length - 1);
            float expected = lerp(this.centers[0], this.centers[this.centers.length - 1], phase);
            max = Math.max(max, Math.abs(expected - this.centers[index]));
        }
        return max * GameplayPamScale.WORLD_SCALE;
    }

    private static GroundSwatchMotion empty() {
        return new GroundSwatchMotion(new float[0], 0f);
    }

    private static void fillMissingFromRight(float[] centers) {
        float next = Float.NaN;
        for (int index = centers.length - 1; index >= 0; index--) {
            if (Float.isFinite(centers[index])) {
                next = centers[index];
            } else if (Float.isFinite(next)) {
                centers[index] = next;
            }
        }
    }

    private static float safeDuration(PamPlayer player, String pamPath, String clip) {
        try {
            return player.clipDurationSeconds(pamPath, clip);
        } catch (RuntimeException ignored) {
            return 0f;
        }
    }

    private static float lerp(float start, float end, float alpha) {
        return start + (end - start) * alpha;
    }
}
