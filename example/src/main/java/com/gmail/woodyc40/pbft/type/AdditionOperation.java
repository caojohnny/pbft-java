package com.gmail.woodyc40.pbft.type;

public class AdditionOperation {
    private final int first;
    private final int second;

    public AdditionOperation(int first, int second) {
        this.first = first;
        this.second = second;
    }

    public int first() {
        return this.first;
    }

    public int second() {
        return this.second;
    }
}
