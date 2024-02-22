package net.iamtakagi.proxyvirtualmotd;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;

import java.awt.Event;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public final class ProxyVirtualMotd extends Plugin {

    @Getter
    private static ProxyVirtualMotd instance;
    @Getter
    private static Gson gson;
    private Config config;
    private AddressCache addressCache;

    @Override
    public void onEnable() {
        instance = this;
        initGson();
        this.config = Config.load();
        this.addressCache = AddressCache.load();
        this.getProxy().getPluginManager().registerListener(this, new ProxyListener());
        this.getLogger().info("Enabled.");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Disabled.");
    }

    public void initGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        JsonDeserializer<AddressData> deserializer = new AddressDataDeserializer();
        gsonBuilder.registerTypeAdapter(AddressData.class, deserializer);
        gson = gsonBuilder.create();
    }

    public class ProxyListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onProxyPing(ProxyPingEvent event) {
            PendingConnection connection = event.getConnection();
            InetSocketAddress virtualHost = connection.getVirtualHost();
            String virtualHostname = virtualHost.getHostName(); // 接続元の仮想ホスト名を取得
            InetSocketAddress socketAddress = event.getConnection().getAddress();
            InetAddress inetAddress = socketAddress.getAddress();
            String hostAddress = inetAddress.getHostAddress();
            AddressData data = addressCache.findByAddress(hostAddress);

            String motd;
            ServerPing res = event.getResponse();

            if (data != null) {
                motd = config.getPlayerMotd();
                String username = External.MojangAPI.getUsernameByUUID(data.getUuid());

                if (motd.contains("%player_name%")) {
                    if (username != null) {
                        motd = motd.replace("%player_name%", username);
                    }
                }

                if(motd.contains("%virtual_hostname%")) {
                    if (virtualHostname != null) {
                        motd = motd.replace("%virtual_hostname%", virtualHostname);
                    }
                }

                // プレイヤーアイコン
                if (config.isPlayerFaviconEnabled()) {
                    BufferedImage headImage64 = External.MCHeadsAPI.getHeadImage64(data.getUuid());
                    if (headImage64 != null) {
                        Favicon favicon = Favicon.create(headImage64);
                        res.setFavicon(favicon);
                    }
                }
            } else {
                motd = config.getDefaultMotd(); // データが無い時
            }

            res.setDescriptionComponent(new ComponentBuilder(motd).getCurrentComponent());
            event.setResponse(res);
        }

        @EventHandler
        public void onLogin(LoginEvent event) {
            getLogger().info(event.getConnection().getUniqueId().toString());
            UUID uuid = event.getConnection().getUniqueId();
            InetSocketAddress socketAddress = event.getConnection().getAddress();
            InetAddress inetAddress = socketAddress.getAddress();
            String hostAddress = inetAddress.getHostAddress();
            AddressData data = addressCache.findByUUID(uuid);

            if (data != null) {
                data.setAddress(hostAddress);
            } else {
                addressCache.getCache().add(new AddressData(uuid, hostAddress));
            }
            addressCache.save();
        }
    }

    /**
     * 利用したい外部のAPI関連
     */
    private static class External {

        static final String API_BASEURL = "https://sessionserver.mojang.com";

        static final class MojangAPI {

            /**
             * UUID から Username を返却します
             * 
             * @param uuid
             * @return
             */
            static String getUsernameByUUID(UUID uuid) {
                String endpoint = API_BASEURL + "/session/minecraft/profile/" + uuid.toString().replace("-", "");
                try {
                    String res = IOUtils.toString(new URL(endpoint), StandardCharsets.UTF_8);
                    JsonArray names = (JsonArray) JsonParser.parseString(res);
                    JsonObject obj = (JsonObject) names.get(names.size() - 1);
                    return obj.get("name").getAsString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }

        static final class MCHeadsAPI {

            static final String API_BASEURL = "https://mc-heads.net";

            /**
             * 64 x 64 のプレイヤーアイコン画像を返却します
             * 
             * @param uuid
             * @return
             */
            static BufferedImage getHeadImage64(UUID uuid) {
                URL url = null;
                BufferedImage image = null;
                try {
                    final String endpoint = API_BASEURL + "/avatar/" + uuid.toString().replace("-", "") + "/64";
                    url = new URL(endpoint);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                if (url == null)
                    return null;
                try {
                    image = ImageIO.read(url);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return image;
            }
        }
    }

    @AllArgsConstructor
    private static class Config {

        @Getter
        private String defaultMotd;
        @Getter
        private String playerMotd;
        @Getter
        private boolean playerFaviconEnabled;

        @AllArgsConstructor
        @Getter
        private enum ConfigProperty {
            DEFAULT_MOTD("default_motd"),
            PLAYER_MOTD("player_motd"),
            PLAYER_FAVICON_ENABLED("player_favicon_enabled");

            private final String path;
        }

        public static Config load() {
            ProxyVirtualMotd instance = ProxyVirtualMotd.getInstance();

            File dataFolder = instance.getDataFolder();
            if (!dataFolder.exists())
                dataFolder.mkdir();

            File configFile = new File(dataFolder, "config.yml");
            if (!configFile.exists()) {
                try (InputStream in = instance.getResourceAsStream("config.yml")) {
                    Files.copy(in, configFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Configuration configuration = null;
            try {
                configuration = ConfigurationProvider.getProvider(YamlConfiguration.class)
                        .load(new File(dataFolder, "config.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            assert configuration != null;

            return new Config(
                    ChatColor.RESET + ChatColor.translateAlternateColorCodes(
                            '&', configuration.getString(ConfigProperty.DEFAULT_MOTD.getPath())),
                    ChatColor.RESET + ChatColor.translateAlternateColorCodes('&',
                            configuration.getString(ConfigProperty.PLAYER_MOTD.getPath())),
                    configuration.getBoolean(
                            ConfigProperty.PLAYER_FAVICON_ENABLED.getPath()));
        }
    }

    @Data
    @AllArgsConstructor
    private class AddressData {
        private UUID uuid;
        private String address;
    }

    private class AddressDataDeserializer implements JsonDeserializer<AddressData> {

        @Override
        public AddressData deserialize(JsonElement jsonElement, Type type,
                JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            return new AddressData(
                    UUID.fromString(jsonObject.get("uuid").getAsString()),
                    jsonObject.get("address").getAsString());
        }
    }

    @AllArgsConstructor
    private static class AddressCache {

        @Getter
        private final List<AddressData> cache;

        private static final Type DATA_TYPE = new TypeToken<AddressData[]>() {
        }.getType();

        public static AddressCache load() {
            File dataFolder = instance.getDataFolder();
            if (!dataFolder.exists())
                dataFolder.mkdir();

            File cacheFile = new File(dataFolder, "cache.json");
            if (!cacheFile.exists()) {
                try (InputStream in = instance.getResourceAsStream("cache.json")) {
                    Files.copy(in, cacheFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            List<AddressData> cache = new ArrayList<>();

            try {
                JsonReader reader = new JsonReader(new FileReader(cacheFile));
                cache = new ArrayList<>(Arrays.asList(ProxyVirtualMotd.getGson().fromJson(reader, DATA_TYPE)));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new AddressCache(cache);
        }

        public void save() {
            File dataFolder = instance.getDataFolder();
            File cacheFile = new File(dataFolder, "cache.json");
            try {
                FileWriter writer = new FileWriter(cacheFile);
                gson.toJson(cache.toArray(), writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public AddressData findByAddress(String address) {
            return this.cache.stream().filter(playerAddress -> address.equals(playerAddress.getAddress())).findFirst()
                    .orElse(null);
        }

        public AddressData findByUUID(UUID uuid) {
            return this.cache.stream().filter(playerAddress -> uuid.equals(playerAddress.getUuid())).findFirst()
                    .orElse(null);
        }
    }
}