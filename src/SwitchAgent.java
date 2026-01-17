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
        System.out.println(getLocalName() + " L2 Switch started");
    }

    @Override
    protected void processMessage(ACLMessage msg) {
        String senderName = msg.getSender().getLocalName();
        String content = msg.getContent();

        // Обновляем MAC таблицу
        macTable.put(senderName, senderName);

        System.out.println(getLocalName() + " received packet from " + senderName);

        // Простая логика коммутации
        if (content.contains("192.168.1.")) {
            forwardPacket(msg, "Router1");
        } else if (content.contains("PONG")) {
            forwardPacket(msg, "PC1");
        }
    }
}