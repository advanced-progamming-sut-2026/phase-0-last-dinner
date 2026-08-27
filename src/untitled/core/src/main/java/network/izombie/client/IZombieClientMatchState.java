package network.izombie.client;

import lombok.Getter;
import network.izombie.protocol.IZombieMatchSnapshot;
import network.izombie.protocol.IZombieRole;
import network.izombie.protocol.IZombieServerEvent;
import network.izombie.protocol.IZombieServerEventType;

@Getter
public final class IZombieClientMatchState implements IZombieClientListener {

    private volatile IZombieClientPhase phase = IZombieClientPhase.IDLE;

    private volatile String matchId;
    private volatile String opponentUsername;
    private volatile IZombieRole role;
    private volatile int stageNumber;
    private volatile IZombieMatchSnapshot snapshot;

    private volatile String pendingInvitationId;
    private volatile String pendingChallengerUsername;
    private volatile int pendingInvitationStage;

    private volatile String statusMessage;
    private volatile String errorMessage;
    private volatile IZombieReceivedReaction receivedReaction;
    private volatile IZombieServerEventType lastEventType;

    @Override
    public synchronized void onIZombieEvent(IZombieServerEvent event) {
        if (event == null || event.getType() == null) {
            return;
        }

        lastEventType = event.getType();

        switch (event.getType()) {
            case ERROR -> handleError(event);

            case ACTION_REJECTED -> handleActionRejected(event);

            case INVITATION_RECEIVED -> handleInvitationReceived(event);

            case INVITATION_DECLINED -> handleInvitationDeclined(event);

            case QUEUE_JOINED -> handleQueueJoined(event);

            case QUEUE_LEFT -> handleQueueLeft(event);

            case MATCH_STARTED -> handleMatchStarted(event);

            case MATCH_SNAPSHOT -> handleSnapshot(event);

            case REACTION_RECEIVED -> handleReaction(event);

            case MATCH_FINISHED -> handleMatchFinished(event);

            case OPPONENT_LEFT -> handleOpponentLeft(event);

            default -> {
            }
        }
    }

    private void handleError(IZombieServerEvent event) {
        errorMessage = event.getMessage();
        statusMessage = event.getMessage();
    }

    private void handleActionRejected(IZombieServerEvent event) {
        errorMessage = event.getMessage();
        statusMessage = event.getMessage();
    }

    private void handleInvitationReceived(IZombieServerEvent event) {
        pendingInvitationId = event.getInvitationId();
        pendingChallengerUsername = event.getOpponentUsername();

        pendingInvitationStage = event.getStageNumber();
        statusMessage = event.getMessage();
        errorMessage = null;

        phase = IZombieClientPhase.INVITATION_RECEIVED;
    }

    private void handleInvitationDeclined(IZombieServerEvent event) {
        clearPendingInvitation();

        statusMessage = event.getMessage();
        phase = IZombieClientPhase.IDLE;
    }

    private void handleQueueJoined(IZombieServerEvent event) {
        statusMessage = event.getMessage();
        errorMessage = null;

        phase = IZombieClientPhase.SEARCHING_RANDOM_MATCH;
    }

    private void handleQueueLeft(IZombieServerEvent event) {
        statusMessage = event.getMessage();
        phase = IZombieClientPhase.IDLE;
    }

    private void handleMatchStarted(IZombieServerEvent event) {
        matchId = event.getMatchId();
        opponentUsername = event.getOpponentUsername();
        role = event.getRole();
        stageNumber = event.getStageNumber();

        snapshot = null;
        receivedReaction = null;
        errorMessage = null;
        statusMessage = event.getMessage();

        clearPendingInvitation();
        phase = IZombieClientPhase.IN_MATCH;
    }

    private void handleSnapshot(IZombieServerEvent event) {
        IZombieMatchSnapshot receivedSnapshot = event.getSnapshot();

        if (receivedSnapshot == null) {
            return;
        }

        if (matchId != null && !matchId.equals(receivedSnapshot.matchId())) {
            return;
        }

        snapshot = receivedSnapshot;
    }

    private void handleReaction(IZombieServerEvent event) {
        if (!belongsToCurrentMatch(event.getMatchId())) {
            return;
        }

        if (event.getReaction() == null) {
            return;
        }

        receivedReaction = new IZombieReceivedReaction(event.getOpponentUsername(), event.getReaction());
    }

    private void handleMatchFinished(IZombieServerEvent event) {
        if (!belongsToCurrentMatch(event.getMatchId())) {
            return;
        }

        if (event.getSnapshot() != null) {
            snapshot = event.getSnapshot();
        }

        statusMessage = event.getMessage();
        phase = IZombieClientPhase.MATCH_FINISHED;
    }

    private void handleOpponentLeft(IZombieServerEvent event) {
        if (!belongsToCurrentMatch(event.getMatchId())) {
            return;
        }

        statusMessage = event.getMessage();
    }

    private boolean belongsToCurrentMatch(String eventMatchId) {
        return matchId != null && matchId.equals(eventMatchId);
    }

    public boolean isInMatch() {
        return phase == IZombieClientPhase.IN_MATCH;
    }

    public boolean isPlantPlayer() {
        return role == IZombieRole.PLANTS;
    }

    public boolean isZombiePlayer() {
        return role == IZombieRole.ZOMBIES;
    }

    public synchronized String consumeErrorMessage() {
        String result = errorMessage;
        errorMessage = null;
        return result;
    }

    public synchronized IZombieReceivedReaction consumeReceivedReaction() {

        IZombieReceivedReaction result = receivedReaction;

        receivedReaction = null;
        return result;
    }

    public synchronized void resetToIdle() {
        phase = IZombieClientPhase.IDLE;

        matchId = null;
        opponentUsername = null;
        role = null;
        stageNumber = 0;
        snapshot = null;

        statusMessage = null;
        errorMessage = null;
        receivedReaction = null;
        lastEventType = null;

        clearPendingInvitation();
    }

    private void clearPendingInvitation() {
        pendingInvitationId = null;
        pendingChallengerUsername = null;
        pendingInvitationStage = 0;
    }

    public synchronized void dismissPendingInvitation() {
        clearPendingInvitation();

        if (phase == IZombieClientPhase.INVITATION_RECEIVED)
            phase = IZombieClientPhase.IDLE;
    }

    public synchronized void markLeavingMatch() {
        if (phase != IZombieClientPhase.IN_MATCH)
            return;

        phase = IZombieClientPhase.LEAVING_MATCH;
        statusMessage = "Leaving I Zombie match...";
    }
}
