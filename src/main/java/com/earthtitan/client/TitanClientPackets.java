package com.earthtitan.client;

import com.earthtitan.network.TitanNetwork;

/** Loaded only through TitanNetwork's safe physical-client DistExecutor callback. */
public final class TitanClientPackets {
    private TitanClientPackets() {
    }

    public static void install() {
        TitanNetwork.installClientReceivers(TitanClientEffects::receiveAbsorb, TitanClientEffects::receiveEffect);
    }
}
