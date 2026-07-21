package view;

import java.util.List;

public interface NewsViewObserver {
    List<String> onShowUnreadNewsRequested();

    List<String> onShowAllNewsRequested();
}
