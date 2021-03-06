package net.creeperhost.minetogether.gui.list;

import net.creeperhost.minetogether.gui.serverlist.data.Friend;
import net.minecraft.util.text.TextComponentString;

public class GuiListEntryFriend extends GuiListEntry
{

    private final Friend friend;

    public GuiListEntryFriend(GuiList list, Friend friend)
    {
        super(list);
        this.friend = friend;
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected)
    {
        this.mc.fontRendererObj.drawString(friend.getName(), x + 5, y + 5, 16777215);
        this.mc.fontRendererObj.drawString(new TextComponentString(friend.isAccepted() ? "Accepted" : "Pending").getText(), x + 5, y + 5 + 10, 16777215);
    }

    public Friend getFriend()
    {
        return friend;
    }
}
