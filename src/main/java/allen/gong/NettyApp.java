package allen.gong;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class NettyApp {
    public static final char[] password = "123456".toCharArray();

    public static KeyStore loadKeyStore() {
        try(
                InputStream file = NettyApp.class.getResourceAsStream("/localhost.pfx");
         ){
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(file, password);
            return ks;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
//            File certChainFile = new File(".\\ssl\\server.crt");
//            File keyFile = new File(".\\ssl\\pkcs8_server.key");
//            File rootFile = new File(".\\ssl\\ca.crt");
//            SslContext sslCtx = SslContextBuilder.forServer(certChainFile, keyFile).trustManager(rootFile).clientAuth(ClientAuth.NONE).build();

//            SslContext sslCtx = SslContextBuilder.forServer(NettyApp.class.getResourceAsStream("/localhost.pfx"), NettyApp.class.getResourceAsStream("/localhost.pfx"), "123456")
//                .clientAuth(ClientAuth.NONE).build();

            KeyStore ks = loadKeyStore();
            Certificate[] certificates = ks.getCertificateChain("localhost");

            X509Certificate[] certChain = new X509Certificate[certificates.length];
            for (int i = 0; i < certificates.length; i++) {
                certChain[i] = (X509Certificate) certificates[i];
            }
            for(Certificate c : certificates){
                System.out.println(c instanceof X509Certificate);
            }

            Key key = ks.getKey("localhost", password);

            SslContext sslCtx = SslContextBuilder.forServer((PrivateKey) key, certChain).clientAuth(ClientAuth.NONE).build();

            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new TestServerInitializer(sslCtx));
            ChannelFuture channelFuture = serverBootstrap.bind(8888).sync();
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SSLException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

        }
    }
}
