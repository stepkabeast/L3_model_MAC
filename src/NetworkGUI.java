import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class NetworkGUI extends JFrame {
    private NetworkPanel networkPanel;
    private JButton pingButton;
    private JButton showPacketInfoButton;
    private JTextArea packetInfoArea;
    private JComboBox<String> sourceDevice;
    private JComboBox<String> targetDevice;
    private Map<String, DeviceShape> devices;
    private JTabbedPane tabbedPane;

    public NetworkGUI() {
        setTitle("L3 Network Simulator - Packet Info");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        devices = new HashMap<>();

        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout());

        sourceDevice = new JComboBox<>(new String[]{"PC1", "PC2"});
        targetDevice = new JComboBox<>(new String[]{"PC1", "PC2"});

        pingButton = new JButton("Start Ping");
        pingButton.addActionListener(this::startPing);

        showPacketInfoButton = new JButton("Показать инфо о пакетах");
        showPacketInfoButton.addActionListener(e -> showPacketInfo());

        controlPanel.add(new JLabel("Откуда:"));
        controlPanel.add(sourceDevice);
        controlPanel.add(new JLabel("Куда:"));
        controlPanel.add(targetDevice);
        controlPanel.add(pingButton);
        controlPanel.add(showPacketInfoButton);

        // Создаем вкладки
        tabbedPane = new JTabbedPane();

        // Вкладка 1: Сетевая схема
        networkPanel = new NetworkPanel();
        JPanel networkTab = new JPanel(new BorderLayout());
        networkTab.add(networkPanel, BorderLayout.CENTER);

        // Вкладка 2: Информация о пакетах
        JPanel packetInfoTab = new JPanel(new BorderLayout());
        packetInfoArea = new JTextArea();
        packetInfoArea.setEditable(false);
        packetInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(packetInfoArea);
        packetInfoTab.add(new JLabel("Информация о передаваемых пакетах:"), BorderLayout.NORTH);
        packetInfoTab.add(scrollPane, BorderLayout.CENTER);

        // Вкладка 3: Сетевые устройства
        JPanel devicesTab = createDevicesInfoPanel();

        tabbedPane.addTab("Сетевая схема", networkTab);
        tabbedPane.addTab("Информация о пакетах", packetInfoTab);
        tabbedPane.addTab("Устройства сети", devicesTab);

        add(controlPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        initializeDevices();
    }

    private JPanel createDevicesInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] deviceInfo = {
                "PC1: IP=192.168.1.10, Шлюз=Router1, Контейнер=Main-Container",
                "Switch1: IP=192.168.1.1, Тип=L2 Switch, Контейнер=Main-Container",
                "Router1: IP=192.168.1.254, Сети: 192.168.1.0/24→Switch1, 192.168.2.0/24→Router2",
                "Router2: IP=192.168.2.254, Сети: 192.168.1.0/24→Router1, 192.168.2.0/24→PC2",
                "PC2: IP=192.168.2.20, Шлюз=Router2, Контейнер=Container2"
        };

        for (String info : deviceInfo) {
            JLabel label = new JLabel(info);
            label.setFont(new Font("Arial", Font.PLAIN, 14));
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            panel.add(label);
        }

        return panel;
    }

    private void initializeDevices() {
        devices.put("PC1", new DeviceShape(100, 200, "PC1\n192.168.1.10", Color.BLUE, "Main-Container"));
        devices.put("Switch1", new DeviceShape(300, 200, "Switch1\n192.168.1.1", Color.GREEN, "Main-Container"));
        devices.put("Router1", new DeviceShape(500, 200, "Router1\n192.168.1.254", Color.ORANGE, "Main-Container"));
        devices.put("Router2", new DeviceShape(500, 400, "Router2\n192.168.2.254", Color.ORANGE, "Container2"));
        devices.put("PC2", new DeviceShape(700, 400, "PC2\n192.168.2.20", Color.BLUE, "Container2"));

        networkPanel.setDevices(devices);
    }

    private void startPing(ActionEvent e) {
        String source = (String) sourceDevice.getSelectedItem();
        String target = (String) targetDevice.getSelectedItem();

        if (source.equals(target)) {
            JOptionPane.showMessageDialog(this, "Выберите разные устройства!");
            return;
        }

        String sourceIP = source.equals("PC1") ? "192.168.1.10" : "192.168.2.20";
        String targetIP = target.equals("PC1") ? "192.168.1.10" : "192.168.2.20";

        packetInfoArea.append("\n=== ЗАПУСК PING ===\n");
        packetInfoArea.append("От: " + source + " (" + sourceIP + ")\n");
        packetInfoArea.append("К: " + target + " (" + targetIP + ")\n");
        packetInfoArea.append("Время: " + new Date() + "\n");

        // Определяем маршрут для анимации
        List<String> route = findRoute(source, target);
        networkPanel.animatePacket(route);

        // Показываем информацию о маршруте
        packetInfoArea.append("Маршрут: " + String.join(" → ", route) + "\n");
        packetInfoArea.append("----------------------------------------\n");

        // Переключаемся на вкладку с информацией о пакетах
        //tabbedPane.setSelectedIndex(1);
    }

    private void showPacketInfo() {
        packetInfoArea.append("\n=== ИНФОРМАЦИЯ О ПАКЕТАХ ===\n");
        packetInfoArea.append("Типы пакетов в системе:\n");
        packetInfoArea.append("1. PING (Echo Request) - запрос проверки доступности\n");
        packetInfoArea.append("2. PONG (Echo Reply) - ответ на PING\n");
        packetInfoArea.append("3. Формат пакета: [ID:TYPE:SRC_IP:DST_IP:SRC_MAC:DST_MAC:TTL:CURRENT:PATH]\n");
        packetInfoArea.append("Пример: PKT-123456:PING:192.168.1.10:192.168.2.20:AA:BB:CC:DD:EE:FF:...\n");
        packetInfoArea.append("----------------------------------------\n");
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
        }

        return route;
    }

    static class DeviceShape {
        int x, y;
        String name;
        Color color;
        String container;

        public DeviceShape(int x, int y, String name, Color color, String container) {
            this.x = x; this.y = y;
            this.name = name;
            this.color = color;
            this.container = container;
        }
    }

    static class NetworkPanel extends JPanel {
        private Map<String, DeviceShape> devices;
        private List<String> currentRoute;
        private int animationStep = 0;
        private Timer animationTimer;
        private String currentPacketInfo = "";

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

            // Создаем информацию о пакете
            String source = route.get(0);
            String target = route.get(route.size() - 1);
            String sourceIP = source.equals("PC1") ? "192.168.1.10" : "192.168.2.20";
            String targetIP = target.equals("PC1") ? "192.168.1.10" : "192.168.2.20";
            String packetId = "PKT-" + System.currentTimeMillis() % 1000000;

            currentPacketInfo = String.format(
                    "Пакет %s: %s → %s\n" +
                            "PING %s → %s\n" +
                            "TTL: 64, Протокол: ICMP",
                    packetId, source, target, sourceIP, targetIP
            );

            animationTimer = new Timer(500, e -> {
                if (animationStep < currentRoute.size() - 1) {
                    animationStep++;
                    repaint();
                } else {
                    ((Timer)e.getSource()).stop();

                    // Показываем завершающее сообщение
                    new Timer(1000, ev -> {
                        currentPacketInfo += "\n✓ Передача завершена";
                        repaint();
                        ((Timer)ev.getSource()).stop();

                        // Через 2 секунды убираем сообщение
                        new Timer(2000, e2 -> {
                            currentPacketInfo = currentPacketInfo.replace("\n✓ Передача завершена", "");
                            animationStep = 0; // Сбрасываем анимацию
                            repaint();
                            ((Timer)e2.getSource()).stop();
                        }).start();
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
            g2d.drawString("Схема L3 сети с передачей пакетов", 300, 30);

            // Рисуем соединения
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(2));
            drawConnection(g2d, "PC1", "Switch1");
            drawConnection(g2d, "Switch1", "Router1");
            drawConnection(g2d, "Router1", "Router2");
            drawConnection(g2d, "Router2", "PC2");

            // Рисуем устройства
            for (DeviceShape device : devices.values()) {
                drawDevice(g2d, device);
            }

            // Анимация пакета
            if (currentRoute != null && animationStep > 0) {
                drawPacket(g2d, currentRoute.get(animationStep - 1),
                        currentRoute.get(animationStep));

                // Показываем информацию о текущем пакете
                drawPacketInfo(g2d);
            }

            // Легенда
            drawLegend(g2d);
        }

        private void drawConnection(Graphics2D g2d, String dev1, String dev2) {
            DeviceShape d1 = devices.get(dev1);
            DeviceShape d2 = devices.get(dev2);
            if (d1 != null && d2 != null) {
                g2d.drawLine(d1.x + 40, d1.y + 30, d2.x, d2.y + 30);
            }
        }

        private void drawDevice(Graphics2D g2d, DeviceShape device) {
            g2d.setColor(device.color);
            g2d.fillRoundRect(device.x, device.y, 80, 60, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(device.x, device.y, 80, 60, 10, 10);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            String[] lines = device.name.split("\n");
            for (int i = 0; i < lines.length; i++) {
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(lines[i]);
                g2d.drawString(lines[i], device.x + 40 - textWidth/2, device.y + 20 + i*15);
            }

            g2d.setFont(new Font("Arial", Font.PLAIN, 9));
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
                g2d.fillOval((x1 + x2) / 2 - 12, (y1 + y2) / 2 - 12, 24, 24);
                g2d.setColor(Color.BLACK);
                g2d.drawOval((x1 + x2) / 2 - 12, (y1 + y2) / 2 - 12, 24, 24);

                // Рисуем символ пакета
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString("📦", (x1 + x2) / 2 - 10, (y1 + y2) / 2 + 4);

                // Показываем текущий переход
                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.drawString(from + " → " + to, (x1 + x2) / 2 - 30, (y1 + y2) / 2 - 20);
            }
        }

        private void drawPacketInfo(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 200)); // Полупрозрачный черный
            g2d.fillRoundRect(50, 450, 800, 120, 15, 15);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 12));

            String[] lines = currentPacketInfo.split("\n");
            for (int i = 0; i < lines.length; i++) {
                g2d.drawString(lines[i], 70, 480 + i * 20);
            }

            // Показываем прогресс
            if (currentRoute != null && animationStep < currentRoute.size() - 1) {
                String progress = String.format("Прогресс: %d/%d",
                        animationStep, currentRoute.size() - 1);
                g2d.drawString(progress, 70, 480 + lines.length * 20);
            }
        }

        private void drawLegend(Graphics2D g2d) {
            int legendX = 650;
            int legendY = 100;

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Легенда:", legendX, legendY);

            g2d.setFont(new Font("Arial", Font.PLAIN, 11));

            drawLegendItem(g2d, legendX, legendY + 20, Color.BLUE, "Компьютер (PC)");
            drawLegendItem(g2d, legendX, legendY + 40, Color.GREEN, "Свитч (Switch)");
            drawLegendItem(g2d, legendX, legendY + 60, Color.ORANGE, "Роутер (Router)");
            drawLegendItem(g2d, legendX, legendY + 80, Color.RED, "Пакет данных");
        }

        private void drawLegendItem(Graphics2D g2d, int x, int y, Color color, String text) {
            g2d.setColor(color);
            g2d.fillRoundRect(x, y - 10, 15, 15, 3, 3);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(x, y - 10, 15, 15, 3, 3);
            g2d.drawString(text, x + 25, y);
        }
    }
}