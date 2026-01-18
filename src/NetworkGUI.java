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
    private JTabbedPane tabbedPane;
    private NetworkConfig config;
    private Map<String, NetworkConfig.DeviceInfo> deviceInfoMap;

    public NetworkGUI(NetworkConfig config) {
        this.config = config;
        this.deviceInfoMap = new HashMap<>();

        // Инициализируем карту устройств из конфигурации
        if (config != null) {
            for (NetworkConfig.DeviceInfo device : config.getAllDevices()) {
                deviceInfoMap.put(device.name, device);
            }
        }

        setTitle("L3 Network Simulator - Packet Info");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLayout(new BorderLayout());

        setupUI();
        initializeDevices();
    }

    private void setupUI() {
        JPanel controlPanel = new JPanel(new FlowLayout());

        // Получаем список PC устройств
        List<String> pcNames = new ArrayList<>();
        for (NetworkConfig.DeviceInfo info : deviceInfoMap.values()) {
            if ("PC".equals(info.type)) {
                pcNames.add(info.name);
            }
        }

        sourceDevice = new JComboBox<>(pcNames.toArray(new String[0]));
        targetDevice = new JComboBox<>(pcNames.toArray(new String[0]));

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

        networkPanel = new NetworkPanel(config);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Сетевая схема", new JScrollPane(networkPanel));
        tabbedPane.addTab("Информация о пакетах", createPacketInfoTab());
        tabbedPane.addTab("Устройства сети", createDevicesInfoPanel());

        add(controlPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initializeDevices() {
        networkPanel.initializeLayout(deviceInfoMap);
    }

    private void showPacketInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Информация о пакетах ===\n\n");

        for (NetworkConfig.DeviceInfo device : deviceInfoMap.values()) {
            info.append(device.name)
                    .append(" (")
                    .append(device.type)
                    .append("):\n")
                    .append("  IP: ").append(device.ip).append("\n")
                    .append("  Сеть: ").append(device.network != null ? device.network : "N/A").append("\n")
                    .append("  Контейнер: ").append(device.container).append("\n");

            if (device.type.equals("PC") && device.gateway != null) {
                info.append("  Шлюз: ").append(device.gateway).append("\n");
            }
            info.append("\n");
        }

        packetInfoArea.setText(info.toString());
        tabbedPane.setSelectedIndex(1);
    }

    private JPanel createPacketInfoTab() {
        JPanel panel = new JPanel(new BorderLayout());
        packetInfoArea = new JTextArea();
        packetInfoArea.setEditable(false);
        packetInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(packetInfoArea);
        panel.add(new JLabel("Информация о передаваемых пакетах:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDevicesInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (NetworkConfig.DeviceInfo info : deviceInfoMap.values()) {
            String gateway = info.type.equals("PC") && info.gateway != null ?
                    "Шлюз=" + info.gateway : "Тип=" + info.type;
            String text = String.format("%s: IP=%s, %s, Сеть=%s, Контейнер=%s",
                    info.name, info.ip,
                    gateway,
                    info.network != null ? info.network : "N/A",
                    info.container);
            JLabel label = new JLabel(text);
            label.setFont(new Font("Arial", Font.PLAIN, 14));
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            panel.add(label);
        }

        return panel;
    }

    private void startPing(ActionEvent e) {
        String source = (String) sourceDevice.getSelectedItem();
        String target = (String) targetDevice.getSelectedItem();

        if (source == null || target == null || source.equals(target)) {
            JOptionPane.showMessageDialog(this, "Выберите разные устройства!");
            return;
        }

        NetworkConfig.DeviceInfo srcInfo = deviceInfoMap.get(source);
        NetworkConfig.DeviceInfo dstInfo = deviceInfoMap.get(target);

        packetInfoArea.append("\n=== ЗАПУСК PING ===\n");
        packetInfoArea.append("От: " + source + " (" + srcInfo.ip + ")\n");
        packetInfoArea.append("К: " + target + " (" + dstInfo.ip + ")\n");
        packetInfoArea.append("Время: " + new Date() + "\n");

        List<String> route = findRoute(source, target);
        networkPanel.animatePacket(route);

        packetInfoArea.append("Маршрут: " + String.join(" → ", route) + "\n");
        packetInfoArea.append("----------------------------------------\n");
    }

    private List<String> findRoute(String source, String target) {
        List<String> route = new ArrayList<>();
        NetworkConfig.DeviceInfo srcInfo = deviceInfoMap.get(source);
        NetworkConfig.DeviceInfo dstInfo = deviceInfoMap.get(target);

        route.add(source);

        if (!srcInfo.container.equals(dstInfo.container)) {
            // Межсетевая маршрутизация
            String srcSubnet = srcInfo.container.replace("subnet", "");
            String dstSubnet = dstInfo.container.replace("subnet", "");

            route.add("Switch" + srcSubnet);
            route.add("Router" + srcSubnet);
            route.add("Router" + dstSubnet);
            route.add("Switch" + dstSubnet);
            route.add(target);
        } else {
            // Внутренняя подсеть
            String subnet = srcInfo.container.replace("subnet", "");
            route.add("Switch" + subnet);
            route.add(target);
        }

        return route;
    }

    class NetworkPanel extends JPanel {
        private Map<String, DeviceShape> devices = new HashMap<>();
        private List<String> currentRoute;
        private int animationStep = 0;
        private Timer animationTimer;
        private String currentPacketInfo = "";
        private final NetworkConfig config;
        private List<Connection> connections = new ArrayList<>();

        public NetworkPanel(NetworkConfig config) {
            this.config = config;
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(1300, 700));
        }

        public void initializeLayout(Map<String, NetworkConfig.DeviceInfo> deviceInfoMap) {
            devices.clear();
            connections.clear();

            if (config == null || config.subnets <= 0) {
                return;
            }

            // Располагаем подсети в колонках
            int columnWidth = 350;
            int startX = 50;
            int startY = 100;
            int deviceHeight = 80;
            int verticalSpacing = 30;

            for (int subnet = 1; subnet <= config.subnets; subnet++) {
                int x = startX + (subnet - 1) * columnWidth;
                int y = startY;

                // Добавляем заголовок подсети
                String subnetNetwork = "192.168." + subnet + ".0/24"; // Базовый шаблон
                DeviceShape subnetTitle = new DeviceShape(
                        x, y - 60,
                        "Подсеть " + subnet + "\n" + subnetNetwork,
                        new Color(200, 220, 240),
                        "subnet" + subnet,
                        true
                );
                devices.put("subnet" + subnet, subnetTitle);

                // Располагаем устройства в подсети
                for (NetworkConfig.DeviceInfo device : deviceInfoMap.values()) {
                    if (device.container.equals("subnet" + subnet)) {
                        DeviceShape shape = new DeviceShape(
                                x, y,
                                device.name + "\n" + device.ip,
                                getDeviceColor(device.type),
                                "subnet" + subnet,
                                false
                        );
                        devices.put(device.name, shape);
                        y += deviceHeight + verticalSpacing;
                    }
                }
            }

            // Создаем связи
            createConnections();

            repaint();
        }

        private void createConnections() {
            // Связи внутри подсетей (PC ↔ Switch ↔ Router)
            for (int subnet = 1; subnet <= config.subnets; subnet++) {
                String subnetName = "subnet" + subnet;

                // Находим устройства в подсети
                String pcName = "PC" + subnet;
                String switchName = "Switch" + subnet;
                String routerName = "Router" + subnet;

                if (devices.containsKey(pcName) && devices.containsKey(switchName)) {
                    connections.add(new Connection(pcName, switchName, Color.BLACK));
                }
                if (devices.containsKey(switchName) && devices.containsKey(routerName)) {
                    connections.add(new Connection(switchName, routerName, Color.BLACK));
                }
            }

            // Связи между роутерами
            if (config.subnets > 1) {
                for (int i = 1; i < config.subnets; i++) {
                    String router1 = "Router" + i;
                    String router2 = "Router" + (i + 1);
                    if (devices.containsKey(router1) && devices.containsKey(router2)) {
                        connections.add(new Connection(router1, router2, Color.RED));
                    }
                }
            }
        }

        private Color getDeviceColor(String type) {
            return switch (type) {
                case "PC" -> new Color(70, 130, 180); // SteelBlue
                case "Switch" -> new Color(60, 179, 113); // MediumSeaGreen
                case "Router" -> new Color(255, 140, 0); // DarkOrange
                default -> Color.GRAY;
            };
        }

        public void animatePacket(List<String> route) {
            this.currentRoute = route;
            this.animationStep = 0;

            if (animationTimer != null) {
                animationTimer.stop();
            }

            if (route == null || route.size() < 2) {
                return;
            }

            String source = route.get(0);
            String target = route.get(route.size() - 1);

            NetworkConfig.DeviceInfo srcInfo = deviceInfoMap.get(source);
            NetworkConfig.DeviceInfo dstInfo = deviceInfoMap.get(target);

            if (srcInfo == null || dstInfo == null) {
                return;
            }

            String sourceIP = srcInfo.ip;
            String targetIP = dstInfo.ip;
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
                    new Timer(1000, ev -> {
                        currentPacketInfo += "\n✓ Передача завершена";
                        repaint();
                        ((Timer)ev.getSource()).stop();
                        new Timer(2000, e2 -> {
                            currentPacketInfo = currentPacketInfo.replace("\n✓ Передача завершена", "");
                            animationStep = 0;
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
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Рисуем связи
            for (Connection conn : connections) {
                drawConnection(g2d, conn);
            }

            // Рисуем устройства
            for (DeviceShape device : devices.values()) {
                drawDevice(g2d, device);
            }

            // Анимация пакета
            if (currentRoute != null && animationStep > 0 && animationStep < currentRoute.size()) {
                String from = currentRoute.get(animationStep - 1);
                String to = currentRoute.get(animationStep);
                drawPacket(g2d, from, to);
                drawPacketInfo(g2d);
            }

            drawLegend(g2d);
        }

        private void drawConnection(Graphics2D g2d, Connection conn) {
            DeviceShape d1 = devices.get(conn.device1);
            DeviceShape d2 = devices.get(conn.device2);

            if (d1 != null && d2 != null) {
                g2d.setColor(conn.color);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(d1.x + d1.width/2, d1.y + d1.height/2,
                        d2.x + d2.width/2, d2.y + d2.height/2);

                // Подпись для межсетевых соединений
                if (conn.color == Color.RED) {
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    int midX = (d1.x + d2.x + d1.width) / 2;
                    int midY = (d1.y + d2.y + d1.height) / 2;
                    g2d.drawString("WAN Link", midX, midY - 5);
                }
            }
        }

        private void drawDevice(Graphics2D g2d, DeviceShape device) {
            if (device.isTitle) {
                // Рисуем заголовок подсети
                g2d.setColor(device.color);
                g2d.fillRoundRect(device.x, device.y, device.width, 50, 10, 10);
                g2d.setColor(Color.BLACK);
                g2d.drawRoundRect(device.x, device.y, device.width, 50, 10, 10);

                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                String[] lines = device.name.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(lines[i]);
                    g2d.drawString(lines[i], device.x + device.width/2 - textWidth/2,
                            device.y + 20 + i * 18);
                }
            } else {
                // Рисуем обычное устройство
                g2d.setColor(device.color);
                g2d.fillRoundRect(device.x, device.y, device.width, device.height, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawRoundRect(device.x, device.y, device.width, device.height, 15, 15);

                // Имя устройства и IP
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 11));
                String[] lines = device.name.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(lines[i]);
                    g2d.drawString(lines[i], device.x + device.width/2 - textWidth/2,
                            device.y + 20 + i * 15);
                }

                // Контейнер
                g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                g2d.setColor(Color.YELLOW);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(device.container);
                g2d.drawString(device.container, device.x + device.width/2 - textWidth/2,
                        device.y + device.height - 10);
            }
        }

        private void drawPacket(Graphics2D g2d, String from, String to) {
            DeviceShape d1 = devices.get(from);
            DeviceShape d2 = devices.get(to);
            if (d1 != null && d2 != null) {
                int x1 = d1.x + d1.width/2;
                int y1 = d1.y + d1.height/2;
                int x2 = d2.x + d2.width/2;
                int y2 = d2.y + d2.height/2;

                // Анимированная точка
                double progress = 0.5; // середина пути
                int packetX = (int) (x1 + (x2 - x1) * progress);
                int packetY = (int) (y1 + (y2 - y1) * progress);

                g2d.setColor(Color.RED);
                g2d.fillOval(packetX - 15, packetY - 15, 30, 30);
                g2d.setColor(Color.WHITE);
                g2d.drawOval(packetX - 15, packetY - 15, 30, 30);

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString("PING", packetX - 12, packetY + 4);

                // Стрелка направления
                drawArrow(g2d, x1, y1, x2, y2);
            }
        }

        private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = 10;

            int x3 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI/6));
            int y3 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI/6));
            int x4 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI/6));
            int y4 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI/6));

            g2d.setColor(Color.RED);
            g2d.fillPolygon(new int[]{x2, x3, x4}, new int[]{y2, y3, y4}, 3);
        }

        private void drawPacketInfo(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 220));
            g2d.fillRoundRect(50, getHeight() - 150, getWidth() - 100, 120, 15, 15);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 12));

            String[] lines = currentPacketInfo.split("\n");
            for (int i = 0; i < lines.length; i++) {
                g2d.drawString(lines[i], 70, getHeight() - 120 + i * 20);
            }

            if (currentRoute != null && animationStep < currentRoute.size() - 1) {
                String progress = String.format("Прогресс: %d/%d", animationStep, currentRoute.size() - 1);
                g2d.drawString(progress, 70, getHeight() - 120 + lines.length * 20);
            }
        }

        private void drawLegend(Graphics2D g2d) {
            int legendX = getWidth() - 250;
            int legendY = 50;

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Легенда:", legendX, legendY);

            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            drawLegendItem(g2d, legendX, legendY + 20, getDeviceColor("PC"), "Компьютер (PC)");
            drawLegendItem(g2d, legendX, legendY + 40, getDeviceColor("Switch"), "Свитч (Switch)");
            drawLegendItem(g2d, legendX, legendY + 60, getDeviceColor("Router"), "Роутер (Router)");
            drawLegendItem(g2d, legendX, legendY + 80, Color.RED, "Пакет данных");
            drawLegendItem(g2d, legendX, legendY + 100, Color.RED, "Межсетевое соединение");
            drawLegendItem(g2d, legendX, legendY + 120, Color.BLACK, "Локальное соединение");
        }

        private void drawLegendItem(Graphics2D g2d, int x, int y, Color color, String text) {
            g2d.setColor(color);
            if (text.contains("соединение")) {
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(x, y - 5, x + 20, y - 5);
                g2d.setStroke(new BasicStroke(1));
            } else {
                g2d.fillRoundRect(x, y - 10, 15, 15, 3, 3);
            }
            g2d.setColor(Color.BLACK);
            if (!text.contains("соединение")) {
                g2d.drawRoundRect(x, y - 10, 15, 15, 3, 3);
            }
            g2d.drawString(text, x + 25, y);
        }
    }

    static class DeviceShape {
        int x, y, width, height;
        String name;
        Color color;
        String container;
        boolean isTitle;

        public DeviceShape(int x, int y, String name, Color color, String container, boolean isTitle) {
            this.x = x;
            this.y = y;
            this.width = isTitle ? 300 : 120;
            this.height = isTitle ? 50 : 70;
            this.name = name;
            this.color = color;
            this.container = container;
            this.isTitle = isTitle;
        }
    }

    static class Connection {
        String device1, device2;
        Color color;

        public Connection(String device1, String device2, Color color) {
            this.device1 = device1;
            this.device2 = device2;
            this.color = color;
        }
    }
}