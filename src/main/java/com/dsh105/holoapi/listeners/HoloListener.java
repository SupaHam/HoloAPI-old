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
import com.dsh105.holoapi.api.events.HoloLineUpdateEvent;
import com.dsh105.holoapi.config.Settings;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class HoloListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHoloLineUpdate(HoloLineUpdateEvent event) {
        if (HoloAPI.getHoloUpdater().getTrackedHolograms().contains(event.getHologram())) {
            if (!HoloAPI.getHoloUpdater().shouldTrack(event.getNewLineContent())) {
                HoloAPI.getHoloUpdater().track(event.getHologram(), false);
            }
        } else {
            HoloAPI.getHoloUpdater().track(event.getHologram());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (h.getLocationFor(player) != null) {
                h.clear(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        for (final Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (player.getLocation().getWorld().getName().equals(h.getWorldName()) && h.getVisibility().isVisibleTo(player, h.getSaveId())) {
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        for (final Hologram h : HoloAPI.getManager().getAllHolograms().keySet()) {
            if (player.getLocation().getWorld().getName().equals(h.getWorldName()) && h.getVisibility().isVisibleTo(player, h.getSaveId())) {
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
