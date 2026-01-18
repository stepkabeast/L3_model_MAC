import jade.lang.acl.ACLMessage;
import java.util.HashMap;
import java.util.Map;

public class SwitchAgent extends DeviceAgent {
    private Map<String, String> macTable = new HashMap<>();
    private String network;
    private NetworkConfig config;

    @Override
    protected String getDeviceType() {
        return "Switch";
    }

    @Override
    protected void setup() {
        super.setup();

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            // args[0] - имя устройства
            // args[1] - IP адрес
            // args[2] - сеть
            // args[4] - имя контейнера
            // args[5] - конфигурация

            if (args.length > 2) {
                network = (String) args[2];
            }
            if (args.length > 5) {
                config = (NetworkConfig) args[5];
            }
        }

        System.out.println(getLocalName() + " L2 Switch запущен, сеть: " + network);
    }

    @Override
    protected void processMessage(ACLMessage msg) {
        String content = msg.getContent();
        PacketInfo packet = PacketInfo.fromMessageString(content);

        if (packet != null) {
            packet.addHop(getLocalName());

            System.out.println("[" + containerName + "] " + getLocalName() +
                    " обрабатывает пакет: " + packet);

            // Обновляем MAC таблицу
            String sender = msg.getSender().getLocalName();
            macTable.put(sender, "Port" + (macTable.size() + 1));

            // Определяем получателя
            String destIP = packet.getDestIP();

            // Проверяем, в нашей ли сети получатель
            if (isInNetwork(destIP, network)) {
                // Ищем устройство в нашей сети по IP
                NetworkConfig.DeviceInfo targetDevice = null;
                for (NetworkConfig.DeviceInfo device : config.getAllDevices()) {
                    if (device.ip.equals(destIP) && device.container.equals(containerName)) {
                        targetDevice = device;
                        break;
                    }
                }

                if (targetDevice != null) {
                    forwardPacket(msg, packet, targetDevice.name);
                } else {
                    // Если не нашли устройство, отправляем на роутер (шлюз по умолчанию)
                    String routerName = "Router" + containerName.replace("subnet", "");
                    forwardPacket(msg, packet, routerName);
                }
            } else {
                // Пакет не для нашей сети, отправляем на роутер
                String routerName = "Router" + containerName.replace("subnet", "");
                forwardPacket(msg, packet, routerName);
            }
        }
    }

    private boolean isInNetwork(String ip, String network) {
        try {
            String[] networkParts = network.split("/");
            String networkIp = networkParts[0];
            int prefix = Integer.parseInt(networkParts[1]);

            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkIp);
            long mask = (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;

            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= Integer.parseInt(octets[i]);
        }
        return result;
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