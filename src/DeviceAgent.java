import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.ControllerException;

public abstract class DeviceAgent extends Agent {
    protected String ipAddress;
    protected String macAddress;
    protected String deviceType;
    protected String containerName;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            ipAddress = (String) args[0];
            if (args.length > 1) {
                containerName = (String) args[1];
            }
        }

        macAddress = generateMAC();
        deviceType = getDeviceType();

        // Если имя контейнера не передали, получаем его
        if (containerName == null || containerName.isEmpty()) {
            try {
                containerName = getContainerController().getContainerName();
            } catch (ControllerException e) {
                throw new RuntimeException(e);
            }
            if (containerName == null) {
                containerName = "Unknown";
            }
        }

        System.out.println(getLocalName() + " (" + deviceType +
                ") IP: " + ipAddress +
                " MAC: " + macAddress +
                " в контейнере: " + containerName);

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
        // Генерируем детерминированный MAC на основе имени агента
        String name = getLocalName();
        int hash = Math.abs(name.hashCode());

        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                (hash >> 16) & 0xFF,
                (hash >> 8) & 0xFF,
                hash & 0xFF,
                (hash >> 24) & 0xFF,
                (hash >> 16) & 0xFF,
                (hash >> 8) & 0xFF);
    }

    protected void forwardPacket(ACLMessage msg, String nextHop, String description) {
        System.out.println("[" + containerName + "] " + getLocalName() +
                " → " + nextHop + ": " + description);
        ACLMessage forwardMsg = new ACLMessage(ACLMessage.INFORM);
        forwardMsg.setContent(msg.getContent());
        forwardMsg.addReceiver(getAID(nextHop));
        send(forwardMsg);
    }
}