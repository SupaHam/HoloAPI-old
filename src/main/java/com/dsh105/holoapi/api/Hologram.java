package com.dsh105.holoapi.api;

import com.dsh105.holoapi.util.TagIdGenerator;
import com.dsh105.holoapi.util.wrapper.*;
import net.minecraft.server.v1_7_R1.DataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Hologram {

    private String worldName;

    private double defX;
    private double defY;
    private double defZ;
    private String[] tags;
    private static double LINE_SPACING = 0.25D;

    private int firstTagId;
    private String saveId;

    private boolean visibleToAll = true;

    protected HashMap<String, Vector> playerToLocationMap = new HashMap<String, Vector>();
    protected HashMap<TagSize, String> imageIdMap = new HashMap<TagSize, String>();

    protected Hologram(String saveId, String worldName, double x, double y, double z, String... lines) {
        this(worldName, x, y, z);
        this.saveId = saveId;
        this.tags = lines;
        this.firstTagId = TagIdGenerator.nextId(this.tags.length);
    }

    private Hologram(String worldName, double x, double y, double z) {
        this.worldName = worldName;
        this.defX = x;
        this.defY = y;
        this.defZ = z;
    }

    public int getTagCount() {
        return this.tags.length;
    }

    public double getDefaultX() {
        return defX;
    }

    public double getDefaultY() {
        return defY;
    }

    public double getDefaultZ() {
        return defZ;
    }

    public String getWorldName() {
        return worldName;
    }

    public Location getDefaultLocation() {
        return new Location(Bukkit.getWorld(this.getWorldName()), this.getDefaultX(), this.getDefaultY(), this.getDefaultZ());
    }

    public HashMap<String, Vector> getPlayerViews() {
        HashMap<String, Vector> map = new HashMap<String, Vector>();
        map.putAll(this.playerToLocationMap);
        return map;
    }

    public String[] getLines() {
        return tags;
    }

    // TODO: Fully implement this
    /*public boolean isVisibleToAll() {
        return visibleToAll;
    }

    public void setVisibleToAll(boolean flag) {
        this.visibleToAll = flag;
    }*/

    public static double getSpacing() {
        return LINE_SPACING;
    }

    public String getSaveId() {
        return saveId;
    }

    protected void setSaveId(String saveId) {
        this.saveId = saveId;
    }

    /*public int getFirstTagId() {
        return firstTagId;
    }*/

    protected void setImageTagMap(HashMap<TagSize, String> map) {
        this.imageIdMap = map;
    }

    protected HashMap<String, Boolean> serialise() {
        HashMap<String, Boolean> map = new HashMap<String, Boolean>();
        ArrayList<String> tags = new ArrayList<String>();
        for (String s : this.tags) {
            tags.add(s);
        }
        boolean cont = true;
        int index = 0;
        while (cont) {
            if (index >= tags.size()) {
                cont = false;
            } else {
                String tag = tags.get(index);
                Map.Entry<TagSize, String> entry = getImageIdOfIndex(index);
                if (entry != null) {
                    index += entry.getKey().getLast() - entry.getKey().getFirst();
                    map.put(entry.getValue(), true);
                } else {
                    map.put(tag, false);
                }
            }
            index++;
        }
        return map;
    }

    protected Map.Entry<TagSize, String> getImageIdOfIndex(int index) {
        for (Map.Entry<TagSize, String> entry : this.imageIdMap.entrySet()) {
            if (entry.getKey().getFirst() == index) {
                return entry;
            }
        }
        return null;
    }

    protected void clearPlayerLocationMap() {
        Iterator<String> i = this.playerToLocationMap.keySet().iterator();
        while (i.hasNext()) {
            Player p = Bukkit.getPlayerExact(i.next());
            if (p != null) {
                int[] ids = new int[this.getTagCount()];

                for (int j = 0; j < this.getTagCount(); j++) {
                    ids[j] = j;
                }
                clearTags(p, ids);
            }
            i.remove();
        }
    }

    public Vector getLocationFor(Player player) {
        return this.playerToLocationMap.get(player.getName());
    }

    public void show(Player observer) {
        this.show(observer, (int) this.getDefaultX(), (int) this.getDefaultY(), (int) this.getDefaultZ());
    }

    public void show(Player observer, Location location) {
        this.show(observer, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public void show(Player observer, int x, int y, int z) {
        /*if (this.playerToLocationMap.containsKey(observer.getName())) {
            this.move(observer, new Vector(x, y, z));
        }*/
        for (int index = 0; index < this.getTagCount(); index++) {
            this.generate(observer, index, -index * LINE_SPACING, x, y, z);
        }
        this.playerToLocationMap.put(observer.getName(), new Vector(x, y, z));
    }

    public void move(Player observer, Location to) {
        this.move(observer, to.toVector());
    }

    public void move(Player observer, Vector vector) {
        /*Vector loc = vector.clone();
        for (int i = 0; i < this.getTagCount(); i++) {
            this.moveTag(observer, i, loc);
            loc.setY(loc.getY() - LINE_SPACING);
        }
        this.playerToLocationMap.put(observer.getName(), vector);*/
        this.show(observer, (int) vector.getX(), (int) vector.getY(), (int) vector.getZ());
    }

    public void clear(Player observer) {
        int[] ids = new int[this.getTagCount()];

        for (int i = 0; i < this.getTagCount(); i++) {
            ids[i] = i;
        }
        clearTags(observer, ids);
        this.playerToLocationMap.remove(observer.getName());
    }

    protected void clearTags(Player observer, int... indices) {
        WrapperPacketEntityDestroy destroy = new WrapperPacketEntityDestroy();
        int[] entityIds = new int[indices.length * 2];

        for (int i = 0; i < indices.length; i++) {
            if (indices[i] <= this.getTagCount()) {
                entityIds[i * 2] = this.getHorseIndex(indices[i]);
                entityIds[i * 2 + 1] = this.getSkullIndex(indices[i] * 2);
            }
        }
        destroy.setEntities(entityIds);
        destroy.send(observer);
    }

    protected void moveTag(Player observer, int index, Vector to) {
        WrapperPacketEntityTeleport teleportHorse = new WrapperPacketEntityTeleport();
        teleportHorse.setEntityId(this.getHorseIndex(index));
        teleportHorse.setX(to.getX());
        teleportHorse.setY(to.getY());
        teleportHorse.setZ(to.getZ());

        WrapperPacketEntityTeleport teleportSkull = new WrapperPacketEntityTeleport();
        teleportSkull.setEntityId(this.getSkullIndex(index));
        teleportSkull.setX(to.getX());
        teleportSkull.setY(to.getY());
        teleportSkull.setZ(to.getZ());

        teleportHorse.send(observer);
        teleportSkull.send(observer);
    }

    protected void generate(Player observer, int index, double diffY, int x, int y, int z) {
        WrapperPacketAttachEntity attach = new WrapperPacketAttachEntity();

        WrapperPacketSpawnEntityLiving horse = new WrapperPacketSpawnEntityLiving();
        horse.setEntityId(this.getHorseIndex(index));
        horse.setEntityType(EntityType.HORSE.getTypeId());
        horse.setX(x);
        horse.setY(y + diffY + 55);
        horse.setZ(z);

        DataWatcher dw = new DataWatcher(null);
        dw.a(10, this.tags[index]);
        dw.a(11, Byte.valueOf((byte) 1));
        dw.a(12, Integer.valueOf(-1700000));
        horse.setDataWatcher(dw);

        WrapperPacketSpawnEntity skull = new WrapperPacketSpawnEntity();
        skull.setEntityId(this.getSkullIndex(index));
        skull.setX(x);
        skull.setY(y + diffY + 55);
        skull.setZ(z);
        skull.setEntityType(66);

        attach.setEntityId(horse.getEntityId());
        attach.setVehicleId(skull.getEntityId());

        horse.send(observer);
        skull.send(observer);
        attach.send(observer);
    }

    private int getHorseIndex(int index) {
        return firstTagId + index * 2;
    }

    private int getSkullIndex(int index) {
        return firstTagId + index * 2 + 1;
    }

}
