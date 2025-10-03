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

import java.util.ArrayList;
import java.util.List;

public class HeartstealPlugin extends JavaPlugin implements Listener {

    private List<Player> deadPlayers = new ArrayList<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        // Create custom Heart item
        ItemStack heart = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = heart.getItemMeta();
        meta.setDisplayName("§c❤ Heart");
        heart.setItemMeta(meta);

        // Heart item recipe
        ItemStack heartRecipeItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta heartMeta = heartRecipeItem.getItemMeta();
        heartMeta.setDisplayName("§c❤ Heart");
        heartRecipeItem.setItemMeta(heartMeta);

        ShapedRecipe heartRecipe = new ShapedRecipe(heartRecipeItem);
        heartRecipe.shape("DDD", "GNG", "DDD");
        heartRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        heartRecipe.setIngredient('N', Material.NETHER_STAR);
        heartRecipe.setIngredient('G', Material.GOLD_INGOT);
        Bukkit.addRecipe(heartRecipe);

        // Revive Heart item
        ItemStack reviveHeart = new ItemStack(Material.NETHER_STAR);
        ItemMeta reviveMeta = reviveHeart.getItemMeta();
        reviveMeta.setDisplayName("§bRevive Heart");
        reviveHeart.setItemMeta(reviveMeta);

        // Crafting recipe
        ShapedRecipe reviveRecipe = new ShapedRecipe(reviveHeart);
        reviveRecipe.shape("DAD", "EGE", "DAD");
        reviveRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        reviveRecipe.setIngredient('A', Material.GOLDEN_APPLE);
        reviveRecipe.setIngredient('E', Material.EMERALD);
        reviveRecipe.setIngredient('G', Material.GOLD_BLOCK);
        Bukkit.addRecipe(reviveRecipe);

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
            // PvP kill: steal heart
            if (victimMax > 2.0) victim.setMaxHealth(victimMax - 2.0);
            killer.setMaxHealth(killer.getMaxHealth() + 2.0);

            killer.sendMessage("§aYou stole a heart from " + victim.getName() + "!");
            victim.sendMessage("§cYou got killed by " + killer.getName() + " and lost a heart!");
        } else {
            // Natural death: remove a heart
            if (victimMax > 2.0) {
                victim.setMaxHealth(victimMax - 2.0);
                victim.sendMessage("§cYou died and lost a heart!");
            }
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

        if ("§c❤ Heart".equals(name)) {
            event.setCancelled(true);
            item.setAmount(item.getAmount() - 1);
            player.setItemInHand(item);

            player.setMaxHealth(player.getMaxHealth() + 2.0);
            player.sendMessage("§aYou used a Heart and gained +1 max health!");
        }

        // Revive Heart usage
        if ("§bRevive Heart".equals(name)) {
            event.setCancelled(true);

            Inventory gui = Bukkit.createInventory(null, 9, "§bRevive Players");

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
    }

    // Revive GUI click
    @EventHandler
    public void onReviveGUIClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§bRevive Players")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        if (!(event.getCurrentItem().getItemMeta() instanceof SkullMeta)) return;

        SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
        String playerName = meta.getDisplayName().replace("§a", "");
        Player toRevive = Bukkit.getPlayer(playerName);
        if (toRevive == null) {
            event.getWhoClicked().sendMessage("§cThat player is not online!");
            return;
        }

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
            meta.setDisplayName("§c❤ Heart");
            heart.setItemMeta(meta);

            player.getInventory().addItem(heart);
            player.sendMessage("§aYou withdrew a heart!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("info")) {
            sender.sendMessage("§aHeartsteal");
            sender.sendMessage("§aVersion: 1.0-ALPHA");
            sender.sendMessage("§aAuthor: Verxus");
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
            meta.setDisplayName("§c❤ Heart");
            heart.setItemMeta(meta);

            target.getInventory().addItem(heart);
            sender.sendMessage("§aGave " + amount + " Hearts to " + target.getName());
            target.sendMessage("§aYou received " + amount + " Heart(s)!");
            return true;
        }

        return false;
    }
}