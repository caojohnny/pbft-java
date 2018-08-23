package com.gmail.woodyc40.pbft;

public abstract class AbstractStateMachine<Op, R, T>
        implements StateMachine<Op, R, T> {
    private final String id;
    private final long timeout;
    private final NodeOptions<Op, R, T> options;

    protected AbstractStateMachine(String id,
                                   long timeout,
                                   NodeOptions<Op, R, T> options) {
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
    public Encoder<Op, R, T> encoder() {
        return this.options.encoder();
    }

    @Override
    public Transport<T> transport() {
        return this.options.transport();
    }
}
