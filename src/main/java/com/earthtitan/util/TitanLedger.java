package com.earthtitan.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Dimension-local reservations survive chunk unloading and world restarts. */
public final class TitanLedger extends SavedData {
    private final Set<UUID> titans = new HashSet<>();

    public static TitanLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TitanLedger::load, TitanLedger::new, "earthtitan_population");
    }

    private static TitanLedger load(CompoundTag tag) {
        TitanLedger result = new TitanLedger();
        for (Tag entry : tag.getList("Titans", Tag.TAG_COMPOUND)) {
            CompoundTag data = (CompoundTag) entry;
            if (data.hasUUID("Id")) result.titans.add(data.getUUID("Id"));
        }
        return result;
    }

    public boolean reserve(UUID id, int max) {
        if (titans.contains(id)) return true;
        if (titans.size() >= max) return false;
        titans.add(id);
        setDirty();
        return true;
    }

    public int count() { return titans.size(); }

    public void release(UUID id) {
        if (titans.remove(id)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (UUID id : titans) {
            CompoundTag item = new CompoundTag();
            item.putUUID("Id", id);
            list.add(item);
        }
        tag.put("Titans", list);
        return tag;
    }
}
