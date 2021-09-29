package io.github.jmkeyes.apache.httpclient;

import io.github.jmkeyes.apache.httpclient.ProxyProtocolConnectionSocketFactory;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertEquals;

public class ProxyProtocolConnectionSocketFactoryTest {
    @Test
    public void testProxyHeaderSentOnSocket() throws IOException {
        // The implementation is oblivious to the wrapped socket factory.
        final ConnectionSocketFactory connectionSocketFactory =
                new ProxyProtocolConnectionSocketFactory(new PlainConnectionSocketFactory());

        // Create a server-side socket for listening on a random port.
        final ServerSocket serverSocket = new ServerSocket(0);
        final int serverPort = serverSocket.getLocalPort();

        // Set up an HTTP context and a Host definition.
        final HttpContext httpContext = new BasicHttpContext();
        final HttpHost httpHost = new HttpHost("localhost", serverPort);

        // Set up remote and local socket addresses to exercise the connection socket factory.
        final InetSocketAddress remoteAddress = new InetSocketAddress("localhost", serverPort);
        final InetSocketAddress localAddress = null; // Don't call socket.bind() automatically.

        // Call into the ConnectionSocketFactory to connect the socket.
        final Socket createdSocket = connectionSocketFactory.createSocket(httpContext);
        final Socket connectedSocket = connectionSocketFactory
                .connectSocket(0, createdSocket, httpHost, remoteAddress, localAddress, httpContext);

        // Write some sentinel data to the connected socket to read back afterward.
        final byte[] expectedData = "YELLOW SUBMARINE".getBytes(US_ASCII);
        connectedSocket.getOutputStream().write(expectedData);

        // This is the server-side view of the "connectedSocket" above.
        final Socket clientSocket = serverSocket.accept();

        // Generate the expectedheader and pull data from the connection.
        final byte[] expectedHeader = getExpectedProxyHeader(connectedSocket).getBytes(US_ASCII);
        final byte[] actualHeader = new byte[expectedHeader.length];
        final int headerBytesRead = clientSocket.getInputStream().read(actualHeader);

        assertNotEquals("Should not signal end-of-file", -1, headerBytesRead);
        assertArrayEquals("Should have matching proxy header", expectedHeader, actualHeader);

        // Pull the sentinel data from the connection and verify that it is intact.
        final byte[] actualData = new byte[expectedData.length];
        final int dataBytesRead = clientSocket.getInputStream().read(actualData);

        assertNotEquals("Should not signal end-of-file", -1, dataBytesRead);
        assertArrayEquals("Should have matching data content", expectedData, actualData);

        // Finish up all the things.
        clientSocket.close();
        connectedSocket.close();
        serverSocket.close();
    }

    private String getExpectedProxyHeader(Socket socket) {
        return String.format(
                "PROXY TCP4 %s %s %d %d\r\n",
                socket.getLocalAddress().getHostAddress(),
                socket.getInetAddress().getHostAddress(),
                socket.getLocalPort(),
                socket.getPort());
    }
}
