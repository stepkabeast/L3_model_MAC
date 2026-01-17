import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public abstract class DeviceAgent extends Agent {
    protected String ipAddress;
    protected String macAddress;
    protected String deviceType;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            ipAddress = (String) args[0];
        }

        macAddress = generateMAC();
        deviceType = getDeviceType();

        System.out.println(getLocalName() + " (" + deviceType +
                ") IP: " + ipAddress + " MAC: " + macAddress +
                " started in container: " + getContainerController().getName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processMessage(msg);
                } else {
                    block();
                }
            }
        });
    }

    protected abstract String getDeviceType();
    protected abstract void processMessage(ACLMessage msg);

    private String generateMAC() {
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                (int)(Math.random() * 255),
                (int)(Math.random() * 255),
                (int)(Math.random() * 255),
                (int)(Math.random() * 255),
                (int)(Math.random() * 255),
                (int)(Math.random() * 255));
    }

    protected void forwardPacket(ACLMessage msg, String nextHop) {
        System.out.println(getLocalName() + " forwarding packet to " + nextHop);
        ACLMessage forwardMsg = new ACLMessage(ACLMessage.INFORM);
        forwardMsg.setContent(msg.getContent());
        forwardMsg.addReceiver(getAID(nextHop));
        send(forwardMsg);
    }
}