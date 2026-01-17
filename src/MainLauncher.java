import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import javax.swing.*;

public class MainLauncher {
    public static void main(String[] args) {
        // Запускаем графический интерфейс
        SwingUtilities.invokeLater(() -> {
            NetworkGUI gui = new NetworkGUI();
            gui.setVisible(true);
        });

        // Запуск JADE контейнеров
        launchJADE();
    }

    private static void launchJADE() {
        try {
            Runtime rt = Runtime.instance();

            // Главный контейнер
            Profile pMain = new ProfileImpl();
            pMain.setParameter(Profile.MAIN_HOST, "localhost");
            pMain.setParameter(Profile.MAIN_PORT, "1099");
            pMain.setParameter(Profile.GUI, "true");
            AgentContainer mainContainer = rt.createMainContainer(pMain);

            // Второй контейнер
            ProfileImpl p2 = new ProfileImpl();
            p2.setParameter(Profile.CONTAINER_NAME, "Container2");
            p2.setParameter(Profile.MAIN_HOST, "localhost");
            p2.setParameter(Profile.MAIN_PORT, "1099");
            AgentContainer container2 = rt.createAgentContainer(p2);

            // Создаем агентов в первом контейнере
            AgentController pc1 = mainContainer.createNewAgent(
                    "PC1", PCAgent.class.getName(), null);
            AgentController switch1 = mainContainer.createNewAgent(
                    "Switch1", SwitchAgent.class.getName(), null);
            AgentController router1 = mainContainer.createNewAgent(
                    "Router1", RouterAgent.class.getName(), null);

            // Создаем агентов во втором контейнере
            AgentController pc2 = container2.createNewAgent(
                    "PC2", PCAgent.class.getName(), null);
            AgentController router2 = container2.createNewAgent(
                    "Router2", RouterAgent.class.getName(), null);

            pc1.start();
            switch1.start();
            router1.start();
            pc2.start();
            router2.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}