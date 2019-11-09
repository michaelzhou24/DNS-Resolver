import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class DNSQuery {

    private int timeout = 2;
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
        this.resolveNS = false;
        socket = new DatagramSocket();
    }

    private String ipv6() {
        if (isIPV6)
            return "AAAA";
        return "A";
    }

    // Recursively go through nameservers
    public String query(String host, InetAddress NS) throws IOException {
        queryCount++;
        if (queryCount > 25) {
            handleError(fqdn+" -3  "+ipv6()+"  "+"0.0.0.0");
        }
        byte[] frame;
        if (!resolveNS)
            frame = buildFrame(host, isIPV6);
        else frame = buildFrame(host, false);

        sendQuery(frame, NS);
        if (isIPV6)
            trace.add("\n\nQuery ID     " + queryID + " " + host + " AAAA --> " + NS.getHostAddress());
        else
            trace.add("\n\nQuery ID     " + queryID + " " + host + " A --> " + NS.getHostAddress());
//        System.out.println(trace.get(trace.size()-1));
        DNSResponse response = parseQuery(host, NS);
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
                        System.out.println(fqdn + "  " + record.getTTL() + "  A  " + record.getTextFqdn());
                    }
                    if (isIPV6 && record.getRecordType() == 28) {
                        System.out.println(fqdn + "  " + record.getTTL() + "  AAAA  " + record.getTextFqdn());
                    }
                }
            }
        } else {
            if (response.getAdditionalInfo().size() == 0) {
                resolveNS = true;
                ArrayList<DNSResourceRecord> authnses = response.getAuthoritativeNSs();
                if (authnses.size() == 0) {
                    handleError(fqdn+"  -6  "+ipv6()+"  0.0.0.0");
                }
                String ns = query(authnses.get(0).getTextFqdn(), rootNS);
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

    /*
      Creates the DNS query packet based on host to query and whether or not it is an IPv6 query
     */
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

    /*
      Sends packet to nameserver.
     */
    private void sendQuery(byte[] frame, InetAddress ns) {
        DatagramPacket dnsReqPacket = new DatagramPacket(frame, frame.length, ns, DNS_SERVER_PORT);
        try {
            socket.send(dnsReqPacket);
        } catch (IOException e) {
            System.err.println("Failed to send DNS request.");
            System.out.println(ns.toString());
        }
    }

    // Return null/empty string if found ans
    // Else return IP address of next NS.
    private DNSResponse parseQuery(String host, InetAddress ns) throws IOException {
        DNSResponse response = null;
        byte[] buf = new byte[1024];
        int i = 0;
        // Try capturing at most 30 packets
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
           // timeout++;
           // if (timeout > 2) {
                handleError(fqdn + " -2  " + ipv6() + "  " + "0.0.0.0");
           // } else {
           //     query(host, ns);
           // }
        }
        try {
            response = new DNSResponse(packet, buf, buf.length, fqdn, isIPV6, queryID);
        } catch (DNSRcodeException e) {
            handleError(fqdn+" -4  "+ipv6()+"  "+"0.0.0.0");
        } catch (DNSRcode3Exception e) {
            handleError(fqdn+" -1  "+ipv6()+"  "+"0.0.0.0");
        }
        queryID++;
        return response;
    }

    private void handleError(String msg) {
        if (toTrace) {
            for (String s : trace) {
                System.out.println(s);
            }
        }
        System.out.println(msg);
        System.exit(0);
    }
}
