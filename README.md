# vertx-http3-experiment

An attempt to use netty incubator codecs with vertx

## Goals

- The first goal of this project is to use either netty-incubator-codec-quic or netty-incubator-codec-http3 to create an http3 server which can use vertx Routes
- Use AI to help write the code

## Issues

- Using vertx to create an http2 server listening on tcp port 443, and presenting the Alt-Svc tag shouldn't be that hard
- Might be tricky to use HPACK vs. QPACK. I'm not sure if header compression is optional
- Is writing a Vertx Route to a QUIC Channel ok? Or will it just be garbage
- What does the http3 codec add - can I just over ride a method as with the quic channel
