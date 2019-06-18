package com.gmail.woodyc40.pbft.client;

import com.gmail.woodyc40.pbft.ClientEncoder;
import com.gmail.woodyc40.pbft.message.ClientRequest;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.google.gson.JsonObject;

public class AdditionClientEncoder implements ClientEncoder<AdditionOperation, String> {
    @Override
    public String encodeRequest(ClientRequest<AdditionOperation> request) {
        AdditionOperation op = request.operation();
        long timestamp = request.timestamp();
        String clientId = request.client().clientId();

        JsonObject root = new JsonObject();
        root.addProperty("type", "REQUEST");
        JsonObject operation = new JsonObject();
        operation.addProperty("first", op.first());
        operation.addProperty("second", op.second());
        root.add("operation", operation);
        root.addProperty("timestamp", timestamp);
        root.addProperty("client", clientId);

        return root.toString();
    }
}
