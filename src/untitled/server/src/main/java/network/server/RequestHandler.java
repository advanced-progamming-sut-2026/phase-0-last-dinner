package network.server;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface RequestHandler {
    JsonObject handle(JsonObject payload);
}
