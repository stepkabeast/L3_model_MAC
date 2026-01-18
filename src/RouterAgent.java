import jade.lang.acl.ACLMessage;
import java.util.*;

public class RouterAgent extends DeviceAgent {
    private Map<String, String> routingTable = new HashMap<>();
    private String network;
    private String gateway;
    private NetworkConfig config;

    @Override
    protected String getDeviceType() {
        return "Router";
    }

    @Override
    protected void setup() {
        super.setup();

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            // args[0] - имя устройства уже обработано в суперклассе
            // args[1] - IP адрес
            // args[2] - сеть
            // args[3] - шлюз (может быть null для роутеров)
            // args[4] - имя контейнера
            // args[5] - конфигурация

            if (args.length > 2) {
                network = (String) args[2];
            }
            if (args.length > 5) {
                config = (NetworkConfig) args[5];
            }
        }

        // Добавляем локальную сеть в таблицу маршрутизации
        if (network != null) {
            routingTable.put(network, "local");
        }

        // Добавляем маршруты из конфигурации
        if (config != null) {
            String mySubnet = containerName.replace("subnet", "");

            for (NetworkConfig.RoutingRule rule : config.getRoutingRules()) {
                // Проверяем, относится ли правило к этому роутеру
                String fromNetwork = rule.fromNetwork;
                String toNetwork = rule.toNetwork;

                // Если маршрут из нашей сети
                if (network != null && isSameNetwork(network, fromNetwork)) {
                    routingTable.put(toNetwork, rule.nextHop);
                    System.out.println(getLocalName() + " добавил маршрут: " +
                            toNetwork + " → " + rule.nextHop);
                }
            }
        }

        System.out.println(getLocalName() + " таблица маршрутизации: " + routingTable);
    }

    @Override
    protected void processMessage(ACLMessage msg) {
        String content = msg.getContent();
        PacketInfo packet = PacketInfo.fromMessageString(content);

        if (packet != null) {
            packet.addHop(getLocalName());

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
                if (nextHop.equals("local")) {
                    // Пакет для локальной сети
                    forwardToLocalDevice(packet, destIP);
                } else {
                    System.out.println("[" + containerName + "] " + getLocalName() +
                            " → " + nextHop + " для " + destIP);

                    packet.addHop(nextHop);

                    ACLMessage forwardMsg = new ACLMessage(ACLMessage.INFORM);
                    forwardMsg.setContent(packet.toMessageString());
                    forwardMsg.addReceiver(getAID(nextHop));
                    send(forwardMsg);
                }
            } else {
                System.out.println("[" + containerName + "] " + getLocalName() +
                        " не нашел маршрут для " + destIP);
            }
        }
    }

    private String findNextHop(String destIP) {
        // Проверяем, принадлежит ли IP локальной сети
        if (network != null && isInNetwork(destIP, network)) {
            return "local";
        }

        // Ищем маршрут в таблице
        for (Map.Entry<String, String> entry : routingTable.entrySet()) {
            String destNetwork = entry.getKey();
            if (isInNetwork(destIP, destNetwork)) {
                return entry.getValue();
            }
        }

        return null;
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

    private boolean isSameNetwork(String network1, String network2) {
        try {
            String[] parts1 = network1.split("/");
            String[] parts2 = network2.split("/");

            if (parts1.length != 2 || parts2.length != 2) {
                return false;
            }

            long ip1 = ipToLong(parts1[0]);
            long ip2 = ipToLong(parts2[0]);
            int mask1 = Integer.parseInt(parts1[1]);
            int mask2 = Integer.parseInt(parts2[1]);

            if (mask1 != mask2) {
                return false;
            }

            long mask = (0xFFFFFFFFL << (32 - mask1)) & 0xFFFFFFFFL;
            return (ip1 & mask) == (ip2 & mask);
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

    private void forwardToLocalDevice(PacketInfo packet, String destIP) {
        // Находим устройство в нашей подсети по IP
        NetworkConfig.DeviceInfo targetDevice = null;
        for (NetworkConfig.DeviceInfo device : config.getAllDevices()) {
            if (device.ip.equals(destIP) && device.container.equals(containerName)) {
                targetDevice = device;
                break;
            }
        }

        if (targetDevice != null) {
            System.out.println("[" + containerName + "] " + getLocalName() +
                    " → " + targetDevice.name + " (локально)");

            packet.addHop(targetDevice.name);

            ACLMessage forwardMsg = new ACLMessage(ACLMessage.INFORM);
            forwardMsg.setContent(packet.toMessageString());
            forwardMsg.addReceiver(getAID(targetDevice.name));
            send(forwardMsg);
        } else {
            System.out.println("[" + containerName + "] " + getLocalName() +
                    " не нашел устройство с IP " + destIP + " в локальной сети");
        }
    }
}