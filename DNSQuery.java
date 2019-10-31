import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class DNSQuery {

    private static short queryID = 0x0000;
    private final int DNS_SERVER_PORT = 53;
    private DatagramSocket socket;
    private String fqdn;
    private InetAddress rootNS;
    private boolean toTrace;
    private boolean isIPV6;

    public DNSQuery(String fqdn, InetAddress rootNS, boolean toTrace, boolean isIPV6) throws SocketException {
        this.fqdn = fqdn;
        this.rootNS = rootNS;
        this.toTrace = toTrace;
        this.isIPV6 = isIPV6;
        socket = new DatagramSocket();
    }

    public void query() {

    }

    private byte[] buildFrame(String host, boolean isIPv6) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(frame);
        // Query ID
        data.writeShort(queryID++);
        // Standard Query
        data.writeShort(0x0100);
        // Question Count
        data.writeShort(0x0001);
        // Answer Record
        data.writeShort(0x0000);
        // Authority Record
        data.writeShort(0x0000);
        // Additional Record
        data.writeShort(0x0000);

        String[] domainParts = host.split("\\.");
        for (int i = 0; i<domainParts.length; i++) {
            byte[] domainBytes = domainParts[i].getBytes("UTF-8");
            data.writeByte(domainBytes.length);
            data.write(domainBytes);
        }
        data.writeByte(0x00);
        // Type A
        if (!isIPv6)
            data.writeShort(0xFF);
        // Class IN
        data.writeShort(0x0001);
        return frame.toByteArray();
    }


    private void sendQuery(byte[] frame, InetAddress rootNameServer) {
        DatagramPacket dnsReqPacket = new DatagramPacket(frame, frame.length, rootNameServer, DNS_SERVER_PORT);
        try {
            socket.send(dnsReqPacket);
        } catch (IOException e) {
            System.err.println("Failed to send DNS request.");
            e.printStackTrace();
        }
    }

    private void parseQuery() throws IOException {
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        // this is the response packet
        for (int i = 0; i < packet.getLength(); i++) {
            System.out.print(String.format("%x", buf[i]) + " " );
        }
        System.out.println();
    }
}
