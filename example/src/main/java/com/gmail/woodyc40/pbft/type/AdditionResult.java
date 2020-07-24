package com.gmail.woodyc40.pbft.type;

public class AdditionResult {
    private final int result;

    public AdditionResult(int result) {
        this.result = result;
    }

    public int result() {
        return this.result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdditionResult)) return false;

        AdditionResult that = (AdditionResult) o;

        return this.result == that.result;
    }

    @Override
    public int hashCode() {
        return this.result;
    }
}
