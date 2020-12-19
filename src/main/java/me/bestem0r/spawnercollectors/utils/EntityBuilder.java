package me.bestem0r.spawnercollectors.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EntityBuilder {

    private EntityBuilder() {}

    public static Entity createEntity(EntityType entityType, Location location) {
        try {
            Class<?> craftWorldClass = getNMS("org.bukkit.craftbukkit.", "CraftWorld");
            Object craftWorldObject = craftWorldClass.cast(location.getWorld());
            Method createEntityMethod = craftWorldObject.getClass().getMethod("createEntity", Location.class, Class.class);

            Object entity = createEntityMethod.invoke(craftWorldObject, location, entityType.getEntityClass());

            return (Entity) entity.getClass().getMethod("getBukkitEntity").invoke(entity);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) { }
        return null;
    }
    public static Class<?> getNMS(String prefix, String nmsClassString) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
        String name = prefix + version + nmsClassString;
        return Class.forName(name);
    }
}

