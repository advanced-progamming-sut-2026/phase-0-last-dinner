package network.izombie.client;

import network.izombie.protocol.IZombieReaction;

public record IZombieReceivedReaction(String senderUsername, IZombieReaction reaction) {
}
