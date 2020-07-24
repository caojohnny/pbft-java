package com.gmail.woodyc40.pbft.client;

import com.gmail.woodyc40.pbft.ClientEncoder;
import com.gmail.woodyc40.pbft.ClientTransport;
import com.gmail.woodyc40.pbft.DefaultClient;
import com.gmail.woodyc40.pbft.message.DefaultClientReply;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AdditionClient extends DefaultClient<AdditionOperation, AdditionResult, String> {
    public AdditionClient(String clientId,
                          int tolerance,
                          long timeoutMs,
                          ClientEncoder<AdditionOperation, String> encoder,
                          ClientTransport<String> transport) {
        super(clientId, tolerance, timeoutMs, encoder, transport);
    }

    public void handleIncomingMessage(String data) {
        // System.out.println(String.format("RECV: CLIENT %s: %s", this.clientId(), data));

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(data, JsonObject.class);

        String type = root.get("type").getAsString();
        if ("REPLY".equals(type)) {
            int viewNumber = root.get("view-number").getAsInt();
            long timestamp = root.get("timestamp").getAsLong();
            int replicaId = root.get("replica-id").getAsInt();
            int result = root.get("result").getAsInt();

            AdditionResult additionResult = new AdditionResult(result);
            DefaultClientReply<AdditionResult> reply = new DefaultClientReply<>(
                    viewNumber,
                    timestamp,
                    this,
                    replicaId,
                    additionResult);
            this.recvReply(reply);
        } else {
            throw new IllegalArgumentException("Unrecognized type: " + type);
        }
    }
}
