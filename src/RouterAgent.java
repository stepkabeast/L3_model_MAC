import jade.lang.acl.ACLMessage;
import java.util.HashMap;
import java.util.Map;

public class RouterAgent extends DeviceAgent {
    private Map<String, String> routingTable = new HashMap<>();
    private String routerType;

    @Override
    protected String getDeviceType() {
        return "Router";
    }

    @Override
    protected void setup() {
        super.setup();

        Object[] args = getArguments();
        if (args != null && args.length > 2) {
            routerType = (String) args[1];
        } else {
            routerType = getLocalName();
        }

        // Настраиваем таблицу маршрутизации в зависимости от типа роутера
        if (routerType.equals("Router1")) {
            ipAddress = "192.168.1.254";
            routingTable.put("192.168.1.0/24", "Switch1");   // Локальная сеть
            routingTable.put("192.168.2.0/24", "Router2");   // Сеть через другой роутер
            routingTable.put("default", "Router2");          // Маршрут по умолчанию
        } else if (routerType.equals("Router2")) {
            ipAddress = "192.168.2.254";
            routingTable.put("192.168.1.0/24", "Router1");   // Обратно в первую сеть
            routingTable.put("192.168.2.0/24", "PC2");       // Локальная сеть
            routingTable.put("default", "Router1");          // Маршрут по умолчанию
        }

        System.out.println("[" + containerName + "] " + getLocalName() +
                " таблица маршрутизации: " + routingTable);
    }

    @Override
    protected void processMessage(ACLMessage msg) {
        String content = msg.getContent();
        System.out.println("[" + containerName + "] " + getLocalName() +
                " обрабатывает: " + content);

        String targetIP = content.split(":")[1];
        String nextHop = findNextHop(targetIP);

        if (nextHop != null) {
            forwardPacket(msg, nextHop, "Маршрутизация к " + targetIP);
        } else {
            System.out.println("[" + containerName + "] " + getLocalName() +
                    " не нашел маршрут для " + targetIP);
        }
    }

    private String findNextHop(String ip) {
        // Проверяем точные совпадения сетей
        if (ip.startsWith("192.168.1.")) {
            return routingTable.get("192.168.1.0/24");
        } else if (ip.startsWith("192.168.2.")) {
            return routingTable.get("192.168.2.0/24");
        }

        // Возвращаем маршрут по умолчанию
        return routingTable.get("default");
    }
}