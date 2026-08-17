package college.java.project.graphics;

import model.chapters.ChapterType;

final class PvzChapterVisuals {
    private PvzChapterVisuals() {
    }

    static String backgroundResourceId(ChapterType chapter) {
        if (chapter == ChapterType.ICE_CAVES) {
            return "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE";
        }
        if (chapter == ChapterType.BIG_WAVE_BEACH) {
            return "IMAGE_BACKGROUNDS_BEACH_TEXTURE";
        }
        if (chapter == ChapterType.MEDIEVAL) {
            return "IMAGE_BACKGROUNDS_DARK_TEXTURE";
        }
        return "IMAGE_BACKGROUNDS_EGYPT_TEXTURE";
    }

    static String displayName(ChapterType chapter) {
        if (chapter == ChapterType.ICE_CAVES) {
            return "ICE CAVES";
        }
        if (chapter == ChapterType.BIG_WAVE_BEACH) {
            return "BIG WAVE BEACH";
        }
        if (chapter == ChapterType.MEDIEVAL) {
            return "DARK AGES";
        }
        return "ANCIENT EGYPT";
    }
}
