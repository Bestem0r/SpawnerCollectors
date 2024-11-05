package me.bestem0r.spawnercollectors.menus;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuConfig;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.core.menu.PlacedClickable;

public class ConfirmMenu extends Menu {

    private final Runnable confirmAction;

    public ConfirmMenu(Runnable confirmAction) {
        super(MenuConfig.fromConfig("menus.confirm"));
        this.confirmAction = confirmAction;
    }

    @Override
    protected void onCreate(MenuContent content) {

        content.fillEdges(ConfigManager.getItem("menus.items.filler").build());

        content.setPlaced(PlacedClickable.fromConfig("menus.confirm.confirm", event -> {
            event.getWhoClicked().closeInventory();
            confirmAction.run();
        }));

        content.setPlaced(PlacedClickable.fromConfig("menus.confirm.cancel", event -> {
            event.getWhoClicked().closeInventory();
        }));
    }
}
