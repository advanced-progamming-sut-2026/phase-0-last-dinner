package view.travellog;

import model.GameMenuRelated.Page;
import model.GameMenuRelated.PageName;
import model.minigame.MiniGameType;
import view.CommandHandler;

public interface TravelLogViewObserver {

    Page onShowCurrentPageRequested();

    Page onChangePageRequested(
            PageName pageName
    );

    CommandHandler onOpenMiniGameRequested(
            MiniGameType miniGameType
    );
}