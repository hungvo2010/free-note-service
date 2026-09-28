# How Netty models per-connection state and handler phase transitions

Research notes, primary sources only: Netty source on GitHub (`github.com/netty/netty`) and the
official javadoc at `netty.io`. Every claim carries an inline citation.

All source line numbers refer to branch `4.1` at commit
`becd891ebef7f62847fb48174278823941120d6a` (2026-09-25), fetched from
`https://raw.githubusercontent.com/netty/netty/4.1/<path>`. `4.1` is a moving branch, so line
numbers may drift; re-fetch against that SHA to reproduce exactly.

For reference, the design being compared against, as it exists in this repo:

- `src/main/java/com/freenote/app/server/core/nio/ConnectionPipeline.java:24` —
  `Map<NetworkRequestData, ConnectionState> connectionStates`
- `src/main/java/com/freenote/app/server/core/nio/state/ConnectionState.java:7` —
  `ConnectionState transition(ConnectionContext context)`, `null` = close
- `src/main/java/com/freenote/app/server/core/nio/state/HandShakeState.java:9` and
  `MessageState.java:15` — the two states
- `src/main/java/com/freenote/app/server/core/context/ReadableContext.java:18` —
  `isHandshakeComplete()` returns `httpUpgradeRequest != null`
- `src/main/java/com/freenote/app/server/core/nio/NIOIncomingSocketHandler.java:67` — the handler
  branches on that boolean

---

## 1. `ChannelHandlerContext`: lifetime and ownership

**Per connection, and more precisely per handler-per-connection.** It is not per event.

`ChannelHandlerContext` is created when a handler is added to a channel's pipeline, one context per
handler instance per channel. `DefaultChannelPipeline.internalAdd` creates it:

```java
// DefaultChannelPipeline.java:118-120
private AbstractChannelHandlerContext newContext(EventExecutorGroup group, String name, ChannelHandler handler) {
    return new DefaultChannelHandlerContext(this, childExecutor(group), name, handler);
}
```

called from `internalAdd` at `DefaultChannelPipeline.java:169` (`newCtx = newContext(group, name, handler);`),
which is the single path behind `addFirst`/`addLast`/`addBefore`/`addAfter`.

Because a context is created per `(pipeline, handler instance)` pair, a handler added to N channels
has N contexts. The class javadoc states this directly:

> "Please note that a `ChannelHandler` instance can be added to more than one `ChannelPipeline`. It
> means a single `ChannelHandler` instance can have more than one `ChannelHandlerContext` and
> therefore the single instance can be invoked with different `ChannelHandlerContext`s if it is
> added to one or more `ChannelPipeline`s more than once."
> — `ChannelHandlerContext.java:70-78`

The pipeline owns and stores the contexts: each context is an `AbstractChannelHandlerContext` node
in the pipeline's linked list (`DefaultChannelPipeline.java:63-64` declares `final HeadContext head;`
and `final TailContext tail;`, constructed at `DefaultChannelPipeline.java:96-97`). The context is
handed to the handler on every callback as a parameter — e.g.
`void channelRead(ChannelHandlerContext ctx, Object msg)` (`ChannelInboundHandler.java:48`) — so the
context outlives any single event.

**Does it carry application state?** The context itself carries what the pipeline needs (executor,
name, next/prev links, handler reference) plus an `AttributeMap` surface. The javadoc advertises it
as a place to *store* state rather than as state:

> "**Storing stateful information** — `attr(AttributeKey)` allow you to store and access stateful
> information that is related with a `ChannelHandler` / `Channel` and its context."
> — `ChannelHandlerContext.java:63-68`

But note the direction of travel in 4.1: `ChannelHandlerContext.attr(AttributeKey)` is deprecated
and delegates to the channel:

```java
// ChannelHandlerContext.java:161-166
/**
 * @deprecated Use {@link Channel#attr(AttributeKey)}
 */
@Deprecated
@Override
<T> Attribute<T> attr(AttributeKey<T> key);
```

```java
// AbstractChannelHandlerContext.java:1166-1167
public <T> Attribute<T> attr(AttributeKey<T> key) {
    return channel().attr(key);
}
```

