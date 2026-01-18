import java.io.Serializable;

public class PacketInfo implements Serializable {
    private String packetId;
    private String sourceIP;
    private String destIP;
    private String sourceMAC;
    private String destMAC;
    private String type; // PING, PONG, ARP, etc.
    private String protocol = "ICMP";
    private int ttl = 64;
    private String currentHop;
    private String path = "";

    public PacketInfo(String sourceIP, String destIP, String type, String packetId) {
        this.sourceIP = sourceIP;
        this.destIP = destIP;
        this.type = type;
        this.packetId = packetId;
    }

    public void addHop(String deviceName) {
        if (!path.isEmpty()) path += " → ";
        path += deviceName;
        currentHop = deviceName;
    }

    public String toMessageString() {
        return String.join(":",
                packetId, type, sourceIP, destIP,
                (sourceMAC != null ? sourceMAC : ""),
                (destMAC != null ? destMAC : ""),
                String.valueOf(ttl),
                currentHop != null ? currentHop : "",
                path
        );
    }

    public static PacketInfo fromMessageString(String message) {
        String[] parts = message.split(":");
        if (parts.length < 9) return null;

        PacketInfo packet = new PacketInfo(parts[2], parts[3], parts[1], parts[0]);
        packet.setSourceMAC(parts[4].isEmpty() ? null : parts[4]);
        packet.setDestMAC(parts[5].isEmpty() ? null : parts[5]);
        packet.setTTL(Integer.parseInt(parts[6]));
        packet.setCurrentHop(parts[7].isEmpty() ? null : parts[7]);
        packet.setPath(parts[8]);
        return packet;
    }

    // Getters and setters
    public String getPacketId() { return packetId; }
    public String getSourceIP() { return sourceIP; }
    public String getDestIP() { return destIP; }
    public String getSourceMAC() { return sourceMAC; }
    public String getDestMAC() { return destMAC; }
    public String getType() { return type; }
    public int getTTL() { return ttl; }
    public String getCurrentHop() { return currentHop; }
    public String getPath() { return path; }

    public void setSourceMAC(String mac) { this.sourceMAC = mac; }
    public void setDestMAC(String mac) { this.destMAC = mac; }
    public void setTTL(int ttl) { this.ttl = ttl; }
    public void setCurrentHop(String hop) { this.currentHop = hop; }
    public void setPath(String path) { this.path = path; }

    @Override
    public String toString() {
        return String.format("Packet[%s]: %s %s → %s (TTL: %d, Path: %s)",
                packetId, type, sourceIP, destIP, ttl, path);
    }
}