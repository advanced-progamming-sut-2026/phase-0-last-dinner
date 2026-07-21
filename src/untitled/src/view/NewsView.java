package view;

import java.util.List;
import java.util.regex.Matcher;

public class NewsView implements CommandHandler {
    private NewsViewObserver observer;

    public void setObserver(NewsViewObserver observer) {
        this.observer = observer;
    }

    @Override
    public void handleCommand(String input) {
        if (this.observer == null) {
            System.out.println("News controller is not connected.");
            return;
        }

        Matcher matcher = NewsCommand.SHOW_UNREAD.getMatcher(input);

        if (matcher != null) {
            this.printNews("Unread news", this.observer.onShowUnreadNewsRequested());
            return;
        }

        matcher = NewsCommand.SHOW_ALL.getMatcher(input);

        if (matcher != null) {
            this.printNews("All news", this.observer.onShowAllNewsRequested());
            return;
        }

        System.out.println("Invalid news command.");
    }

    private void printNews(String title, List<String> news) {
        System.out.println(title);

        if (news == null || news.isEmpty()) {
            System.out.println("No news was found.");
            return;
        }

        for (String item : news) {
            System.out.println("- " + item);
        }
    }
}
