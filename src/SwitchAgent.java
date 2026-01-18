import jade.lang.acl.ACLMessage;
import java.util.HashMap;
import java.util.Map;

public class SwitchAgent extends DeviceAgent {
    private Map<String, String> macTable = new HashMap<>();

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
        String sender = msg.getSender().getLocalName();
        String content = msg.getContent();

        // Обновляем MAC таблицу
        macTable.put(sender, "Port" + macTable.size());

        System.out.println("[" + containerName + "] " + getLocalName() +
                " получил пакет от " + sender);

        // Определяем куда форвардить
        if (content.contains("192.168.1.10") && content.contains("PING")) {
            forwardPacket(msg, "PC1", "Пересылка PING на PC1");
        } else if (content.contains("192.168.1.254") || content.contains("Router1")) {
            forwardPacket(msg, "Router1", "Пересылка на роутер");
        } else if (content.contains("PONG") && content.contains("192.168.1.10")) {
            forwardPacket(msg, "PC1", "Возврат PONG на PC1");
        }
    }
}