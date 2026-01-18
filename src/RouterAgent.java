import jade.lang.acl.ACLMessage;
import java.util.HashMap;
import java.util.Map;

public class RouterAgent extends DeviceAgent {
    private Map<String, String> routingTable = new HashMap<>();
    private String routerType;

    @Override
    protected String getDeviceType() {
        return "Router";
    }

    @Override
    protected void setup() {
        super.setup();

        Object[] args = getArguments();
        routerType = (args != null && args.length > 1) ? (String) args[1] : getLocalName();

        if (routerType.equals("Router1")) {
            ipAddress = "192.168.1.254";
            routingTable.put("192.168.1.0/24", "Switch1");
            routingTable.put("192.168.2.0/24", "Router2");
            routingTable.put("default", "Router2");
        } else if (routerType.equals("Router2")) {
            ipAddress = "192.168.2.254";
            routingTable.put("192.168.1.0/24", "Router1");
            routingTable.put("192.168.2.0/24", "PC2");
            routingTable.put("default", "Router1");
        }

        System.out.println("[" + containerName + "] " + getLocalName() +
                " таблица маршрутизации: " + routingTable);
    }

    @Override
    protected void processMessage(ACLMessage msg) {
        String content = msg.getContent();
        PacketInfo packet = PacketInfo.fromMessageString(content);

        if (packet != null) {
            packet.addHop(getLocalName());
            packet.setCurrentHop(getLocalName());

            System.out.println("[" + containerName + "] " + getLocalName() +
                    " обрабатывает: " + packet);

            // Уменьшаем TTL
            packet.setTTL(packet.getTTL() - 1);
            if (packet.getTTL() <= 0) {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " отбрасывает пакет: TTL истек");
                return;
            }

            String destIP = packet.getDestIP();
            String nextHop = findNextHop(destIP);

            if (nextHop != null) {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " → " + nextHop + " для " + destIP);

                packet.addHop(nextHop);

                ACLMessage forwardMsg = new ACLMessage(ACLMessage.INFORM);
                forwardMsg.setContent(packet.toMessageString());
                forwardMsg.addReceiver(getAID(nextHop));
                send(forwardMsg);
            } else {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " не нашел маршрут для " + destIP);
            }
        }
    }

    private String findNextHop(String ip) {
        if (ip.startsWith("192.168.1.")) {
            return routingTable.get("192.168.1.0/24");
        } else if (ip.startsWith("192.168.2.")) {
            return routingTable.get("192.168.2.0/24");
        }
        return routingTable.get("default");
    }
}