import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class PCAgent extends DeviceAgent {
    private String defaultGateway = "Router1";

    @Override
    protected String getDeviceType() {
        return "PC";
    }

    @Override
    protected void setup() {
        super.setup();

        if (getLocalName().equals("PC1")) {
            ipAddress = "192.168.1.10";
        } else if (getLocalName().equals("PC2")) {
            ipAddress = "192.168.2.20";
            defaultGateway = "Router2";
        }

        System.out.println(getLocalName() + " default gateway: " + defaultGateway);
    }

    @Override
    protected void processMessage(ACLMessage msg) {
        if (msg.getContent().startsWith("PING:")) {
            String[] parts = msg.getContent().split(":");
            String targetIP = parts[1];

            System.out.println(getLocalName() + " received PING for " + targetIP);

            // Отправляем ответ
            if (ipAddress.equals(targetIP)) {
                ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                reply.setContent("PONG:" + ipAddress + ":" + parts[2]);
                reply.addReceiver(msg.getSender());
                send(reply);
                System.out.println(getLocalName() + " sending PONG");
            }
        } else if (msg.getContent().startsWith("PONG:")) {
            System.out.println(getLocalName() + " received PONG from " +
                    msg.getContent().split(":")[1]);
        }
    }

    public void sendPing(String targetIP) {
        System.out.println(getLocalName() + " initiating PING to " + targetIP);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("PING:" + targetIP + ":" + this.ipAddress);

        // Определяем следующий хоп
        if (targetIP.startsWith("192.168.1.") && getLocalName().equals("PC1")) {
            msg.addReceiver(new AID("Switch1", AID.ISLOCALNAME));
        } else {
            msg.addReceiver(new AID(defaultGateway, AID.ISLOCALNAME));
        }

        send(msg);
    }
}