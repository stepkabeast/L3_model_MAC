import jade.lang.acl.ACLMessage;
import java.util.HashMap;
import java.util.Map;

public class RouterAgent extends DeviceAgent {
    private Map<String, String> routingTable = new HashMap<>();

    @Override
    protected String getDeviceType() {
        return "Router";
    }

    @Override
    protected void setup() {
        super.setup();

        if (getLocalName().equals("Router1")) {
            ipAddress = "192.168.1.254";
            routingTable.put("192.168.1.0/24", "Switch1");
            routingTable.put("192.168.2.0/24", "Router2");
        } else if (getLocalName().equals("Router2")) {
            ipAddress = "192.168.2.254";
            routingTable.put("192.168.1.0/24", "Router1");
            routingTable.put("192.168.2.0/24", "PC2");
        }

        System.out.println(getLocalName() + " routing table: " + routingTable);
    }

    @Override
    protected void processMessage(ACLMessage msg) {
        String content = msg.getContent();
        System.out.println(getLocalName() + " processing: " + content);

        String targetIP = content.split(":")[1];
        String nextHop = findNextHop(targetIP);

        if (nextHop != null) {
            forwardPacket(msg, nextHop);
        }
    }

    private String findNextHop(String ip) {
        for (Map.Entry<String, String> entry : routingTable.entrySet()) {
            String network = entry.getKey().split("/")[0];
            String mask = entry.getKey().split("/")[1];

            if (isInNetwork(ip, network, Integer.parseInt(mask))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isInNetwork(String ip, String network, int mask) {
        // Упрощенная проверка сети
        return ip.startsWith(network.substring(0, network.lastIndexOf('.')));
    }
}