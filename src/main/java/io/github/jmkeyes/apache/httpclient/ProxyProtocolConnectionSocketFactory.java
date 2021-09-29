package io.github.jmkeyes.apache.httpclient;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ProxyProtocolConnectionSocketFactory extends PlainConnectionSocketFactory {
    // PROXY protocol header templates.
    private static final String PROXY_TEMPLATE = "PROXY %s %s %s %d %d\r\n";
    private static final String PROXY_UNKNOWN = "PROXY UNKNOWN\r\n";

    // The underlying ConnectionSocketFactory we're decorating.
    private final ConnectionSocketFactory socketFactory;

    /**
     * Wrap a given {@link ConnectionSocketFactory} to send a PROXY protocol header on connect.
     *
     * @param socketFactory The {@link ConnectionSocketFactory} to wrap.
     */
    public ProxyProtocolConnectionSocketFactory(ConnectionSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    /**
     * Generate a PROXY protocol header that is compliant with the PROXY protocol v1 spec.
     */
    private String generateProxyHeader(Socket socket) {
        // Find the source and destination host addresses as IP addresses.
        final String srcHostAddr = socket.getLocalAddress().getHostAddress();
        final String dstHostAddr = socket.getInetAddress().getHostAddress();

        // Find the source and destination ports as integers.
        final int srcPort = socket.getLocalPort();
        final int dstPort = socket.getPort();

        // The length of the address in bytes implies the IP address version.
        switch (socket.getLocalAddress().getAddress().length) {
            case 4:
                // If this was an IPv4 address use protocol designation "TCP4".
                return String.format(PROXY_TEMPLATE, "TCP4", srcHostAddr, dstHostAddr, srcPort, dstPort);
            case 16:
                // If this was an IPv6 address use protocol designation "TCP6".
                return String.format(PROXY_TEMPLATE, "TCP6", srcHostAddr, dstHostAddr, srcPort, dstPort);
            default:
                // If we couldn't tell, then send "UNKNOWN" and finish the header.
                return PROXY_UNKNOWN;
        }
    }

    @Override
    public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host,
                                InetSocketAddress remoteAddress, InetSocketAddress localAddress,
                                HttpContext context) throws IOException {
        // Connect the socket using the underlying PlainConnectionSocketFactory so we can send ASCII.
        final Socket sock = super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);

        // Build a PROXY protocol header based on the now-connected socket.
        final byte[] proxyHeader = this.generateProxyHeader(sock).getBytes(StandardCharsets.US_ASCII);

        // Write the protocol header to the start of the stream.
        sock.getOutputStream().write(proxyHeader);

        // If we're using a layered connection socket factory then call into it.
        if (socketFactory instanceof LayeredConnectionSocketFactory) {
            final LayeredConnectionSocketFactory lcsf = (LayeredConnectionSocketFactory) this.socketFactory;
            return lcsf.createLayeredSocket(sock, host.getHostName(), remoteAddress.getPort(), context);
        }

        return sock;
    }
}
