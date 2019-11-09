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
    private boolean resolveNS;
    private short queryID = 0x0000;
    private DatagramSocket socket;
    private String fqdn;
    private InetAddress rootNS;
    private boolean toTrace;
    private boolean isIPV6;
    private List<String> trace;
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
    public String query(String host, InetAddress NS) throws IOException {
        queryCount++;
        byte[] frame = buildFrame(host, isIPV6);
        sendQuery(frame, NS);
        if (isIPV6)
            trace.add("\n\nQuery ID     " + queryID + " " + host + " AAAA --> " + NS.getHostAddress());
        else
            trace.add("\n\nQuery ID     " + queryID + " " + host + " A --> " + NS.getHostAddress());
        System.out.println(trace.get(trace.size()-1));
        DNSResponse response = parseQuery();
        trace.addAll(response.getTrace());
        if (response.isAuthoritative()) {
            // we have the ip
//            for (DNSResourceRecord answer : response.getAnswers()) {
//                System.out.println(answer.getTextFqdn());
//                System.out.println(answer.getHostName());
//                System.out.println(answer.getRecordType());
//            }
            if (resolveNS) {
                return response.getAnswers().get(0).getTextFqdn();
            } else if (response.isCNAME()) {
                String hostName = response.getAnswers().get(0).getTextFqdn();
                return query(hostName, rootNS);
            } else {
                if (toTrace) {
                    for (String s : trace) {
                        System.out.println(s);
                    }
                }

                for (DNSResourceRecord record : response.getAnswers()) {
                    if (!isIPV6 && record.getRecordType() == 1) {
                        System.out.println(fqdn + " " + record.getTTL() + " " + record.getTextFqdn());
                    }
                    if (isIPV6 && record.getRecordType() == 5) {
                        System.out.println(fqdn + " " + record.getTTL() + " " + record.getTextFqdn());
                    }
                }
            }
        } else {
            if (response.getAdditionalInfo().size() == 0) {
                resolveNS = true;
                String ns = query(response.getAuthoritativeNSs().get(0).getTextFqdn(), rootNS);
                resolveNS = false;
                return query(host, InetAddress.getByName(ns));
            } else {
                String ns = "";
                for (DNSResourceRecord record : response.getAdditionalInfo()) {
                    if (record.getRecordType() == 1) {
                        ns = record.getTextFqdn();
                        break;
                    }
                }
                return query(host, InetAddress.getByName(ns));
            }
        }
        return null;
    }

    private byte[] buildFrame(String host, boolean isIPv6) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(frame);

        data.writeShort(queryID);
        data.writeShort(0x0100);
        data.writeShort(0x0001);
        data.writeShort(0x0000);
        data.writeShort(0x0000);
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
            System.out.println(rootNameServer.toString());
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
        queryID++;
//        if (response.queryID == queryID)
//            break;
        return response;
    }
}
