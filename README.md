# vertx-http3-experiment

## Goal

1. An attempt to create an http3 server (returning fortunes) using vertx routes.

- As best I can tell vertx does not yet support http3 (but I could be totally wrong).
- Converting a Vertx Web RoutingContext to something for Netty will be interesting.

2. To create the entire project with AI.

## Outcome

1. What to expect from AI

- About 50 queries into this and I'm still not sure any of the AI generated code works
- Sometimes the code does not implement an Http3 server at all
- Often code that is generated, while it looks good, either it calls classes that do not exist, methods that do not exist or calls methods with the wrong arguments; however, the syntax of the program is correct.
- Seem to do a better job if the futures are in different classes or functions rather than inline
- Seems to do better when asked for small chunks rather than a whole solution
- Sometimes classes are used which an import does not exist for, or are dependant on an unknown library.
- May seem obvious, but the answers may reflect an older version of an API

2. Notes about http3

- Http3 seems great. But since its UDP, make sure your statefull firewall can handle it.
- Http3 requires QUIC. However, to QUIC, http3 is an upper layer (i.e. it doesn't care about it). In the OSI model think about TCP as layer 4, QUIC as layer 5, and HTTP as layer 6/7. An http3 server is not establishing a layer on tcp with quic functionality. An http3 server is estabishing a stream (aka channel) on top of quic. This may seem mindbending for people used to implementing protocols on tcp - especially since quic is in userspace running as a part of the server. An http3 server just handles data on a quic channel.
- Breaking this down a bit farther it means one piece of the program focuses on creating quic on top of udp, and a different part of the program focuses on creating http3 on top of quic.
- That means that you do not specify a port when defining an http3 channel
- If you actually want a client to use it, make sure your Http 2 server returns the Alt-Svc header
- Its a little unclear to me which frame type to act on as I have seen several results
- HTTTP3 Does not use chuncked encoding and Content-Length header is optional
- HTTP3 is based on streams and uses HEADER and DATA frames
- What should the http version value be?

```
example java program using netty QuicServerCodecBuilder and an http3 channelhandler to create a rest fortune server with endpoints to add and retrieve fortunes from h2. Do not use Http3ServerCodecBuilder. Place handlers, and futures in seperate classes. Do not use vertx. Include the imports for each class
```
