package network.izombie.server;

import network.izombie.protocol.IZombieClientMessage;
import network.izombie.protocol.IZombieRole;
import network.izombie.protocol.IZombieServerEvent;
import network.izombie.protocol.IZombieServerEventType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Locale;

public final class IZombieMultiplayerService implements IZombieMatchLifecycle {

    private static final int MINIMUM_STAGE = 1;
    private static final int MAXIMUM_STAGE = 3;

    private final IZombieServerTransport transport;
    private final IZombieMatchFactory matchFactory;
    private final Random random;
    private final AtomicLong idSequence;

    private final Map<String, Invitation> invitations;
    private final Map<String, IZombieMatchSession> matchesByUser;
    private final Deque<QueueEntry> randomQueue;

    public IZombieMultiplayerService(IZombieServerTransport transport, IZombieMatchFactory matchFactory) {
        this(transport, matchFactory, new Random());
    }

    public IZombieMultiplayerService(IZombieServerTransport transport, IZombieMatchFactory matchFactory, Random random) {
        if (transport == null || matchFactory == null) {
            throw new IllegalArgumentException("IZombie multiplayer dependencies are required.");
        }

        this.transport = transport;
        this.matchFactory = matchFactory;
        this.random = random == null ? new Random() : random;

        this.idSequence = new AtomicLong();
        this.invitations = new LinkedHashMap<>();
        this.matchesByUser = new LinkedHashMap<>();
        this.randomQueue = new ArrayDeque<>();
    }

    public synchronized void handleMessage(String authenticatedUsername, IZombieClientMessage message) {
        if (!isValidUsername(authenticatedUsername)) {
            return;
        }

        if (message == null || message.getType() == null) {
            sendError(authenticatedUsername, requestId(message), "Invalid IZombie request.");
            return;
        }

        switch (message.getType()) {
            case INVITE_PLAYER:
                invite(authenticatedUsername, message);
                break;

            case RESPOND_TO_INVITATION:
                respondToInvitation(authenticatedUsername, message);
                break;

            case JOIN_RANDOM_QUEUE:
                joinRandomQueue(authenticatedUsername, message);
                break;

            case LEAVE_RANDOM_QUEUE:
                leaveRandomQueue(authenticatedUsername, message.getRequestId());
                break;

            default:
                routeMatchMessage(authenticatedUsername, message);
                break;
        }
    }

    public synchronized void handleDisconnect(String username) {
        removeFromQueue(username);
        removeInvitationsFor(username);

        IZombieMatchSession match = this.matchesByUser.get(userKey(username));

        if (match != null) {
            match.handleDisconnect(username);
        }
    }

    private void invite(String challenger, IZombieClientMessage message) {
        String target = clean(message.getTargetUsername());

        if (!isValidStage(message.getStageNumber())) {
            sendError(challenger, message.getRequestId(), "Stage number must be between 1 and 3.");
            return;
        }

        if (!isValidInvitationTarget(challenger, target, message.getRequestId())) {
            return;
        }

        String invitationId = nextId("invite");

        Invitation invitation = new Invitation(challenger, target, message.getStageNumber());

        this.invitations.put(invitationId, invitation);

        this.transport.sendToUser(target, IZombieServerEvent.invitation(invitationId, challenger, message.getStageNumber()));
    }

    private boolean isValidInvitationTarget(String challenger, String target, String requestId) {
        if (!isValidUsername(target)) {
            sendError(challenger, requestId, "The selected username is invalid.");
            return false;
        }

        if (challenger.equalsIgnoreCase(target)) {
            sendError(challenger, requestId, "You cannot invite yourself.");
            return false;
        }

        if (!this.transport.isUserOnline(target)) {
            sendError(challenger, requestId, "The selected user is offline.");
            return false;
        }

        if (isBusy(challenger) || isBusy(target)) {
            sendError(challenger, requestId, "One of the players is already in a match.");
            return false;
        }

        return true;
    }

    private void respondToInvitation(String recipient, IZombieClientMessage message) {
        String invitationId = clean(message.getInvitationId());

        Invitation invitation = this.invitations.remove(invitationId);

        if (invitation == null || !invitation.recipient().equalsIgnoreCase(recipient)) {
            sendError(recipient, message.getRequestId(), "This invitation is no longer available.");
            return;
        }

        if (!message.isAccepted()) {
            declineInvitation(invitation, recipient);
            return;
        }

        if (!canStartInvitedMatch(invitation, recipient, message.getRequestId())) {
            return;
        }

        startRandomRoleMatch(invitation.challenger(), recipient, invitation.stageNumber());
    }

