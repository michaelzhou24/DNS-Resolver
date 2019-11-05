import java.net.InetAddress;

/** A resource record corresponds to each RR returned by the DNS server. It contains a host name,
 * a record type (represented by a number): A(1), NS(2), CNAME(5), SOA(6), MX(15), AAAA(28), OTHER(0);
 * an IP address or a textual fqdn and a TTL
 */

public class DNSResourceRecord {

    private String hostName;
    private int recordType;
    private long TTL;
    private String textFqdn;
    private InetAddress inetAddress;


    public DNSResourceRecord(String hostName, int rType, long ttl, String fqdn) {
        this.hostName = hostName;
        this.recordType = rType;
        this.TTL = ttl;
        this.textFqdn = fqdn;
        this.inetAddress = null;
    }

    public DNSResourceRecord(String hostName, int rType, long ttl, InetAddress addr) {
        this(hostName, rType, ttl, addr.getHostAddress());
        this.inetAddress = addr;
    }


    public String getHostName(){
        return hostName;
    }

    public int getRecordType(){
        return recordType;
    }

    public long getTTL(){
        return TTL;
    }

    public String getTextFqdn(){
        return textFqdn;
    }

    public InetAddress getInetAddress(){
        return inetAddress;
    }

}
