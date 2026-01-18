import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class PCAgent extends DeviceAgent {
    private String defaultGateway;
    private String subnet;

    @Override
    protected String getDeviceType() {
        return "PC";
    }

    @Override
    protected void setup() {
        super.setup();

        // Определяем настройки по имени агента
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

        if (content.startsWith("PING:")) {
            String[] parts = content.split(":");
            String targetIP = parts[1];
            String sourceIP = parts[2];

            System.out.println("[" + containerName + "] " + getLocalName() +
                    " получил PING от " + sourceIP + " для " + targetIP);

            // Если пинг адресован нам
            if (ipAddress.equals(targetIP)) {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " отправляет PONG");

                ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                reply.setContent("PONG:" + ipAddress + ":" + sourceIP);
                reply.addReceiver(msg.getSender());
                send(reply);
            }
        } else if (content.startsWith("PONG:")) {
            String[] parts = content.split(":");
            String responderIP = parts[1];
            String originalSource = parts[2];

            if (ipAddress.equals(originalSource)) {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " получил PONG от " + responderIP + " ✓ Пинг успешен!");
            }
        }
    }

    // Метод для отправки ping (может вызываться из GUI)
    public void sendPing(String targetIP) {
        System.out.println("\n=== Инициация PING ===");
        System.out.println("[" + containerName + "] " + getLocalName() +
                " запускает PING до " + targetIP);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("PING:" + targetIP + ":" + this.ipAddress);

        // Определяем маршрут в зависимости от подсети
        if (targetIP.startsWith("192.168.1.") && getLocalName().equals("PC1")) {
            msg.addReceiver(new AID("Switch1", AID.ISLOCALNAME));
            System.out.println("[" + containerName + "] " + getLocalName() +
                    " → Switch1 (через свитч)");
        } else if (targetIP.startsWith("192.168.2.") && getLocalName().equals("PC2")) {
            msg.addReceiver(new AID("Router2", AID.ISLOCALNAME));
            System.out.println("[" + containerName + "] " + getLocalName() +
                    " → Router2 (шлюз по умолчанию)");
        } else {
            // Отправляем на шлюз по умолчанию
            msg.addReceiver(new AID(defaultGateway, AID.ISLOCALNAME));
            System.out.println("[" + containerName + "] " + getLocalName() +
                    " → " + defaultGateway + " (шлюз по умолчанию)");
        }

        send(msg);
    }
}