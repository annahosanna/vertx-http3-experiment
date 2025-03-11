// Fortune.java
package com.example;

import java.util.UUID;

public class Fortune {
    private UUID id;
    private String text;

    public Fortune(String text) {
        this.id = UUID.randomUUID();
        this.text = text;
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}

// FortuneDatabase.java
package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FortuneDatabase {
    private List<Fortune> fortunes = new ArrayList<>();
    private Random random = new Random();

    public FortuneDatabase() {
        fortunes.add(new Fortune("You will find happiness"));
        fortunes.add(new Fortune("Good fortune will be yours"));
        fortunes.add(new Fortune("A pleasant surprise is waiting for you"));
    }

    public Fortune getRandomFortune() {
        return fortunes.get(random.nextInt(fortunes.size()));
    }
}

// FortuneHandler.java
package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.incubator.codec.http3.Http3HeadersFrame;

public class FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final FortuneDatabase database;
    private final ObjectMapper mapper;

    public FortuneHandler(FortuneDatabase database) {
        this.database = database;
        this.mapper = new ObjectMapper();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) throws Exception {
        if (frame.headers().path().equals("/fortunes")) {
            Fortune fortune = database.getRandomFortune();
            String json = mapper.writeValueAsString(fortune);

            ByteBuf content = Unpooled.copiedBuffer(json.getBytes());

            Http3HeadersFrame responseHeaders = Http3HeadersFrame.builder()
                .status(HttpResponseStatus.OK.codeAsText())
                .set(HttpHeaderNames.CONTENT_TYPE, "application/json")
                .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                .build();

            ctx.write(responseHeaders);
            ctx.writeAndFlush(content);
        }
    }
}

// CertificateGenerator.java
package com.example;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class CertificateGenerator {
    public static File generateSelfSignedCertificate() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal("CN=localhost");

        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(dnName);
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L));
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

        X509Certificate cert = certGen.generate(keyPair.getPrivate());

        File certFile = File.createTempFile("cert", ".pem");
        try (FileWriter fw = new FileWriter(certFile)) {
            fw.write("-----BEGIN CERTIFICATE-----\n");
            fw.write(Base64.getEncoder().encodeToString(cert.getEncoded()));
            fw.write("\n-----END CERTIFICATE-----\n");
        }

        return certFile;
    }
}

// Server.java
package com.example;

import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

public class Server {
    private final FortuneDatabase database;
    private final int port;

    public Server(int port) {
        this.database = new FortuneDatabase();
        this.port = port;
    }

    public void start() throws Exception {
        File certFile = CertificateGenerator.generateSelfSignedCertificate();

        QuicServerCodecBuilder quicServerCodecBuilder = new QuicServerCodecBuilder()
            .certificateChain(certFile)
            .privateKey(certFile)
            .applicationProtocols(Http3.supportedApplicationProtocols());

        HttpServer.create()
            .port(port)
            .handle((request, response) -> {
                FortuneHandler handler = new FortuneHandler(database);
                return Mono.just(response);
            })
            .bindNow();
    }

    public static void main(String[] args) throws Exception {
        new Server(8443).start();
    }
}