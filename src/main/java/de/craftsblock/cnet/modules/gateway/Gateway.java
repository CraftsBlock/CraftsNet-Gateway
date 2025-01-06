package de.craftsblock.cnet.modules.gateway;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.meta.annotations.Meta;
import de.craftsblock.craftsnet.builder.ActivateType;

import java.io.IOException;

@Meta(name = "CNetGateway")
public class Gateway extends Addon {

    public static void main(String[] args) throws IOException {
        CraftsNet.create(Gateway.class)
                .withArgs(args)
                .withWebServer(ActivateType.ENABLED)
                .withWebSocketServer(ActivateType.ENABLED)
                .build();
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

}
