package controller;

import model.Menu.GameMenuContext;
import model.Menu.MenuType;
import model.chapters.ChapterType;
import model.level.LevelType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ChapterController implements MenuController {

    private static final Map<ChapterType, LevelType> SPECIAL_LEVEL_BY_CHAPTER = createSpecialLevelMap();

    private final LoginController loginController;
    private ChapterType selectedChapter;
    private LevelType selectedLevel;

    public ChapterController(LoginController loginController) {
        if (loginController == null) {
            throw new IllegalArgumentException("loginController is required");
        }

        this.loginController = loginController;
    }

    private static Map<ChapterType, LevelType> createSpecialLevelMap() {
        Map<ChapterType, LevelType> map = new EnumMap<>(ChapterType.class);
        map.put(ChapterType.ANCIENT_EGYPT, LevelType.CONVEYOR_BELT);
        map.put(ChapterType.ICE_CAVES, LevelType.DEADLINE);
        map.put(ChapterType.BIG_WAVE_BEACH, LevelType.NIGHT_OPS);
        map.put(ChapterType.MEDIEVAL, LevelType.LOVE_YOUR_PLANTS);
        return Collections.unmodifiableMap(map);
    }

    private GameMenuContext getMenuContext() {
        return this.loginController.getMenuContext();
    }

    public void enterChapterMenu(ChapterType chapterType) {
        if (chapterType == null) {
            throw new IllegalArgumentException("chapterType is required");
        }

        this.selectedChapter = chapterType;
        this.selectedLevel = null;
        this.getMenuContext().enterMenu(MenuType.CHAPTER_MENU);
    }

    public ChapterType getSelectedChapter() {
        return this.selectedChapter;
    }

    public List<LevelType> getAvailableLevels() {
        if (this.selectedChapter == null) {
            return Collections.emptyList();
        }

        LevelType specialLevel = SPECIAL_LEVEL_BY_CHAPTER.get(this.selectedChapter);
        return Collections.unmodifiableList(Arrays.asList(LevelType.NORMAL, specialLevel));
    }


    public boolean selectLevel(LevelType levelType) {
        if (this.selectedChapter == null || levelType == null) {
            return false;
        }

        if (!this.getAvailableLevels().contains(levelType)) {
            return false;
        }

        this.selectedLevel = levelType;

        try {
            this.getMenuContext().enterMenu(MenuType.PLANT_PICK_MENU);
        } catch (IllegalStateException e) {
            this.selectedLevel = null;
            return false;
        }

        return true;
    }

    public LevelType getSelectedLevel() {
        return this.selectedLevel;
    }

    @Override
    public void changeMenu() {
        this.getMenuContext().exitMenu();
    }

    @Override
    public MenuType getCurrentMenu() {
        return this.getMenuContext().getCurrentMenu();
    }
}