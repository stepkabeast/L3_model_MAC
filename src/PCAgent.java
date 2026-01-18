import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.UUID;

public class PCAgent extends DeviceAgent {
    private String defaultGateway;
    private String subnet;
    private String packetLog = "";

    @Override
    protected String getDeviceType() {
        return "PC";
    }

    @Override
    protected void setup() {
        super.setup();

        if (getLocalName().equals("PC1")) {
            ipAddress = "192.168.1.10";
            defaultGateway = "Router1";
            subnet = "192.168.1.0/24";
            System.out.println(getLocalName() + " default gateway: " + defaultGateway);
        } else if (getLocalName().equals("PC2")) {
            ipAddress = "192.168.2.20";
            defaultGateway = "Router2";
            subnet = "192.168.2.0/24";
            System.out.println(getLocalName() + " default gateway: " + defaultGateway);
        }
    }

    @Override
    protected void processMessage(ACLMessage msg) {
        String content = msg.getContent();
        PacketInfo packet = PacketInfo.fromMessageString(content);

        if (packet != null) {
            packet.addHop(getLocalName());

            if (packet.getType().equals("PING")) {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " получил PING пакет: " + packet);

                logPacketInfo(packet);

                // Если пинг адресован нам
                if (ipAddress.equals(packet.getDestIP())) {
                    System.out.println("[" + containerName + "] " + getLocalName() +
                            " отправляет PONG");

                    // Создаем ответный PONG пакет
                    PacketInfo pongPacket = new PacketInfo(
                            ipAddress, packet.getSourceIP(), "PONG",
                            packet.getPacketId() + "-response"
                    );
                    pongPacket.setSourceMAC(macAddress);
                    pongPacket.setDestMAC(packet.getSourceMAC());
                    pongPacket.addHop(getLocalName());

                    ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                    reply.setContent(pongPacket.toMessageString());
                    reply.addReceiver(msg.getSender());
                    send(reply);

                    logPacketInfo(pongPacket);
                }
            } else if (packet.getType().equals("PONG")) {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " получил PONG пакет: " + packet);

                logPacketInfo(packet);

                if (ipAddress.equals(packet.getDestIP())) {
                    System.out.println("[" + containerName + "] ✓ " + getLocalName() +
                            " Пинг успешен! Путь: " + packet.getPath());
                }
            }
        }
    }

    public void sendPing(String targetIP) {
        String packetId = "PKT-" + UUID.randomUUID().toString().substring(0, 8);

        PacketInfo pingPacket = new PacketInfo(ipAddress, targetIP, "PING", packetId);
        pingPacket.setSourceMAC(macAddress);
        pingPacket.addHop(getLocalName());

        System.out.println("\n=== Инициация PING ===");
        System.out.println("[" + containerName + "] " + getLocalName() +
                " запускает PING до " + targetIP);
        System.out.println("ID пакета: " + packetId);
        System.out.println("Пакет: " + pingPacket);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent(pingPacket.toMessageString());

        // Определяем маршрут
        if (targetIP.startsWith("192.168.1.") && getLocalName().equals("PC1")) {
            msg.addReceiver(new AID("Switch1", AID.ISLOCALNAME));
            System.out.println("[" + containerName + "] " + getLocalName() +
                    " → Switch1");
        } else if (targetIP.startsWith("192.168.2.") && getLocalName().equals("PC2")) {
            msg.addReceiver(new AID("Router2", AID.ISLOCALNAME));
            System.out.println("[" + containerName + "] " + getLocalName() +
                    " → Router2");
        } else {
            msg.addReceiver(new AID(defaultGateway, AID.ISLOCALNAME));
            System.out.println("[" + containerName + "] " + getLocalName() +
                    " → " + defaultGateway);
        }

        send(msg);
        logPacketInfo(pingPacket);
    }

    private void logPacketInfo(PacketInfo packet) {
        packetLog += packet + "\n";
        if (packetLog.length() > 1000) {
            packetLog = packetLog.substring(packetLog.length() - 500);
        }
    }

    public String getPacketLog() {
        return packetLog;
    }
}