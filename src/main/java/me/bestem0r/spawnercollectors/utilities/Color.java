package me.bestem0r.spawnercollectors.utilities;

import me.bestem0r.spawnercollectors.SCPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Color {

    public static class Builder {

        private String path = null;
        private List<String> lore = null;
        private HashMap<String, String> replaceList = new HashMap<>();
        private boolean addPredix = false;

        private final FileConfiguration config;

        public Builder(SCPlugin plugin) {
            this.config = plugin.getConfig();
        }

        public Builder(SCPlugin plugin, List<String> lore) {
            this.config = plugin.getConfig();
            this.lore = lore;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }
        public Builder replace(String replace, String value) {
            replaceList.put(replace, value);
            return this;
        }
        public Builder replaceWithCurrency(String replace, String value) {
            String currency = config.getString("currency");
            String valueCurrency = (config.getBoolean("currency_before") ? currency + value : value + currency);
            replaceList.put(replace, valueCurrency);
            return this;
        }
        public Builder addPrefix() {
            this.addPredix = true;
            return this;
        }

        public String build() {
            String text = config.getString(path);
            if (text == null) return path;
            for (String replace : replaceList.keySet()) {
                text = text.replace(replace, replaceList.get(replace));
            }
            if (addPredix) {
                String prefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix"));
                return prefix + " " + ChatColor.translateAlternateColorCodes('&', text);
            } else {
                return ChatColor.translateAlternateColorCodes('&', text);
            }
        }

        public ArrayList<String> buildLore() {
            List<String> loreList = (lore == null ? config.getStringList(path) : lore);
            ArrayList<String> returnLore = new ArrayList<>();
            for (String lore : loreList) {
                for (String replace : replaceList.keySet()) {
                    lore = lore.replace(replace, replaceList.get(replace));
                }
                returnLore.add(ChatColor.translateAlternateColorCodes('&', lore));
            }
            return returnLore;
        }
    }
}
