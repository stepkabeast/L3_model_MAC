import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.ControllerException;

public abstract class DeviceAgent extends Agent {
    protected String deviceName;
    protected String ipAddress;
    protected String macAddress;
    protected String deviceType;
    protected String containerName;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            deviceName = (String) args[0];
            if (args.length > 1) {
                ipAddress = (String) args[1];
            }
            if (args.length > 4) {
                containerName = (String) args[4];
            }
        }

        if (containerName == null || containerName.isEmpty()) {
            try {
                containerName = getContainerController().getContainerName();
            } catch (ControllerException e) {
                containerName = "Unknown";
            }
        }

        // Генерируем MAC на основе имени устройства
        macAddress = generateMAC(deviceName);
        deviceType = getDeviceType();

        System.out.println("Запуск агента: " + deviceName +
                " (" + deviceType + ")" +
                " IP: " + ipAddress +
                " MAC: " + macAddress +
                " Контейнер: " + containerName);

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

    private String generateMAC(String name) {
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
        System.out.println("[" + containerName + "] " + deviceName +
                " → " + nextHop + ": " + description);
        ACLMessage forwardMsg = new ACLMessage(ACLMessage.INFORM);
        forwardMsg.setContent(msg.getContent());
        forwardMsg.addReceiver(getAID(nextHop));
        send(forwardMsg);
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getContainerName() {
        return containerName;
    }
}