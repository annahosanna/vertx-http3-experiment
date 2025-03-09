# vertx-http3-experiment

## Goal

1. An attempt to create an http3 server (returning fortunes) using vertx routes.

- As best I can tell vertx does not yet support http3 (but I could be totally wrong).
- Converting a Vertx Web RoutingContext to something for Netty will be interesting.

2. To create the entire project with AI.

## Outcome

- About 50 queries into this and I'm still not sure any of the AI generated code works:

1. Sometimes the code does not implement an Http3 server at all
2. Often code that is generated, while it looks good, either it calls classes that do not exist, methods that do not exist or calls methods with the wrong arguments; however, the syntax of the program is correct.
3. Seem to do a better job if the futures are in different classes or functions rather than inline
4. Seems to do better when asked for small chunks rather than a whole solution
5. Sometimes classes are used which an import does not exist for, or are dependant on an unknown library.
