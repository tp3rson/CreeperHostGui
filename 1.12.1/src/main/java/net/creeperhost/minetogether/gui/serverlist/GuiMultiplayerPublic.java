package net.creeperhost.minetogether.gui.serverlist;

import net.creeperhost.minetogether.CreeperHost;
import net.creeperhost.minetogether.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.LanServerDetector;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.lang.reflect.Field;

public class GuiMultiplayerPublic extends GuiMultiplayer
{

    public enum SortOrder
    {
        RANDOM("multiplayer.sort.random"),
        PLAYER("multiplayer.sort.player"),
        NAME("multiplayer.sort.name"),
        UPTIME("multiplayer.sort.uptime"),
        LOCATION("multiplayer.sort.location");

        private static SortOrder[] vals = values();

        public String translate;

        SortOrder(String translate)
        {
            this.translate = translate;
        }

        public SortOrder next()
        {
            return vals[(this.ordinal()+1) % vals.length];
        }
    }

    private boolean initialized;
    private GuiScreen parent;
    private GuiButton modeToggle;
    private GuiButton sortOrderButton;
    public boolean isPublic = true;
    public SortOrder sortOrder = SortOrder.RANDOM;

    public GuiMultiplayerPublic(GuiScreen parentScreen)
    {
        super(parentScreen);
        parent = parentScreen;
    }

    public GuiMultiplayerPublic(GuiScreen parentScreen, boolean isPublic, SortOrder order)
    {
        this(parentScreen);
        this.isPublic = isPublic;
        sortOrder = order;
    }

    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        if (this.initialized)
        {
            this.ourServerListSelector.setDimensions(this.width, this.height, 32, this.height - 64);
        }
        else
        {
            this.initialized = true;
            setServerList(new ServerListPublic(this.mc, this));
            ourSavedServerList.loadServerList();
            setLanServerList(new LanServerDetector.LanServerList());

            try
            {
                setLanServerDetector(new LanServerDetector.ThreadLanServerFind(this.ourLanServerList));
                ourLanServerDetector.start();
            }
            catch (Exception exception)
            {
            }

            setServerListSelector(new ServerSelectionListPublic(this, this.mc, this.width, this.height, 32, this.height - 64, 46));
            ourServerListSelector.updateOnlineServers(this.ourSavedServerList);
        }

