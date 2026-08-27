package network.izombie.client;

import lombok.Getter;
import network.izombie.protocol.IZombieClientMessage;
import network.izombie.protocol.IZombieReaction;
import network.izombie.protocol.IZombieReactionCatalog;
import network.izombie.protocol.IZombieRole;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class IZombieClientController {

    private final IZombieClientGateway gateway;
    @Getter
    private final IZombieClientMatchState state;

    private final AtomicLong requestSequence = new AtomicLong();

    private final String requestPrefix = UUID.randomUUID().toString();

    public IZombieClientController(IZombieClientGateway gateway, IZombieClientMatchState state) {
        if (gateway == null || state == null) {
            throw new IllegalArgumentException("Gateway and client state are required.");
        }

        this.gateway = gateway;
        this.state = state;

        gateway.setListener(state);
    }

    public boolean isConnected() {
        return gateway.isConnected();
    }

    public IZombieClientCommandResult invitePlayer(String targetUsername, int stageNumber) {
        if (targetUsername == null || targetUsername.isBlank()) {

            return IZombieClientCommandResult.failure("Opponent username cannot be empty.");
        }

        if (!isValidStage(stageNumber)) {
            return invalidStage();
        }

        IZombieClientMessage message = IZombieClientMessage.invite(nextRequestId(), targetUsername.trim(), stageNumber);

        return send(message);
    }

    public IZombieClientCommandResult respondToPendingInvitation(boolean accepted) {

        String invitationId = state.getPendingInvitationId();

        if (invitationId == null || invitationId.isBlank()) {

            return IZombieClientCommandResult.failure("There is no pending invitation.");
        }

        IZombieClientMessage message = IZombieClientMessage.respondToInvitation(nextRequestId(), invitationId, accepted);

        IZombieClientCommandResult result = send(message);

        if (result.sent()) {
            state.dismissPendingInvitation();
        }

        return result;
    }

    public IZombieClientCommandResult joinRandomQueue(int stageNumber) {
        if (!isValidStage(stageNumber)) {
            return invalidStage();
        }

        if (state.isInMatch()) {
            return IZombieClientCommandResult.failure("You are already inside a match.");
        }

        IZombieClientMessage message = IZombieClientMessage.joinRandomQueue(nextRequestId(), stageNumber);

        return send(message);
    }

    public IZombieClientCommandResult leaveRandomQueue() {
        if (state.getPhase() != IZombieClientPhase.SEARCHING_RANDOM_MATCH) {
            return IZombieClientCommandResult.failure("You are not searching for a random match.");
        }

        IZombieClientMessage message = IZombieClientMessage.leaveRandomQueue(nextRequestId());

        return send(message);
    }

    public IZombieClientCommandResult placeUnit(String unitKey, int column, int row) {
        if (!state.isInMatch()) {
            return IZombieClientCommandResult.failure("There is no active match.");
        }

        if (unitKey == null || unitKey.isBlank()) {
            return IZombieClientCommandResult.failure("A plant or zombie must be selected.");
        }

        if (!isInsideBoard(column, row)) {
            return IZombieClientCommandResult.failure("Selected position is outside the board.");
        }

        String matchId = state.getMatchId();

        if (matchId == null || matchId.isBlank()) {
            return IZombieClientCommandResult.failure("Match ID is missing.");
        }

        IZombieRole role = state.getRole();
        IZombieClientMessage message;

        if (role == IZombieRole.PLANTS) {
            message = IZombieClientMessage.placePlant(nextRequestId(), matchId, unitKey, column, row);
        } else if (role == IZombieRole.ZOMBIES) {
            message = IZombieClientMessage.placeZombie(nextRequestId(), matchId, unitKey, column, row);
        } else {
            return IZombieClientCommandResult.failure("Your match role is missing.");
        }

        return send(message);
    }

    public IZombieClientCommandResult sendReaction(String reactionId) {
        if (!state.isInMatch()) {
            return IZombieClientCommandResult.failure("There is no active match.");
        }

        if (!reactionExists(reactionId)) {
            return IZombieClientCommandResult.failure("Selected reaction does not exist.");
        }

        IZombieClientMessage message = IZombieClientMessage.sendReaction(nextRequestId(), state.getMatchId(), reactionId);

        return send(message);
    }

    public IZombieClientCommandResult leaveMatch() {
        if (!state.isInMatch())
            return IZombieClientCommandResult.failure("There is no active match.");

        IZombieClientMessage message = IZombieClientMessage.leaveMatch(nextRequestId(), state.getMatchId());

        IZombieClientCommandResult result = send(message);

        if (result.sent())
            state.markLeavingMatch();

        return result;
    }

    public void resetFinishedMatch() {
        if (state.getPhase() == IZombieClientPhase.MATCH_FINISHED) {
            state.resetToIdle();
        }
    }

    private IZombieClientCommandResult send(IZombieClientMessage message) {
        if (!gateway.isConnected()) {
            return IZombieClientCommandResult.failure("Connection to the server is unavailable.");
        }

        try {
            gateway.send(message);
            return IZombieClientCommandResult.success();
        } catch (RuntimeException exception) {
            return IZombieClientCommandResult.failure("Could not send the request to the server.");
        }
    }

    private boolean reactionExists(String reactionId) {
        if (reactionId == null || reactionId.isBlank()) {
            return false;
        }

        for (IZombieReaction reaction : IZombieReactionCatalog.getAll()) {

            if (reaction.id().equalsIgnoreCase(reactionId)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidStage(int stageNumber) {
        return stageNumber >= 1 && stageNumber <= 3;
    }

    private boolean isInsideBoard(int column, int row) {
        return column >= 0 && column < 9 && row >= 0 && row < 5;
    }

    private IZombieClientCommandResult invalidStage() {
        return IZombieClientCommandResult.failure("Stage number must be between 1 and 3.");
    }

    private String nextRequestId() {
        return requestPrefix + "-" + requestSequence.incrementAndGet();
    }
}
