package me.bestem0r.spawnercollectors.menus;


import me.bestem0r.spawnercollectors.utilities.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class MenuItem extends ItemStack {

    public static class Builder {

        private final Material material;
        private String name = "";
        private ArrayList<String> lore = new ArrayList<>();

        public Builder(Material material) {
            this.material = material;
        }

        public Builder nameFromPath(String path) {
            this.name = new Color.Builder().path(path).build();
            return this;
        }
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder lore(ArrayList<String> lore) {
            this.lore = lore;
            return this;
        }

        public MenuItem build() {
            MenuItem item = new MenuItem(material);

            ItemMeta itemMeta = item.getItemMeta();

            if (itemMeta != null) {
                if (!name.equals("")) itemMeta.setDisplayName(name);
                itemMeta.setLore(lore);
                item.setItemMeta(itemMeta);
            }
            return item;
        }
    }
    private MenuItem(Material material) {
        super(material);
    }
}
