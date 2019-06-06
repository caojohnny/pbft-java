package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;

public class Main {
    public static void main(String[] args) {
        Client<AdditionOperation, AdditionResult> client = null;

        AdditionOperation operation = new AdditionOperation(1, 1);
        Ticket<AdditionOperation, AdditionResult> ticket = client.sendRequest(operation);

        ticket.result().thenAccept(result -> System.out.println("Result: " + result.result()));
    }
}
