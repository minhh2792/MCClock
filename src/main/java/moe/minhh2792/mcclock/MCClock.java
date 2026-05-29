package moe.minhh2792.mcclock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.List;

@SuppressWarnings("deprecation")
public final class MCClock extends JavaPlugin {

    private ClockRenderer clockRenderer;
    private int mapId = -1;
    private BukkitTask tickTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("clock_face.png", false);
        saveResource("clock_face_1.png", false);

        BufferedImage scaledFace = loadScaledFace();
        if (scaledFace == null) {
            getLogger().severe("Failed to load clock face — disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        clockRenderer = new ClockRenderer(scaledFace, getConfig().getInt("hands.thickness", 1),
                "smooth".equalsIgnoreCase(getConfig().getString("hands.mode", "smooth")),
                ZoneId.of(getConfig().getString("timezone", "UTC")),
                getConfig().getLong("hands.latency-compensation-ms", 0));

        mapId = getConfig().getInt("map-id", -1);
        if (mapId >= 0) {
            MapView view = Bukkit.getMap(mapId);
            if (view != null) {
                attachRenderer(view);
            } else {
                getLogger().warning("Map ID " + mapId + " not found — will create a new one on /mcclock get.");
                mapId = -1;
            }
        }

        var mcclockCmd = getCommand("mcclock");
        assert mcclockCmd != null;
        ClockCommand clockCommand = new ClockCommand(this);
        mcclockCmd.setExecutor(clockCommand);
        mcclockCmd.setTabCompleter(clockCommand);

        getServer().getPluginManager().registerEvents(new InvisibleFrameListener(this), this);

        startTickTask();
    }

    @Override
    public void onDisable() {
        stopTickTask();
    }

    public boolean reloadPlugin() {
        reloadConfig();

        BufferedImage scaledFace = loadScaledFace();
        if (scaledFace == null) {
            getLogger().severe("Failed to reload clock face.");
            return false;
        }
        clockRenderer = new ClockRenderer(scaledFace, getConfig().getInt("hands.thickness", 1),
                "smooth".equalsIgnoreCase(getConfig().getString("hands.mode", "smooth")),
                ZoneId.of(getConfig().getString("timezone", "UTC")),
                getConfig().getLong("hands.latency-compensation-ms", 0));

        if (mapId >= 0) {
            MapView view = Bukkit.getMap(mapId);
            if (view != null) {
                attachRenderer(view);
            } else {
                getLogger().warning("Map ID " + mapId + " not found after reload.");
                mapId = -1;
            }
        }

        stopTickTask();
        startTickTask();

        return true;
    }

    private void startTickTask() {
        if (!getConfig().getBoolean("tick-sound.enabled", true)) return;

        double radius = getConfig().getDouble("tick-sound.radius", 5.0);
        float volume = (float) getConfig().getDouble("tick-sound.volume", 0.3);
        float pitch = (float) getConfig().getDouble("tick-sound.pitch", 2.0);
        String soundName = getConfig().getString("tick-sound.sound", "BLOCK_NOTE_BLOCK_HAT");

        Sound sound;
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid tick-sound.sound '" + soundName + "', using default.");
            sound = Sound.BLOCK_NOTE_BLOCK_HAT;
        }

        final Sound finalSound = sound;
        final double radiusSq = radius * radius;

        tickTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (!(entity instanceof ItemFrame frame)) continue;
                    if (frame.isVisible()) continue;

                    ItemStack frameItem = frame.getItem();
                    if(frameItem.getType() != Material.FILLED_MAP) continue;
                    if(mapId <= 0 || !(frameItem.getItemMeta() instanceof org.bukkit.inventory.meta.MapMeta mapMeta)
                                  || !mapMeta.hasMapView()
                                  || mapMeta.getMapView() == null
                                  || mapMeta.getMapView().getId() != mapId) {
                        continue;
                    }

                    for (Player player : world.getPlayers()) {
                        if (player.getLocation().distanceSquared(frame.getLocation()) <= radiusSq) {
                            player.playSound(frame.getLocation(), finalSound, volume, pitch);
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    private void stopTickTask() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    public MapView getOrCreateMapView() {
        if (mapId >= 0) {
            MapView view = Bukkit.getMap(mapId);
            if (view != null) return view;
        }
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            getLogger().severe("No worlds loaded — cannot create map.");
            return null;
        }
        MapView view = Bukkit.createMap(worlds.get(0));
        view.setScale(MapView.Scale.CLOSEST);
        attachRenderer(view);
        mapId = view.getId();
        getConfig().set("map-id", mapId);
        saveConfig();
        return view;
    }

    public ItemStack createInvisibleFrameItem(int amount) {
        ItemStack item = new ItemStack(Material.GLOW_ITEM_FRAME, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.AQUA + "Invisible Item Frame");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place on a wall to create",
                ChatColor.GRAY + "an invisible item frame."
        ));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(this, "invisible_frame"),
                PersistentDataType.BYTE,
                (byte) 1
        );
        item.setItemMeta(meta);
        return item;
    }

    public boolean isInvisibleFrameItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer()
                .has(new NamespacedKey(this, "invisible_frame"), PersistentDataType.BYTE);
    }

    public void markOwnedFrame(ItemFrame frame) {
        frame.getPersistentDataContainer().set(
                new NamespacedKey(this, "owned_frame"),
                PersistentDataType.BYTE,
                (byte) 1
        );
    }

    public boolean isOwnedFrame(ItemFrame frame) {
        return frame.getPersistentDataContainer()
                .has(new NamespacedKey(this, "owned_frame"), PersistentDataType.BYTE);
    }

    private void attachRenderer(MapView view) {
        view.getRenderers().forEach(view::removeRenderer);
        view.addRenderer(clockRenderer);
    }

    private BufferedImage loadScaledFace() {
        try {
            String fileName = getConfig().getString("clock-face", "clock_face.png");
            java.io.File customFile = new java.io.File(getDataFolder(), fileName);
            InputStream is = customFile.exists()
                    ? new java.io.FileInputStream(customFile)
                    : getResource(fileName);
            if (is == null) return null;
            try (is) {
                BufferedImage original = ImageIO.read(is);
                if (original == null) return null;
                BufferedImage scaled = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(original, 0, 0, 128, 128, null);
                g.dispose();
                return scaled;
            }
        } catch (Exception e) {
            getLogger().severe("Error loading clock face: " + e.getMessage());
            return null;
        }
    }
}
