package moe.minhh2792.mcclock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class InvisibleFrameListener implements Listener {

    private final MCClock plugin;

    public InvisibleFrameListener(MCClock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;

        ItemStack item = event.getItemStack();
        if (item == null) return;

        if (plugin.isInvisibleFrameItem(item)) {
            frame.setVisible(false);
            plugin.markOwnedFrame(frame);
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!plugin.isOwnedFrame(frame)) return;

        event.setCancelled(true);

        Location loc = frame.getLocation();

        ItemStack contents = frame.getItem();
        if (contents.getType() != Material.AIR) {
            frame.getWorld().dropItemNaturally(loc, contents);
        }

        frame.getWorld().dropItemNaturally(loc, plugin.createInvisibleFrameItem(1));
        frame.remove();
    }
}
