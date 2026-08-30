package model.minigame.izombieminigame.multiplayer;

import lombok.Getter;
import network.izombie.protocol.IZombieClientMessage;
import network.izombie.protocol.IZombieMatchSnapshot;
import network.izombie.protocol.IZombieMatchStatus;
import network.izombie.protocol.IZombieReaction;
import network.izombie.protocol.IZombieReactionCatalog;
import network.izombie.protocol.IZombieRole;
import network.izombie.protocol.IZombieServerEvent;
import network.izombie.server.IZombieMatchLifecycle;
import network.izombie.server.IZombieMatchSession;
import network.izombie.server.IZombieServerTransport;

import java.util.Objects;

public class IZombieAuthoritativeMatchSession implements IZombieMatchSession {

    private final String matchId;
    private final String plantUsername;
    private final String zombieUsername;
    @Getter
    private final int stageNumber;

    private final IZombieMultiplayerIntegration integration;
    private final IZombieMatchLoadout loadout;
    private final IZombiePlacementService placementService;
    private final IZombieSnapshotBuilder snapshotBuilder;

    private final IZombieServerTransport transport;
    private final IZombieMatchLifecycle lifecycle;

    private long serverTick;
    private long remainingTicks;
    private IZombieMatchStatus status;
    private boolean closed;

    public IZombieAuthoritativeMatchSession(String matchId, String plantUsername, String zombieUsername,
                                            int stageNumber, IZombieMultiplayerIntegration integration,
                                            IZombieServerTransport transport, IZombieMatchLifecycle lifecycle) {
        this.matchId = requireText(matchId, "Match ID");
        this.plantUsername = requireText(plantUsername, "Plant username");
        this.zombieUsername = requireText(zombieUsername, "Zombie username");

        if (plantUsername.equalsIgnoreCase(zombieUsername)) {
            throw new IllegalArgumentException("Both match players cannot have the same username.");
        }

        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException("I, Zombie stage must be between 1 and 3.");
        }

        this.stageNumber = stageNumber;
        this.integration = Objects.requireNonNull(integration);
        this.transport = Objects.requireNonNull(transport);
        this.lifecycle = Objects.requireNonNull(lifecycle);

        this.loadout = new IZombieMatchLoadoutFactory(integration, stageNumber).create();

        this.placementService = new IZombiePlacementService(integration, loadout);

        this.snapshotBuilder = new IZombieSnapshotBuilder(matchId, stageNumber, plantUsername, zombieUsername,
            integration, loadout);

        this.remainingTicks = IZombieMatchRules.MATCH_DURATION_TICKS;

