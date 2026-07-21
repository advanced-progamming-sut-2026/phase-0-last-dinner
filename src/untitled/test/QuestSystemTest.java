import controller.ApplicationController;
import controller.TravelLogController;
import model.GameMenuRelated.Page;
import model.GameMenuRelated.PageName;
import model.GameMenuRelated.Quest;
import model.GameMenuRelated.QuestObj;
import model.GameMenuRelated.TravelLog;
import model.User.User;
import model.User.UserGender;
import model.User.UserRepository;
import org.junit.Test;
import view.travellog.TravelLogView;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class QuestSystemTest {

    @Test
    public void travelLogContainsAllQuestsOnSortedCategoryPages() {
        TravelLog travelLog = new TravelLog();

        assertEquals(3, travelLog.getPage(PageName.ADVENTURE).getQuestObjects().size());
        assertEquals(14, travelLog.getPage(PageName.COMMUNITY).getQuestObjects().size());
        assertEquals(3, travelLog.getPage(PageName.CHALLENGES).getQuestObjects().size());

        Page dailyPage = travelLog.getPage(PageName.COMMUNITY);
        for (int index = 1; index < dailyPage.getQuestObjects().size(); index++) {
            int previous = dailyPage.getQuestObjects().get(index - 1).getQuest().getPriority().ordinal();
            int current = dailyPage.getQuestObjects().get(index).getQuest().getPriority().ordinal();
            assertTrue(previous >= current);
        }
    }

    @Test
    public void completedQuestRewardCanOnlyBeClaimedOnce() {
        User user = createUser();
        QuestObj quest = user.getTravelLog().findQuest(Quest.ONLY_CACTUS);
        quest.setCompletionPercentage(100);

        TravelLogController controller = new TravelLogController(
                new TravelLogView(),
                user.getTravelLog(),
                user,
                null
        );
        controller.onChangePageRequested(PageName.COMMUNITY);

        assertTrue(controller.onClaimQuestRequested("Only Cactus").startsWith("Quest reward claimed"));
        assertEquals(20, user.getDiamond());
        assertEquals(1, user.getCompletedQuests());
        assertEquals("Quest reward was already claimed.", controller.onClaimQuestRequested("ONLY_CACTUS"));
        assertEquals(20, user.getDiamond());
    }

    @Test
    public void dailyResetDoesNotClearMainQuestProgress() {
        TravelLog travelLog = new TravelLog();
        travelLog.setProgress(Quest.ONLY_CACTUS, 100);
        travelLog.setProgress(Quest.CHAPTER_HUNTER, 60);
        assertTrue(travelLog.findQuest(Quest.ONLY_CACTUS).claimReward());

        travelLog.resetDailyQuests();

        assertEquals(0, travelLog.findQuest(Quest.ONLY_CACTUS).getCompletionPercentage());
        assertFalse(travelLog.findQuest(Quest.ONLY_CACTUS).isRewardClaimed());
        assertEquals(60, travelLog.findQuest(Quest.CHAPTER_HUNTER).getCompletionPercentage());
    }

    @Test
    public void questCsvIsPackagedWithTheGame() throws Exception {
        try (InputStream input = QuestSystemTest.class.getClassLoader()
                .getResourceAsStream("data/quests.csv")) {
            assertNotNull(input);
        }
    }

    @Test
    public void applicationRoutesTravelLogCommandsToTheQuestView() throws Exception {
        Path storage = Files.createTempFile("pvz-quest-menu", ".json");
        Files.deleteIfExists(storage);

        try {
            ApplicationController controller = new ApplicationController(new UserRepository(storage));
            controller.execute(
                    "register -u questuser -p Strong#123 Strong#123 "
                            + "-n QuestUser -e quest@example.com -g male"
            );
            controller.execute("pick question -q 1 -a blue -c blue");
            controller.execute("login -u questuser -p Strong#123");
            controller.execute("menu enter game");
            controller.execute("menu travel-log");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PrintStream previousOutput = System.out;
            try {
                System.setOut(new PrintStream(output, true, "UTF-8"));
                assertEquals("", controller.execute("travel log page community"));
            } finally {
                System.setOut(previousOutput);
            }

            String text = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(text.contains("Travel Log page: COMMUNITY"));
            assertTrue(text.contains("Only Cactus"));
            assertTrue(text.contains("condition:"));
            assertTrue(text.contains("reward:"));
        } finally {
            Files.deleteIfExists(storage);
        }
    }

    private User createUser() {
        User user = new User(
                "quest-user",
                "hashed-password",
                "quest-player",
                "quest@example.com",
                1,
                "answer",
                UserGender.MALE
        );
        user.initializeMissingFields();
        return user;
    }
}
