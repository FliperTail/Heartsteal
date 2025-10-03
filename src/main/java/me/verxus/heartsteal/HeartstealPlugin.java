package me.verxus.heartsteal;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LifestealPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
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
        victim.sendMessage("§cYou lost one heart!");
    }
}
