public class PacketInfo {
    private String sourceIP;
    private String destIP;
    private String sourceMAC;
    private String destMAC;
    private String type; // PING или PONG
    private String currentDevice;

    public PacketInfo(String sourceIP, String destIP, String type, String currentDevice) {
        this.sourceIP = sourceIP;
        this.destIP = destIP;
        this.type = type;
        this.currentDevice = currentDevice;
    }

    public String toString() {
        return type + ":" + destIP + ":" + sourceIP + ":" + currentDevice;
    }

    public String getSourceIP() { return sourceIP; }
    public String getDestIP() { return destIP; }
    public String getType() { return type; }
    public String getCurrentDevice() { return currentDevice; }
}