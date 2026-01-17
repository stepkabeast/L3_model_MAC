import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;

public class NetworkGUI extends JFrame {
    private NetworkPanel networkPanel;
    private JButton pingButton;
    private JComboBox<String> sourceDevice;
    private JComboBox<String> targetDevice;
    private Map<String, DeviceShape> devices;

    public NetworkGUI() {
        setTitle("L3 Network Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLayout(new BorderLayout());

        devices = new HashMap<>();

        // Панель управления
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        sourceDevice = new JComboBox<>(new String[]{"PC1", "PC2"});
        targetDevice = new JComboBox<>(new String[]{"PC1", "PC2"});

        pingButton = new JButton("Start Ping");
        pingButton.addActionListener(e -> startPing());

        controlPanel.add(new JLabel("From:"));
        controlPanel.add(sourceDevice);
        controlPanel.add(new JLabel("To:"));
        controlPanel.add(targetDevice);
        controlPanel.add(pingButton);

        // Панель сети
        networkPanel = new NetworkPanel();

        add(controlPanel, BorderLayout.NORTH);
        add(networkPanel, BorderLayout.CENTER);

        initializeDevices();
    }

    private void initializeDevices() {
        // Создаем устройства на схеме
        devices.put("PC1", new DeviceShape(100, 200, "PC1", Color.BLUE));
        devices.put("Switch1", new DeviceShape(300, 200, "Switch1", Color.GREEN));
        devices.put("Router1", new DeviceShape(500, 200, "Router1", Color.ORANGE));
        devices.put("Router2", new DeviceShape(500, 400, "Router2", Color.ORANGE));
        devices.put("PC2", new DeviceShape(700, 400, "PC2", Color.BLUE));

        networkPanel.setDevices(devices);
    }

    private void startPing() {
        String source = (String) sourceDevice.getSelectedItem();
        String target = (String) targetDevice.getSelectedItem();

        if (source.equals(target)) {
            JOptionPane.showMessageDialog(this, "Select different devices!");
            return;
        }

        // Определяем маршрут
        List<String> route = findRoute(source, target);
        networkPanel.animatePacket(route);
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
}

class DeviceShape {
    int x, y;
    String name;
    Color color;

    public DeviceShape(int x, int y, String name, Color color) {
        this.x = x; this.y = y;
        this.name = name;
        this.color = color;
    }
}

class NetworkPanel extends JPanel {
    private Map<String, DeviceShape> devices;
    private List<String> currentRoute;
    private int animationStep = 0;
    public Timer animationTimer;

    public NetworkPanel() {
        setBackground(Color.WHITE);
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
    }

    private void drawConnection(Graphics2D g2d, String dev1, String dev2) {
        DeviceShape d1 = devices.get(dev1);
        DeviceShape d2 = devices.get(dev2);
        if (d1 != null && d2 != null) {
            g2d.drawLine(d1.x + 40, d1.y + 20, d2.x, d2.y + 20);
        }
    }

    private void drawDevice(Graphics2D g2d, DeviceShape device) {
        g2d.setColor(device.color);
        g2d.fillRoundRect(device.x, device.y, 80, 40, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(device.x, device.y, 80, 40, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(device.name);
        g2d.drawString(device.name, device.x + 40 - textWidth/2, device.y + 25);
    }

    private void drawPacket(Graphics2D g2d, String from, String to) {
        DeviceShape d1 = devices.get(from);
        DeviceShape d2 = devices.get(to);

        if (d1 != null && d2 != null) {
            int x1 = d1.x + 40;
            int y1 = d1.y + 20;
            int x2 = d2.x;
            int y2 = d2.y + 20;

            g2d.setColor(Color.RED);
            g2d.fillOval((x1 + x2) / 2 - 10, (y1 + y2) / 2 - 10, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawOval((x1 + x2) / 2 - 10, (y1 + y2) / 2 - 10, 20, 20);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("PING", (x1 + x2) / 2 - 12, (y1 + y2) / 2 + 4);
        }
    }
}