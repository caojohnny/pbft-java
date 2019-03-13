package com.gmail.woodyc40.pbft;

public abstract class AbstractStateMachine<Op, R, T>
        implements StateMachine<Op, R, T> {
    private final int id;
    private final Verifier<Op> verifier;
    private final NodeOptions<Op, R, T> options;

    private State state = State.PRE_PREPARE;

    protected AbstractStateMachine(int id,
                                   Verifier<Op> verifier,
                                   NodeOptions<Op, R, T> options) {
        this.id = id;
        this.verifier = verifier;
        this.options = options;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public Verifier<Op> verifier() {
        return this.verifier;
    }

    @Override
    public State state() {
        return this.state;
    }

    protected void state(State state) {
        this.state = state;
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
