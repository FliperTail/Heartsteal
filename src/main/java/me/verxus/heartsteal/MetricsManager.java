package me.verxus.heartsteal;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MetricsManager {

    public static void setupMetrics(JavaPlugin plugin) {
        int pluginId = 27526;
        Metrics metrics = new Metrics(plugin, pluginId);

        // ✅ Corrected for bStats v3
        metrics.addCustomChart(new SingleLineChart("servers_using_heartsteal", () -> 1));
        metrics.addCustomChart(new SingleLineChart("average_player_count", () -> Bukkit.getOnlinePlayers().size()));

        plugin.getLogger().info("bStats metrics have been initialized!");
    }
}
