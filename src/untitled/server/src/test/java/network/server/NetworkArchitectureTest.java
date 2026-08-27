package network.server;

import network.client.GameClient;
import network.protocol.NetworkResponse;
import network.protocol.RequestType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NetworkArchitectureTest {
    @Test
    public void clientCanReachServerThroughRequestResponseProtocol() throws Exception {
        try (GameServer server = new GameServer("127.0.0.1", 0, RequestRouter.withDefaults())) {
            server.start();
            try (GameClient client = new GameClient("127.0.0.1", server.getPort())) {
                NetworkResponse response = client.send(RequestType.PING, null);

                assertTrue(response.isSuccessful());
                assertEquals("ok", response.getPayload().get("status").getAsString());
            }
        }
    }
}
