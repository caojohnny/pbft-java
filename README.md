# `pbft-java`

A Practical Byzantine Fault Tolerance (PBFT) emulator built in Java.

# Implementation

For the most part, the implementation of the PBFT protocol
attempts to follow the original document as closely as
possible. However, there are many places where I am simply
too stupid to comprehend what is going on in the protocol,
and there may be a few deviations from the standard.

In the default implementations, clients are allowed to send
asynchronous requests. Clients implement a ticketing system
whereby requests are assigned a ticket and placed into an
internal table, which will allow lookups based on the
timestamp of the original request. Because requests may be
sent quickly, timestamps begin at 0 and count up instead of
using the system clock for simplicity. Clients check on the
reply status of a ticket in an infinite loop, as long as
timeouts continue to occur, clients will continue to
multicast the same request to all replicas. Clients accept
a quorum of `f + 1` replies, and future replies from the
remaining nodes are ignored.

In the default implementations, replicas are allowed to
handle asynchronous requests. The message logging style
also uses a ticketing system, whereby operations that
are pending are identified by view number and the assigned
sequence number. Tickets are phased rather than the entire
replica. All messages received as well as sent are added to
the respective ticket. Replicas check for prepared state as
well as committed-local state each time a phase-pertinent
message is sent (`PRE-PREPARE`, `PREPARE`, or `COMMIT`),
and thus these messages are allowed to arrive out-of-order.
Replicas execute all requested operations synchronously.
Replicas are allowed to send a `PREPARE` or `COMMIT`
message only once to cut down on traffic. Cryptography,
such as digesting, MACs, and message signing are not
specified, and implementors are allowed to not verify those
if desired. Replicas become prepared as soon as the
`2f`th matching `PREPARE` message arrives, and become 
committed-local as soon as the `2f + 1`th matching 
`COMMIT` arrives. Because these conditions only occur once,
future phase messages are ignored if the condition they are
changing are already true.

# Building

``` shell
git clone https://github.com/caojohnny/pbft-java.git
cd pbft-java
mvn clean install
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
- Operations implemented by the client should implement
`equals` and `hashCode`

#### Replicas

- Replicas need to implement their own `ReplicaEncoder` to
transform the messages into a transmissible format
    - Encoders handle message signing and MACs
    - The **default** implementation requires that full
    messages and checkpoints are encoded for those messages
    pertaining to view changes, but this can be changed if
    the user wishes to accommodate for additional messages
    to retrieve missing information
- Replicas need to implement `ReplicaTransport` in order
for the `Replica` to send messages
- Replicas need to implement their own incoming message
handlers that both decode and decide which hooks to call -
the required hooks are:
  - `#recvRequest(...)`
  - `#recvPrePrepare(...)`
  - `#recvPrepare(...)`
  - `#recvCommit(...)`
  - `#recvCheckpoint(...)`
  - `#recvViewChange(...)`
  - `#recvNewView(...)`
- Replicas need to implement their own `Digesters` if
needed
- Replicas need to call `#checkTimeout(...)` in a loop to
ensure that client timeouts cause view changes as needed

# Demo

A primitive implementation of the PBFT protocol using Redis to
communicate between replicas using JSON serialization to demonstrate
addition operations sent to the replica state machines and retrieve
a result even with 1 faulty node. The source for the implementation
can be found in the `pbft-java-example` module.

Here is the output:

