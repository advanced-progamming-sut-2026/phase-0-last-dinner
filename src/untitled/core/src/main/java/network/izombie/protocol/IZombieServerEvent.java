package network.izombie.protocol;

import lombok.Getter;

@Getter
public final class IZombieServerEvent {
    private final IZombieServerEventType type;
    private final String requestId;
    private final String message;
    private final String invitationId;
    private final String matchId;
    private final String opponentUsername;
    private final IZombieRole role;
    private final int stageNumber;
    private final IZombieReaction reaction;
    private final IZombieMatchSnapshot snapshot;

    private IZombieServerEvent(IZombieServerEventType type, String requestId, String message, String invitationId,
                               String matchId, String opponentUsername, IZombieRole role, int stageNumber,
                               IZombieReaction reaction, IZombieMatchSnapshot snapshot) {
        this.type = type;
        this.requestId = requestId;
        this.message = message;
        this.invitationId = invitationId;
        this.matchId = matchId;
        this.opponentUsername = opponentUsername;
        this.role = role;
        this.stageNumber = stageNumber;
        this.reaction = reaction;
        this.snapshot = snapshot;
    }

    public static IZombieServerEvent error(String requestId, String message) {
        return basic(IZombieServerEventType.ERROR, requestId, message);
    }

    public static IZombieServerEvent actionRejected(String requestId, String matchId, String message) {
        return new IZombieServerEvent(IZombieServerEventType.ACTION_REJECTED, requestId, message, null,
            matchId, null, null, 0, null, null);
    }

    public static IZombieServerEvent information(IZombieServerEventType type, String requestId, String message) {
        return basic(type, requestId, message);
    }

    public static IZombieServerEvent invitation(String invitationId, String challenger, int stageNumber) {
        return new IZombieServerEvent(IZombieServerEventType.INVITATION_RECEIVED, null, challenger +
            " invited you to I, Zombie.", invitationId, null, challenger, null, stageNumber, null,
            null);
    }

    public static IZombieServerEvent matchStarted(String matchId, String opponentUsername, IZombieRole role, int stageNumber) {
        return new IZombieServerEvent(IZombieServerEventType.MATCH_STARTED, null, "I, Zombie match started.",
            null, matchId, opponentUsername, role, stageNumber, null, null);
    }

    public static IZombieServerEvent snapshot(IZombieMatchSnapshot snapshot) {
        String matchId = snapshot == null ? null : snapshot.matchId();

        return new IZombieServerEvent(IZombieServerEventType.MATCH_SNAPSHOT, null, null, null,
            matchId, null, null, 0, null, snapshot);
    }

    public static IZombieServerEvent reaction(String matchId, String senderUsername, IZombieReaction reaction) {
        return new IZombieServerEvent(IZombieServerEventType.REACTION_RECEIVED, null, null, null,
            matchId, senderUsername, null, 0, reaction, null);
    }

    public static IZombieServerEvent matchFinished(IZombieMatchSnapshot snapshot, String message) {
        String matchId = snapshot == null ? null : snapshot.matchId();

        return new IZombieServerEvent(IZombieServerEventType.MATCH_FINISHED, null, message, null, matchId,
            null, null, 0, null, snapshot);
    }

    public static IZombieServerEvent opponentLeft(String matchId, String opponentUsername) {
        return new IZombieServerEvent(IZombieServerEventType.OPPONENT_LEFT, null, opponentUsername +
            " left the match.", null, matchId, opponentUsername, null, 0, null, null);
    }

    private static IZombieServerEvent basic(IZombieServerEventType type, String requestId, String message) {
        return new IZombieServerEvent(type, requestId, message, null, null, null, null,
            0, null, null);
    }
}
