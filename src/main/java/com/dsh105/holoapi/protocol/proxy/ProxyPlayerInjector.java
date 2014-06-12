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

package com.dsh105.holoapi.protocol.proxy;

import com.dsh105.holoapi.protocol.InjectionManager;
import com.dsh105.holoapi.protocol.PlayerInjector;
import com.dsh105.holoapi.protocol.PlayerUtil;
import com.dsh105.holoapi.reflection.Constants;
import com.dsh105.holoapi.reflection.FieldAccessor;
import com.dsh105.holoapi.reflection.SafeField;
import com.dsh105.holoapi.reflection.utility.CommonReflection;
import com.dsh105.holoapi.util.ReflectionUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingQueue;
import org.bukkit.entity.Player;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ProxyPlayerInjector extends ForwardingQueue implements PlayerInjector {

    // Fields
    // The Object lock
    private final FieldAccessor<Object> h_lock = new SafeField<Object>(ReflectionUtil.getNMSClass("NetworkManager"), Constants.NETWORK_FIELD_LOCK.getName());  // Those fields haven't changed in 50 years!
    // The inboundQueue field
    private final FieldAccessor<Queue> inboundQueue = new SafeField<Queue>(ReflectionUtil.getNMSClass("NetworkManager"), Constants.NETWORK_FIELD_INBOUNDQUEUE.getName());

    private Player player;

    private InjectionManager injectionManager;

    private boolean injected;

    private boolean closed;

    private boolean exempted;

    private Object nmsHandle;

    private Object playerConnection;
    private Object networkManager;

    protected Queue oldQueue;
    protected Queue delegate;

    protected Object lock;

    public ProxyPlayerInjector(final Player player, final InjectionManager injectionManager) {
        Preconditions.checkNotNull(player, "Player can't be NULL!");
        Preconditions.checkNotNull(injectionManager, "InjectionManager can't be NULL!");

        this.player = player;
        this.injectionManager = injectionManager;

        this.networkManager = getNetworkManager();
    }

    private Object getLock() {
        if (this.lock != null) {
            return this.lock;
        }
        return this.lock = h_lock.get(getNetworkManager());
    }

    @Override
    public Object getNmsHandle() {
        if (this.nmsHandle != null) {
            return this.nmsHandle;
        }
        return this.nmsHandle = PlayerUtil.toNMS(this.player);
    }

    @Override
    public Object getPlayerConnection() {
        if (this.playerConnection != null) {
            return this.playerConnection;
        }
        return this.playerConnection = PlayerUtil.getPlayerConnection(getNmsHandle());
    }

    @Override
    public Object getNetworkManager() {
        if (this.networkManager != null) {
            return this.networkManager;
        }
        return this.networkManager = PlayerUtil.getNetworkManager(getPlayerConnection());
    }

    @Override
    public boolean inject() {
        synchronized (getLock()) {
            if (this.closed)
                return false;

            oldQueue = inboundQueue.get(getNetworkManager());

            if (oldQueue == null)
                throw new IllegalStateException("InboundQueue is NULL for player: " + this.player.getName());  // Deprecation; Ugh;

            this.delegate = new ConcurrentLinkedQueue();
            delegate.addAll(oldQueue);

            // Swap the fields
            inboundQueue.set(getNetworkManager(), delegate());
            injected = true;

            return true;
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            try {
                oldQueue.addAll(delegate);
                inboundQueue.set(getNetworkManager(), oldQueue);
                this.closed = true;
            } catch (Exception e) {
                // Failed to re-swap the queue :'(
                this.closed = false;
                throw new RuntimeException("Failed to re-swap the queue for player: " + player.getName());
            }
        }
    }

    @Override
    public Player getPlayer() {
        return this.player;
    }

    @Override
    public void setPlayer(Player player) {
        Preconditions.checkNotNull(player, "Player can't be NULL!");

        this.player = player;

        this.networkManager = getNetworkManager();
    }

    @Override
    public void sendPacket(Object packet) {
        // TODO: send packet here. I have no idea if it's even necessary as we will drop 1.6 support
    }

    @Override
    public boolean isInjected() {
        return this.injected;
    }

    @Override
    public boolean isExempted() {
        return this.exempted;
    }

    @Override
    public void setExempted(boolean state) {
        this.exempted = state;
    }

    @Override
    protected Queue delegate() {
        return this.delegate;
    }

    @Override
    public boolean add(Object packet) {
        if (isExempted())
            return delegate().add(packet);
        if (packet.getClass().getSimpleName().equals(CommonReflection.getEntityUsePacket().getSimpleName())) {
            //Handle packet add here
        }
        return delegate().add(packet);
    }
}
