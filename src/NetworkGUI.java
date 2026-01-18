import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class NetworkGUI extends JFrame {
    private NetworkPanel networkPanel;
    private JButton pingButton;
    private JButton startAgentsButton;
    private JTextArea logArea;
    private JComboBox<String> sourceDevice;
    private JComboBox<String> targetDevice;
    private Map<String, DeviceShape> devices;

    public NetworkGUI() {
        setTitle("L3 Network Simulator - JADE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLayout(new BorderLayout());

        devices = new HashMap<>();

        // Панель управления
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        sourceDevice = new JComboBox<>(new String[]{"PC1", "PC2"});
        targetDevice = new JComboBox<>(new String[]{"PC1", "PC2"});

        pingButton = new JButton("Start Ping");
        pingButton.addActionListener(e -> startPing());
        pingButton.setEnabled(true);

        startAgentsButton = new JButton("Перезапустить агентов");
        startAgentsButton.addActionListener(e -> restartAgents());

        controlPanel.add(new JLabel("Откуда:"));
        controlPanel.add(sourceDevice);
        controlPanel.add(new JLabel("Куда:"));
        controlPanel.add(targetDevice);
        controlPanel.add(pingButton);
        controlPanel.add(startAgentsButton);

        // Панель логов
        JPanel logPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.append("=== Лог системы L3 сети ===\n");
        logArea.append("Main-Container: PC1, Switch1, Router1\n");
        logArea.append("Container2: PC2, Router2\n");
        logArea.append("----------------------------\n");

        JScrollPane scrollPane = new JScrollPane(logArea);
        logPanel.add(new JLabel("Лог событий:"), BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        // Панель сети
        networkPanel = new NetworkPanel();

        // Добавляем все панели
        add(controlPanel, BorderLayout.NORTH);
        add(networkPanel, BorderLayout.CENTER);
        add(logPanel, BorderLayout.SOUTH);

        initializeDevices();
    }

    private void initializeDevices() {
        // Создаем устройства на схеме
        devices.put("PC1", new DeviceShape(100, 200, "PC1\n192.168.1.10", Color.BLUE, "Main-Container"));
        devices.put("Switch1", new DeviceShape(300, 200, "Switch1\n192.168.1.1", Color.GREEN, "Main-Container"));
        devices.put("Router1", new DeviceShape(500, 200, "Router1\n192.168.1.254", Color.ORANGE, "Main-Container"));
        devices.put("Router2", new DeviceShape(500, 400, "Router2\n192.168.2.254", Color.ORANGE, "Container2"));
        devices.put("PC2", new DeviceShape(700, 400, "PC2\n192.168.2.20", Color.BLUE, "Container2"));

        networkPanel.setDevices(devices);
    }

    private void startPing() {
        String source = (String) sourceDevice.getSelectedItem();
        String target = (String) targetDevice.getSelectedItem();

        if (source.equals(target)) {
            logArea.append("Ошибка: Выберите разные устройства!\n");
            JOptionPane.showMessageDialog(this, "Select different devices!");
            return;
        }

        logArea.append("\n=== Запуск PING ===\n");
        logArea.append(source + " → " + target + "\n");

        // Определяем маршрут для анимации
        List<String> route = findRoute(source, target);
        networkPanel.animatePacket(route);

        logArea.append("Маршрут: " + String.join(" → ", route) + "\n");
        logArea.append("См. вывод в консоли для деталей JADE агентов\n");

        // Прокручиваем лог вниз
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void restartAgents() {
        logArea.append("\n=== Перезапуск агентов ===\n");
        logArea.append("Функционал перезапуска будет добавлен\n");
        logArea.append("Для перезапуска закройте и запустите программу\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private List<String> findRoute(String source, String target) {
        List<String> route = new ArrayList<>();

        if (source.equals("PC1") && target.equals("PC2")) {
            route.add("PC1");
            route.add("Switch1");
            route.add("Router1");
            route.add("Router2");
            route.add("PC2");
        } else if (source.equals("PC2") && target.equals("PC1")) {
            route.add("PC2");
            route.add("Router2");
            route.add("Router1");
            route.add("Switch1");
            route.add("PC1");
        } else if (source.equals("PC1") && target.equals("Switch1")) {
            route.add("PC1");
            route.add("Switch1");
        } else if (source.equals("PC1") && target.equals("Router1")) {
            route.add("PC1");
            route.add("Switch1");
            route.add("Router1");
        } else if (source.equals("PC2") && target.equals("Router2")) {
            route.add("PC2");
            route.add("Router2");
        }

        return route;
    }

    // Вложенный класс для графического представления устройства
    static class DeviceShape {
        int x, y;
        String name;
        Color color;
        String container;

        public DeviceShape(int x, int y, String name, Color color, String container) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.color = color;
            this.container = container;
        }
    }

    // Вложенный класс для отрисовки сетевой схемы
    static class NetworkPanel extends JPanel {
        private Map<String, DeviceShape> devices;
        private List<String> currentRoute;
        private int animationStep = 0;
        private Timer animationTimer;

        public NetworkPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(900, 600));
        }

        public void setDevices(Map<String, DeviceShape> devices) {
            this.devices = devices;
            repaint();
        }

        public void animatePacket(List<String> route) {
            this.currentRoute = route;
            this.animationStep = 0;

            if (animationTimer != null) {
                animationTimer.stop();
            }

            animationTimer = new Timer(500, e -> {
                if (animationStep < currentRoute.size() - 1) {
                    animationStep++;
                    repaint();
                } else {
                    ((Timer)e.getSource()).stop();
                    // Возвращаем к исходному состоянию через 1 секунду
                    new Timer(1000, ev -> {
                        animationStep = 0;
                        repaint();
                        ((Timer)ev.getSource()).stop();
                    }).start();
                }
            });
            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            if (devices == null) return;

            // Рисуем заголовок
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Схема L3 сети (JADE Multi-Agent System)", 300, 30);

            // Рисуем соединения
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(2));

            // PC1 - Switch1
            drawConnection(g2d, "PC1", "Switch1");
            // Switch1 - Router1
            drawConnection(g2d, "Switch1", "Router1");
            // Router1 - Router2
            drawConnection(g2d, "Router1", "Router2");
            // Router2 - PC2
            drawConnection(g2d, "Router2", "PC2");

            // Рисуем устройства
            for (DeviceShape device : devices.values()) {
                drawDevice(g2d, device);
            }

            // Анимация пакета
            if (currentRoute != null && animationStep > 0) {
                drawPacket(g2d, currentRoute.get(animationStep - 1),
                        currentRoute.get(animationStep));
            }

            // Легенда
            drawLegend(g2d);
        }

        private void drawConnection(Graphics2D g2d, String dev1, String dev2) {
            DeviceShape d1 = devices.get(dev1);
            DeviceShape d2 = devices.get(dev2);
            if (d1 != null && d2 != null) {
                g2d.drawLine(d1.x + 40, d1.y + 30, d2.x, d2.y + 30);

                // Рисуем стрелочку
                int midX = (d1.x + 40 + d2.x) / 2;
                int midY = (d1.y + 30 + d2.y + 30) / 2;

                // Стрелочка для двусторонней связи
                g2d.setColor(Color.DARK_GRAY);
                Polygon arrow = new Polygon();
                arrow.addPoint(midX, midY);
                arrow.addPoint(midX - 5, midY - 3);
                arrow.addPoint(midX - 5, midY + 3);
                g2d.fill(arrow);
            }
        }

        private void drawDevice(Graphics2D g2d, DeviceShape device) {
            // Рисуем основное тело устройства
            g2d.setColor(device.color);
            g2d.fillRoundRect(device.x, device.y, 80, 60, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(device.x, device.y, 80, 60, 10, 10);

            // Рисуем текст (имя и IP)
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 11));

            String[] lines = device.name.split("\n");
            for (int i = 0; i < lines.length; i++) {
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(lines[i]);
                g2d.drawString(lines[i], device.x + 40 - textWidth/2, device.y + 20 + i*15);
            }

            // Рисуем контейнер
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.setColor(Color.YELLOW);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(device.container);
            g2d.drawString(device.container, device.x + 40 - textWidth/2, device.y + 50);
        }

        private void drawPacket(Graphics2D g2d, String from, String to) {
            DeviceShape d1 = devices.get(from);
            DeviceShape d2 = devices.get(to);

            if (d1 != null && d2 != null) {
                int x1 = d1.x + 40;
                int y1 = d1.y + 30;
                int x2 = d2.x;
                int y2 = d2.y + 30;

                // Рисуем движущийся пакет
                g2d.setColor(Color.RED);
                g2d.fillOval((x1 + x2) / 2 - 10, (y1 + y2) / 2 - 10, 20, 20);
                g2d.setColor(Color.BLACK);
                g2d.drawOval((x1 + x2) / 2 - 10, (y1 + y2) / 2 - 10, 20, 20);
                g2d.setFont(new Font("Arial", Font.BOLD, 9));
                g2d.setColor(Color.WHITE);
                g2d.drawString("PACKET", (x1 + x2) / 2 - 18, (y1 + y2) / 2 + 3);
            }
        }

        private void drawLegend(Graphics2D g2d) {
            int legendX = 50;
            int legendY = 500;

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Легенда:", legendX, legendY);

            g2d.setFont(new Font("Arial", Font.PLAIN, 12));

            // PC
            g2d.setColor(Color.BLUE);
            g2d.fillRoundRect(legendX, legendY + 20, 20, 15, 5, 5);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(legendX, legendY + 20, 20, 15, 5, 5);
            g2d.drawString("- Компьютер (PC)", legendX + 30, legendY + 32);

            // Switch
            g2d.setColor(Color.GREEN);
            g2d.fillRoundRect(legendX, legendY + 40, 20, 15, 5, 5);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(legendX, legendY + 40, 20, 15, 5, 5);
            g2d.drawString("- Свитч (Switch)", legendX + 30, legendY + 52);

            // Router
            g2d.setColor(Color.ORANGE);
            g2d.fillRoundRect(legendX, legendY + 60, 20, 15, 5, 5);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(legendX, legendY + 60, 20, 15, 5, 5);
            g2d.drawString("- Роутер (Router)", legendX + 30, legendY + 72);

            // Packet
            g2d.setColor(Color.RED);
            g2d.fillOval(legendX, legendY + 80, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(legendX, legendY + 80, 20, 20);
            g2d.drawString("- Пакет данных", legendX + 30, legendY + 95);

            // Контейнеры
            g2d.setColor(Color.BLACK);
            g2d.drawString("Контейнеры JADE:", legendX + 200, legendY);

            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            g2d.setColor(Color.YELLOW);
            g2d.drawString("Main-Container", legendX + 210, legendY + 20);
            g2d.setColor(Color.BLACK);
            g2d.drawString(": PC1, Switch1, Router1", legendX + 290, legendY + 20);

            g2d.setColor(Color.YELLOW);
            g2d.drawString("Container2", legendX + 210, legendY + 40);
            g2d.setColor(Color.BLACK);
            g2d.drawString(": PC2, Router2", legendX + 290, legendY + 40);
        }
    }

    // Точка входа для тестирования GUI
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NetworkGUI gui = new NetworkGUI();
            gui.setVisible(true);
        });
    }
}