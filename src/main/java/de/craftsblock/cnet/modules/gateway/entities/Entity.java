package de.craftsblock.cnet.modules.gateway.entities;

import de.craftsblock.craftscore.utils.id.Snowflake;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public abstract class Entity {

    private final long id;
    private final String idHex;

    public Entity() {
        this(Snowflake.generate());
    }

    public Entity(@NotNull ByteBuffer buffer) {
        this(buffer.readLong());
    }

    protected Entity(long id) {
        this.id = id;
        this.idHex = Long.toHexString(this.id);
    }

    public @NotNull String getId() {
        return String.valueOf(this.id);
    }

    public long getIdLong() {
        return this.id;
    }

    public String getLongHex() {
        return this.idHex;
    }

    public void write(@NotNull ByteBuffer buffer) {
        buffer.writeLong(this.getIdLong());
    }

}
