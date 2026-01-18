import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import javax.swing.*;

public class MainLauncher {
    private static Runtime rt;
    private static AgentContainer subnet1;
    private static AgentContainer subnet2;

    public static void main(String[] args) {
        // Сначала запускаем JADE
        launchJADE();

        // Затем запускаем графический интерфейс
        SwingUtilities.invokeLater(() -> {
            NetworkGUI gui = new NetworkGUI();
            gui.setVisible(true);

            // Обработчик закрытия окна
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
            // Получаем экземпляр Runtime
            rt = Runtime.instance();

            // Создаем профиль для главного контейнера
            Profile pMain = new ProfileImpl();
            pMain.setParameter(Profile.MAIN_HOST, "localhost");
            pMain.setParameter(Profile.MAIN_PORT, "1099");
            pMain.setParameter(Profile.GUI, "true");

            // Создаем главный контейнер
            subnet1 = rt.createMainContainer(pMain);

            // Создаем профиль для второго контейнера
            Profile p2 = new ProfileImpl();
            p2.setParameter(Profile.CONTAINER_NAME, "subnet2");
            p2.setParameter(Profile.MAIN_HOST, "localhost");
            p2.setParameter(Profile.MAIN_PORT, "1099");
            p2.setParameter(Profile.GUI, "false");

            // Создаем второй контейнер
            subnet2 = rt.createAgentContainer(p2);

            System.out.println("\n=== Создание агентов в Main-Container ===");

            // Создаем агентов в главном контейнере с правильными аргументами
            AgentController pc1 = subnet1.createNewAgent(
                    "PC1", PCAgent.class.getName(), new Object[]{"192.168.1.10", "Main-Container"});

            AgentController switch1 = subnet1.createNewAgent(
                    "Switch1", SwitchAgent.class.getName(), new Object[]{"192.168.1.1", "Main-Container"});

            AgentController router1 = subnet1.createNewAgent(
                    "Router1", RouterAgent.class.getName(), new Object[]{"192.168.1.254", "Router1", "Main-Container"});

            pc1.start();
            switch1.start();
            router1.start();

            System.out.println("\n=== Создание агентов в subnet2 ===");

            // Создаем агентов во втором контейнере
            AgentController pc2 = subnet2.createNewAgent(
                    "PC2", PCAgent.class.getName(), new Object[]{"192.168.2.20", "subnet2"});

            AgentController router2 = subnet2.createNewAgent(
                    "Router2", RouterAgent.class.getName(), new Object[]{"192.168.2.254", "Router2", "subnet2"});

            pc2.start();
            router2.start();

            System.out.println("\n=== Все агенты успешно запущены ===");

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private static void shutdownPlatform() {
        System.out.println("\n=== Завершение работы платформы ===");
        if (rt != null) {
            rt.shutDown();
        }
    }
}