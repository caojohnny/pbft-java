package com.gmail.woodyc40.pbft;

public abstract class AbstractClient<Op, R, T> implements Client<Op, R, T> {
    private final NodeOptions<Op, R, T> options;

    protected AbstractClient(NodeOptions<Op, R, T> options) {
        this.options = options;
    }

    @Override
    public long timeout() {
        return this.options.timeout();
    }

    @Override
    public int tolerance() {
        return this.options.tolerance();
    }

    @Override
    public Roster roster() {
        return this.options.roster();
    }

    @Override
    public Digester<Op, R, T> digester() {
        return this.options.digester();
    }

    @Override
    public Encoder<Op, R, T> encoder() {
        return this.options.encoder();
    }

    @Override
    public Transport<T> transport() {
        return this.options.transport();
    }
}
