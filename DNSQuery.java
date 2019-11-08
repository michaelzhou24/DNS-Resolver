import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class DNSQuery {

    private final int DNS_SERVER_PORT = 53;

    private short queryID = 0x0000;
    private DatagramSocket socket;
    private String fqdn;
    private InetAddress rootNS;
    private boolean toTrace;
    private boolean isIPV6;
    private List<DNSResourceRecord> trace;
    private int queryCount;

    public DNSQuery(String fqdn, InetAddress rootNS, boolean toTrace, boolean isIPV6) throws Exception {
        trace = new ArrayList<>();
        this.fqdn = fqdn;
        this.rootNS = rootNS;
        this.toTrace = toTrace;
        this.isIPV6 = isIPV6;
        this.queryCount = 0;
        socket = new DatagramSocket();
    }

    // Recursively go through nameservers
    public void query(InetAddress NS) throws IOException {
        queryCount++;
        byte[] frame = buildFrame(fqdn, isIPV6);
        sendQuery(frame, NS);
        DNSResponse response = parseQuery();

        if (response.isAuthoritative()) {
            // we have the ip
            DNSResourceRecord answer = response.getAnswers().get(0);
            System.out.println(answer.getTextFqdn());
            System.out.println(answer.getHostName());
            System.out.println(answer.getRecordType());
        } else {
            String ns = response.getAdditionalInfo().get(0).getTextFqdn();
            trace.add(response.getAdditionalInfo().get(0));
            query(InetAddress.getByName(ns));
        }
    }

    private byte[] buildFrame(String host, boolean isIPv6) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(frame);
        // Query ID
        data.writeShort(queryID);
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
            data.writeShort(0x0001);
        else
            data.writeShort(0x001c);
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

    // Return null/empty string if found ans
    // Else return IP address of next NS.
    private DNSResponse parseQuery() throws IOException {
        DNSResponse response = null;
        byte[] buf = new byte[1024];
        int i = 0;
        // Try capturing at most 30 packets
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            System.out.println("Exception receiving packet!");
        }
        response = new DNSResponse(packet, buf, buf.length, fqdn, isIPV6, queryID);
        System.out.println(response.queryID);
        System.out.println(queryID);
        queryID++;
//        if (response.queryID == queryID)
//            break;

        return response;
    }
}
