package network.server;

import model.minigame.izombieminigame.multiplayer.IZombieAuthoritativeMatchFactory;
import network.izombie.server.IZombieMultiplayerService;
import network.izombie.transport.IZombieNetworkServerAdapter;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ServerLauncher {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8082;

    private ServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]);
        String host = System.getProperty("pvz.server.host", DEFAULT_HOST);
        Path storagePath = Paths.get(System.getProperty(
                "pvz.server.data",
                Paths.get(System.getProperty("user.home"),
                        ".plants-vs-zombies-2", "server", "users.json").toString()
        ));
        RequestRouter router = RequestRouter.withDefaults();
        ServerUserRepository repository = new ServerUserRepository(storagePath);
        ServerAccountService accountService = new ServerAccountService(repository);

        PollingIZombieServerNetworkPort izombieNetworkPort = new PollingIZombieServerNetworkPort(router, accountService);
        IZombieNetworkServerAdapter izombieServerAdapter = new IZombieNetworkServerAdapter(izombieNetworkPort);
        IZombieAuthoritativeMatchFactory izombieMatchFactory = new IZombieAuthoritativeMatchFactory();
        IZombieMultiplayerService izombieMultiplayerService = new IZombieMultiplayerService(izombieServerAdapter,
            izombieMatchFactory);
        izombieServerAdapter.attachService(izombieMultiplayerService);

        accountService.registerRoutes(router);
        new ServerLeaderboardService(repository, accountService).registerRoutes(router);
        GameServer server = new GameServer(host, port, router);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    izombieMatchFactory.close();
                    izombieServerAdapter.close();
                    izombieNetworkPort.close();
                },
                "izombie-server-shutdown"
            ));

        server.start();
        System.out.println("Game server listening on port " + server.getPort());
        server.awaitTermination();
    }
}