        this.status = IZombieMatchStatus.RUNNING;
    }

    @Override
    public String getMatchId() {
        return matchId;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public IZombieRole getRole(String username) {
        if (username == null)
            return null;

        if (plantUsername.equalsIgnoreCase(username))
            return IZombieRole.PLANTS;

        if (zombieUsername.equalsIgnoreCase(username))
            return IZombieRole.ZOMBIES;

        return null;
    }

    @Override
    public synchronized IZombieMatchSnapshot getSnapshot() {
        return snapshotBuilder.build(serverTick, remainingTicks, status);
    }

    @Override
    public synchronized void handleMessage(String authenticatedUsername, IZombieClientMessage message) {
        if (closed || message == null) {
            return;
        }

        IZombieRole role = getRole(authenticatedUsername);

        if (role == null) {
            reject(authenticatedUsername, message, "You are not a participant in this match.");
            return;
        }

        if (status != IZombieMatchStatus.RUNNING) {
            reject(authenticatedUsername, message, "This match is no longer running.");
            return;
        }

        if (message.getType() == null) {
            reject(authenticatedUsername, message, "Action type is missing.");
            return;
        }

        switch (message.getType()) {
            case PLACE_PLANT -> handlePlantPlacement(authenticatedUsername, role, message);

            case PLACE_ZOMBIE -> handleZombiePlacement(authenticatedUsername, role, message);

            case SEND_REACTION -> handleReaction(authenticatedUsername, message);

            case LEAVE_MATCH -> handlePlayerLeft(authenticatedUsername);

            default -> reject(authenticatedUsername, message, "This action is not supported inside a match.");
        }
    }

    private void handlePlantPlacement(String username, IZombieRole role, IZombieClientMessage message) {
        if (role != IZombieRole.PLANTS) {
            reject(username, message, "Only the plant player can place plants.");
            return;
        }

        IZombieMatchActionResult result = placementService.placePlant(message.getUnitKey(), message.getColumn(),
            message.getRow());

        if (!result.successful()) {
            reject(username, message, result.message());
            return;
        }

        broadcastSnapshot();
    }

    private void handleZombiePlacement(String username, IZombieRole role, IZombieClientMessage message) {
        if (role != IZombieRole.ZOMBIES) {
            reject(username, message, "Only the zombie player can place zombies.");
            return;
        }

        IZombieMatchActionResult result = placementService.placeZombie(message.getUnitKey(), message.getColumn(),
            message.getRow());

        if (!result.successful()) {
            reject(username, message, result.message());
            return;
        }

        broadcastSnapshot();
    }

    private void handleReaction(String senderUsername, IZombieClientMessage message) {
        IZombieReaction reaction = findReaction(message.getReactionId());

        if (reaction == null) {
            reject(senderUsername, message, "Selected reaction does not exist.");
            return;
        }

        String opponentUsername = getOpponent(senderUsername);

        transport.sendToUser(opponentUsername, IZombieServerEvent.reaction(matchId, senderUsername, reaction));
    }

    private IZombieReaction findReaction(String reactionId) {
        if (reactionId == null || reactionId.isBlank()) {
            return null;
        }

        for (IZombieReaction reaction : IZombieReactionCatalog.getAll()) {

            if (reaction.id().equalsIgnoreCase(reactionId)) {
                return reaction;
            }
        }

        return null;
    }

    public synchronized void advanceOneTick() {
        if (closed || status != IZombieMatchStatus.RUNNING) {
            return;
        }

        serverTick++;
        remainingTicks = Math.max(0, remainingTicks - 1);

        loadout.plantResources().advanceOneTick();
        loadout.zombieResources().advanceOneTick();

        producePassiveSunIfNeeded();
        integration.advanceOneTick();

        if (areAllBrainsEaten()) {
            finishMatch(IZombieMatchStatus.ZOMBIES_WON, "The zombie player ate all brains.");
            return;
        }

        if (remainingTicks == 0) {
            finishMatch(IZombieMatchStatus.PLANTS_WON, "The plant player protected the remaining brains.");
            return;
        }

        broadcastSnapshot();
    }

    private void producePassiveSunIfNeeded() {
        if (!IZombieMatchRules.shouldProducePassiveSun(serverTick)) {
            return;
        }

        int amount = IZombieMatchRules.PASSIVE_SUN_AMOUNT;

        loadout.plantResources().addSun(amount);
        loadout.zombieResources().addSun(amount);
    }

    private boolean areAllBrainsEaten() {
        for (int row = 0; row < IZombieMatchRules.BOARD_ROW_COUNT; row++) {
            if (!integration.isBrainEaten(row + 1)) {
                return false;
            }
        }

        return true;
    }

    private void broadcastSnapshot() {
        IZombieMatchSnapshot snapshot = getSnapshot();
        IZombieServerEvent event = IZombieServerEvent.snapshot(snapshot);

        transport.sendToUser(plantUsername, event);
        transport.sendToUser(zombieUsername, event);
    }

    private void reject(String username, IZombieClientMessage message, String reason) {
        if (username == null) {
            return;
        }

        String requestId = message == null ? null : message.getRequestId();

        transport.sendToUser(username, IZombieServerEvent.actionRejected(requestId, matchId, reason));
    }

    @Override
    public synchronized void handleDisconnect(String username) {
        if (closed || getRole(username) == null) {
            return;
        }

        handlePlayerLeft(username);
    }

    private void handlePlayerLeft(String username) {
        IZombieRole leavingRole = getRole(username);

        if (leavingRole == null) {
            return;
        }

        String opponentUsername = getOpponent(username);

        transport.sendToUser(opponentUsername, IZombieServerEvent.opponentLeft(matchId, username));

        IZombieMatchStatus winnerStatus = leavingRole == IZombieRole.PLANTS ? IZombieMatchStatus.ZOMBIES_WON :
            IZombieMatchStatus.PLANTS_WON;

        finishMatch(winnerStatus, username + " left the match.");
    }

    private void finishMatch(IZombieMatchStatus finalStatus, String message) {
        if (closed) {
            return;
        }

        status = finalStatus;

        IZombieMatchSnapshot finalSnapshot = getSnapshot();

        IZombieServerEvent event = IZombieServerEvent.matchFinished(finalSnapshot, message);

        transport.sendToUser(plantUsername, event);
        transport.sendToUser(zombieUsername, event);

        closeMatch();
    }

    private void closeMatch() {
        if (closed) {
            return;
        }

        closed = true;

        lifecycle.onMatchClosed(matchId, plantUsername, zombieUsername);
    }

    private String getOpponent(String username) {
        if (username == null)
            return null;

        if (plantUsername.equalsIgnoreCase(username))
            return zombieUsername;

        if (zombieUsername.equalsIgnoreCase(username))
            return plantUsername;

        return null;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }

        return value.trim();
    }

    public synchronized void cancelBecauseOfServerError() {
        if (closed)
            return;

        finishMatch(IZombieMatchStatus.CANCELLED, "The match was cancelled because of a server error.");
    }
}
