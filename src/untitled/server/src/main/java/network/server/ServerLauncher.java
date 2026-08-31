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
        int port = args.length == 0
            ? portSetting("pvz.server.port", "PVZ_SERVER_PORT", DEFAULT_PORT)
            : parsePort(args[0]);
        String host = setting("pvz.server.host", "PVZ_SERVER_BIND_HOST", DEFAULT_HOST);
        String defaultStorage = Paths.get(System.getProperty("user.home"),
            ".plants-vs-zombies-2", "server", "users.json").toString();
        Path storagePath = Paths.get(setting("pvz.server.data", "PVZ_SERVER_DATA", defaultStorage));
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

    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environment);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int portSetting(String property, String environment, int fallback) {
        return parsePort(setting(property, environment, String.valueOf(fallback)));
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Server port must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Server port must be a number", exception);
        }
    }
}
