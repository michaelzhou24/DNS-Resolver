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
    private static String fqdn;
    private static boolean tracingOn = false;
    private static boolean IPV6Query = false;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        int argCount = args.length;


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
        query.query(rootNameServer);
    }

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
}
