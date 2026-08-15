package controller;

import model.Menu.MenuType;
import model.User.User;
import view.NewsView;
import view.NewsViewObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewsController implements MenuController, NewsViewObserver {
    private final User user;

    public NewsController() {
        this.user = null;
    }

    public NewsController(NewsView view, User user) {
        if (view == null || user == null) {
            throw new IllegalArgumentException("News view and user are required");
        }

        user.initializeMissingFields();
        this.user = user;
        view.setObserver(this);
    }

    @Override
    public MenuType getCurrentMenu() {
        return MenuType.NEWS_MENU;
    }

    @Override
    public void changeMenu() {
    }

    public List<String> showUnreadNews() {
        if (this.user == null) {
            return Collections.emptyList();
        }

        // khabar ha ghabl az khunde shodan negah dashte mishan
        List<String> unreadNews = new ArrayList<>(this.user.getUnreadNews());
        this.user.getUnreadNews().clear();
        return unreadNews;
    }

    public List<String> showAllNews() {
        if (this.user == null) {
            return Collections.emptyList();
        }

        return this.user.getAllNews();
    }

    public boolean hasUnreadNews() {
        return this.user != null && this.user.hasUnreadNews();
    }

    public int getUnreadNewsCount() {
        return this.user == null ? 0 : this.user.getUnreadNews().size();
    }

    @Override
    public List<String> onShowUnreadNewsRequested() {
        return this.showUnreadNews();
    }

    @Override
    public List<String> onShowAllNewsRequested() {
        return this.showAllNews();
    }
}
