package network.izombie.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IZombieReactionCatalog {
    private static final Map<String, IZombieReaction> REACTIONS = createReactions();

    private IZombieReactionCatalog() {
    }

    public static IZombieReaction find(String reactionId) {
        if (reactionId == null) {
            return null;
        }

        return REACTIONS.get(reactionId.trim().toLowerCase());
    }

    public static List<IZombieReaction> getAll() {
        return List.copyOf(REACTIONS.values());
    }

    private static Map<String, IZombieReaction> createReactions() {
        Map<String, IZombieReaction> reactions = new LinkedHashMap<>();

        addText(reactions, "text_good_luck", "GOOD LUCK!");

        addText(reactions, "text_nice_move", "NICE MOVE!");

        addText(reactions, "text_well_played", "WELL PLAYED!");

        addEmoji(reactions, "emoji_laugh", "😀");

        addEmoji(reactions, "emoji_angry", "😠");

        addEmoji(reactions, "emoji_surprised", "😮");

        return reactions;
    }

    private static void addText(Map<String, IZombieReaction> reactions, String id, String value) {
        reactions.put(id, new IZombieReaction(id, IZombieReactionKind.TEXT, value));
    }

    private static void addEmoji(Map<String, IZombieReaction> reactions, String id, String value) {
        reactions.put(id, new IZombieReaction(id, IZombieReactionKind.EMOJI, value));
    }
}
