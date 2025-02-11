package de.craftsblock.cnet.modules.gateway.entities;

import de.craftsblock.cnet.modules.gateway.Gateway;
import de.craftsblock.craftscore.utils.id.Snowflake;
import de.craftsblock.craftsnet.CraftsNet;
import org.jetbrains.annotations.NotNull;

public abstract class Entity {

    private final Gateway gateway;

    private final long id;
    private final String idHex;

    public Entity(Gateway gateway) {
        this(gateway, Snowflake.generate());
    }

    protected Entity(Gateway gateway, long id) {
        this.gateway = gateway;
        this.id = id;
        this.idHex = Long.toHexString(this.id);
    }

    public Gateway getGateway() {
        return gateway;
    }

    public CraftsNet getCraftsNet() {
        return gateway.craftsNet();
    }

    public @NotNull String getId() {
        return String.valueOf(this.id);
    }

    public long getIdLong() {
        return this.id;
    }

    public String getIdHex() {
        return this.idHex;
    }

}
