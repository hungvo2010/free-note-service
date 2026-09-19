# Free Note Service

A WebSocket service for collaborative drawing. Clients connect to a server, work on a Draft together, and receive each other's edits as they happen.

## Language

### The document

**Draft**:
A collaborative drawing document, identified by a draft identifier.
_Avoid_: Note, document, board, canvas

**Shape**:
A single drawing primitive belonging to a Draft.
_Avoid_: Element, object, item

**Action**:
One change to a Draft, carrying the Shapes it adds or alters. Typed as CONNECT, ADD, UPDATE, REMOVE or NOOP.
_Avoid_: Operation, edit, mutation, event

### Presence

**Connection**:
One WebSocket socket between a client and a server. Owned by the server that accepted it, and existing only inside that server.
_Avoid_: Session, socket, client, channel

**Room**:
The Connections currently collaborating on one Draft, as known to a single server. A Room is server-local, so one Draft has a separate Room on each server holding its participants.
_Avoid_: Channel, group, session, draft

**Participant**:
A person editing a Draft. May hold several Connections at once and may outlive them across reconnects. Not modelled in the current system.
_Avoid_: User, account, member, client

### Identity

**senderId**:
A value a client attaches to each message. Echoed to other clients to attribute a change. A client assertion — unverified, changeable between messages, and not an identity.
_Avoid_: UserId, participant id, client id