        this.createButtons();
    }

    @Override
    public boolean canMoveUp(ServerListEntryNormal p_175392_1_, int p_175392_2_)
    {
        return false;
    }

    @Override
    public boolean canMoveDown(ServerListEntryNormal p_175394_1_, int p_175394_2_)
    {
        return false;
    }

    @Override
    public void createButtons()
    {
        super.createButtons();
        for(GuiButton button: buttonList)
        {
            if (button.id != 0 && button.id != 1 && button.id != 3 && button.id != 7)
            {
                button.visible = false;
            } else if (button.id == 1) // original connect button
            {
                button.displayString = I18n.format("selectServer.add");
            } else if (button.id == 3) // original add button
            {
                button.displayString = I18n.format("selectServer.refresh");
            } else if (button.id == 7) // original edit button
            {
                button.displayString = Util.localize("multiplayer.friends");
                button.enabled = true;
            }
        }
        modeToggle = new GuiButton(80085, width - 5 - 80, 5, 80, 20, Util.localize(isPublic ? "multiplayer.button.public" : "multiplayer.button.private"));
        sortOrderButton = new GuiButton(80085101, width - 5 - 80 - 80, 5, 80, 20, Util.localize("multiplayer.sort", Util.localize(sortOrder.translate)));
        buttonList.add(modeToggle);
        buttonList.add(sortOrderButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 3)
        {
            refresh();
            return;
        } else if (button.id == modeToggle.id) {
            isPublic = !isPublic;
            button.displayString = Util.localize(isPublic ? "multiplayer.button.public" : "multiplayer.button.private");
            refresh();
            return;
        } else if (button.id == 7) {
            CreeperHost.proxy.openFriendsGui();
            return;
        } else if (button.id == sortOrderButton.id) {
            sortOrder = sortOrder.next();
            sortOrderButton.displayString = Util.localize("multiplayer.sort", Util.localize(sortOrder.translate));
            ourServerListSelector.sort();
            return;
        }
        super.actionPerformed(button);
    }

    private void refresh()
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiMultiplayerPublic(parent, isPublic, sortOrder));
    }

    @Override
    public void connectToSelected()
    {
        GuiListExtended.IGuiListEntry entry = this.ourServerListSelector.getSelected() < 0 ? null : this.ourServerListSelector.getListEntry(this.ourServerListSelector.getSelected());
        ServerList savedServerList = new ServerList(this.mc);
        savedServerList.loadServerList();
        savedServerList.addServerData(((ServerListEntryNormal)entry).getServerData());
        savedServerList.saveServerList();

        Minecraft mc = Minecraft.getMinecraft();
        if (parent instanceof GuiMultiplayer)
        {
            mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
            return;
        }

        mc.displayGuiScreen(parent);
    }

    @Override
    public void drawCenteredString(FontRenderer fontRendererIn, String text, int x, int y, int color)
    {
        if (text.equals(I18n.format("multiplayer.title")))
        {
            text = Util.localize("multiplayer.public");
        }
        super.drawCenteredString(fontRendererIn, text, x, y, color);
    }

    private ServerListPublic ourSavedServerList = null;
    private static Field savedServerListField;
    private void setServerList(ServerListPublic serverList)
    {
        ourSavedServerList = serverList;
        if (savedServerListField == null)
        {
            savedServerListField = ReflectionHelper.findField(GuiMultiplayer.class, "field_146804_i", "savedServerList");
            savedServerListField.setAccessible(true);
        }

        try
        {
            savedServerListField.set(this, serverList);
        }
        catch (IllegalAccessException e)
        {
            CreeperHost.logger.error("Unable to set server list", e);
        }
    }

    private LanServerDetector.ThreadLanServerFind ourLanServerDetector = null;
    private static Field lanServerDetectorField;
    private void setLanServerDetector(LanServerDetector.ThreadLanServerFind detector)
    {
        ourLanServerDetector = detector;
        if (lanServerDetectorField == null)
        {
            lanServerDetectorField = ReflectionHelper.findField(GuiMultiplayer.class, "field_146800_B", "lanServerDetector");
            lanServerDetectorField.setAccessible(true);
        }

        try
        {
            lanServerDetectorField.set(this, detector);
        }
        catch (IllegalAccessException e)
        {
            CreeperHost.logger.error("Unable to set server list", e);
        }
    }

    private LanServerDetector.LanServerList ourLanServerList = null;
    private static Field lanServerListField;
    private void setLanServerList(LanServerDetector.LanServerList detector)
    {
        ourLanServerList = detector;
        if (lanServerListField == null)
        {
            lanServerListField = ReflectionHelper.findField(GuiMultiplayer.class, "field_146799_A", "lanServerList");
            lanServerListField.setAccessible(true);
        }

        try
        {
            lanServerListField.set(this, detector);
        }
        catch (IllegalAccessException e)
        {
            CreeperHost.logger.error("Unable to set server list", e);
        }
    }

    private ServerSelectionListPublic ourServerListSelector = null;
    private static Field serverListSelectorField;
    private void setServerListSelector(ServerSelectionListPublic list)
    {
        ourServerListSelector = list;
        if (serverListSelectorField == null)
        {
            serverListSelectorField = ReflectionHelper.findField(GuiMultiplayer.class, "field_146803_h", "serverListSelector");
            serverListSelectorField.setAccessible(true);
        }

        try
        {
            serverListSelectorField.set(this, list);
        }
        catch (IllegalAccessException e)
        {
            CreeperHost.logger.error("Unable to set server list", e);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        super.drawScreen(mouseX, mouseY, partialTicks);

        GlStateManager.disableRescaleNormal(); // fix colour when hovering
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        drawCenteredString(fontRendererObj, Util.localize("multiplayer.public.random"), this.width / 2, this.height - 62, 0xFFFFFF);

        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
    }
}