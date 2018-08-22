package com.gmail.woodyc40.pbft;

public class NodeOptions<Op, R, T> {
    private final int tolerance;
    private final Roster roster;
    private final Digester<Op> digester;
    private final Encoder<Op, R, T> encoder;
    private final Transport<T> transport;

    public NodeOptions(int tolerance,
                       Roster roster,
                       Digester<Op> digester,
                       Encoder<Op, R, T> encoder,
                       Transport<T> transport) {
        this.tolerance = tolerance;
        this.roster = roster;
        this.digester = digester;
        this.encoder = encoder;
        this.transport = transport;
    }

    public int tolerance() {
        return this.tolerance;
    }

    public Roster roster() {
        return this.roster;
    }

    public Digester<Op> digester() {
        return this.digester;
    }

    public Encoder<Op, R, T> encoder() {
        return this.encoder;
    }

    public Transport<T> transport() {
        return this.transport;
    }
}
