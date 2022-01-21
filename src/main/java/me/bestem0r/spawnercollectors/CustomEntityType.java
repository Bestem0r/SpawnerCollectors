package me.bestem0r.spawnercollectors;

import gcspawners.ASAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;

public class CustomEntityType {

    private boolean custom = false;

    private EntityType entityType;
    private String customType;

    public CustomEntityType(EntityType type) {
        this.entityType = type;
    }

    public CustomEntityType(String type) {
        if (isEntity(type)) {
            this.entityType = EntityType.valueOf(type);
        } else if (Bukkit.getPluginManager().isPluginEnabled("AdvancedSpawners")) {
            if (ASAPI.getCustomMobs().contains(type)) {
                this.customType = type;
                this.custom = true;
            }
        } else {
            throw new IllegalArgumentException("No mob type: " + type);
        }
    }

    private boolean isEntity(String entityTest) {
        for (EntityType entityType : EntityType.values()) {
            if (entityType.name().equals(entityTest)) {
                return true;
            }
        }
        return false;
    }

    public String name() {
        return custom ? customType : entityType.name();
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public boolean isCustom() {
        return custom;
    }
}
