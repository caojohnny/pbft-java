# `pbft-java`

A Practical Byzantine Fault Tolerance (PBFT) emulator built in Java.

# Building

``` shell
git clone https://github.com/AgentTroll/pbft-java.git
cd pbft-java
mvn install
```

# Usage

The majority of *response* logic has been implemented by
the `Default*` implementation modules. This means that
given input, the default implementations handles the
response to those inputs. The user does need  to implement
a few components in order to correctly use the provided
implementations.

Sample implementations for the required components can
be found the `pbft-java-example` module.

#### Clients

- Clients need to implement their own `ClientEncoder` to
transform the messages into a transmissible format
    - Encoders handle message signing and MACs
- Clients also need to implement the `ClientTransport` in
order for the `Client` to send messages
- Clients need to implement their own incoming message
handlers that both decode the message and decide which
hooks to call - the required hook is 
`Client#recvReply(...)`
- Client users should call `Client#checkTimeout(...)` in
a loop after sending requests in order to ensure liveness

#### Replicas

- Replicas need to implement their own `ReplicaEncoder` to
transform the messages into a transmissible format
    - Encoders handle message signing and MACs
- Replicas need to implement `ReplicaTransport` in order
for the `Replica` to send messages
- Replicas need to implement their own incoming message
handlers that both decode and decide which hooks to call -
the required hooks are:
  - `#recvRequest(...)`
  - `#recvPrePrepare(...)`
  - `#recvPrepare(...)`
  - `#recvCommit(...)`
- Replicas need to implement their own `Digesters` if
needed

# Demo

A primitive implementation of the PBFT protocol using Redis to
communicate between replicas using JSON serialization to demonstrate
addition operations sent to the replica state machines and retrieve
a result even with 1 faulty node. The source for the implementation
can be found in the `pbft-java-example` module.

Here is the output:

```
Client SEND -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
Replica 0 RECV: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
Replica SEND -> 1: {"type":"PRE-PREPARE","view-number":0,"seq-number":0,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
Replica 1 RECV: {"type":"PRE-PREPARE","view-number":0,"seq-number":0,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
Replica SEND -> 2: {"type":"PRE-PREPARE","view-number":0,"seq-number":0,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
Replica SEND -> 3: {"type":"PRE-PREPARE","view-number":0,"seq-number":0,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
Replica 2 RECV: {"type":"PRE-PREPARE","view-number":0,"seq-number":0,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
Replica 3 RECV: {"type":"PRE-PREPARE","view-number":0,"seq-number":0,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
Replica SEND -> 0: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica SEND -> 0: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica SEND -> 0: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica SEND -> 2: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica SEND -> 1: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica SEND -> 1: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica 0 RECV: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica SEND -> 2: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica SEND -> 3: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica 2 RECV: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica 0 RECV: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica SEND -> 3: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica 3 RECV: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica SEND -> 0: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica SEND -> 0: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica 1 RECV: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica SEND -> 1: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica SEND -> 1: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":0}
Replica SEND -> 0: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica SEND -> 1: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica SEND -> 3: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica SEND -> 2: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":0}
Replica SEND -> 2: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica SEND -> 2: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica 3 RECV: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica SEND -> 3: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica 2 RECV: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica 3 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica SEND -> 3: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":0}
Replica 3 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica 2 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":0}
Replica 2 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica SEND -> client-0: {"type":"REPLY","view-number":0,"timestamp":0,"client-id":"client-0","replica-id":3,"result":2}
Replica 1 RECV: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica SEND -> client-0: {"type":"REPLY","view-number":0,"timestamp":0,"client-id":"client-0","replica-id":2,"result":2}
Client client-0 RECV: {"type":"REPLY","view-number":0,"timestamp":0,"client-id":"client-0","replica-id":3,"result":2}
Replica 1 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica 1 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":0}
Replica SEND -> client-0: {"type":"REPLY","view-number":0,"timestamp":0,"client-id":"client-0","replica-id":1,"result":2}
Replica 2 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Replica 0 RECV: {"type":"PREPARE","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica 1 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":3}
Replica 0 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":2}
Replica 3 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":0}
Client client-0 RECV: {"type":"REPLY","view-number":0,"timestamp":0,"client-id":"client-0","replica-id":2,"result":2}
Replica 0 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":3}
==========================
==========================
1 + 1 = 2
==========================
==========================
Replica SEND -> client-0: {"type":"REPLY","view-number":0,"timestamp":0,"client-id":"client-0","replica-id":0,"result":-92879089}
Replica 0 RECV: {"type":"COMMIT","view-number":0,"seq-number":0,"digest":"","replica-id":1}
Client client-0 RECV: {"type":"REPLY","view-number":0,"timestamp":0,"client-id":"client-0","replica-id":1,"result":2}
Client client-0 RECV: {"type":"REPLY","view-number":0,"timestamp":0,"client-id":"client-0","replica-id":0,"result":-92879089}

Process finished with exit code 0
```

# Credits

Built with [IntelliJ IDEA](https://www.jetbrains.com/idea/)

Uses [GSON](https://github.com/google/gson) and [Jedis](https://github.com/xetorthio/jedis)

# References

  - [Practical Byzantine Fault Tolerance](http://pmg.csail.mit.edu/papers/osdi99.pdf)
  - [Practical BFT](https://courses.cs.washington.edu/courses/csep552/13sp/lectures/10/pbft.pdf)
  - [Practical Byzantine Fault Tolerance](http://www.scs.stanford.edu/14au-cs244b/notes/pbft.txt)
  - [Distributed Algorithms Practical Byzantine Fault Tolerance](https://disi.unitn.it/~montreso/ds/handouts17/10-pbft.pdf)