```
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 1: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 2: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 3: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 1: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 2: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 3: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: CLIENT -> 1: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: CLIENT -> 2: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: CLIENT -> 3: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 1: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 2: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 3: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 1: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 2: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 3: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 1: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: CLIENT -> 2: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: CLIENT -> 3: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 0: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 2: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 3: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 0: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 2: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 3: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 0: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 0: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 1: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 2: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}
SEND: REPLICA -> 3: {"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2}
SEND: REPLICA -> 2: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 3: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 0: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 2: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 3: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 0: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 2: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 3: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 0: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 2: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 3: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 0: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 2: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 3: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3}],"prepared-proofs":[]}
SEND: REPLICA -> 0: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 2: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: REPLICA -> 3: {"type":"NEW-VIEW","new-view-number":1,"view-change-proofs":[{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":2},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":3},{"type":"VIEW-CHANGE","new-view-number":1,"last-seq-number":0,"checkpoint-proofs":[],"prepared-proofs":[],"replica-id":1}],"prepared-proofs":[]}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 1: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 2: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: CLIENT -> 3: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"REQUEST","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"PRE-PREPARE","view-number":1,"seq-number":0,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"PRE-PREPARE","view-number":1,"seq-number":0,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 3: {"type":"PRE-PREPARE","view-number":1,"seq-number":0,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":0,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":0,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":0,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":0,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"PREPARE","view-number":1,"seq-number":0,"digest":"","replica-id":2}
SEND: REPLICA -> 2: {"type":"PREPARE","view-number":1,"seq-number":0,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"PRE-PREPARE","view-number":1,"seq-number":1,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"PRE-PREPARE","view-number":1,"seq-number":1,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 3: {"type":"PRE-PREPARE","view-number":1,"seq-number":1,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":0,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":0,"digest":"","replica-id":3}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: CLIENT -> 1: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":0,"digest":"","replica-id":2}
SEND: CLIENT -> 2: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":0,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":0,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"PRE-PREPARE","view-number":1,"seq-number":2,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":1,"digest":"","replica-id":2}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":0,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":1,"digest":"","replica-id":2}
SEND: CLIENT -> 3: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 3: {"type":"PREPARE","view-number":1,"seq-number":1,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":1,"digest":"","replica-id":3}
SEND: CLIENT -> 0: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":1,"digest":"","replica-id":3}
SEND: CLIENT -> 1: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: CLIENT -> 2: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: CLIENT -> 3: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"PREPARE","view-number":1,"seq-number":1,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":1,"digest":"","replica-id":2}
SEND: REPLICA -> 2: {"type":"PRE-PREPARE","view-number":1,"seq-number":2,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"REQUEST","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":1,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":1,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":1,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":1,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"PRE-PREPARE","view-number":1,"seq-number":2,"digest":"","operation":{"first":1,"second":1},"timestamp":0,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":1,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":2,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":2,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"REQUEST","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 3: {"type":"PREPARE","view-number":1,"seq-number":2,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":0,"digest":"","replica-id":1}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":2,"digest":"","replica-id":3}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":0,"digest":"","replica-id":1}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":2,"digest":"","replica-id":3}
SEND: REPLICA -> 2: {"type":"PREPARE","view-number":1,"seq-number":2,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":0,"digest":"","replica-id":1}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":2,"digest":"","replica-id":3}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":0,"client-id":"client-0","replica-id":2,"result":2}
SEND: REPLICA -> 0: {"type":"PRE-PREPARE","view-number":1,"seq-number":3,"digest":"","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":2,"digest":"","replica-id":3}
SEND: REPLICA -> 2: {"type":"PRE-PREPARE","view-number":1,"seq-number":3,"digest":"","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":2,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"PRE-PREPARE","view-number":1,"seq-number":3,"digest":"","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"CHECKPOINT","last-seq-number":0,"digest":"","replica-id":2}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":0,"client-id":"client-0","replica-id":3,"result":2}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":0,"client-id":"client-0","replica-id":1,"result":2}
==========================
==========================
1 + 1 = 2
==========================
==========================
SEND: REPLICA -> 1: {"type":"CHECKPOINT","last-seq-number":0,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"CHECKPOINT","last-seq-number":0,"digest":"","replica-id":1}
SEND: REPLICA -> 2: {"type":"CHECKPOINT","last-seq-number":0,"digest":"","replica-id":1}
SEND: REPLICA -> 3: {"type":"CHECKPOINT","last-seq-number":0,"digest":"","replica-id":2}
SEND: REPLICA -> 3: {"type":"CHECKPOINT","last-seq-number":0,"digest":"","replica-id":1}
SEND: REPLICA -> 0: {"type":"CHECKPOINT","last-seq-number":0,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":1,"digest":"","replica-id":1}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":1,"digest":"","replica-id":1}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":1,"digest":"","replica-id":1}
SEND: REPLICA -> 1: {"type":"CHECKPOINT","last-seq-number":0,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"PRE-PREPARE","view-number":1,"seq-number":4,"digest":"","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"CHECKPOINT","last-seq-number":0,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":2,"digest":"","replica-id":2}
SEND: REPLICA -> 2: {"type":"PRE-PREPARE","view-number":1,"seq-number":4,"digest":"","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":2,"digest":"","replica-id":2}
SEND: REPLICA -> 3: {"type":"PRE-PREPARE","view-number":1,"seq-number":4,"digest":"","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":2,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"PRE-PREPARE","view-number":1,"seq-number":5,"digest":"","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"PRE-PREPARE","view-number":1,"seq-number":5,"digest":"","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":3,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":3,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":3,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":3,"digest":"","replica-id":2}
SEND: REPLICA -> 3: {"type":"PRE-PREPARE","view-number":1,"seq-number":5,"digest":"","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"PREPARE","view-number":1,"seq-number":3,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"PREPARE","view-number":1,"seq-number":3,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"PRE-PREPARE","view-number":1,"seq-number":6,"digest":"","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"PRE-PREPARE","view-number":1,"seq-number":6,"digest":"","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":0,"client-id":"client-0","replica-id":2,"result":2}
SEND: REPLICA -> 3: {"type":"PRE-PREPARE","view-number":1,"seq-number":6,"digest":"","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":0,"client-id":"client-0","replica-id":3,"result":2}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":4,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"PRE-PREPARE","view-number":1,"seq-number":7,"digest":"","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":4,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":4,"digest":"","replica-id":2}
SEND: REPLICA -> 2: {"type":"PRE-PREPARE","view-number":1,"seq-number":7,"digest":"","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 3: {"type":"PREPARE","view-number":1,"seq-number":4,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":5,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":5,"digest":"","replica-id":2}
SEND: REPLICA -> 3: {"type":"PREPARE","view-number":1,"seq-number":5,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":4,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"PRE-PREPARE","view-number":1,"seq-number":7,"digest":"","operation":{"first":2,"second":2},"timestamp":1,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"PREPARE","view-number":1,"seq-number":4,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":3,"digest":"","replica-id":2}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":0,"client-id":"client-0","replica-id":1,"result":2}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":5,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":5,"digest":"","replica-id":3}
SEND: REPLICA -> 2: {"type":"PREPARE","view-number":1,"seq-number":5,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":3,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"PRE-PREPARE","view-number":1,"seq-number":8,"digest":"","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":3,"digest":"","replica-id":3}
SEND: REPLICA -> 2: {"type":"PRE-PREPARE","view-number":1,"seq-number":8,"digest":"","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":3,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":3,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":6,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":6,"digest":"","replica-id":3}
SEND: REPLICA -> 2: {"type":"PREPARE","view-number":1,"seq-number":6,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":3,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":6,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":6,"digest":"","replica-id":2}
SEND: REPLICA -> 3: {"type":"PREPARE","view-number":1,"seq-number":6,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":4,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"PRE-PREPARE","view-number":1,"seq-number":8,"digest":"","operation":{"first":3,"second":3},"timestamp":2,"client":"client-0"}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":4,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":2,"digest":"","replica-id":1}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":4,"digest":"","replica-id":3}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":2,"digest":"","replica-id":1}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":5,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":2,"digest":"","replica-id":1}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":5,"digest":"","replica-id":3}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":0,"client-id":"client-0","replica-id":1,"result":2}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":5,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":3,"digest":"","replica-id":1}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":7,"digest":"","replica-id":3}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":3,"digest":"","replica-id":1}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":7,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":7,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":7,"digest":"","replica-id":2}
SEND: REPLICA -> 2: {"type":"PREPARE","view-number":1,"seq-number":7,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":3,"digest":"","replica-id":1}
SEND: REPLICA -> 3: {"type":"PREPARE","view-number":1,"seq-number":7,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":4,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":6,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":4,"digest":"","replica-id":2}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":4,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":6,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":5,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":4,"digest":"","replica-id":1}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":6,"digest":"","replica-id":3}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":5,"digest":"","replica-id":2}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":4,"digest":"","replica-id":1}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":8,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":5,"digest":"","replica-id":2}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":4,"digest":"","replica-id":1}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":8,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"PREPARE","view-number":1,"seq-number":8,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"PREPARE","view-number":1,"seq-number":8,"digest":"","replica-id":2}
SEND: REPLICA -> 2: {"type":"PREPARE","view-number":1,"seq-number":8,"digest":"","replica-id":3}
SEND: REPLICA -> 3: {"type":"PREPARE","view-number":1,"seq-number":8,"digest":"","replica-id":2}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":0,"client-id":"client-0","replica-id":3,"result":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":6,"digest":"","replica-id":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":5,"digest":"","replica-id":1}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":6,"digest":"","replica-id":2}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":1,"client-id":"client-0","replica-id":3,"result":4}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":5,"digest":"","replica-id":1}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":6,"digest":"","replica-id":2}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":5,"digest":"","replica-id":1}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":0,"client-id":"client-0","replica-id":2,"result":2}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":7,"digest":"","replica-id":3}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":1,"client-id":"client-0","replica-id":1,"result":4}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":7,"digest":"","replica-id":3}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":1,"client-id":"client-0","replica-id":2,"result":4}
==========================
==========================
2 + 2 = 4
==========================
==========================
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":7,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":7,"digest":"","replica-id":2}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":7,"digest":"","replica-id":3}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":6,"digest":"","replica-id":1}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":7,"digest":"","replica-id":2}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":2,"client-id":"client-0","replica-id":3,"result":6}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":2,"client-id":"client-0","replica-id":2,"result":6}
==========================
==========================
3 + 3 = 6
==========================
==========================
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":8,"digest":"","replica-id":2}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":8,"digest":"","replica-id":2}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":8,"digest":"","replica-id":2}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":1,"client-id":"client-0","replica-id":2,"result":4}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":6,"digest":"","replica-id":1}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":6,"digest":"","replica-id":1}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":2,"client-id":"client-0","replica-id":2,"result":6}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":7,"digest":"","replica-id":1}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":7,"digest":"","replica-id":1}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":8,"digest":"","replica-id":3}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":1,"client-id":"client-0","replica-id":2,"result":4}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":7,"digest":"","replica-id":1}
SEND: REPLICA -> 1: {"type":"COMMIT","view-number":1,"seq-number":8,"digest":"","replica-id":3}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":8,"digest":"","replica-id":3}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":2,"client-id":"client-0","replica-id":1,"result":6}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":1,"client-id":"client-0","replica-id":1,"result":4}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":1,"client-id":"client-0","replica-id":3,"result":4}
SEND: REPLICA -> 0: {"type":"COMMIT","view-number":1,"seq-number":8,"digest":"","replica-id":1}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":2,"client-id":"client-0","replica-id":3,"result":6}
SEND: REPLICA -> 2: {"type":"COMMIT","view-number":1,"seq-number":8,"digest":"","replica-id":1}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":1,"client-id":"client-0","replica-id":3,"result":4}
SEND: REPLICA -> 3: {"type":"COMMIT","view-number":1,"seq-number":8,"digest":"","replica-id":1}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":2,"client-id":"client-0","replica-id":2,"result":6}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":2,"client-id":"client-0","replica-id":1,"result":6}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":2,"client-id":"client-0","replica-id":3,"result":6}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":1,"client-id":"client-0","replica-id":1,"result":4}
SEND: REPLY -> client-0: {"type":"REPLY","view-number":1,"timestamp":2,"client-id":"client-0","replica-id":1,"result":6}
```

