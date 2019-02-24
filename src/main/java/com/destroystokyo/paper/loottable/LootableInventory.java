package com.destroystokyo.paper.loottable;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents an Inventory that contains a Loot Table associated to it that will
 * automatically fill on first open.
 *
 * A new feature and API is provided to support automatically refreshing the contents
 * of the inventory based on that Loot Table after a configurable amount of time has passed.
 *
 * The behavior of how the Inventory is filled based on the loot table may vary based
 * on Minecraft versions and the Loot Table feature.
 */
public interface LootableInventory extends Lootable {

    /**
     * Server owners have to enable whether or not an object in a world should refill
     *
     * @return If the world this inventory is currently in has Replenishable Lootables enabled
     */
    boolean isRefillEnabled();

    /**
     * Whether or not this object has ever been filled
     *
     * @return Has ever been filled
     */
    boolean hasBeenFilled();

    /**
     * Has this player ever looted this block
     *
     * @param player The player to check
     * @return Whether or not this player has looted this block
     */
    default boolean hasPlayerLooted(Player player) {
        return hasPlayerLooted(player.getUniqueId());
    }

    /**
     * Has this player ever looted this block
     *
     * @param player The player to check
     * @return Whether or not this player has looted this block
     */
    boolean hasPlayerLooted(UUID player);

    /**
     * Gets the timestamp, in milliseconds, of when the player last looted this object
     *
     * @param player The player to check
     * @return Timestamp last looted, or null if player has not looted this object
     */
    default Long getLastLooted(Player player) {
        return getLastLooted(player.getUniqueId());
    }

    /**
     * Gets the timestamp, in milliseconds, of when the player last looted this object
     *
     * @param player The player to check
     * @return Timestamp last looted, or null if player has not looted this object
     */
    Long getLastLooted(UUID player);

    /**
     * Change the state of whether or not a player has looted this block
     *
     * @param player The player to change state for
     * @param looted true to add player to looted list, false to remove
     * @return The previous state of whether the player had looted this or not
     */
    default boolean setHasPlayerLooted(Player player, boolean looted) {
        return setHasPlayerLooted(player.getUniqueId(), looted);
    }

    /**
     * Change the state of whether or not a player has looted this block
     *
     * @param player The player to change state for
     * @param looted true to add player to looted list, false to remove
     * @return The previous state of whether the player had looted this or not
     */
    boolean setHasPlayerLooted(UUID player, boolean looted);

    /**
     * Returns Whether or not this object has been filled and now has a pending refill
     *
     * @return Has pending refill
     */
    boolean hasPendingRefill();

    /**
     * Gets the timestamp in milliseconds that the Lootable object was last refilled
     *
     * @return -1 if it was never refilled, or timestamp in milliseconds
     */
    long getLastFilled();

    /**
     * Gets the timestamp in milliseconds that the Lootable object will refill
     *
     * @return -1 if it is not scheduled for refill, or timestamp in milliseconds
     */
    long getNextRefill();

    /**
     * Sets the timestamp in milliseconds of the next refill for this object
     *
     * @param refillAt timestamp in milliseconds. -1 to clear next refill
     * @return The previous scheduled time to refill, or -1 if was not scheduled
     */
    long setNextRefill(long refillAt);
}