    private boolean canStartInvitedMatch(Invitation invitation, String recipient, String requestId) {
        if (!this.transport.isUserOnline(invitation.challenger())) {
            sendError(recipient, requestId, "The challenger is no longer online.");
            return false;
        }

        if (isBusy(invitation.challenger()) || isBusy(recipient)) {
            sendError(recipient, requestId, "One of the players is already in a match.");
            return false;
        }

        return true;
    }

    private void declineInvitation(Invitation invitation, String recipient) {
        IZombieServerEvent event = IZombieServerEvent.information(IZombieServerEventType.INVITATION_DECLINED,
            null, recipient + " declined your IZombie invitation.");

        this.transport.sendToUser(invitation.challenger(), event);
    }

    private void joinRandomQueue(String username, IZombieClientMessage message) {
        if (!isValidStage(message.getStageNumber())) {
            sendError(username, message.getRequestId(), "Stage number must be between 1 and 3.");
            return;
        }

        if (isBusy(username) || isQueued(username)) {
            sendError(username, message.getRequestId(), "You are already busy or waiting for a match.");
            return;
        }

        QueueEntry opponent = findOpponent(username, message.getStageNumber());

        if (opponent == null) {
            addToRandomQueue(username, message);
            return;
        }

        startRandomRoleMatch(opponent.username(), username, message.getStageNumber());
    }

    private void addToRandomQueue(String username, IZombieClientMessage message) {
        this.randomQueue.addLast(new QueueEntry(username, message.getStageNumber()));

        sendInformation(username, IZombieServerEventType.QUEUE_JOINED, message.getRequestId(),
            "Waiting for another player.");
    }

    private QueueEntry findOpponent(String username, int stageNumber) {
        Iterator<QueueEntry> iterator = this.randomQueue.iterator();

        while (iterator.hasNext()) {
            QueueEntry entry = iterator.next();

            if (!this.transport.isUserOnline(entry.username()) || isBusy(entry.username())) {
                iterator.remove();
                continue;
            }

            if (!entry.username().equalsIgnoreCase(username) && entry.stageNumber() == stageNumber) {
                iterator.remove();
                return entry;
            }
        }

        return null;
    }

    private void leaveRandomQueue(String username, String requestId) {
        boolean removed = removeFromQueue(username);

        String message = removed ? "You left the random queue." : "You were not in the random queue.";

        sendInformation(username, IZombieServerEventType.QUEUE_LEFT, requestId, message);
    }

    private void routeMatchMessage(String username, IZombieClientMessage message) {
        IZombieMatchSession match = this.matchesByUser.get(userKey(username));

        if (match == null) {
            sendError(username, message.getRequestId(), "You do not have an active IZombie match.");
            return;
        }

        if (message.getMatchId() == null || !match.getMatchId().equals(message.getMatchId())) {
            sendError(username, message.getRequestId(), "The match identifier is invalid.");
            return;
        }

        match.handleMessage(username, message);
    }

    private void startRandomRoleMatch(String firstUsername, String secondUsername, int stageNumber) {
        boolean firstControlsPlants = this.random.nextBoolean();

        String plantUsername = firstControlsPlants ? firstUsername : secondUsername;

        String zombieUsername = firstControlsPlants ? secondUsername : firstUsername;

        startMatch(plantUsername, zombieUsername, stageNumber);
    }

    private void startMatch(String plantUsername, String zombieUsername, int stageNumber) {
        String matchId = nextId("match");

        IZombieMatchSession match;

        try {
            match = this.matchFactory.create(matchId, plantUsername, zombieUsername, stageNumber, this.transport,
                this);
        } catch (RuntimeException exception) {
            System.err.println("Could not create I, Zombie match " + matchId + " for " + plantUsername + " and "
                    + zombieUsername);
            exception.printStackTrace(System.err);

            notifyMatchCreationFailure(plantUsername, zombieUsername);
            return;
        }

        if (match == null) {
            notifyMatchCreationFailure(plantUsername, zombieUsername);
            return;
        }

        registerMatch(match, plantUsername, zombieUsername);

        sendMatchStarted(match, plantUsername, zombieUsername, stageNumber);
    }

