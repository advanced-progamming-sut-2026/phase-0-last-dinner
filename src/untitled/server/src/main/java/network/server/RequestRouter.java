package network.server;

import com.google.gson.JsonObject;
import network.protocol.NetworkRequest;
import network.protocol.NetworkResponse;
import network.protocol.RequestType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class RequestRouter {
    private final Map<RequestType, RequestHandler> handlers = new ConcurrentHashMap<>();

    public void register(RequestType type, RequestHandler handler) {
        this.handlers.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(handler, "handler"));
    }

    public NetworkResponse route(NetworkRequest request) {
        if (request == null || request.getRequestId() == null || request.getType() == null) {
            return NetworkResponse.failure(null, "Invalid request");
        }
        RequestHandler handler = this.handlers.get(request.getType());
        if (handler == null) {
            return NetworkResponse.failure(request.getRequestId(), "Unsupported request type");
        }
        try {
            return NetworkResponse.success(request.getRequestId(), handler.handle(request.getPayload()));
        } catch (IllegalArgumentException e) {
            return NetworkResponse.failure(request.getRequestId(), e.getMessage());
        } catch (RuntimeException e) {
            return NetworkResponse.failure(request.getRequestId(), "Server could not process the request");
        }
    }

    public static RequestRouter withDefaults() {
        RequestRouter router = new RequestRouter();
        router.register(RequestType.PING, ignored -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("status", "ok");
            return payload;
        });
        return router;
    }
}
