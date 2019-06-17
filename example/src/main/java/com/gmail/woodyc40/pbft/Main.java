package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;

public class Main {
    private static final int TOLERANCE = 1;
    private static final long TIMEOUT_MS = 1000;
    private static final int REPLICA_COUNT = 3 * TOLERANCE + 1;

    public static void main(String[] args) {
        Client<AdditionOperation, AdditionResult> client = null;

        AdditionOperation operation = new AdditionOperation(1, 1);
        ClientTicket<AdditionOperation, AdditionResult> ticket = client.sendRequest(operation);

        ticket.result().thenAccept(result -> System.out.println("Result: " + result.result()));
    }
}
