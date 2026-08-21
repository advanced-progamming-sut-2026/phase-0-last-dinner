package view;

import model.chapters.ChapterType;
import model.level.LevelType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LevelNpcDialogs {
    private static final String LEADER_NAME = "Lucius Artorius Castus";
    private static final String LEADER_PORTRAIT = "Assets/Exports/askelad.png";
    private static final String ALLY_NAME = "Sadie";
    private static final String ALLY_PORTRAIT = "Assets/Exports/sadie.png";

    private static final Map<Key, List<NpcDialogLine>> INTRO_DIALOGS = new HashMap<>();
    private static final Map<Key, List<NpcDialogLine>> WIN_DIALOGS = new HashMap<>();

    static {
        Key egyptNormal = new Key(ChapterType.ANCIENT_EGYPT, LevelType.NORMAL);

        INTRO_DIALOGS.put(egyptNormal, Collections.singletonList(new NpcDialogLine(
            LEADER_NAME,
            LEADER_PORTRAIT,
            "As  long  as  I'm  the  one  guarding  Sadie,  you  motherfuckers  ain't  laying  a "
                + " hand  on  her!"
        )));

        WIN_DIALOGS.put(egyptNormal, Collections.singletonList(new NpcDialogLine(
            ALLY_NAME,
            ALLY_PORTRAIT,
            "Thanks,  good  boys."
        )));
    }

    private LevelNpcDialogs() {
    }

    public static List<NpcDialogLine> getIntroDialog(ChapterType chapter, LevelType level) {
        return INTRO_DIALOGS.get(new Key(chapter, level));
    }

    public static List<NpcDialogLine> getWinDialog(ChapterType chapter, LevelType level) {
        return WIN_DIALOGS.get(new Key(chapter, level));
    }

    private static final class Key {
        private final ChapterType chapter;
        private final LevelType level;

        private Key(ChapterType chapter, LevelType level) {
            this.chapter = chapter;
            this.level = level;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key)) {
                return false;
            }
            Key key = (Key) other;
            return this.chapter == key.chapter && this.level == key.level;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.chapter, this.level);
        }
    }
}
