/*
 * Copyright (c) 2024. Vebjørn Viem Elvekrok
 * All rights reserved.
 */

package me.bestem0r.spawnercollectors.loot;

import org.bukkit.inventory.ItemStack;

public class ItemCompression {

    private final SimpleItem itemForCompression;
    private final int amount;
    private final ItemStack compressedItem;

    public ItemCompression(SimpleItem itemForCompression, int amount, ItemStack compressedItem) {
        this.itemForCompression = itemForCompression;
        this.amount = amount;
        this.compressedItem = compressedItem.clone();
    }

    public boolean shouldCompress(SimpleItem item, int amount) {
        return itemForCompression.equals(item) && amount >= this.amount;
    }

    public ItemStack getCompressedItem() {
        return compressedItem.clone();
    }

    public int getAmount() {
        return amount;
    }
}
