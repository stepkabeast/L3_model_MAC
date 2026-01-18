import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import javax.swing.*;
import java.util.*;
import jade.util.Logger;

public class MainLauncher {
    private static Runtime rt;
    private static List<AgentContainer> containers = new ArrayList<>();
    private static NetworkConfig config;
    private static final Logger logger = Logger.getMyLogger(MainLauncher.class.getName());

    public static void main(String[] args) {
        config = NetworkConfig.load("config/network-config.json");
        config.initDeviceCache();

        launchJADE();

        SwingUtilities.invokeLater(() -> {
            NetworkGUI gui = new NetworkGUI(config);
            gui.setVisible(true);

            gui.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    shutdownPlatform();
                    System.exit(0);
                }
            });
        });
    }

    private static void launchJADE() {
        try {
            rt = Runtime.instance();

            for (int subnet = 1; subnet <= config.subnets; subnet++) {
                String containerName = "subnet" + subnet;
                AgentContainer container;

                if (subnet == 1) {
                    Profile pMain = new ProfileImpl();
                    pMain.setParameter(Profile.MAIN_HOST, "localhost");
                    pMain.setParameter(Profile.MAIN_PORT, "1099");
                    pMain.setParameter(Profile.GUI, "true");
                    pMain.setParameter(Profile.CONTAINER_NAME, containerName);
                    container = rt.createMainContainer(pMain);
                } else {
                    Profile p = new ProfileImpl();
                    p.setParameter(Profile.CONTAINER_NAME, containerName);
                    p.setParameter(Profile.MAIN_HOST, "localhost");
                    p.setParameter(Profile.MAIN_PORT, "1099");
                    p.setParameter(Profile.GUI, "false");
                    container = rt.createAgentContainer(p);
                }

                containers.add(container);
                createAgentsInContainer(container, subnet);
            }

            logger.log(Logger.INFO, "Agents started");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createAgentsInContainer(AgentContainer container, int subnet) {
        String containerName = "subnet" + subnet;
        String subnetStr = String.valueOf(subnet);

        // Получаем все шаблоны устройств
        for (NetworkConfig.DeviceTemplate template : config.devices) {
            for (int i = 0; i < template.count; i++) {
                String name = template.type + subnet;
                if (template.count > 1) {
                    name += "_" + (i + 1);
                }

                String ip = template.ipBase.replace("{subnet}", subnetStr);
                String network = template.network.replace("{subnet}", subnetStr);
                String gateway = template.gateway != null ?
                        template.gateway.replace("{subnet}", subnetStr) : null;

                Object[] args = {
                        name,       // 0 - имя устройства
                        ip,         // 1 - IP адрес
                        network,    // 2 - сеть
                        gateway,    // 3 - шлюз (если есть)
                        containerName, // 4 - имя контейнера
                        config      // 5 - конфигурация
                };

                try {
                    Class<?> agentClass = getAgentClassForType(template.type);

                    AgentController ac = container.createNewAgent(
                            name,
                            agentClass.getName(),
                            args
                    );
                    ac.start();

                    logger.log(Logger.INFO, "Agent " + name + " is ready");
                    System.out.println("Агент " + name + " успешно создан и запущен");

                } catch (Exception e) {
                    System.err.println("Ошибка при создании агента " + name + " в " + containerName);
                    e.printStackTrace();
                }
            }
        }
    }

    private static Class<?> getAgentClassForType(String type) {
        return switch (type) {
            case "PC" -> PCAgent.class;
            case "Switch" -> SwitchAgent.class;
            case "Router" -> RouterAgent.class;
            default -> throw new IllegalArgumentException("Неизвестный тип устройства: " + type);
        };
    }

    private static void shutdownPlatform() {
        logger.log(Logger.INFO, "Shutting down platform...");
        if (rt != null) {
            rt.shutDown();
        }
    }

    public static NetworkConfig getConfig() {
        return config;
    }
}