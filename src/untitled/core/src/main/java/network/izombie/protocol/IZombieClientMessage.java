package network.izombie.protocol;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class IZombieClientMessage {
    private IZombieClientMessageType type;
    private String requestId;
    private String targetUsername;
    private String invitationId;
    private String matchId;
    private int stageNumber;
    private boolean accepted;
    private String unitKey;
    private int column;
    private int row;
    private String reactionId;

    private IZombieClientMessage() {
    }

    public static IZombieClientMessage invite(String requestId, String targetUsername, int stageNumber) {
        IZombieClientMessage message = create(IZombieClientMessageType.INVITE_PLAYER, requestId);

        message.targetUsername = targetUsername;
        message.stageNumber = stageNumber;
        return message;
    }

    public static IZombieClientMessage respondToInvitation(String requestId, String invitationId, boolean accepted) {
        IZombieClientMessage message = create(IZombieClientMessageType.RESPOND_TO_INVITATION, requestId);

        message.invitationId = invitationId;
        message.accepted = accepted;
        return message;
    }

    public static IZombieClientMessage joinRandomQueue(String requestId, int stageNumber) {
        IZombieClientMessage message = create(IZombieClientMessageType.JOIN_RANDOM_QUEUE, requestId);

        message.stageNumber = stageNumber;
        return message;
    }

    public static IZombieClientMessage leaveRandomQueue(String requestId) {
        return create(IZombieClientMessageType.LEAVE_RANDOM_QUEUE, requestId);
    }

    public static IZombieClientMessage placePlant(String requestId, String matchId, String plantName, int column, int row) {
        return createPlacement(IZombieClientMessageType.PLACE_PLANT, requestId, matchId, plantName, column, row);
    }

    public static IZombieClientMessage placeZombie(String requestId, String matchId, String zombieAlias, int column, int row) {
        return createPlacement(IZombieClientMessageType.PLACE_ZOMBIE, requestId, matchId, zombieAlias, column, row);
    }

    public static IZombieClientMessage sendReaction(String requestId, String matchId, String reactionId) {
        IZombieClientMessage message = create(IZombieClientMessageType.SEND_REACTION, requestId);

        message.matchId = matchId;
        message.reactionId = reactionId;
        return message;
    }

    public static IZombieClientMessage leaveMatch(String requestId, String matchId) {
        IZombieClientMessage message = create(IZombieClientMessageType.LEAVE_MATCH, requestId);

        message.matchId = matchId;
        return message;
    }

    private static IZombieClientMessage createPlacement(IZombieClientMessageType type, String requestId, String matchId,
                                                        String unitKey, int column, int row) {
        IZombieClientMessage message = create(type, requestId);

        message.matchId = matchId;
        message.unitKey = unitKey;
        message.column = column;
        message.row = row;
        return message;
    }

    private static IZombieClientMessage create(IZombieClientMessageType type, String requestId) {
        IZombieClientMessage message = new IZombieClientMessage();

        message.type = type;
        message.requestId = requestId;
        return message;
    }
}
