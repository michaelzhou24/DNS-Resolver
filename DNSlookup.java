import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 *
 */

/**
 * @author Donald Acton This example is adapted from Kurose & Ross Feel free to
 *         modify and rearrange code as you see fit
 */
public class DNSlookup {

    private static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
    private static final int MAX_PERMITTED_ARGUMENT_COUNT = 3;
    private static final int DNS_SERVER_PORT = 53;
    private static DatagramSocket socket;
    private static String fqdn;
    private static DNSResponse response; // Just to force compilation
    private static boolean tracingOn = false;
    private static boolean IPV6Query = false;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        int argCount = args.length;
        // int argCount = args.length;
        // boolean tracingOn = false;
        // boolean IPV6Query = false;

        InetAddress rootNameServer;
        String UserInput;

        if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
            usage();
            return;
        }

        rootNameServer = InetAddress.getByName(args[0]);
        fqdn = args[1];

        if (argCount == 3) { // option provided
            if (args[2].equals("-t"))
                tracingOn = true;
            else if (args[2].equals("-6"))
                IPV6Query = true;
            else if (args[2].equals("-t6")) {
                tracingOn = true;
                IPV6Query = true;
            } else { // option present but wasn't valid option
                usage();
                return;
            }
        }
        DNSQuery query = new DNSQuery(fqdn, rootNameServer, tracingOn, IPV6Query);
        String result = query.query(rootNameServer);
    }

    private static void sendQuery(byte[] frame, InetAddress rootNameServer) {
        DatagramPacket dnsReqPacket = new DatagramPacket(frame, frame.length, rootNameServer, DNS_SERVER_PORT);
        try {
            socket.send(dnsReqPacket);
        } catch (IOException e) {
            System.err.println("Failed to send DNS request.");
            e.printStackTrace();
        }
    }

    // Start adding code here to initiate the lookup
    private static void findAndPrintTrace(String domainName) {

    }

    // Encode query as a message following the doamin protocal
//    private static byte[] encodeQuery(int queryID) {
//        byte[] queryBuffer = new byte[512];
//        int thirdByte = queryID >>> 8;
//        int forthByte = queryID & 0xff;
//        queryBuffer[0] = (byte) thirdByte;
//        queryBuffer[1] = (byte) forthByte;
//        int QROpcodeAATCRD = 0; // 0 iterative, 1 recursive
//        queryBuffer[2] = (byte) QROpcodeAATCRD;
//        int RAZRCODE = 0;
//        queryBuffer[3] = (byte) RAZRCODE;
//        int QDCOUNT = 1;
//        queryBuffer[4] = (byte) 0;
//        queryBuffer[5] = (byte) QDCOUNT;
//        int ANCOUNT = 0;
//        queryBuffer[6] = (byte) 0;
//        queryBuffer[7] = (byte) ANCOUNT;
//        int NSCOUNT = 0;
//        queryBuffer[8] = (byte) 0;
//        queryBuffer[9] = (byte) NSCOUNT;
//        int ARCOUNT = 0;
//        queryBuffer[10] = (byte) 0;
//        queryBuffer[11] = (byte) ARCOUNT;
//        int ptr = 12;
//        String[] labels = fqdn.split("\\.");
//        for (int i = 0; i < labels.length; i++) {
//            String label = labels[i];
//            queryBuffer[ptr++] = (byte) label.length();
//            for (char c : label.toCharArray()) {
//                queryBuffer[ptr++] = (byte) ((int) c);
//            }
//        }
//        queryBuffer[ptr++] = (byte) 0; // end of QNAME
//         // encode query type
//         int QTYPE;
//         if (IPV6Query = true){
//         QTYPE = 1;
//         } else {QTYPE = 1;}
//        queryBuffer[ptr++] = (byte) ((QTYPE >>> 8) & 0xff);
//        queryBuffer[ptr++] = (byte) (QTYPE & 0xff);
//        int QCLASS = 1; // always Internet(IN)
//        queryBuffer[ptr++] = (byte) 0;
//        queryBuffer[ptr++] = (byte) QCLASS;
//        return Arrays.copyOfRange(queryBuffer, 0, ptr);
//    }


    private static void decodeOneRR(byte[] responseBuffer){
        //
    }

    // require work on decoding Authority section and Additional section 
    // should be similar with the answer section



















    private static void usage() {
        System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-6|-t|t6]");
        System.out.println("   where");
        System.out.println("       rootDNS - the IP address (in dotted form) of the root");
        System.out.println("                 DNS server you are to start your search at");
        System.out.println("       name    - fully qualified domain name to lookup");
        System.out.println("       -6      - return an IPV6 address");
        System.out.println("       -t      - trace the queries made and responses received");
        System.out.println("       -t6     - trace the queries made, responses received and return an IPV6 address");
    }

    // ---------------------------------------- Helper Functions
    // -------------------------------------------

    // 2 byte -> int
    private static int TwoByteToInt(byte b1, byte b2) {
        return ((b1 & 0xFF) << 8) + (b2 & 0xFF);
    }

    // 4 byte -> int
    private static int FoutByteToInt(byte b1, byte b2, byte b3, byte b4) {
        return ((b1 & 0xFF) << 24) + ((b2 & 0xFF) << 16) + ((b3 & 0xFF) << 8) + (b4 & 0xFF);
    }

}
