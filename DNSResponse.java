import java.io.IOException;
import java.net.DatagramPacket;

// Lots of the action associated with handling a DNS query is processing 
// the response. Although not required you might find the following skeleton of
// a DNSreponse helpful. The class below has a bunch of instance data that typically needs to be 
// parsed from the response. If you decide to use this class keep in mind that it is just a 
// suggestion.  Feel free to add or delete methods or instance variables to best suit your implementation.



public class DNSResponse {
    private int queryID;                  // this is for the response it must match the one in the request 
    private int expQueryID;               // Expected query ID
    private int answerCount = 0;          // number of answers  
    private boolean decoded = false;      // Was this response successfully decoded
    private int nsCount = 0;              // number of nscount response records
    private int additionalCount = 0;      // number of additional (alternate) response records
    private boolean authoritative = false;// Is this an authoritative record
    private String fdqn;
    private boolean isIPV6;
    private DatagramPacket responsePacket;
    private byte[] buf;
    // Note you will almost certainly need some additional instance variables.

    // When in trace mode you probably want to dump out all the relevant information in a response
    // Getters
    public boolean isAuthoritative() {
        return authoritative;
    }

	void dumpResponse() {
        for (int i = 0; i < responsePacket.getLength(); i++) {
            System.out.print(String.format("%x", buf[i]) + " " );
        }
	}

    // The constructor: you may want to add additional parameters, but the two shown are 
    // probably the minimum that you need.

	public DNSResponse (DatagramPacket responsePacket, byte[] data,
                        int len, String fdqn, boolean isIPV6, int queryID)  throws IOException {
	    buf = data;
	    this.responsePacket = responsePacket;
	    this.fdqn = fdqn;
	    this.isIPV6 = isIPV6;
	    this.expQueryID = queryID;
	    // The following are probably some of the things 
	    // you will need to do.
	    // Extract the query ID

	    // Make sure the message is a query response and determine
	    // if it is an authoritative response or not

	    // determine answer count

	    // determine NS Count

	    // determine additional record count

	    // Extract list of answers, name server, and additional information response 
	    // records
        decodeResponse(buf);
	}

//    public void readResponse() throws IOException {
//        DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf));
//
//        queryID = (int) din.readShort();
//        System.out.println(String.format("%x", queryID));
//        short flags = din.readShort();
//        System.out.println(String.format("%x", flags));
//        // get info from flags
//        // verify response
//        String s = Integer.toBinaryString(flags).substring(16,32);
//        System.out.println(s);
//
//
//        short questions = din.readShort();
//        System.out.println(String.format("%x", questions));
//        answerCount = (int) din.readShort();
//        System.out.println(String.format("%x", answerCount));
//        short numAuth = din.readShort();
//        System.out.println(String.format("%x", numAuth));
//        short numAdditional = din.readShort();
//        System.out.println(String.format("%x", numAdditional));
//
//        // find fdqn
//        StringBuilder responseFdqn = new StringBuilder();
//        int recLen = 0;
//        while ((recLen = din.readByte()) > 0) {
//            byte[] record = new byte[recLen];
//            for (int i = 0; i < recLen; i++) {
//                record[i] = din.readByte();
//            }
//            responseFdqn.append(new String(record, "UTF-8"));
//            responseFdqn.append('.');
//        }
//        responseFdqn.setLength(responseFdqn.length() - 1);
//        System.out.println(responseFdqn.toString());
//    }

    // Decode response header
    private static void decodeResponse(byte[] responseBuffer) {
        int pointer = 0;
        int responseID = TwoByteToInt(responseBuffer[0], responseBuffer[1]);
        int QR = (responseBuffer[2] & 0x80) >>> 7; // get 1st bit
        int opCode = (responseBuffer[2] & 0x78) >>> 3; // get 2nd, 3rd, 4th and 5th bit
        int AA = (responseBuffer[2] & 0x04) >>> 2; // geth 6th
        int TC = (responseBuffer[2] & 0x02) >>> 1; // get 7th bit
        int RD = responseBuffer[2] & 0x01; // get 8th bit
        int RA = responseBuffer[3] & 0x80;
        int RCODE = responseBuffer[3] & 0x0F;
        System.out.println("responseID: " + responseID);
        System.out.println("QR: " + QR);
        System.out.println("opCode: " + opCode);
        System.out.println("AA: " + AA);
        System.out.println("TC: " + TC);
        System.out.println("RD: " + RD);
        System.out.println("RA: " + RA);
        System.out.println("RCODE: " + RCODE);

        String message = "";
        switch (RCODE) {
            case 0:
                message = "No error condition";
                break;
            case 1:
                message = "FAILED. Format error, Name server can't interpret query";
                break;
            case 2:
                message = "FAILED. Name server error";
                break;
            case 3:
                message = "FAILED. Name error – name doesnot exist";
                break;
            case 4:
                message = "FAILED. Requested quet type not supported";
                break;
            case 5:
                message = "FAILED. Request refused";
                break;
            default:
                message = "FAILED. Unknown RCODE";
                break;
        }
        System.out.println(message);

        int QDCOUNT = TwoByteToInt(responseBuffer[4], responseBuffer[5]);
        int ANCOUNT = TwoByteToInt(responseBuffer[6], responseBuffer[7]);
        int NSCOUNT = TwoByteToInt(responseBuffer[8], responseBuffer[9]);
        int ARCOUNT = TwoByteToInt(responseBuffer[10], responseBuffer[11]);

        System.out.println("QDCOUNT: " + QDCOUNT);
        System.out.println("ANCOUNT: " + ANCOUNT);
        System.out.println("NSCOUNT: " + NSCOUNT);
        System.out.println("ARCOUNT: " + ARCOUNT);


        // requires work on translating hex into char

        int QTYPE = TwoByteToInt(responseBuffer[pointer++], responseBuffer[pointer++]);
        int QCLASS = TwoByteToInt(responseBuffer[pointer++], responseBuffer[pointer++]);

        System.out.println("QTYPE: " + QTYPE);
        System.out.println("QCLASS: " + QCLASS);

        System.out.println("  Answers (" + ANCOUNT + ")");
        for (int i = 0; i < ANCOUNT; i++) {
            // requires method decoding each one of the RR received
        }

    }


    // You will probably want a method to extract a compressed FQDN, IP address
    // cname, authoritative DNS servers and other values like the query ID etc.


    // You will also want methods to extract the response records and record
    // the important values they are returning. Note that an IPV6 reponse record
    // is of type 28. It probably wouldn't hurt to have a response record class to hold
    // these records.



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
