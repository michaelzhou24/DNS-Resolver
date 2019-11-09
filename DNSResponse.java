import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

// Lots of the action associated with handling a DNS query is processing 
// the response. Although not required you might find the following skeleton of
// a DNSreponse helpful. The class below has a bunch of instance data that typically needs to be 
// parsed from the response. If you decide to use this class keep in mind that it is just a 
// suggestion.  Feel free to add or delete methods or instance variables to best suit your implementation.

public class DNSResponse {
    private boolean isCNAME;
    private List<String> trace;
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
    private int pointer = 0;              // pointer for en/decoding messages
    // Note you will almost certainly need some additional instance variables.
    private ArrayList<DNSResourceRecord> answers;
    private ArrayList<DNSResourceRecord> additionalInfo;
    private ArrayList<DNSResourceRecord> AuthoritativeNSs;

    // When in trace mode you probably want to dump out all the relevant information in a response
    // Getters


    public boolean isCNAME() {
        return isCNAME;
    }

    public int getQueryID() {
        return queryID;
    }

    public List<String> getTrace() {
        return trace;
    }

    public ArrayList<DNSResourceRecord> getAnswers() {
        return answers;
    }

    public ArrayList<DNSResourceRecord> getAdditionalInfo() {
        return additionalInfo;
    }

