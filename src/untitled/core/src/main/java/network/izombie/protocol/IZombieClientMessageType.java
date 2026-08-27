package network.izombie.protocol;

public enum IZombieClientMessageType {
    INVITE_PLAYER,
    RESPOND_TO_INVITATION,
    JOIN_RANDOM_QUEUE,
    LEAVE_RANDOM_QUEUE,
    PLACE_PLANT,
    PLACE_ZOMBIE,
    SEND_REACTION,
    LEAVE_MATCH
}
