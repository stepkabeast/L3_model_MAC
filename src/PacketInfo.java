public class PacketInfo {
    private String sourceIP;
    private String destIP;
    private String sourceMAC;
    private String destMAC;
    private String type; // PING или PONG

    public PacketInfo(String sourceIP, String destIP, String type) {
        this.sourceIP = sourceIP;
        this.destIP = destIP;
        this.type = type;
    }

    public String toString() {
        return type + ":" + destIP + ":" + sourceIP;
    }

    // Getters and setters
    public String getSourceIP() { return sourceIP; }
    public String getDestIP() { return destIP; }
    public String getType() { return type; }
}