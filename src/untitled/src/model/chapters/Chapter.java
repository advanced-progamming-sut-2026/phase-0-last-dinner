package model.chapters;
import lombok.Getter;
import model.level.Level;
import model.mechanism.Board;

import java.util.ArrayList;
@Getter

public abstract class Chapter {
   private ChapterType chapter;
   private ArrayList<Level> levels;
   protected Chapter(ChapterType chapter) {
      this.chapter = chapter;
      this.levels = new ArrayList<>();
   }
   public abstract Board buildBoard();
}