# Credits

Built with [IntelliJ IDEA](https://www.jetbrains.com/idea/)

Uses [GSON](https://github.com/google/gson) and [Jedis](https://github.com/xetorthio/jedis)

# References

  - [Practical Byzantine Fault Tolerance](http://pmg.csail.mit.edu/papers/osdi99.pdf)
  - [Practical BFT](https://courses.cs.washington.edu/courses/csep552/13sp/lectures/10/pbft.pdf)
  - [Practical Byzantine Fault Tolerance](http://www.scs.stanford.edu/14au-cs244b/notes/pbft.txt)
  - [Distributed Algorithms Practical Byzantine Fault Tolerance](https://disi.unitn.it/~montreso/ds/handouts17/10-pbft.pdf)
  - [PBFT Presentation](https://courses.cs.vt.edu/~cs5204/fall05-gback/presentations/PBFT.pdf)
  - [Practical Byzantine Fault Tolerance](https://www.microsoft.com/en-us/research/wp-content/uploads/2017/01/thesis-mcastro.pdf)
  - [10.BFT](https://www.cs.utexas.edu/~lorenzo/corsi/cs380d/past/10S/notes/week11.pdf)
  - [Byzantine Fault Tolerance](http://www.cs.cmu.edu/~srini/15-440-all/2016.Fall/lectures/22-BFT.ppt)
  - [Practical Byzantine Fault Tolerance ](https://people.eecs.berkeley.edu/~kubitron/courses/cs294-4-F03/slides/lec09-practical.ppt)
