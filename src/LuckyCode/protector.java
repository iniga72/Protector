package LuckyCode;

import LuckyCode.protect.database.Database;
import LuckyCode.protect.database.SQLite;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.*;
import java.io.*;
import org.bukkit.configuration.file.*;
import org.bukkit.command.*;
import com.sk89q.worldguard.bukkit.*;
import com.sk89q.worldguard.protection.regions.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import com.sk89q.worldguard.*;
import java.util.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;

public class protector extends JavaPlugin implements Listener
{
    FileConfiguration config;
    public static Database db;
    private Map<Player, Inventory> inv;
    private Map<String, String> rg;

    public protector() {
        this.config = null;
        this.inv = new HashMap<Player, Inventory>();
        this.rg = new HashMap<String, String>();
    }

    @EventHandler
    public void delete(PlayerCommandPreprocessEvent e){
        if(e.isCancelled()) return;
        String[] ms = e.getMessage().split(" ");
        if(ms.length != 3) return;
        if(e.getMessage().startsWith("/rg remove") || e.getMessage().startsWith("/rg delete") || e.getMessage().startsWith("/region remove") || e.getMessage().startsWith("/rg delete")){
            Player p = e.getPlayer();
            if(WGBukkit.getRegionManager(p.getWorld()).getRegion(ms[2]) == null) return;
            LocalPlayer localPlayer = Objects.requireNonNull(this.getWorldGuard()).wrapPlayer(p);
            if (WGBukkit.getRegionManager(p.getWorld()).getRegion(ms[2]).isOwner(localPlayer)) {
                protector.db.removeProtector(ms[2]);
            }
        }
    }

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        final File file = new File(this.getDataFolder() + File.separator + "config.yml");

        if (!file.exists()) {
            this.getConfig().options().copyDefaults(true);
            this.saveDefaultConfig();
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        db = new SQLite(this);
        db.load();
    }

