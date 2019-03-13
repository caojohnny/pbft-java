package com.gmail.woodyc40.pbft;

public class NodeOptions<Op, R, T> {
    private final int tolerance;
    private final long timeout;
    private final Roster roster;
    private final Digester<Op> digester;
    private final Encoder<Op, R, T> encoder;
    private final Transport<T> transport;

    public NodeOptions(int tolerance,
                       long tmeout,
                       Roster roster,
                       Digester<Op> digester,
                       Encoder<Op, R, T> encoder,
                       Transport<T> transport) {
        this.tolerance = tolerance;
        this.timeout = tmeout;
        this.roster = roster;
        this.digester = digester;
        this.encoder = encoder;
        this.transport = transport;
    }

    public int tolerance() {
        return this.tolerance;
    }

    public long timeout() {
        return this.timeout;
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
