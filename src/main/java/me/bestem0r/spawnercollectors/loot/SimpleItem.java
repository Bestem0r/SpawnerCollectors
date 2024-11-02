/*
 * Copyright (c) 2022. Vebjørn Viem Elvekrok
 * All rights reserved.
 */

package me.bestem0r.spawnercollectors.loot;

import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SimpleItem {

    private final ItemStack item;

    public SimpleItem(String s) {
        Material material;
        short durability;
        if (s.contains(":")) {
            material = Material.valueOf(s.split(":")[0]);
            durability = (short) Integer.parseInt(s.split(":")[1]);
        } else {
            material = Material.valueOf(s);
            durability = 0;
        }
        this.item = new ItemStack(material, 1, durability);
    }

    public SimpleItem(ItemStack i) {
        this.item = i.clone();
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public void setAmount(int amount) {
        item.setAmount(amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof SimpleItem) {
            SimpleItem i = (SimpleItem) obj;
            return i.getMaterial() == getMaterial() && i.getDurability() == getDurability();
        }
        
        if (obj instanceof ItemStack) {
            ItemStack i = (ItemStack) obj;
            return SpawnerUtils.compareItems(item, i);
        }
        
        return false;
    }

    @Override
    public String toString() {
        return item.getDurability() == 0 ? item.getType().name() : item.getType().name() + ":" + item.getDurability();
    }

    @Override
    public int hashCode() {
        return getMaterial().hashCode() + item.getDurability();
    }

    public Material getMaterial() {
        return item.getType();
    }

    public SimpleItem clone() {
        return new SimpleItem(item);
    }

    public double getDurability() {
        return item.getDurability();
    }
}
