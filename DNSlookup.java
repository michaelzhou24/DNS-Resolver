
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
	private static final int DNS_SERVER_PORT = 53;
	private static DatagramSocket socket;
	private static String fqdn;
	private static DNSResponse response; // Just to force compilation
	private static short queryID = 0x0000;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		int argCount = args.length;
		boolean tracingOn = false;
		boolean IPV6Query = false;
		InetAddress rootNameServer;
		String UserInput;

		if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
			usage();
			return;
		}

		rootNameServer = InetAddress.getByName(args[0]);
		fqdn = args[1];

		if (argCount == 3) {  // option provided
			if (args[2].equals("-t"))
				tracingOn = true;
			else if (args[2].equals("-6"))
				IPV6Query = true;
			else if (args[2].equals("-t6")) {
				tracingOn = true;
				IPV6Query = true;
			} else  { // option present but wasn't valid option
				usage();
				return;
			}
		}
		socket = new DatagramSocket();
//		Scanner in = new Scanner(System.in);
//		System.out.println("DNSLOOKUP> ");
//		UserInput = in.nextLine();
//		String[] commandArgs = UserInput.split(" ");
//		if (commandArgs[0].equalsIgnoreCase("lookup")){
//			//look up
//			findAndPrintTrace(commandArgs[1]);
//
//		}
		byte[] frame = buildFrame(fqdn, IPV6Query);
		sendQuery(frame, rootNameServer);
		parseQuery();
	}

	private static byte[] buildFrame(String host, boolean isIPv6) throws IOException {
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

	private static void sendQuery(byte[] frame, InetAddress rootNameServer) {
		DatagramPacket dnsReqPacket = new DatagramPacket(frame, frame.length, rootNameServer, DNS_SERVER_PORT);
		try {
			socket.send(dnsReqPacket);
		} catch (IOException e) {
			System.err.println("Failed to send DNS request.");
			e.printStackTrace();
		}
	}

	private static void parseQuery() throws IOException {
		byte[] buf = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		// this is the response packet
		for (int i = 0; i < packet.getLength(); i++) {
			System.out.print(String.format("%x", buf[i]) + " " );
		}
		System.out.println();
	}

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

