package netty.cookbook.chapter2.recipe11;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;

public class HttpDownloader {

	static final String URL = System.getProperty("url", "http://www.mc2ads.com/");

	public static void main(String[] args) throws Exception {
		URI uri = new URI(URL);
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
		String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
		int port = uri.getPort();
		if (port == -1) {
			if ("http".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("https".equalsIgnoreCase(scheme)) {
				port = 443;
			}
		}

		if (!"http".equalsIgnoreCase(scheme)
				&& !"https".equalsIgnoreCase(scheme)) {
			System.err.println("Only HTTP(S) is supported.");
			return;
		}

		// Configure SSL context if necessary.
		final boolean ssl = "https".equalsIgnoreCase(scheme);
		final SslContext sslCtx;
		if (ssl) {
			sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
		} else {
			sslCtx = null;
		}

		// Configure the client.
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class)
					.handler(new HttpDownloadertInitializer(sslCtx));
			// Make the connection attempt.
			Channel ch = b.connect(host, port).sync().channel();
			// Prepare the HTTP request.
			HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
			HttpHeaders headers = request.headers();
			headers.set(HttpHeaders.Names.HOST, host);
			headers.set(HttpHeaders.Names.CONNECTION,HttpHeaders.Values.CLOSE);
			headers.set(HttpHeaders.Names.ACCEPT_ENCODING,HttpHeaders.Values.GZIP);
			// Set some example cookies.
			headers.set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(
					new DefaultCookie("my-cookie", "foo")));
			// Send the HTTP request.
			ch.writeAndFlush(request);
			// Wait for the server to close the connection.
			ch.closeFuture().sync();
			Thread.sleep(1000);
		} finally {
			// Shut down executor threads to exit.
			group.shutdownGracefully();
		}		
	}
}