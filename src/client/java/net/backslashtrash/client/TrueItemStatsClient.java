package net.backslashtrash.client;

import net.backslashtrash.client.tooltip.TooltipHandler;
import net.fabricmc.api.ClientModInitializer;

public class TrueItemStatsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TooltipHandler.register();
    }
}