So in Netty 4.1 the handler-scoped and channel-scoped attribute maps are the same map: the
channel's. (Javadoc:
[ChannelHandlerContext](https://netty.io/4.1/api/io/netty/channel/ChannelHandlerContext.html),
[Channel.attr](https://netty.io/4.1/api/io/netty/channel/Channel.html#attr-io.netty.util.AttributeKey-).)

---

## 2. `ChannelPipeline`: per connection, and handler sharing

**One pipeline per channel.** Stated flatly in the javadoc:

> "**Creation of a pipeline** — Each channel has its own pipeline and it is created automatically
> when a new channel is created."
> — `ChannelPipeline.java:39-41`

Confirmed in `AbstractChannel`: the pipeline is an instance field created in the channel
constructor.

```java
// AbstractChannel.java:49
private final DefaultChannelPipeline pipeline;
// AbstractChannel.java:74
pipeline = newChannelPipeline();
// AbstractChannel.java:118
protected DefaultChannelPipeline newChannelPipeline() { ... }
```

and `DefaultChannelPipeline` holds `private final Channel channel;` (`DefaultChannelPipeline.java:66`),
whose javadoc says it "is usually created by a `Channel` implementation when the `Channel` is
created" (`DefaultChannelPipeline.java:41-44`).

**So: connection-scoped = pipeline-scoped = channel-scoped.** Everything downstream of that —
contexts, attribute map, the list of handlers currently installed — is per connection.

**Handler instances, by contrast, are NOT per connection by default, and that is an opt-in
distinction.** A handler instance may be reused across pipelines only if it is `@Sharable`; the
pipeline enforces this at add time:

```java
// DefaultChannelPipeline.java:544-554
private static void checkMultiplicity(ChannelHandler handler) {
    if (handler instanceof ChannelHandlerAdapter) {
        ChannelHandlerAdapter h = (ChannelHandlerAdapter) handler;
        if (!h.isSharable() && h.added) {
            throw new ChannelPipelineException(
                    h.getClass().getName() +
                    " is not a @Sharable handler, so can't be added or removed multiple times.");
        }
        h.added = true;
    }
}
```

`isSharable()` is annotation detection, cached:

```java
// ChannelHandlerAdapter.java:45-62
public boolean isSharable() {
    ...
    sharable = clazz.isAnnotationPresent(Sharable.class);
    ...
}
```

The javadoc is explicit about the tradeoff and calls the annotation documentary, not behavioral
enforcement of thread-safety:

> "If a `ChannelHandler` is annotated with the `@Sharable` annotation, it means you can create an
> instance of the handler just once and add it to one or more `ChannelPipeline`s multiple times
> without a race condition. If this annotation is not specified, you have to create a new handler
> instance every time you add it to a pipeline because it has unshared state such as member
> variables."
> — `ChannelHandler.java:159-166` (see also the annotation declaration itself,
> `ChannelHandler.java:200-218`)

`ChannelInitializer` is the worked example of the sharable case — it is declared `@Sharable`
because one instance configures every channel:

```java
// ChannelInitializer.java:53-60
@Sharable
public abstract class ChannelInitializer<C extends Channel> extends ChannelInboundHandlerAdapter {
    ...
    // We use a Set as a ChannelInitializer is usually shared between all Channels in a Bootstrap /
    // ServerBootstrap. This way we can reduce the memory usage compared to use Attributes.
    private final Set<ChannelHandlerContext> initMap = Collections.newSetFromMap(
            new ConcurrentHashMap<ChannelHandlerContext, Boolean>());
```

Note the comment at `ChannelInitializer.java:57-58`: it keeps a `Set<ChannelHandlerContext>` rather
than using attributes, deliberately, for memory reasons. Attributes are not the only idiom Netty
uses for per-connection state; they are the idiomatic one for *surviving* state.

(Javadoc: [ChannelPipeline](https://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html),
[ChannelHandler](https://netty.io/4.1/api/io/netty/channel/ChannelHandler.html).)

---

## 3. `AttributeKey` / `AttributeMap`: where per-connection application state goes

`AttributeMap` is the storage abstraction; `Channel` implements it
(`Channel.java:77`: `public interface Channel extends AttributeMap, ChannelOutboundInvoker, Comparable<Channel>`),
so `channel.attr(key)` is the primary accessor. The API is two methods wide:

```java
// AttributeMap.java:18-33
/**
 * Holds {@link Attribute}s which can be accessed via {@link AttributeKey}.
 *
 * Implementations must be Thread-safe.
 */
public interface AttributeMap {
    <T> Attribute<T> attr(AttributeKey<T> key);
    <T> boolean hasAttr(AttributeKey<T> key);
}
```

and `Attribute` values are atomically updatable:

> "An attribute which allows to store a value reference. It may be updated atomically and so is
> thread-safe."
> — `Attribute.java:18-21`

### The stated reason this API exists

The `ChannelHandler` javadoc frames attributes as the escape hatch for the case where you *don't*
want a handler instance per connection:

> "**Using `AttributeKey`s** — Although it's recommended to use member variables to store the state
> of a handler, for some reason you might not want to create many handler instances. In such a
> case, you can use `AttributeKey`s which is provided by `ChannelHandlerContext`"
> — `ChannelHandler.java:106-111`

and then:

> "Now that the state of the handler is attached to the `ChannelHandlerContext`, you can add the
> same handler instance to different pipelines"
> — `ChannelHandler.java:139-140`

So `AttributeKey` exists to make a single handler instance serve many connections by moving its
state off the instance and onto the connection. It is an allocation tradeoff, not a primitive
Netty considers more correct than a field.

### The `String`-keyed constant-pool pitfall

`AttributeKey` is pooled by name, and the pool is a static, non-releasing map. The javadoc warns
about the naming consequence:

> "Key which can be used to access `Attribute` out of the `AttributeMap`. Be aware that it is not be
> possible to have multiple keys with the same name."
> — `AttributeKey.java:19-20`
> (mirrored at https://netty.io/4.1/api/io/netty/util/AttributeKey.html)

The mechanism behind that warning, in source:

```java
// AttributeKey.java:25-32
public final class AttributeKey<T> extends AbstractConstant<AttributeKey<T>> {

    private static final ConstantPool<AttributeKey<Object>> pool = new ConstantPool<AttributeKey<Object>>() {
        ...
    };
```

```java
// AttributeKey.java:37-40
public static <T> AttributeKey<T> valueOf(String name) {
    return (AttributeKey<T>) pool.valueOf(name);
}
```

The pool is `private static final` (`AttributeKey.java:27`) and its backing map is an instance field
of a static singleton, with no eviction path:

```java
// ConstantPool.java:28-30
private final ConcurrentMap<String, T> constants = PlatformDependent.newConcurrentHashMap();
```

```java
// ConstantPool.java:65-76
private T getOrCreate(String name) {
    T constant = constants.get(name);
    if (constant == null) {
        final T tempConstant = newConstant(nextId(), name);
        constant = constants.putIfAbsent(name, tempConstant);
        ...
```

Two consequences fall out of this and are worth stating precisely, because they are the real
content of the warning:

1. **Keys are globally interned by name string.** A second `valueOf("auth")` anywhere in the JVM
   returns the *same* `AttributeKey`, so two unrelated components that both pick the name `"auth"`
   silently share one slot on every channel. `AbstractConstant` also enforces uniqueness of the
   name per pool. `exists(String)` and `newInstance(String)` exist to probe and to fail loudly on
   this:
   ```java
   // ConstantPool.java:98-108
   private T createOrThrow(String name) {
       T constant = constants.get(name);
       if (constant == null) { ... }
       throw new IllegalArgumentException(String.format("'%s' is already in use", name));
   }
   ```
2. **Names entered into the pool are never removed.** `getOrCreate` only ever inserts; nothing in
   `ConstantPool` (`ConstantPool.java:26-117`, the whole class) removes. So a key derived from
   dynamic data — a per-tenant, per-user, per-request string — permanently grows a static map.
   Because of (1) and (2), the sanctioned way to build a key is the two-argument form, which
   namespaces by class name:
   ```java
   // AttributeKey.java:58-61
   public static <T> AttributeKey<T> valueOf(Class<?> firstNameComponent, String secondNameComponent) {
       return (AttributeKey<T>) pool.valueOf(firstNameComponent, secondNameComponent);
   }
   ```
   ```java
   // ConstantPool.java:40-45
   public T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
       return valueOf(
               checkNotNull(firstNameComponent, "firstNameComponent").getName() +
               '#' +
               checkNotNull(secondNameComponent, "secondNameComponent"));
   }
   ```
   Netty itself follows this rule everywhere it defines a key; the websocket server is one example:

   ```java
   // WebSocketServerProtocolHandler.java:102-103
   private static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER_ATTR_KEY =
           AttributeKey.valueOf(WebSocketServerHandshaker.class, "HANDSHAKER");
   ```

   Note also that `AttributeKey` keys are `static final` *constants declared once*, never built per
   connection — the pool is a namespace registry, not a per-connection data structure.

(Javadoc: [AttributeKey](https://netty.io/4.1/api/io/netty/util/AttributeKey.html),
[AttributeMap](https://netty.io/4.1/api/io/netty/util/AttributeMap.html),
[Attribute](https://netty.io/4.1/api/io/netty/util/Attribute.html).)

---

## 4. How Netty does the handshake → websocket phase transition

This is the central finding, and it confirms the hypothesis: **Netty's phase transition is a
pipeline mutation. The handshake handler literally removes itself, and the phase is represented by
which handlers are installed — not by a boolean.**

### The two participants

`WebSocketServerProtocolHandler` is the long-lived handler the user installs. It is a
`MessageToMessageDecoder<WebSocketFrame>` (`WebSocketServerProtocolHandler.java:54`:
`public class WebSocketServerProtocolHandler extends WebSocketProtocolHandler`, and
`WebSocketProtocolHandler.java:34`:
`abstract class WebSocketProtocolHandler extends MessageToMessageDecoder<WebSocketFrame>`), so from
the moment it is added it decodes *websocket frames*, not HTTP.

The HTTP side is a separate, package-private handler it installs for itself:

```java
// WebSocketServerProtocolHandler.java:220-227
@Override
public void handlerAdded(ChannelHandlerContext ctx) {
    ChannelPipeline cp = ctx.pipeline();
    if (cp.get(WebSocketServerProtocolHandshakeHandler.class) == null) {
        // Add the WebSocketHandshakeHandler before this one.
        cp.addBefore(ctx.name(), WebSocketServerProtocolHandshakeHandler.class.getName(),
                new WebSocketServerProtocolHandshakeHandler(serverConfig));
    }
    ...
```

```java
// WebSocketServerProtocolHandshakeHandler.java:42
class WebSocketServerProtocolHandshakeHandler extends ChannelInboundHandlerAdapter {
```

Note `handlerAdded` constructs `new WebSocketServerProtocolHandshakeHandler(...)` — a fresh
instance per connection, not shared.

### The transition itself

Inside `channelRead`, after deciding the request is a websocket upgrade, the handshake handler (a)
stores the handshaker on the channel as an attribute and (b) removes itself from the pipeline:

```java
// WebSocketServerProtocolHandshakeHandler.java:80-88
// Ensure we set the handshaker and replace this handler before we
// trigger the actual handshake. Otherwise we may receive websocket bytes in this handler
// before we had a chance to replace it.
//
// See https://github.com/netty/netty/issues/9471.
WebSocketServerProtocolHandler.setHandshaker(ctx.channel(), handshaker);
ctx.pipeline().remove(this);

final ChannelFuture handshakeFuture = handshaker.handshake(ctx, req);
```

`setHandshaker` is a plain attribute write:

```java
// WebSocketServerProtocolHandler.java:269-275
static WebSocketServerHandshaker getHandshaker(Channel channel) {
    return channel.attr(HANDSHAKER_ATTR_KEY).get();
}

static void setHandshaker(Channel channel, WebSocketServerHandshaker handshaker) {
    channel.attr(HANDSHAKER_ATTR_KEY).set(handshaker);
}
```

`handshaker.handshake(...)` then mutates the pipeline a second time, stripping the HTTP codecs and
installing the websocket frame codecs:

```java
// WebSocketServerHandshaker.java:260-266
ChannelPipeline p = channel.pipeline();
if (p.get(HttpObjectAggregator.class) != null) {
    p.remove(HttpObjectAggregator.class);
}
if (p.get(HttpContentCompressor.class) != null) {
    p.remove(HttpContentCompressor.class);
}
```

```java
// WebSocketServerHandshaker.java:278-285
p.addBefore(ctx.name(), "wsencoder", newWebSocketEncoder());
p.addBefore(ctx.name(), "wsdecoder", newWebsocketDecoder());
...
p.replace(ctx.name(), "wsdecoder", newWebsocketDecoder());
```

and on completion of the handshake write, removes the leftover HTTP response encoder:

```java
// WebSocketServerHandshaker.java:287-298
invoker.writeAndFlush(response).addListener(new ChannelFutureListener() {
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
            ChannelPipeline p = future.channel().pipeline();
            p.remove(encoderName);
            ...
```

### Where the phase lives, and whether it is a boolean

**There is no phase boolean and no state machine object.** Phase is encoded structurally, twice
over:

1. **Handler presence.** `WebSocketServerProtocolHandshakeHandler` is in the pipeline before the
   handshake and gone after (`WebSocketServerProtocolHandshakeHandler.java:86`). Any subsequent
   HTTP upgrade attempt has no HTTP-side handler to land on.
2. **Codec presence.** The HTTP decoder/aggregator are removed and the websocket frame
   encoder/decoder are inserted (`WebSocketServerHandshaker.java:260-286`), so the pipeline's
   *type* of `Object` flowing through changes from `HttpObject` to `WebSocketFrame`. The
   distinction is enforced by the decoder chain, not by an `if`.

The one piece of state that does survive the transition is the handshaker, and it is stored as a
channel attribute (`WebSocketServerProtocolHandler.java:270`, `:274`), retrievable later by the
post-handshake handler:

```java
// WebSocketServerProtocolHandler.java:236-243
protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
    if (serverConfig.handleCloseFrames() && frame instanceof CloseWebSocketFrame) {
        WebSocketServerHandshaker handshaker = getHandshaker(ctx.channel());
```

There *is* a boolean field on the handshake handler, but it is not phase:

```java
// WebSocketServerProtocolHandshakeHandler.java:44-47
private final WebSocketServerProtocolConfig serverConfig;
private ChannelHandlerContext ctx;
private ChannelPromise handshakePromise;
private boolean isWebSocketPath;
```

`isWebSocketPath` is set at `WebSocketServerProtocolHandshakeHandler.java:65` and read at `:111` and
`:113`. It records whether *this* request's URI matched the configured websocket path — a routing
decision, consulted to decide whether to pass the message along or drop it. It says nothing about
whether the connection has completed a handshake; that question is answered by whether this handler
still exists. `ctx` and `handshakePromise` are per-connection fields on the instance
(`:53-57`, `handlerAdded`), consistent with section 5.

### How the application is told

Rather than letting application code inspect a phase flag, Netty fires an event *at the moment of
transition*:

```java
// WebSocketServerProtocolHandshakeHandler.java:96-103
localHandshakePromise.trySuccess();
// Kept for compatibility
ctx.fireUserEventTriggered(
        WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE);
ctx.fireUserEventTriggered(
        new WebSocketServerProtocolHandler.HandshakeComplete(
                req.uri(), req.headers(), handshaker.selectedSubprotocol()));
```

The official example reacts to that event by mutating the pipeline again — removing the HTTP index
page handler:

```java
// example/src/main/java/io/netty/example/http/websocketx/server/WebSocketFrameHandler.java:45-53
@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
        //Channel upgrade to websocket, remove WebSocketIndexPageHandler.
        ctx.pipeline().remove(WebSocketIndexPageHandler.class);
    } else {
        super.userEventTriggered(ctx, evt);
    }
}
```

That example also shows the whole phase split at construction time — HTTP handlers, then the
protocol handler, then the frame handler, all in one pipeline:

```java
// example/.../WebSocketServerInitializer.java:47-52
pipeline.addLast(new HttpServerCodec());
pipeline.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
pipeline.addLast(new WebSocketIndexPageHandler(WEBSOCKET_PATH));
pipeline.addLast(new WebSocketServerCompressionHandler(MAX_CONTENT_LENGTH));
pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
pipeline.addLast(new WebSocketFrameHandler());
```

Note `HandshakeComplete` carries the request URI and headers
(`WebSocketServerProtocolHandler.java:78-100`) — i.e. the data the post-handshake phase needs is
*handed to* the next phase in an event object rather than left behind in a mutable context for the
next phase to go read.

(Javadoc:
[WebSocketServerProtocolHandler](https://netty.io/4.1/api/io/netty/handler/codec/http/websocketx/WebSocketServerProtocolHandler.html).
`WebSocketServerProtocolHandshakeHandler` is package-private, so it has no javadoc page — source is
the only reference.)

---

## 5. How a stateful per-connection inbound handler is written in Netty

**The default and recommended pattern is: the handler instance holds the fields, and you allocate
one instance per connection.** This is stated as a recommendation in the `ChannelHandler` javadoc:

> "**State management** — A `ChannelHandler` often needs to store some stateful information. The
> simplest and recommended approach is to use member variables"
> — `ChannelHandler.java:61-64`

with the worked example structured as a field plus a branch on it:

```java
// ChannelHandler.java:70-88 (javadoc excerpt)
public class DataServerHandler extends SimpleChannelInboundHandler<Message> {

    private boolean loggedIn;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message message) {
        if (message instanceof LoginMessage) {
            authenticate((LoginMessage) message);
            loggedIn = true;
        } else (message instanceof GetDataMessage) {
            if (loggedIn) {
                ctx.writeAndFlush(fetchSecret((GetDataMessage) message));
            } else {
                fail();
            }
        }
    }
```

and the correctness condition that comes with it — a per-connection field is only safe if the
instance is not shared:

> "Because the handler instance has a state variable which is dedicated to one connection, you have
> to create a new handler instance for each new channel to avoid a race condition where an
> unauthenticated client can get the confidential information"
> — `ChannelHandler.java:90-93`

with the corresponding initializer:

```java
// ChannelHandler.java:97-102 (javadoc excerpt)
public class DataServerInitializer extends ChannelInitializer<Channel> {
    @Override
    public void initChannel(Channel channel) {
        channel.pipeline().addLast("handler", new DataServerHandler());
    }
}
```

Note that the field-based version is a `boolean` (`loggedIn`) — the same shape as the naive
alternative to a state machine. Netty's objection is not to the boolean; it is to sharing the
instance that owns it.

The official user guide teaches the same thing. Its `DiscardServerHandler` extends
`ChannelInboundHandlerAdapter` and overrides `channelRead(ChannelHandlerContext, Object)` and
`exceptionCaught`, and the guide's stream-fragmentation fix allocates a cumulative `ByteBuf` as a
handler field and releases it in `handlerRemoved()`, describing `handlerAdded()`/`handlerRemoved()`
as "two life cycle listener methods" for exactly this purpose
(https://netty.io/wiki/user-guide-for-4.x.html). Handlers are added inside
`ChannelInitializer.initChannel`, and the guide says of the handler added there: "The handler
specified here will always be evaluated by a newly accepted Channel."

Real Netty code follows this. `WebSocketProtocolHandler` — a handler that is *not* `@Sharable` and
is added once per connection — keeps per-connection mutable state as an instance field:

```java
// WebSocketProtocolHandler.java:40
private ChannelPromise closeSent;
// WebSocketProtocolHandler.java:123
closeSent = promise;
```

and `WebSocketServerProtocolHandshakeHandler` does the same for `ctx`, `handshakePromise` and
`isWebSocketPath` (`WebSocketServerProtocolHandshakeHandler.java:45-47`), initialized in
`handlerAdded` (`:53-57`):

```java
// WebSocketServerProtocolHandshakeHandler.java:53-57
@Override
public void handlerAdded(ChannelHandlerContext ctx) {
    this.ctx = ctx;
    handshakePromise = ctx.newPromise();
}
```

**The `@Sharable` + attributes variant** is the alternative, for when you do not want an instance
per connection (`ChannelHandler.java:106-151`), and it is the variant Netty uses for
`ChannelInitializer` (`ChannelInitializer.java:53`). Note `ChannelHandlerAdapter.ensureNotSharable()`
exists to let a handler refuse sharing outright:

```java
// ChannelHandlerAdapter.java:35-39
protected void ensureNotSharable() {
    if (isSharable()) {
        throw new IllegalStateException("ChannelHandler " + getClass().getName() + " is not allowed to be shared");
    }
}
```

(Javadoc:
[ChannelInboundHandler](https://netty.io/4.1/api/io/netty/channel/ChannelInboundHandler.html),
[ChannelInboundHandlerAdapter](https://netty.io/4.1/api/io/netty/channel/ChannelInboundHandlerAdapter.html),
[ChannelHandler](https://netty.io/4.1/api/io/netty/channel/ChannelHandler.html).)

---

## 6. Is there a Netty state-machine example?

**There is no `StateMachine` abstraction in Netty.** A recursive listing of the entire `4.1` tree
(4443 paths, `https://api.github.com/repos/netty/netty/git/trees/4.1?recursive=1`) contains no file
named `*StateMachine*`. The only `*State*.java` types are three enums:
`codec/src/main/java/io/netty/handler/codec/ProtocolDetectionState.java`,
`handler/src/main/java/io/netty/handler/pcap/State.java`, and
`handler/src/main/java/io/netty/handler/timeout/IdleState.java`.

What exists instead, in two flavours:

**(a) An explicit private `State` enum as a field inside a codec.** `HttpObjectDecoder` — the HTTP
parser — is the closest thing in Netty to a hand-written state machine:

```java
// HttpObjectDecoder.java:222-240
/**
 * The internal state of {@link HttpObjectDecoder}.
 * <em>Internal use only</em>.
 */
private enum State {
    SKIP_INITIAL_LINE_CHARS,
    SKIP_CONTROL_CHARS,
    READ_INITIAL,
    READ_HEADER,
    READ_VARIABLE_LENGTH_CONTENT,
    READ_FIXED_LENGTH_CONTENT,
    READ_CHUNK_SIZE,
    READ_CHUNKED_CONTENT,
    READ_CHUNK_DELIMITER,
    READ_CHUNK_FOOTER,
    BAD_MESSAGE,
    UPGRADED
}

private State currentState = State.SKIP_INITIAL_LINE_CHARS;
```

Notably it includes an `UPGRADED` terminal state (`HttpObjectDecoder.java:237`, entered at `:716`),
i.e. even the parser that models upgrade as a state does so inside a handler whose lifetime is
bounded by the codec stage, and the enum is `private` + documented "Internal use only"
(`HttpObjectDecoder.java:223-224`). It is an implementation detail, not an extension point.

**(b) The pipeline itself as the state machine.** `PortUnificationServerHandler` is the official
example, described in its own javadoc as:

> "Manipulates the current pipeline dynamically to switch protocols or enable SSL or GZIP."
> — `example/src/main/java/io/netty/example/portunification/PortUnificationServerHandler.java:37-40`

Its `decode` sniffs bytes and dispatches to a method per protocol; every branch ends by installing
the handlers for that protocol and removing itself:

```java
// PortUnificationServerHandler.java:60-82 (abridged)
protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (in.readableBytes() < 5) { return; }
    if (isSsl(in)) {
        enableSsl(ctx);
    } else {
        ...
        } else if (isHttp(magic1, magic2)) {
            switchToHttp(ctx);
        } else if (isFactorial(magic1)) {
            switchToFactorial(ctx);
        } else {
            in.clear();
            ctx.close();
        }
    }
}
```

```java
// PortUnificationServerHandler.java:131-138
private void switchToHttp(ChannelHandlerContext ctx) {
    ChannelPipeline p = ctx.pipeline();
    p.addLast("decoder", new HttpRequestDecoder());
    p.addLast("encoder", new HttpResponseEncoder());
    p.addLast("deflater", new HttpContentCompressor((CompressionOptions[]) null));
    p.addLast("handler", new HttpSnoopServerHandler());
    p.remove(this);
}
```

This is exactly the same move as the websocket handshake: detect a phase boundary, swap the handler
set, remove the discriminator. The "state" is the pipeline's current composition. Compare
`enableSsl` (`:116-121`) and `enableGzip` (`:123-129`), which likewise end with `p.remove(this)`.

There is also a small enum used for the *return value* of detection, not for stored phase:

```java
// ProtocolDetectionState.java:18-36
/**
 * The state of the current detection.
 */
public enum ProtocolDetectionState {
    /** Need more data to detect the protocol. */
    NEEDS_MORE_DATA,
    /** The data was invalid. */
    INVALID,
    /** Protocol was detected, */
    DETECTED
}
```

---

## Comparison: Netty's model vs. this repo's model

Stated as facts about each design, with the cost each one pays.

### Where the two models agree

- **Per-connection state is real and must be owned by something connection-scoped.** Netty:
  `AbstractChannel.java:49` (one pipeline per channel) and `ChannelPipeline.java:41`. This repo:
  `ConnectionPipeline.java:24`, a `ConcurrentHashMap` keyed by `NetworkRequestData`. Both are maps
  from connection identity to state; the difference is that Netty's maps are one pipeline object
  per channel, while ours is a single global map with the connection as key.
- **Something must own the phase, and it must be consulted on every read.** Netty consults it
  implicitly (which handlers are installed; the decoder chain decides what `Object` type arrives).
  This repo consults it explicitly (`ConnectionState.transition`, and again at
  `NIOIncomingSocketHandler.java:67`).
- **Both designs put the pre-handshake phase and the post-handshake phase on the same
  per-connection container.** No connection object is recreated at the boundary in either model.
- **Both have a "close the connection" outcome reachable from the phase logic.** Netty:
  `ctx.close()` (`PortUnificationServerHandler.java:80`) and
  `WebSocketServerProtocolHandshakeHandler.java:166`. This repo: `transition()` returning `null`
  (`ConnectionPipeline.java:38-42`).

### Where they differ

| Aspect | Netty | This repo |
|---|---|---|
| How phase changes | The pipeline is mutated: a handler removes itself (`WebSocketServerProtocolHandshakeHandler.java:86`), codecs are swapped (`WebSocketServerHandshaker.java:260-286`) | A `ConnectionState` object is replaced in a map (`ConnectionPipeline.java:43-45`); the handler set is fixed |
| What represents "post-handshake" | The absence of the HTTP handler and the presence of the websocket codecs | The presence of a `MessageState` instance holding the `HttpUpgradeRequest` (`MessageState.java:11`) |
| Where the upgrade request lives after handshake | On the channel, as an attribute (`WebSocketServerProtocolHandler.java:102-103`, `:270`, `:274`), plus copied into the `HandshakeComplete` event (`:100-102`) | In `MessageState.request`, copied back into a per-event `ReadableContext` on every read (`ConnectionPipeline.java:65`) |
| How the handler learns the phase | By the type of message it receives (the decoder guarantees frames) and by a one-shot `HandshakeComplete` event | By reading `ReadableContext.isHandshakeComplete()` (`NIOIncomingSocketHandler.java:67`) |
| Number of phase representations | One — the pipeline composition | Two — the `ConnectionState` in the map, and the derived boolean `httpUpgradeRequest != null` (`ReadableContext.java:18-20`) |
| Objects created per read event | Netty allocates per event too, but the message itself (`ByteBuf`/frame) carries the data; `ChannelHandlerContext` is not rebuilt | `ConnectionContext` and `ReadableContext` are rebuilt per read event (`ConnectionPipeline.java:58-72`, called from `:34` inside `process`) |
| Who enforces "don't reuse this handler" | The framework: `checkMultiplicity` throws (`DefaultChannelPipeline.java:544-554`) | Not applicable — there is one `PerConnectionHandler` shared across all connections by construction (`ConnectionPipeline.java:27-29`) |

On the specific question of count of truth sources: in Netty, `HandshakeComplete` is fired once
(`WebSocketServerProtocolHandshakeHandler.java:98-102`) and is not retained as a queryable flag; the
*durable* answer to "what phase is this connection in" is the pipeline. This repo has the phase in
two places at once — `connectionStates` holds the authoritative `ConnectionState`, while
`ReadableContext.isHandshakeComplete()` derives the same fact from a nullable field for the
handler's benefit. The two are kept consistent only because the pipeline copies
`MessageState.request` back into the context each event (`ConnectionPipeline.java:65`) after the
handler wrote it there on the handshake event
(`NIOIncomingSocketHandler.java:78`).

### What Netty's design costs

- **Pipeline mutation is a synchronized, list-splicing operation.** `internalAdd` runs under
  `synchronized (this)` (`DefaultChannelPipeline.java:165`), and add/remove fires
  `handlerAdded`/`handlerRemoved` callbacks, some deferred until registration
  (`DefaultChannelPipeline.java:191-195`). Mutating a pipeline is heavier than writing a field.
- **A handler instance per connection for every non-`@Sharable` handler.** Netty allocates
  `new WebSocketServerProtocolHandshakeHandler(serverConfig)`
  (`WebSocketServerProtocolHandler.java:225-226`) per channel, and the user's frame handler too
  (`WebSocketServerInitializer.java:52`). The `@Sharable` + `AttributeKey` route exists precisely to
  avoid this, at the cost of an attribute lookup per access.
- **`@Sharable` discipline is enforced only partially.** The check at
  `DefaultChannelPipeline.java:544-554` catches re-adding an already-added non-sharable instance,
  but the annotation itself is documented as advisory ("provided for documentation purpose",
  `ChannelHandler.java:167-169`) — it does not make a handler thread-safe, and the framework cannot
  verify the claim.
- **Reasoning about ordering requires knowing the pipeline layout and its history.** The post-
  handshake invariant ("no HTTP objects can arrive") is a property of the current handler set,
  which changes over the connection's life. That is invisible from any single handler's source; the
  ordering constraint at `WebSocketServerProtocolHandshakeHandler.java:80-84` (set the handshaker
  and remove the handler *before* starting the handshake, or bytes can arrive at the wrong handler)
  is evidence that the invariant is subtle enough to have produced a real bug
  (netty/netty#9471, referenced in the comment).
- **Phase is not queryable.** There is no `isHandshakeComplete()` to ask. A handler that needs to
  know must either have been told via `HandshakeComplete` at the transition
  (`WebSocketServerProtocolHandshakeHandler.java:100-102`) or infer it from the attribute being
  set (`WebSocketServerProtocolHandler.java:238-239`). Netty answers "what phase" with "what
  handlers" plus an event, not with a predicate.

### What this repo's design costs

- **Two sources of truth for phase.** `connectionStates` (`ConnectionPipeline.java:24`) and
  `ReadableContext.isHandshakeComplete()` (`ReadableContext.java:18-20`) both encode handshake vs.
  message. They can only diverge if the copy-back at `ConnectionPipeline.java:65` is skipped or if
  a caller constructs a `ConnectionContext` by another route; nothing enforces the invariant
  structurally. Netty has one representation, and it cannot be inconsistent with itself.
- **The `ConnectionState` machine and the handler's boolean are independent decision procedures
  that must agree.** `HandShakeState.transition` (`HandShakeState.java:9`) and
  `NIOIncomingSocketHandler.handle` (`NIOIncomingSocketHandler.java:67`) each independently ask
  "is the handshake done?" and must reach the same answer for the design to work, since the first
  commits the phase change and the second chooses the code path.
- **`ReadableContext` exists to bridge two collaborators, not to hold connection state.**
  It is rebuilt every read (`ConnectionPipeline.java:63-66`) and its `httpUpgradeRequest` field is
  written by the handler (`NIOIncomingSocketHandler.java:78`) and read back by the state machine
  (`HandShakeState.java:9-10`), then re-populated by the pipeline from `MessageState` on subsequent
  events (`ConnectionPipeline.java:65`). It is a per-event mailbox whose only payload is data that
  already exists in `MessageState`. Its `TracingContext` field is genuinely per-event, which is a
  reason the object exists; the request field is not.
- **Phase transitions allocate.** `HandShakeState.transition` returns `new MessageState(...)`
  (`HandShakeState.java:10`) — once per connection, which is comparable to Netty's per-connection
  handler allocation.
- **The state machine's vocabulary is larger than the problem.** Two state classes and a
  `transition` protocol (`ConnectionState.java:5-8`) encode a two-phase lifecycle, where the
  discriminator is one nullable field. The `null`-means-close convention
  (`ConnectionPipeline.java:38-42`) puts a control-flow signal in a return value with no type-level
  marker.
- **The absent enforcement has a mirror.** Netty throws if a non-sharable handler is reused
  (`DefaultChannelPipeline.java:548-550`). Here the opposite holds by construction: exactly one
  `PerConnectionHandler` instance serves every connection (`ConnectionPipeline.java:27-29`), so all
  per-connection data *must* live in the map — which is consistent, but means the handler can never
  be the owner of per-connection state even if that would be simpler.

### Does Netty's approach apply at this size?

Facts relevant to that judgement:

- **Netty's phase mechanism is not separable from its pipeline.** Removing a handler only *means*
  something because the pipeline is the dispatch mechanism and the decoder chain defines what
  arrives. There is no way to adopt "remove yourself from the pipeline" without first having a
  pipeline with ordered, typed handler stages. Netty's own HTTP stack needs that structure anyway:
  `HttpServerCodec` → `HttpObjectAggregator` → `WebSocketServerProtocolHandler` →
  application handler (`WebSocketServerInitializer.java:47-52`), each with a distinct input type.
- **The empty-phase problem.** Netty's `HandShakeState` is stateless (`HandShakeState.java:5`) —
  in the pipeline model there is nothing to represent, because "pre-handshake" is *being* the
  handshake handler. A one-state-object-per-phase design only earns its keep if states carry
  different data; here one of the two carries nothing.
- **Attribute maps are themselves machinery.** `AttributeKey`, `AttributeMap`, `Attribute`,
  `ConstantPool`, and `AbstractConstant` (four files plus a base class) exist to provide a
  connection-scoped, namespaced, thread-safe, lazily-allocated map. A design that already has a
  `Map<NetworkRequestData, ConnectionState>` (`ConnectionPipeline.java:24`) has the same capability
  in one line, with the connection as the key instead of holding the map on the connection.
- **Netty's own escape hatches point the other way at small scale.** Where Netty does not need
  attribute lookup, it uses handler fields (`WebSocketProtocolHandler.java:40`) or a plain
  `Set<ChannelHandlerContext>` with the explicit justification "This way we can reduce the memory
  usage compared to use Attributes" (`ChannelInitializer.java:57-58`). For a two-phase lifecycle
  whose phase-2 payload is a single already-parsed object, the whole attribute/pipeline apparatus
  addresses problems — handler reuse, thread-safety of a shared instance, typed stage boundaries —
  that a single shared handler over a per-connection map does not have.
- **Where Netty's model would still be over-engineering here:** `checkMultiplicity`, `@Sharable`
  detection and its cache (`ChannelHandlerAdapter.java:45-62`), the pending-callback list for
  handlers added before registration (`DefaultChannelPipeline.java:75-83`), package-private
  self-removing helper handlers (`WebSocketServerProtocolHandshakeHandler.java:42`), and the
  `HANDSHAKER_ATTR_KEY` indirection (`WebSocketServerProtocolHandler.java:102-103`) are all costs
  incurred to support multiple handlers, dynamic reconfiguration and framework extensibility. A
  server with one handler and a two-state lifecycle uses none of those degrees of freedom.
- **The one Netty property that has no equivalent here** is that Netty's phase representation is
  *single-valued by construction*: the pipeline cannot simultaneously contain and not contain the
  handshake handler. That property is what the two-sources-of-truth issue in this repo lacks, and it
  is obtainable without adopting pipelines — by deriving the handler's branch from the same
  `ConnectionState` the pipeline consults, instead of from a parallel boolean.

---

## Source index

Fetched from `https://raw.githubusercontent.com/netty/netty/4.1/<path>` at commit
`becd891ebef7f62847fb48174278823941120d6a`:

- `transport/src/main/java/io/netty/channel/ChannelHandlerContext.java`
- `transport/src/main/java/io/netty/channel/AbstractChannelHandlerContext.java`
- `transport/src/main/java/io/netty/channel/ChannelPipeline.java`
- `transport/src/main/java/io/netty/channel/DefaultChannelPipeline.java`
- `transport/src/main/java/io/netty/channel/ChannelHandler.java`
- `transport/src/main/java/io/netty/channel/ChannelHandlerAdapter.java`
- `transport/src/main/java/io/netty/channel/ChannelInboundHandler.java`
- `transport/src/main/java/io/netty/channel/Channel.java`
- `transport/src/main/java/io/netty/channel/AbstractChannel.java`
- `transport/src/main/java/io/netty/channel/ChannelInitializer.java`
- `common/src/main/java/io/netty/util/AttributeKey.java`
- `common/src/main/java/io/netty/util/AttributeMap.java`
- `common/src/main/java/io/netty/util/Attribute.java`
- `common/src/main/java/io/netty/util/ConstantPool.java`
- `codec/src/main/java/io/netty/handler/codec/ProtocolDetectionState.java`
- `codec-http/src/main/java/io/netty/handler/codec/http/HttpObjectDecoder.java`
- `codec-http/src/main/java/io/netty/handler/codec/http/websocketx/WebSocketServerProtocolHandler.java`
- `codec-http/src/main/java/io/netty/handler/codec/http/websocketx/WebSocketServerProtocolHandshakeHandler.java`
- `codec-http/src/main/java/io/netty/handler/codec/http/websocketx/WebSocketServerHandshaker.java`
- `codec-http/src/main/java/io/netty/handler/codec/http/websocketx/WebSocketProtocolHandler.java`
- `example/src/main/java/io/netty/example/portunification/PortUnificationServerHandler.java`
- `example/src/main/java/io/netty/example/http/websocketx/server/WebSocketFrameHandler.java`
- `example/src/main/java/io/netty/example/http/websocketx/server/WebSocketServerInitializer.java`

Javadoc:
[ChannelHandler](https://netty.io/4.1/api/io/netty/channel/ChannelHandler.html) ·
[ChannelHandlerContext](https://netty.io/4.1/api/io/netty/channel/ChannelHandlerContext.html) ·
[ChannelPipeline](https://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html) ·
[ChannelInboundHandler](https://netty.io/4.1/api/io/netty/channel/ChannelInboundHandler.html) ·
[AttributeKey](https://netty.io/4.1/api/io/netty/util/AttributeKey.html) ·
[AttributeMap](https://netty.io/4.1/api/io/netty/util/AttributeMap.html) ·
[WebSocketServerProtocolHandler](https://netty.io/4.1/api/io/netty/handler/codec/http/websocketx/WebSocketServerProtocolHandler.html) ·
[Netty user guide (4.x)](https://netty.io/wiki/user-guide-for-4.x.html)
