package network.protocol;

import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.UUID;

public final class NetworkRequest {
    private String requestId;
    private RequestType type;
    private JsonObject payload;

    public NetworkRequest() {
    }

    public NetworkRequest(String requestId, RequestType type, JsonObject payload) {
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.type = Objects.requireNonNull(type, "type");
        this.payload = payload == null ? new JsonObject() : payload;
    }

    public static NetworkRequest create(RequestType type, JsonObject payload) {
        return new NetworkRequest(UUID.randomUUID().toString(), type, payload);
    }

    public String getRequestId() {
        return this.requestId;
    }

    public RequestType getType() {
        return this.type;
    }

    public JsonObject getPayload() {
        if (this.payload == null) {
            this.payload = new JsonObject();
        }
        return this.payload;
    }
}
