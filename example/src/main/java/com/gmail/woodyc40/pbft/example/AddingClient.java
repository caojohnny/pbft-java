package com.gmail.woodyc40.pbft.example;

import com.gmail.woodyc40.pbft.*;
import com.gmail.woodyc40.pbft.example.type.AdditionOperation;
import com.gmail.woodyc40.pbft.example.type.AdditionResult;
import com.gmail.woodyc40.pbft.protocol.Reply;
import com.google.gson.JsonElement;

public class AddingClient implements Client<AdditionOperation, AdditionResult, JsonElement> {
    public AddingClient() {
    }

    @Override
    public long timeout() {
        return 1000L;
    }

    @Override
    public Roster roster() {
        return null;
    }

    @Override
    public void sendRequest(AdditionOperation operation) {

    }

    @Override
    public void recvReply(Reply<AdditionResult> reply) {

    }

    @Override
    public int id() {
        return 0;
    }

    @Override
    public int tolerance() {
        return 1;
    }

    @Override
    public Digester<AdditionOperation> digester() {
        return EmptyDigester.instance();
    }

    @Override
    public Encoder<AdditionOperation, AdditionResult, JsonElement> encoder() {
        return null;
    }

    @Override
    public Transport<JsonElement> transport() {
        return null;
    }
}
