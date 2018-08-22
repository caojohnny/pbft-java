package com.gmail.woodyc40.pbft;

public abstract class AbstractClient<Op, T, R> implements Client<Op, T, R> {
    private final String id;
    private final long timeout;
    private final NodeOptions<Op, T, R> options;

    protected AbstractClient(String id,
                             long timeout,
                             NodeOptions<Op, T, R> options) {
        this.id = id;
        this.timeout = timeout;
        this.options = options;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public long timeout() {
        return this.timeout;
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
    public Digester<Op> digester() {
        return this.options.digester();
    }

    @Override
    public Encoder<Op, T, R> encoder() {
        return this.options.encoder();
    }

    @Override
    public Transport<R> transport() {
        return this.options.transport();
    }
}
