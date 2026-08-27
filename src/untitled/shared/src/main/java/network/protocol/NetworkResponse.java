package network.protocol;

import com.google.gson.JsonObject;

public final class NetworkResponse {
    private String requestId;
    private boolean successful;
    private String message;
    private JsonObject payload;

    public NetworkResponse() {
    }

    private NetworkResponse(String requestId, boolean successful, String message, JsonObject payload) {
        this.requestId = requestId;
        this.successful = successful;
        this.message = message;
        this.payload = payload == null ? new JsonObject() : payload;
    }

    public static NetworkResponse success(String requestId, JsonObject payload) {
        return new NetworkResponse(requestId, true, "", payload);
    }

    public static NetworkResponse failure(String requestId, String message) {
        return new NetworkResponse(requestId, false, message, new JsonObject());
    }

    public String getRequestId() {
        return this.requestId;
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    public String getMessage() {
        return this.message;
    }

    public JsonObject getPayload() {
        if (this.payload == null) {
            this.payload = new JsonObject();
        }
        return this.payload;
    }
}
