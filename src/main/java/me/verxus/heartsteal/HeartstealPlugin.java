package me.verxus.heartsteal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class HeartstealPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("withdraw").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;
            double currentMax = player.getMaxHealth();

            if (currentMax <= 2.0) {
                player.sendMessage("§cYou cannot withdraw any more hearts!");
                return true;
            }

            // Remove 1 heart
            player.setMaxHealth(currentMax - 2.0);

            // Create the Heart item
            ItemStack heart = new ItemStack(Material.NETHER_STAR, 1);
            ItemMeta meta = heart.getItemMeta();
            meta.setDisplayName("§c❤ Heart ❤");
            heart.setItemMeta(meta);

            // Give it to the player
            player.getInventory().addItem(heart);

            player.sendMessage("§aYou withdrew a heart!");
            return true;
        });

        getLogger().info("Heartsteal plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Heartsteal plugin disabled!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) return;

        Player killer = event.getEntity().getKiller();
        Player victim = event.getEntity();

        // Remove 1 heart from victim (if possible)
        double victimMax = victim.getMaxHealth();
        if (victimMax > 2.0) {
            victim.setMaxHealth(victimMax - 2.0);
        }

        // Add 1 heart to killer
        killer.setMaxHealth(killer.getMaxHealth() + 2.0);

        killer.sendMessage("§aYou stole a heart from " + victim.getName() + "!");
        victim.sendMessage("§cYou got killed from" + killer.getName() + " and lost a heart!");
    }

    @EventHandler
    public void onHeartUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        if (item.getType() != Material.NETHER_STAR) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!"§c❤ Heart ❤".equals(item.getItemMeta().getDisplayName())) return;

        // Consume the item
        item.setAmount(item.getAmount() - 1);

        // Add 1 heart
        player.setMaxHealth(player.getMaxHealth() + 2.0);
        player.sendMessage("§aYou used a Heart and gained +1 max health!");
    }
}
