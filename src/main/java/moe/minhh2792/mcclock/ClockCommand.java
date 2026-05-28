package moe.minhh2792.mcclock;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class ClockCommand implements CommandExecutor {

    private final MCClock plugin;

    public ClockCommand(MCClock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 64.");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                return;
            }
        }

        MapView view = plugin.getOrCreateMapView();
        if (view == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create map — no worlds loaded.");
            return;
        }

        String name = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("item.name", "&bMCClock"));
        List<String> lore = plugin.getConfig().getStringList("item.lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

        ItemStack item = new ItemStack(Material.FILLED_MAP, amount);
        MapMeta meta = (MapMeta) item.getItemMeta();
        if (meta == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create map item.");
            return;
        }
        meta.setMapView(view);
        meta.setDisplayName(name);
        if (!lore.isEmpty()) meta.setLore(lore);
        item.setItemMeta(meta);

        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "Given " + amount + " clock map(s) - (Map ID: " + view.getId() + ")");
    }

    private void handleReload(CommandSender sender) {
        boolean success = plugin.reloadPlugin();
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "MCClock reloaded successfully.");
        } else {
            sender.sendMessage(ChatColor.RED + "MCClock reload failed — check console for details.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "--- MCClock ---");
        sender.sendMessage(ChatColor.YELLOW + "/mcclock get [amount]" + ChatColor.WHITE + " — Get clock map(s)");
        sender.sendMessage(ChatColor.YELLOW + "/mcclock reload" + ChatColor.WHITE + " — Reload config and map");
    }
}