    public boolean onCommand(final CommandSender sender, final Command cmd, final String cmdlabel, final String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof Player) {
                final Player p = (Player)sender;
                if (!p.hasPermission("protector.reload")) {
                    p.sendMessage(this.config.getString("messages.permission").replace("&", "§"));
                    return true;
                }
            }
            final File file = new File(this.getDataFolder() + File.separator + "config.yml");
            this.config = (FileConfiguration)YamlConfiguration.loadConfiguration(file);
            sender.sendMessage("§aReload completed.");
            return true;
        }
        if (!(sender instanceof Player)) {
            return true;
        }
        final Player p = (Player)sender;
        if (!p.hasPermission("protector.protect")) {
            p.sendMessage(this.config.getString("messages.permission").replace("&", "§"));
            return true;
        }
        final ArrayList<String> regions = new ArrayList<String>();
        final LocalPlayer localPlayer = Objects.requireNonNull(this.getWorldGuard()).wrapPlayer(p);
        for (final String s : WGBukkit.getRegionManager(p.getWorld()).getRegions().keySet()) {
            if (Objects.requireNonNull(WGBukkit.getRegionManager(p.getWorld()).getRegion(s)).isOwner(localPlayer)) {
                regions.add(s);
            }
        }
        if (regions == null) {
            p.sendMessage(this.config.getString("messages.noregions").replace("&", "§"));
            return true;
        }
        Inventory i = this.inv.get(p);
        final String name = this.config.getString("menu.name");
        final int itemssize = regions.size() / 9 + 1;
        i = Bukkit.createInventory((InventoryHolder)null, itemssize * 9, name);
        int is = 0;
        for (final String s2 : regions) {
            final List<String> dosc = (List<String>)this.config.getStringList("menu.discription");
            final ArrayList<String> desc = new ArrayList<String>();
            String status = this.config.getString("menu.status.disable");
            ItemStack item = new ItemStack(Material.WOOL, 1, (short)14);
            if (protector.db.getProtector(s2) != null && protector.db.getProtector(s2).equalsIgnoreCase(p.getName().toLowerCase())) {
                status = this.config.getString("menu.status.enable");
                item = new ItemStack(Material.WOOL, 1, (short)5);
            }
            for (final String l : dosc) {
                desc.add(l.replace("$status", status).replace("&", "§"));
            }
            final String itemname = this.config.getString("menu.itemname").replace("$region", s2).replace("&", "§");
            final ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(itemname);
            meta.setLore((List)desc);
            item.setItemMeta(meta);
            i.setItem(is, item);
            this.rg.put(p.getName() + "_" + is, s2);
            ++is;
        }
        this.inv.put(p, i);
        p.openInventory(i);
        return true;
    }

    private WorldGuardPlugin getWorldGuard() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null;
        }
        return (WorldGuardPlugin)plugin;
    }

    @EventHandler
    public void click(final InventoryClickEvent e) {
        final Inventory i = this.inv.get(e.getWhoClicked());
        if (e.getCurrentItem() == null) {
            return;
        }
        if (e.getCurrentItem().getTypeId() == 0) {
            return;
        }
        if (e.getCurrentItem().getItemMeta().getDisplayName() == null) {
            return;
        }
        if (i == null) {
            return;
        }
        e.setCancelled(true);
        final String region = this.rg.get(e.getWhoClicked().getName() + "_" + e.getSlot());
        ItemStack item = null;
        String status = this.config.getString("menu.status.enable");
        if (protector.db.getProtector(region) != null && protector.db.getProtector(region).equalsIgnoreCase(e.getWhoClicked().getName().toLowerCase())) {
            status = this.config.getString("menu.status.disable");
            item = new ItemStack(Material.WOOL, 1, (short)14);
            protector.db.removeProtector(region);
        }
        else {
            protector.db.setProtector(region, e.getWhoClicked().getName().toLowerCase());
            item = new ItemStack(Material.WOOL, 1, (short)5);
        }
        final List<String> dosc = (List<String>)this.config.getStringList("menu.discription");
        final ArrayList<String> desc = new ArrayList<String>();
        for (final String l : dosc) {
            desc.add(l.replace("$status", status).replace("&", "§"));
        }
        final String itemname = this.config.getString("menu.itemname").replace("$region", region).replace("&", "§");
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(itemname);
        meta.setLore((List)desc);
        item.setItemMeta(meta);
        i.setItem(e.getSlot(), item);
    }

    @EventHandler
    public void BlockBreakEvent(final BlockBreakEvent e) {
        final Player p = e.getPlayer();
        if(config.getBoolean("bypass") && p.hasPermission("protector.bypass"))return;
        LocalPlayer localPlayer = Objects.requireNonNull(this.getWorldGuard()).wrapPlayer(p);
        for (final ProtectedRegion r : WGBukkit.getRegionManager(p.getWorld()).getApplicableRegions(e.getBlock().getLocation())) {
            if (protector.db.getProtector(r.getId()) != null) {
                if (!Objects.requireNonNull(WGBukkit.getRegionManager(p.getWorld()).getRegion(r.getId())).isOwner(localPlayer) && !Objects.requireNonNull(WGBukkit.getRegionManager(p.getWorld()).getRegion(r.getId())).isMember(localPlayer)) {
                    e.setCancelled(true);
                    p.sendMessage(this.config.getString("messages.pretect").replace("&", "§"));
                }
            }
        }
    }

    @EventHandler
    public void BlockPlaceEvent(final BlockPlaceEvent e) {
        final Player p = e.getPlayer();
        if(config.getBoolean("bypass") && p.hasPermission("protector.bypass"))return;
        LocalPlayer localPlayer = Objects.requireNonNull(this.getWorldGuard()).wrapPlayer(p);
        for (final ProtectedRegion r : WGBukkit.getRegionManager(p.getWorld()).getApplicableRegions(e.getBlock().getLocation())) {
            if (protector.db.getProtector(r.getId()) != null) {
                if (!Objects.requireNonNull(WGBukkit.getRegionManager(p.getWorld()).getRegion(r.getId())).isOwner(localPlayer) && !Objects.requireNonNull(WGBukkit.getRegionManager(p.getWorld()).getRegion(r.getId())).isMember(localPlayer)) {
                    e.setCancelled(true);
                    p.sendMessage(this.config.getString("messages.pretect").replace("&", "§"));
                }
            }
        }
    }

    @EventHandler
    public void onclose(final InventoryCloseEvent e) {
        this.inv.remove(e.getPlayer());
    }
}
