/*
 * This file is part of HoloAPI.
 *
 * HoloAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoloAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoloAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dsh105.holoapi.listeners;

import com.dsh105.holoapi.HoloAPI;
import com.dsh105.holoapi.api.AnimatedHologram;
import com.dsh105.holoapi.api.Hologram;
import com.dsh105.holoapi.api.MultiColourFormat;
import com.dsh105.holoapi.api.events.HoloLineUpdateEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HoloListener implements Listener {

    @EventHandler
    public void onHoloLineUpdate(HoloLineUpdateEvent event) {
        String key = HoloAPI.getConfig(HoloAPI.ConfigType.MAIN).getString("multicolorFormat.character", "&s");
        if (MultiColourFormat.CACHE.contains(event.getHologram()) && event.getOldLineContent().contains(key)) {

        }
        if (!MultiColourFormat.CACHE.contains(event.getHologram()) && event.getNewLineContent().contains(key)) {
            MultiColourFormat.CACHE.add(event.getHologram());
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        for (Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (event.getTo().getWorld().getName().equals(h.getWorldName())) {
                if (h.getLocationFor(player) != null && h.getVisibility().isVisibleTo(player, h.getSaveId())) {
                    h.show(player, true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if(event.isCancelled()) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() &&
            from.getBlockY() == to.getBlockY() &&
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        String worldName = to.getWorld().getName();
        for (Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (worldName.equals(h.getWorldName())) {
                Vector l = h.getLocationFor(player);
                if (h.isWithinRadius(player)) {
                    if(l == null) {
                        // Lets check that we're not sending the same location to the player...
//                        int x = NumberConversions.floor(h.getDefaultX());
//                        int y = NumberConversions.floor(h.getDefaultY());
//                        int z = NumberConversions.floor(h.getDefaultZ());
//                        if (l != null && l.getBlockX() == x && l.getBlockY() == y && l.getBlockZ() == z) {
//                            return;
//                        }
                        h.show(player, true);
                    }
                } else {
                    h.clear(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (h.getLocationFor(player) != null) {
                h.clear(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        for (final Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (player.getLocation().getWorld().getName().equals(h.getWorldName()) &&
                h.getVisibility().isVisibleTo(player, h.getSaveId())) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (h instanceof AnimatedHologram && !((AnimatedHologram) h).isAnimating()) {
                            ((AnimatedHologram) h).animate();
                        }
                        h.show(player, true);
                    }
                }.runTaskLater(HoloAPI.getCore(), 40L);
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        for (final Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (player.getLocation().getWorld().getName().equals(h.getWorldName()) &&
                h.getVisibility().isVisibleTo(player, h.getSaveId())) {
                if (h instanceof AnimatedHologram && !((AnimatedHologram) h).isAnimating()) {
                    ((AnimatedHologram) h).animate();
                }
                h.show(player, true);
            } else if (event.getFrom().getName().equals(h.getWorldName()) && h.getLocationFor(player) != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        h.clear(player);
                    }
                }.runTaskLater(HoloAPI.getCore(), 20L);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (h.getDefaultLocation().getChunk().equals(event.getChunk())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (h.getDefaultLocation().getChunk().equals(event.getChunk())) {
                for (Entity e : h.getDefaultLocation().getWorld().getEntities()) {
                    if (e instanceof Player) {
                        if (h.getVisibility().isVisibleTo((Player) e, h.getSaveId())) {
                            h.show((Player) e, true);
                        }
                    }
                }
            }
        }
    }
}
