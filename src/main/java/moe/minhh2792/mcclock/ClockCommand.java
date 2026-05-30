package moe.minhh2792.mcclock;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class ClockCommand implements CommandExecutor, TabCompleter {

    private final MCClock plugin;

    public ClockCommand(MCClock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("mcclock.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "get" -> handleGet(sender, args);
            case "getframe" -> handleGetFrame(sender, args);
            case "place" -> handlePlace(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleGet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /mcclock get.");
            return;
        }

        Integer amount = parseAmount(sender, args);
        if (amount == null) return;

        MapView view = plugin.getOrCreateMapView();
        if (view == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create map — no worlds loaded.");
            return;
        }

        ItemStack item = buildClockMapItem(view);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create map item.");
            return;
        }
        item.setAmount(amount);

        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "Given " + amount + " clock map(s) - (Map ID: " + view.getId() + ")");
    }

    private void handleGetFrame(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /mcclock getframe.");
            return;
        }

        Integer amount = parseAmount(sender, args);
        if (amount == null) return;

        player.getInventory().addItem(plugin.createInvisibleFrameItem(amount));
        player.sendMessage(ChatColor.GREEN + "Given " + amount + " invisible item frame(s). Place it like a normal frame!");
    }

    private void handlePlace(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /mcclock place.");
            return;
        }

        RayTraceResult result = player.rayTraceBlocks(5);
        if (result == null || result.getHitBlock() == null || result.getHitBlockFace() == null) {
            sender.sendMessage(ChatColor.RED + "No block in range. Look at a block within 5 blocks.");
            return;
        }

        Block hitBlock = result.getHitBlock();
        BlockFace face = result.getHitBlockFace();

        Block frameBlock = hitBlock.getRelative(face);
        if (frameBlock.getType() != Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Not enough space to place the clock there.");
            return;
        }

        MapView view = plugin.getOrCreateMapView();
        if (view == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create map — no worlds loaded.");
            return;
        }

        ItemStack mapItem = buildClockMapItem(view);
        if (mapItem == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create clock map item.");
            return;
        }

        ItemFrame frame = (ItemFrame) player.getWorld().spawnEntity(
                frameBlock.getLocation(), EntityType.ITEM_FRAME);
        frame.setFacingDirection(face, true);
        frame.setVisible(false);
        frame.setFixed(false);
        frame.setItem(mapItem);
        plugin.markOwnedFrame(frame);

        sender.sendMessage(ChatColor.GREEN + "Clock placed! (Map ID: " + view.getId() + ")");
    }

    private Integer parseAmount(CommandSender sender, String[] args) {
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 64.");
                    return null;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                return null;
            }
        }
        return amount;
    }

    private void handleReload(CommandSender sender) {
        boolean success = plugin.reloadPlugin();
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "MCClock reloaded successfully.");
        } else {
            sender.sendMessage(ChatColor.RED + "MCClock reload failed — check console for details.");
        }
    }

    private ItemStack buildClockMapItem(MapView view) {
        String name = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("item.name", "&bMCClock"));
        List<String> lore = plugin.getConfig().getStringList("item.lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

        ItemStack item = new ItemStack(Material.FILLED_MAP, 1);
        MapMeta meta = (MapMeta) item.getItemMeta();
        if (meta == null) return null;
        meta.setMapView(view);
        meta.setDisplayName(name);
        if (!lore.isEmpty()) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("mcclock.admin")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("get", "getframe", "place", "reload");
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        List<String> amounts = Arrays.asList("1", "2", "4", "8", "16", "32", "64");

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("get")) {
                return amounts.stream()
                        .filter(s -> s.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
            if (sub.equals("getframe")) {
                return amounts.stream()
                        .filter(s -> s.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "--- MCClock ---");
        sender.sendMessage(ChatColor.YELLOW + "/mcclock get [amount]" + ChatColor.WHITE + " — Get clock map(s)");
        sender.sendMessage(ChatColor.YELLOW + "/mcclock getframe [amount]" + ChatColor.WHITE + " — Get invisible item frame(s)");
        sender.sendMessage(ChatColor.YELLOW + "/mcclock place" + ChatColor.WHITE + " — Place clock directly on the block you're looking at");
        sender.sendMessage(ChatColor.YELLOW + "/mcclock reload" + ChatColor.WHITE + " — Reload config and map");
    }
}
