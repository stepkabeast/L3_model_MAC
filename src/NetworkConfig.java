import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class NetworkConfig {

    @SerializedName("subnets")
    public int subnets;

    @SerializedName("devices")
    public List<DeviceTemplate> devices;

    @SerializedName("routing")
    public List<RoutingRule> routing;

    private transient Map<String, DeviceInfo> deviceInfoCache = new HashMap<>();
    private transient Map<String, List<DeviceInfo>> subnetDevicesCache = new HashMap<>();

    public static class DeviceTemplate {
        @SerializedName("type")
        public String type;

        @SerializedName("count")
        public int count;

        @SerializedName("ipBase")
        public String ipBase;

        @SerializedName("network")
        public String network;

        @SerializedName("gateway")
        public String gateway;
    }

    public static class RoutingRule {
        @SerializedName("from")
        public String fromNetwork;

        @SerializedName("to")
        public String toNetwork;

        @SerializedName("nextHop")
        public String nextHop;
    }

    public static NetworkConfig load(String path) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(new FileReader(path), NetworkConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки конфигурации: " + e.getMessage(), e);
        }
    }

    /**
     * Инициализирует кэш информации об устройствах
     */
    public void initDeviceCache() {
        deviceInfoCache.clear();
        subnetDevicesCache.clear();

        for (int subnet = 1; subnet <= subnets; subnet++) {
            String subnetStr = String.valueOf(subnet);
            List<DeviceInfo> devicesInSubnet = new java.util.ArrayList<>();

            for (DeviceTemplate template : devices) {
                for (int i = 0; i < template.count; i++) {
                    String name = template.type + subnet;
                    if (template.count > 1) {
                        name += "_" + (i + 1);
                    }

                    String ip = template.ipBase.replace("{subnet}", subnetStr);
                    String network = template.network.replace("{subnet}", subnetStr);
                    String gateway = template.gateway != null ?
                            template.gateway.replace("{subnet}", subnetStr) : null;

                    DeviceInfo deviceInfo = new DeviceInfo(
                            name, ip, template.type,
                            "subnet" + subnet, gateway, network
                    );

                    deviceInfoCache.put(name, deviceInfo);
                    devicesInSubnet.add(deviceInfo);
                }
            }
            subnetDevicesCache.put("subnet" + subnet, devicesInSubnet);
        }
    }

    public DeviceInfo getDeviceInfo(String name) {
        return deviceInfoCache.get(name);
    }

    public List<DeviceInfo> getDevicesInSubnet(String subnetName) {
        return subnetDevicesCache.getOrDefault(subnetName, new java.util.ArrayList<>());
    }

    public List<DeviceInfo> getAllDevices() {
        return new java.util.ArrayList<>(deviceInfoCache.values());
    }

    public List<RoutingRule> getRoutingRules() {
        return routing != null ? routing : new java.util.ArrayList<>();
    }

    public String getGatewayForDevice(String deviceName) {
        DeviceInfo info = deviceInfoCache.get(deviceName);
        if (info != null && info.gateway != null) {
            return info.gateway;
        }
        return null;
    }

    public static class DeviceInfo {
        public String name;
        public String ip;
        public String type;
        public String container;
        public String gateway;
        public String network;

        public DeviceInfo(String name, String ip, String type, String container,
                          String gateway, String network) {
            this.name = name;
            this.ip = ip;
            this.type = type;
            this.container = container;
            this.gateway = gateway;
            this.network = network;
        }
    }
}