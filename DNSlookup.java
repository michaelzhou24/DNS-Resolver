
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

/**
 *
 */

/**
 * @author Donald Acton
 * This example is adapted from Kurose & Ross
 * Feel free to modify and rearrange code as you see fit
 */
public class DNSlookup {

	private static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
	private static final int MAX_PERMITTED_ARGUMENT_COUNT = 3;

	private static DNSResponse response; // Just to force compilation


	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		int argCount = args.length;
		boolean tracingOn = false;
		boolean IPV6Query = false;
		InetAddress rootNameServer;

		if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
			usage();
			return;
		}

//		rootNameServer = InetAddress.getByName(args[0]);
//		String fqdn = args[1];
		// Temp commented for testing
		rootNameServer = InetAddress.getByName("199.7.83.42");
		String fqdn = "cs.ubc.ca";

		if (argCount == 3) {  // option provided
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
		query.query();
	}

//		Scanner in = new Scanner(System.in);
//		System.out.println("DNSLOOKUP> ");
//		UserInput = in.nextLine();
//		String[] commandArgs = UserInput.split(" ");
//		if (commandArgs[0].equalsIgnoreCase("lookup")){
//			//look up
//			findAndPrintTrace(commandArgs[1]);
//
//		}

	//Encode query as a message following the doamin protocal
//	private static byte[] encodeQuery(int queryID) {
//		byte[] queryBuffer = new byte[512];
//		int thirdByte = queryID >>> 8;
//		int forthByte = queryID & 0xff;
//		queryBuffer[0] = (byte) thirdByte;
//		queryBuffer[1] = (byte) forthByte;
//		int QROpcodeAATCRD = 0; // 0 iterative, 1 recursive
//		queryBuffer[2] = (byte) QROpcodeAATCRD;
//		int RAZRCODE = 0;
//		queryBuffer[3] = (byte) RAZRCODE;
//		int QDCOUNT = 1;
//		queryBuffer[4] = (byte) 0;
//		queryBuffer[5] = (byte) QDCOUNT;
//		int ANCOUNT = 0;
//		queryBuffer[6] = (byte) 0;
//		queryBuffer[7] = (byte) ANCOUNT;
//		int NSCOUNT = 0;
//		queryBuffer[8] = (byte) 0;
//		queryBuffer[9] = (byte) NSCOUNT;
//		int ARCOUNT = 0;
//		queryBuffer[10] = (byte) 0;
//		queryBuffer[11] = (byte) ARCOUNT;
//		int ptr = 12;
//		String[] labels = fqdn.split("\\.");
//		for (int i = 0 ; i < labels.length; i++) {
//			String label = labels[i];
//			queryBuffer[ptr++] = (byte) label.length();
//			for (char c : label.toCharArray()) {
//				queryBuffer[ptr++] = (byte) ((int) c);
//			}
//		}
//		queryBuffer[ptr++] = (byte) 0; //end of QNAME
//		int QTYPE = node.getType().getCode();
//		queryBuffer[ptr++] = (byte) ((QTYPE >>> 8) & 0xff);
//		queryBuffer[ptr++] = (byte) (QTYPE & 0xff);
//		int QCLASS = 1; // always Internet(IN)
//		queryBuffer[ptr++] = (byte) 0;
//		queryBuffer[ptr++] = (byte) QCLASS;
//		return Arrays.copyOfRange(queryBuffer, 0, ptr);
//	}

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