    public ArrayList<DNSResourceRecord> getAuthoritativeNSs() {
        return AuthoritativeNSs;
    }

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
                        int len, String fdqn, boolean isIPV6, int queryID)  throws DNSRcode3Exception, DNSRcodeException {
	    buf = data;
	    this.isCNAME = false;
	    this.responsePacket = responsePacket;
	    this.fdqn = fdqn;
	    this.isIPV6 = isIPV6;
	    this.expQueryID = queryID;
        decodeResponse(buf);
	}


    // Decode response header
    private void decodeResponse(byte[] responseBuffer) throws DNSRcode3Exception, DNSRcodeException{


        // header section:

        int responseID = TwoByteToInt(responseBuffer[0], responseBuffer[1]);
        queryID = responseID;
        int QR = (responseBuffer[2] & 0x80) >>> 7; // get 1st bit
        int opCode = (responseBuffer[2] & 0x78) >>> 3; // get 2nd, 3rd, 4th and 5th bit
        int AA = (responseBuffer[2] & 0x04) >>> 2; // geth 6th
        int TC = (responseBuffer[2] & 0x02) >>> 1; // get 7th bit
        int RD = responseBuffer[2] & 0x01; // get 8th bit
        int RA = responseBuffer[3] & 0x80;
        int RCODE = responseBuffer[3] & 0x0F;

        String message = "";
        switch (RCODE) {
            case 0:
                message = "No error condition";
                break;
            case 3:
                throw new DNSRcode3Exception();
            default:
                throw new DNSRcodeException();
        }
        // System.out.println(message);

        int QDCOUNT = TwoByteToInt(responseBuffer[4], responseBuffer[5]);
        int ANCOUNT = TwoByteToInt(responseBuffer[6], responseBuffer[7]);
        int NSCOUNT = TwoByteToInt(responseBuffer[8], responseBuffer[9]);
        int ARCOUNT = TwoByteToInt(responseBuffer[10], responseBuffer[11]);
        if (ANCOUNT > 0)
            authoritative = true;

        // question section:

        pointer = 12;
        String receivedQNAME = "";
        while(true) {
            int labelLength = responseBuffer[pointer++] & 0xFF;
            if (labelLength == 0)
                break;
            for (int i = 0; i < labelLength; i++) {
                char ch = (char) (responseBuffer[pointer++] & 0xFF);
                receivedQNAME += ch;
            }
            receivedQNAME += '.';
        }

        int QTYPE = TwoByteToInt(responseBuffer[pointer++], responseBuffer[pointer++]);
        int QCLASS = TwoByteToInt(responseBuffer[pointer++], responseBuffer[pointer++]);

        // answer section:

        trace = new ArrayList<>();
        trace.add("Response ID: " + queryID + " Authoritative " + authoritative);
        DNSResourceRecord record = null;
        answers = new ArrayList<>();
        trace.add("  Answers (" + ANCOUNT + ")");
        for (int i = 0; i < ANCOUNT; i++) {
            try {
                record = decodeOneRR(responseBuffer);
            } catch (Exception e) {
                System.out.println("Unkown Host Exception caught");
            }
            if (record != null){
                answers.add(record);
                trace.add(record.getTrace());
            }
        }

        // Authoritative section:

        AuthoritativeNSs = new ArrayList<DNSResourceRecord>();
        trace.add("  Nameservers: (" + ANCOUNT + ")");
        for (int i = 0; i < NSCOUNT; i++){
            try {
                record = decodeOneRR(responseBuffer);
            } catch (Exception e) {
                System.out.println("Unkown Host Exception caught");
            }
            if (record != null) {
                AuthoritativeNSs.add(record);
                trace.add(record.getTrace());
            }
        }


        additionalInfo = new ArrayList<DNSResourceRecord>();
        trace.add("  Additional Information (" + ARCOUNT + ")");
        for (int i=0; i < ARCOUNT; i++) {
            try {
                record = decodeOneRR(responseBuffer);
            } catch (Exception e) {
                System.out.println("Unknown Host Exception caught");
            }
            if (record != null) {
                additionalInfo.add(record);
                trace.add(record.getTrace());
            }
        }
    }


    private DNSResourceRecord decodeOneRR (byte[] responseBuffer) throws Exception {
        DNSResourceRecord record = null;
        String hostName = getNameAtPointer(responseBuffer, pointer);
        int typeCode = TwoByteToInt(responseBuffer[pointer++], responseBuffer[pointer++]);
        int classCode = TwoByteToInt(responseBuffer[pointer++], responseBuffer[pointer++]);
        long TTL = FourByteToInt(responseBuffer[pointer++], responseBuffer[pointer++], responseBuffer[pointer++], responseBuffer[pointer++]);
        int RDATALength = TwoByteToInt(responseBuffer[pointer++], responseBuffer[pointer++]);
        if (typeCode == 1) { // A IPV4 address
            String address = "";
            for (int j = 0; j < RDATALength; j++) {
                int octet = responseBuffer[pointer++] & 0xFF;
                address += octet + ".";
            }
            address = address.substring(0, address.length() - 1);
            InetAddress addr = null;
            addr = InetAddress.getByName(address);

            record = new DNSResourceRecord(hostName, typeCode, TTL, addr);
        }
        else if (typeCode == 28) { // AAAA IPV6 address
            String address = "";
            for (int j = 0; j < RDATALength / 2; j ++) {
                int octet = TwoByteToInt(responseBuffer[pointer++], responseBuffer[pointer++]);
                String hex = Integer.toHexString(octet);
                address += hex + ":";
            }
            address = address.substring(0, address.length() - 1);
            InetAddress addr = null;
            addr = InetAddress.getByName(address);
            record = new DNSResourceRecord(hostName, typeCode, TTL, addr);
        }

        else if (typeCode == 2 || typeCode == 5 || typeCode == 6){ // NS or CNAME text fqdn
            String fqdn = getNameAtPointer(responseBuffer, pointer);
            record = new DNSResourceRecord(hostName, typeCode, TTL, fqdn);
            if (record.getRecordType() == 5)
                isCNAME = true;
        }

        else{ // all other types
            String fqdn = getNameAtPointer(responseBuffer, pointer);
            record = new DNSResourceRecord(hostName, typeCode, TTL, fqdn);
        }
        return record;
    }





    // ---------------------------------- Helper Functions ---------------------------------

    /**
     * 2 byte -> int
     * @param b1
     * @param b2
     * @return int
     */
    private int TwoByteToInt(byte b1, byte b2) {
        return ((b1 & 0xFF) << 8) + (b2 & 0xFF);
    }


    /**
     * 4 byte -> int
     * @param b1
     * @param b2
     * @param b3
     * @param b4
     * @return int
     */

    private int FourByteToInt(byte b1, byte b2, byte b3, byte b4) {
        return ((b1 & 0xFF) << 24) + ((b2 & 0xFF) << 16) + ((b3 & 0xFF) << 8) + (b4 & 0xFF);
    }

    private String getNameAtPointer(byte[] buffer, int ptr){
        String name = "";
        while(true) {
            int labelLength = buffer[ptr++] & 0xFF;
            if (labelLength == 0)
                break;
                // Identify message compression used, recursive call to retrieve name
            else if (labelLength >= 192) {
                int newPtr = (buffer[ptr++] & 0xFF) + 256 * (labelLength - 192);
                name += getNameAtPointer(buffer, newPtr);
                break;
            }
            // function to decode encoded name
            else {
                for (int i = 0; i < labelLength; i++) {
                    char ch = (char) (buffer[ptr++] & 0xFF);
                    name += ch;
                }
                name += '.';
            }
        }

        pointer = ptr;
        if (name.length() > 0 && name.charAt(name.length() - 1) == '.') {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }
}
