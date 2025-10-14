package me.verxus.heartsteal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class HeartstealPlugin extends JavaPlugin implements Listener {

    private final String VERSION = "2.0-ALPHA";
    private final String MODRINTH_PROJECT = "heartsteal";

    private List<Player> deadPlayers = new ArrayList<>();
    private List<UUID> selfRevivePlayers = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("bStats metrics enabled!");
        Bukkit.getPluginManager().registerEvents(this, this);

        // Create custom Heart item
        ItemStack heart = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = heart.getItemMeta();
        meta.setDisplayName("§c§l❤ Heart");
        heart.setItemMeta(meta);

        // Heart item recipe
        ItemStack heartRecipeItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta heartMeta = heartRecipeItem.getItemMeta();
        heartMeta.setDisplayName("§c§l❤ Heart");
        heartRecipeItem.setItemMeta(heartMeta);

        ShapedRecipe heartRecipe = new ShapedRecipe(heartRecipeItem);
        heartRecipe.shape("DDD", "GNG", "DDD");
        heartRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        heartRecipe.setIngredient('N', Material.NETHER_STAR);
        heartRecipe.setIngredient('G', Material.GOLD_BLOCK);
        Bukkit.addRecipe(heartRecipe);

        // Revive Heart item
        ItemStack reviveHeart = new ItemStack(Material.NETHER_STAR);
        ItemMeta reviveMeta = reviveHeart.getItemMeta();
        reviveMeta.setDisplayName("§b§l❤ Revive Heart ❤");
        reviveHeart.setItemMeta(reviveMeta);

        // Crafting recipe
        ShapedRecipe reviveRecipe = new ShapedRecipe(reviveHeart);
        reviveRecipe.shape("DAD", "ENE", "DAD");
        reviveRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        reviveRecipe.setIngredient('A', Material.GOLDEN_APPLE);
        reviveRecipe.setIngredient('E', Material.EMERALD);
        reviveRecipe.setIngredient('N', Material.NETHER_STAR);
        Bukkit.addRecipe(reviveRecipe);

        // Self Revive Token
        ItemStack selfRevive = new ItemStack(Material.NETHER_STAR);
        ItemMeta selfMeta = selfRevive.getItemMeta();
        selfMeta.setDisplayName("§6§l❤ Self Revive Token ❤");
        selfRevive.setItemMeta(selfMeta);

        // Crafting recipe
        ShapedRecipe selfReviveRecipe = new ShapedRecipe(selfRevive);
        selfReviveRecipe.shape("GEG", "NTN", "GEG");
        selfReviveRecipe.setIngredient('G', Material.GOLD_BLOCK);
        selfReviveRecipe.setIngredient('E', Material.EMERALD_BLOCK);
        selfReviveRecipe.setIngredient('N', Material.NETHER_STAR);
        selfReviveRecipe.setIngredient('T', Material.TOTEM);
        Bukkit.addRecipe(selfReviveRecipe);

        MetricsManager.setupMetrics(this);

        getLogger().info("Heartsteal plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Heartsteal plugin disabled!");
    }

    // Heart stealing on PvP or natural death
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Visual lightning at death location
        victim.getWorld().strikeLightningEffect(victim.getLocation());

        double victimMax = victim.getMaxHealth();
        if (killer instanceof Player) {
            if (victimMax > 2.0) victim.setMaxHealth(victimMax - 2.0);

            double killerMax = killer.getMaxHealth();
            if (killerMax < 42.0) {
                killer.setMaxHealth(Math.min(42.0, killerMax + 2.0));
                killer.sendMessage("§aYou stole a heart from " + victim.getName() + "!");
            } else {
                killer.sendMessage("§eYou are already at the max of 21 hearts!");
            }

            victim.sendMessage("§cYou got killed by " + killer.getName() + " and lost a heart!");
        }


        if (selfRevivePlayers.contains(victim.getUniqueId())) {
            selfRevivePlayers.remove(victim.getUniqueId());

            new BukkitRunnable() {
                @Override
                public void run() {
                    victim.spigot().respawn();
                    victim.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    victim.setHealth(2.0);
                }
            }.runTaskLater(this, 20L);

            return;
        }

        // Check if dead
        if (victim.getMaxHealth() <= 2.0) {
            deadPlayers.add(victim);

            // Delay to avoid conflicts with death event
            new BukkitRunnable() {
                @Override
                public void run() {
                    victim.setGameMode(org.bukkit.GameMode.SPECTATOR);
                    victim.sendMessage("§cYou have no more hearts! Wait for someone to revive you.");
                }
            }.runTaskLater(this, 1L);
        }
    }

    // Using a Heart to gain max health
    @EventHandler
    public void onHeartUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) return;

        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() != Material.NETHER_STAR) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = item.getItemMeta().getDisplayName();

        if ("§c§l❤ Heart".equals(name)) {
            event.setCancelled(true);

            double currentMax = player.getMaxHealth();
            if (currentMax >= 42.0) {
                player.sendMessage("§eYou already have the maximum of 21 hearts!");
                return;
            }

            item.setAmount(item.getAmount() - 1);
            player.setItemInHand(item);

            player.setMaxHealth(Math.min(42.0, currentMax + 2.0));
            player.sendMessage("§aYou used a Heart and gained +1 max health!");
        }

        // Revive Heart usage
        if ("§b§l❤ Revive Heart ❤".equals(name)) {
            event.setCancelled(true);

            Inventory gui = Bukkit.createInventory(null, 18, "§a§l!Revive Players!");

            for (Player dead : deadPlayers) {
                ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                skullMeta.setOwner(dead.getName());
                skullMeta.setDisplayName("§a" + dead.getName());
                skull.setItemMeta(skullMeta);
                gui.addItem(skull);
            }

            player.openInventory(gui);

            item.setAmount(item.getAmount() - 1);
            player.setItemInHand(item);
        }

        if ("§6§l❤ Self Revive Token ❤".equals(name)) {
            event.setCancelled(true);

            if (selfRevivePlayers.contains(player.getUniqueId())) {
                player.sendMessage("§4§lYou already have a self revive at the ready!");
                return;
            }

            selfRevivePlayers.add(player.getUniqueId());
            player.sendMessage("§6§lYou used a self revive! It will now be used to bring you back when you die!");

            item.setAmount(item.getAmount() - 1);
            player.setItemInHand(item);
        }
    }

    // Revive GUI click
    @EventHandler
    public void onReviveGUIClick(InventoryClickEvent event) {
        String guiTitle = "§a§l!Revive Players!";
        if (!event.getView().getTitle().equals(guiTitle)) return;

        event.setCancelled(true); // Cancel all clicks immediately

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (!(meta instanceof SkullMeta)) return;

        String playerName = ChatColor.stripColor(meta.getDisplayName());
        Player toRevive = Bukkit.getPlayerExact(playerName);

        if (toRevive == null) {
            event.getWhoClicked().sendMessage("§cThat player is not online!");
            return;
        }

        // Revive logic
        toRevive.setGameMode(org.bukkit.GameMode.SURVIVAL);
        toRevive.setMaxHealth(10.0);
        toRevive.setHealth(10.0);
        deadPlayers.remove(toRevive);

        Player reviver = (Player) event.getWhoClicked();
        reviver.sendMessage("§aYou revived " + toRevive.getName() + "!");
        toRevive.sendMessage("§aYou have been revived by " + reviver.getName() + "!");

        reviver.closeInventory();
    }

    // Commands
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("withdraw")) {
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

            player.setMaxHealth(currentMax - 2.0);

            ItemStack heart = new ItemStack(Material.NETHER_STAR, 1);
            ItemMeta meta = heart.getItemMeta();
            meta.setDisplayName("§c§l❤ Heart");
            heart.setItemMeta(meta);

            player.getInventory().addItem(heart);
            player.sendMessage("§aYou withdrew a heart!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("info")) {
            sender.sendMessage(ChatColor.GREEN + "Heartsteal Plugin");
            sender.sendMessage(ChatColor.GREEN + "Version: " + VERSION);
            sender.sendMessage(ChatColor.GREEN + "Author: Verxus");

            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                String latest = fetchLatestVersionFromModrinth("heartsteal");
                if (latest == null) {
                    sender.sendMessage(ChatColor.RED + "Failed to check for updates (Modrinth API unreachable).");
                } else if (!latest.equalsIgnoreCase(VERSION)) {
                    sender.sendMessage(ChatColor.YELLOW + "A new version is available: " + latest);
                    sender.sendMessage(ChatColor.GRAY + "Check it out on Modrinth: https://modrinth.com/plugin/" + MODRINTH_PROJECT);
                } else {
                    sender.sendMessage(ChatColor.GREEN + "You are using the latest version!");
                }
            });
            return true;
        }

        if (command.getName().equalsIgnoreCase("giveheart")) {
            if (!sender.hasPermission("heartsteal.admin")) {
                sender.sendMessage("§cYou do not have permission to use this command!");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage("§cUsage: /giveheart <player> <amount>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cAmount must be a number!");
                return true;
            }

            ItemStack heart = new ItemStack(Material.NETHER_STAR, amount);
            ItemMeta meta = heart.getItemMeta();
            meta.setDisplayName("§c§l❤ Heart");
            heart.setItemMeta(meta);

            target.getInventory().addItem(heart);
            sender.sendMessage("§aGave " + amount + " Hearts to " + target.getName());
            target.sendMessage("§aYou received " + amount + " Heart(s)!");
            return true;
        }

        return false;
    }

   @SuppressWarnings("unchecked")
    private String fetchLatestVersionFromModrinth(String projectId) {
        try {
            URL url = new URL("https://api.modrinth.com/v2/project/" + projectId + "/version");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "HeartstealPlugin/2.0-ALPHA");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse as JSON array
            JSONParser parser = new JSONParser();
            JSONArray versions = (JSONArray) parser.parse(response.toString());
            if (versions.isEmpty()) return null;

            JSONObject latest = (JSONObject) versions.get(0);
            return (String) latest.get("version_number");

        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to check Modrinth API: " + e.getMessage());
            return null;
        }
    }
}