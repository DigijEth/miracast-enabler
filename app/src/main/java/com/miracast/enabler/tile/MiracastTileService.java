package com.miracast.enabler.tile;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.miracast.enabler.util.WfdManager;

/**
 * Quick Settings tile for Miracast.
 *
 * Tap behavior:
 * - If disconnected: opens the Cast settings screen (where WFD sinks appear)
 * - If connected: disconnects the current WFD session
 *
 * The tile state reflects the current WFD connection status.
 */
public class MiracastTileService extends TileService {

    private WfdManager wfdManager;

    @Override
    public void onCreate() {
        super.onCreate();
        wfdManager = new WfdManager(this);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();

        int state = wfdManager.getConnectionState();
        if (state == WfdManager.STATE_CONNECTED) {
            wfdManager.disconnect();
            updateTileState();
        } else {
            // Open Cast settings where WFD sinks will now appear
            Intent intent = new Intent("android.settings.CAST_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        int state = wfdManager.getConnectionState();
        switch (state) {
            case WfdManager.STATE_CONNECTED:
                tile.setState(Tile.STATE_ACTIVE);
                String displayName = wfdManager.getActiveDisplayName();
                tile.setSubtitle(displayName != null ? displayName : "Connected");
                break;
            case WfdManager.STATE_CONNECTING:
                tile.setState(Tile.STATE_ACTIVE);
                tile.setSubtitle("Connecting...");
                break;
            default:
                tile.setState(Tile.STATE_INACTIVE);
                tile.setSubtitle(null);
                break;
        }

        tile.updateTile();
    }
}
