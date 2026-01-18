import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.UUID;

public class PCAgent extends DeviceAgent {
    private String gateway;
    private String network;
    private NetworkConfig config;

    @Override
    protected String getDeviceType() {
        return "PC";
    }

    @Override
    protected void setup() {
        super.setup();

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            // args[0] - имя устройства
            // args[1] - IP адрес
            // args[2] - сеть
            // args[3] - шлюз
            // args[4] - имя контейнера
            // args[5] - конфигурация

            if (args.length > 2) {
                network = (String) args[2];
            }
            if (args.length > 3) {
                gateway = (String) args[3];
            }
            if (args.length > 5) {
                config = (NetworkConfig) args[5];
            }
        }

        System.out.println(getLocalName() + " сеть: " + network + ", шлюз: " + gateway);
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
                }
            } else if (packet.getType().equals("PONG")) {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " получил PONG пакет: " + packet);

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

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent(pingPacket.toMessageString());

        // Определяем, в той же ли сети получатель
        boolean sameNetwork = isInSameNetwork(targetIP);

        if (sameNetwork) {
            // Отправляем напрямую на свитч
            msg.addReceiver(new AID("Switch" + containerName.replace("subnet", ""),
                    AID.ISLOCALNAME));
        } else {
            // Отправляем на шлюз
            if (gateway != null) {
                msg.addReceiver(new AID(gateway, AID.ISLOCALNAME));
            } else {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " не имеет шлюза для отправки пакета");
                return;
            }
        }

        send(msg);
    }

    private boolean isInSameNetwork(String targetIP) {
        if (network == null || targetIP == null) {
            return false;
        }

        try {
            String[] networkParts = network.split("/");
            String networkIp = networkParts[0];
            int prefix = Integer.parseInt(networkParts[1]);

            long ipLong = ipToLong(targetIP);
            long networkLong = ipToLong(networkIp);
            long mask = (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;

            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= Integer.parseInt(octets[i]);
        }
        return result;
    }
}