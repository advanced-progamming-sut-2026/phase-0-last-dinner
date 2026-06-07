package model.chapters;
import lombok.Getter;
import model.level.Level;
import java.util.ArrayList;
@Getter

public abstract class Chapter {
   private ChapterType chapter;
   private ArrayList<Level> levels;
}
