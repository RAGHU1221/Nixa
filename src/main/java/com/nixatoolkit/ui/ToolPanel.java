package com.nixatoolkit.ui;

/** Optional lifecycle hook - called each time a panel becomes the visible card. */
public interface ToolPanel {
    default void onShow() {
    }
}
