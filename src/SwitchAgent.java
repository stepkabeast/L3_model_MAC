import jade.lang.acl.ACLMessage;
import java.util.HashMap;
import java.util.Map;

public class SwitchAgent extends DeviceAgent {
    private Map<String, String> macTable = new HashMap<>(); // MAC -> Port/AID

    @Override
    protected String getDeviceType() {
        return "Switch";
    }

    @Override
    protected void setup() {
        super.setup();
        ipAddress = "192.168.1.1";
        System.out.println(getLocalName() + " L2 Switch запущен");
    }

    @Override
    protected void processMessage(ACLMessage msg) {
        String content = msg.getContent();
        PacketInfo packet = PacketInfo.fromMessageString(content);

        if (packet != null) {
            packet.addHop(getLocalName());
            packet.setCurrentHop(getLocalName());

            System.out.println("[" + containerName + "] " + getLocalName() +
                    " обрабатывает пакет: " + packet);

            // Обновляем MAC таблицу
            String sender = msg.getSender().getLocalName();
            macTable.put(sender, "Port" + (macTable.size() + 1));

            // Определяем куда форвардить на основе IP
            String destIP = packet.getDestIP();

            if (destIP.startsWith("192.168.1.")) {
                if (destIP.equals("192.168.1.10")) {
                    forwardPacket(msg, packet, "PC1");
                } else if (destIP.equals("192.168.1.254")) {
                    forwardPacket(msg, packet, "Router1");
                }
            } else if (packet.getType().equals("PONG")) {
                // Для PONG пакетов возвращаем на PC1
                forwardPacket(msg, packet, "PC1");
            }
        }
    }

    private void forwardPacket(ACLMessage msg, PacketInfo packet, String nextHop) {
        packet.addHop(nextHop);

        System.out.println("[" + containerName + "] " + getLocalName() +
                " → " + nextHop + ": " + packet.getType() + " пакет");

        ACLMessage forwardMsg = new ACLMessage(ACLMessage.INFORM);
        forwardMsg.setContent(packet.toMessageString());
        forwardMsg.addReceiver(getAID(nextHop));
        send(forwardMsg);
    }
}