    private void registerMatch(IZombieMatchSession match, String plantUsername, String zombieUsername) {
        this.matchesByUser.put(userKey(plantUsername), match);
        this.matchesByUser.put(userKey(zombieUsername), match);

        removeFromQueue(plantUsername);
        removeFromQueue(zombieUsername);

        removeInvitationsFor(plantUsername);
        removeInvitationsFor(zombieUsername);
    }

    private void sendMatchStarted(IZombieMatchSession match, String plantUsername, String zombieUsername, int stageNumber) {
        this.transport.sendToUser(plantUsername, IZombieServerEvent.matchStarted(match.getMatchId(), zombieUsername,
            IZombieRole.PLANTS, stageNumber));

        this.transport.sendToUser(zombieUsername, IZombieServerEvent.matchStarted(match.getMatchId(), plantUsername,
            IZombieRole.ZOMBIES, stageNumber));

        sendInitialSnapshot(match, plantUsername, zombieUsername);
    }

    private void sendInitialSnapshot(IZombieMatchSession match, String plantUsername, String zombieUsername) {
        if (match.getSnapshot() == null) {
            return;
        }

        IZombieServerEvent event = IZombieServerEvent.snapshot(match.getSnapshot());

        this.transport.sendToUser(plantUsername, event);

        this.transport.sendToUser(zombieUsername, event);
    }

    private void notifyMatchCreationFailure(String plantUsername, String zombieUsername) {
        removeFromQueue(plantUsername);
        removeFromQueue(zombieUsername);

        removeInvitationsFor(plantUsername);
        removeInvitationsFor(zombieUsername);

        sendInformation(plantUsername, IZombieServerEventType.QUEUE_LEFT, null,
            "Random matchmaking was cancelled.");
        sendInformation(zombieUsername, IZombieServerEventType.QUEUE_LEFT, null,
            "Random matchmaking was cancelled.");

        sendError(plantUsername, null, "The IZombie match could not be created.");
        sendError(zombieUsername, null, "The IZombie match could not be created.");
    }

    @Override
    public synchronized void onMatchClosed(String matchId, String plantUsername, String zombieUsername) {
        removeMatchFor(matchId, plantUsername);
        removeMatchFor(matchId, zombieUsername);
    }

    private void removeMatchFor(String matchId, String username) {
        String key = userKey(username);

        IZombieMatchSession match = this.matchesByUser.get(key);

        if (match != null && match.getMatchId().equals(matchId))
            this.matchesByUser.remove(key);
    }

    private void removeInvitationsFor(String username) {
        this.invitations.values().removeIf(
            invitation ->
                invitation.challenger().equalsIgnoreCase(username) || invitation.recipient().equalsIgnoreCase(username)
        );
    }

    private boolean removeFromQueue(String username) {
        return this.randomQueue.removeIf(entry -> entry.username().equalsIgnoreCase(username));
    }

    private boolean isQueued(String username) {
        for (QueueEntry entry : this.randomQueue) {
            if (entry.username().equalsIgnoreCase(username))
                return true;
        }

        return false;
    }

    private boolean isBusy(String username) {
        return this.matchesByUser.containsKey(userKey(username));
    }

    private boolean isValidStage(int stageNumber) {
        return stageNumber >= MINIMUM_STAGE && stageNumber <= MAXIMUM_STAGE;
    }

    private boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty();
    }

    private String nextId(String prefix) {
        return prefix + "-" + this.idSequence.incrementAndGet();
    }

    private String requestId(IZombieClientMessage message) {
        return message == null ? null : message.getRequestId();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String userKey(String username) {
        String cleaned = clean(username);
        if (cleaned == null)
            return "";
        return cleaned.toLowerCase(Locale.ROOT);
    }

    private void sendError(String username, String requestId, String message) {
        if (!isValidUsername(username)) {
            return;
        }

        this.transport.sendToUser(username, IZombieServerEvent.error(requestId, message));
    }

    private void sendInformation(String username, IZombieServerEventType type, String requestId, String message) {
        this.transport.sendToUser(username, IZombieServerEvent.information(type, requestId, message));
    }

    private record Invitation(String challenger, String recipient, int stageNumber) {
    }

    private record QueueEntry(String username, int stageNumber) {
    }
}
