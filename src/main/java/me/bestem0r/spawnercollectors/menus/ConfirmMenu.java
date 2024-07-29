package me.bestem0r.spawnercollectors.menus;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;

public class ConfirmMenu extends Menu {

    private final Runnable confirmAction;

    public ConfirmMenu(Runnable confirmAction) {
        super(27, ConfigManager.getString("menus.confirm.title"));
        this.confirmAction = confirmAction;
    }

    @Override
    protected void onCreate(MenuContent content) {

        content.fillEdges(ConfigManager.getItem("menus.items.filler").build());

        content.setClickable(12, Clickable.fromConfig("menus.confirm.confirm", event -> {
            event.getWhoClicked().closeInventory();
            confirmAction.run();
        }));

        content.setClickable(14, Clickable.fromConfig("menus.confirm.cancel", event -> {
            event.getWhoClicked().closeInventory();
        }));
    }
}
