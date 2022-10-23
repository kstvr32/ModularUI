package com.gtnewhorizons.modularui.common.internal.wrapper;

import static codechicken.lib.render.FontUtils.fontRenderer;

import codechicken.nei.ItemPanels;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerDrawHandler;
import codechicken.nei.guihook.IContainerObjectHandler;
import com.gtnewhorizons.modularui.ModularUI;
import com.gtnewhorizons.modularui.api.GlStateManager;
import com.gtnewhorizons.modularui.api.drawable.GuiHelper;
import com.gtnewhorizons.modularui.api.drawable.Text;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.math.Color;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.screen.Cursor;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.widget.IDragAndDropHandler;
import com.gtnewhorizons.modularui.api.widget.IVanillaSlot;
import com.gtnewhorizons.modularui.api.widget.IWidgetParent;
import com.gtnewhorizons.modularui.api.widget.Interactable;
import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.config.Config;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
@Optional.Interface(modid = "NotEnoughItems", iface = "codechicken.nei.api.INEIGuiHandler")
public class ModularGui extends GuiContainerAccessor implements INEIGuiHandler {

    private final ModularUIContext context;
    private Pos2d mousePos = Pos2d.ZERO;

    @Nullable
    private Interactable lastClicked;

    private long lastClick = -1;
    private long lastFocusedClick = -1;
    private int drawCalls = 0;
    private long drawTime = 0;
    private int fps = 0;

    private float partialTicks;

    public ModularGui(ModularUIContainer container) {
        super(container);
        this.context = container.getContext();
        this.context.initializeClient(this);
    }

    public ModularUIContext getContext() {
        return context;
    }

    public Cursor getCursor() {
        return context.getCursor();
    }

    public Pos2d getMousePos() {
        return mousePos;
    }

    //    @Override
    //    public void onResize(@NotNull Minecraft mc, int w, int h) {
    //        super.onResize(mc, w, h);
    //        context.resize(new Size(w, h));
    //    }

    public void setMainWindowArea(Pos2d pos, Size size) {
        this.guiLeft = pos.x;
        this.guiTop = pos.y;
        this.xSize = size.width;
        this.ySize = size.height;
    }

    @Override
    public void initGui() {
        super.initGui();
        context.resize(new Size(width, height));
        this.context.buildWindowOnStart();
        this.context.getCurrentWindow().onOpen();
    }

    public GuiContainerAccessor getAccessor() {
        return this;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        mousePos = new Pos2d(mouseX, mouseY);

        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        // mainly for invtweaks compat
        drawVanillaElements(mouseX, mouseY, partialTicks);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        setHoveredSlot(null);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
        if (shouldShowNEI()) {
            // Copied from GuiContainerManager#renderObjects but without translation
            for (IContainerDrawHandler drawHandler : GuiContainerManager.drawHandlers) {
                drawHandler.renderObjects(this, mouseX, mouseY);
            }
            for (IContainerDrawHandler drawHandler : GuiContainerManager.drawHandlers) {
                drawHandler.postRenderObjects(this, mouseX, mouseY);
            }

            if (!shouldRenderOurTooltip() && shouldRenderNEITooltip() && GuiContainerManager.getManager() != null) {
                GuiContainerManager.getManager().renderToolTips(mouseX, mouseY);
            }
        }
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.disableStandardItemLighting();
        this.drawGuiContainerForegroundLayer(mouseX, mouseY);
        RenderHelper.enableGUIStandardItemLighting();

        setHoveredSlot(null);
        Widget hovered = getCursor().getHovered();
        if (hovered instanceof IVanillaSlot) {
            setHoveredSlot(((IVanillaSlot) hovered).getMcSlot());
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(i, j, 0);
        GlStateManager.popMatrix();

        InventoryPlayer inventoryplayer = this.mc.thePlayer.inventory;
        ItemStack itemstack = getDraggedStack() == null ? inventoryplayer.getItemStack() : getDraggedStack();
        GlStateManager.translate((float) i, (float) j, 0.0F);
        if (itemstack != null) {
            int k2 = getDraggedStack() == null ? 8 : 16;
            String s = null;

            if (getDraggedStack() != null && getIsRightMouseClick()) {
                itemstack = itemstack.copy();
                itemstack.stackSize = (int) Math.ceil((float) itemstack.stackSize / 2.0F);
            } else if (this.isDragSplitting() && this.getDragSlots().size() > 1) {
                itemstack = itemstack.copy();
                itemstack.stackSize = getDragSplittingRemnant();

                if (itemstack.stackSize < 1) {
                    s = EnumChatFormatting.YELLOW + "0";
                }
            }

            this.drawItemStack(itemstack, mouseX - i - 8, mouseY - j - k2, s);
        }

        if (getReturningStack() != null) {
            float f = (float) (Minecraft.getSystemTime() - getReturningStackTime()) / 100.0F;

            if (f >= 1.0F) {
                f = 1.0F;
                setReturningStack(null);
            }

            int l2 = getReturningStackDestSlot().xDisplayPosition - getTouchUpX();
            int i3 = getReturningStackDestSlot().yDisplayPosition - getTouchUpY();
            int l1 = getTouchUpX() + (int) ((float) l2 * f);
            int i2 = getTouchUpY() + (int) ((float) i3 * f);
            this.drawItemStack(getReturningStack(), l1, i2, null);
        }

        GlStateManager.popMatrix();

        if (Config.debug) {
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            drawDebugScreen();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
    }

    private void drawItemStack(ItemStack stack, int x, int y, String altText) {
        GlStateManager.translate(0.0F, 0.0F, 32.0F);
        this.zLevel = 200.0F;
        itemRender.zLevel = 200.0F;
        FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) font = fontRenderer;
        itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), stack, x, y);
        itemRender.renderItemOverlayIntoGUI(
                font, mc.getTextureManager(), stack, x, y - (getDragSlots() != null ? 0 : 8), altText);
        this.zLevel = 0.0F;
        itemRender.zLevel = 0.0F;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        if (Config.debug) {
            long time = Minecraft.getSystemTime() / 1000;
            if (drawTime != time) {
                fps = drawCalls;
                drawCalls = 0;
                drawTime = time;
            }
            drawCalls++;
        }
        context.forEachWindowBottomToTop(window -> window.frameUpdate(partialTicks));
        drawDefaultBackground();

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        for (ModularWindow window : context.getOpenWindowsReversed()) {
            if (window.isEnabled()) {
                window.drawWidgets(partialTicks, false);
            }
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableLighting();
        RenderHelper.enableStandardItemLighting();

        this.partialTicks = partialTicks;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        GlStateManager.pushMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        Widget hovered = context.getCursor().getHovered();
        if (shouldRenderOurTooltip()) {
            if (hovered instanceof IVanillaSlot
                    && ((IVanillaSlot) hovered).getMcSlot().getHasStack()) {
                renderToolTip(
                        ((IVanillaSlot) hovered).getMcSlot().getStack(),
                        mouseX,
                        mouseY,
                        ((IVanillaSlot) hovered).getExtraTooltip());
            } else if (hovered.getTooltipShowUpDelay() <= context.getCursor().getTimeHovered()) {
                List<Text> tooltip = new ArrayList<>(hovered.getTooltip()); // avoid UOE
                if (ModularUI.isNEILoaded && hovered.hasNEITransferRect()) {
                    String transferRectTooltip = hovered.getNEITransferRectTooltip();
                    if (transferRectTooltip != null) {
                        tooltip.add(new Text(transferRectTooltip));
                    }
                }
                if (!tooltip.isEmpty()) {
                    GuiHelper.drawHoveringText(
                            tooltip,
                            context.getMousePos(),
                            context.getScaledScreenSize(),
                            400,
                            1,
                            false,
                            Alignment.CenterLeft,
                            hovered.isTooltipHasSpaceAfterFirstLine());
                }
            }
        }

        if (context.getCurrentWindow().isEnabled()) {
            context.getCurrentWindow().drawWidgets(partialTicks, true);
        }
        context.getCursor().draw(partialTicks);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableLighting();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    /**
     * @return False if NEI wants to draw their own tooltip e.g. ItemPanel
     */
    protected boolean shouldRenderOurTooltip() {
        return context.getCursor().getHovered() != null && !context.getCursor().isHoldingSomething();
    }

    protected boolean shouldRenderNEITooltip() {
        // taken from GuiContainerManager#getStackMouseOver but don't check #getSlotMouseOver
        // as it sees our slot even if it's disabled
        for (IContainerObjectHandler objectHandler : GuiContainerManager.objectHandlers) {
            ItemStack item = objectHandler.getStackUnderMouse(
                    this, context.getCursor().getPos().x, context.getCursor().getPos().y);
            if (item != null) return true;
        }
        return false;
    }

    public void drawDebugScreen() {
        Size screenSize = context.getScaledScreenSize();
        int neiYOffset = shouldShowNEI() ? 20 : 0;
        int color = Color.rgb(180, 40, 115);
        int lineY = screenSize.height - 13 - neiYOffset;
        drawString(fontRenderer, "Mouse Pos: " + getMousePos(), 5, lineY, color);
        lineY -= 11;
        drawString(fontRenderer, "FPS: " + fps, 5, screenSize.height - 24 - neiYOffset, color);
        lineY -= 11;
        Widget hovered = context.getCursor().findHoveredWidget(true);
        if (hovered != null) {
            Size size = hovered.getSize();
            Pos2d pos = hovered.getAbsolutePos();
            IWidgetParent parent = hovered.getParent();

            drawBorder(pos.x, pos.y, size.width, size.height, color, 1f);
            drawBorder(
                    parent.getAbsolutePos().x,
                    parent.getAbsolutePos().y,
                    parent.getSize().width,
                    parent.getSize().height,
                    Color.withAlpha(color, 0.3f),
                    1f);
            drawText("Pos: " + hovered.getPos(), 5, lineY, 1, color, false);
            lineY -= 11;
            drawText("Size: " + size, 5, lineY, 1, color, false);
            lineY -= 11;
            drawText(
                    "Parent: " + (parent instanceof ModularWindow ? "ModularWindow" : parent.toString()),
                    5,
                    lineY,
                    1,
                    color,
                    false);
            lineY -= 11;
            drawText("Class: " + hovered, 5, lineY, 1, color, false);
            lineY -= 11;
            if (hovered instanceof SlotWidget) {
                BaseSlot slot = ((SlotWidget) hovered).getMcSlot();
                drawText("Slot Index: " + slot.getSlotIndex(), 5, lineY, 1, color, false);
                lineY -= 11;
                drawText("Shift-Click Priority: " + slot.getShiftClickPriority(), 5, lineY, 1, color, false);
            }
        }
        color = Color.withAlpha(color, 25);
        for (int i = 5; i < screenSize.width; i += 5) {
            drawVerticalLine(i, 0, screenSize.height, color);
        }

        for (int i = 5; i < screenSize.height; i += 5) {
            drawHorizontalLine(0, screenSize.width, i, color);
        }
        drawRect(mousePos.x, mousePos.y, mousePos.x + 1, mousePos.y + 1, Color.withAlpha(Color.GREEN.normal, 0.8f));
    }

    protected void renderToolTip(ItemStack stack, int x, int y, List<String> extraLines) {
        FontRenderer font = null;
        List lines = new ArrayList();
        if (stack != null) {
            font = stack.getItem().getFontRenderer(stack);
            List<String> itemStackTooltips =
                    stack.getTooltip(context.getPlayer(), this.mc.gameSettings.advancedItemTooltips);
            for (int i = 0; i < itemStackTooltips.size(); i++) {
                if (i == 0) {
                    itemStackTooltips.set(0, stack.getRarity().rarityColor.toString() + itemStackTooltips.get(0));
                } else {
                    itemStackTooltips.set(i, EnumChatFormatting.GRAY + itemStackTooltips.get(i));
                }
            }
            lines.addAll(itemStackTooltips);
        }
        lines.addAll(extraLines);
        this.drawHoveringText(lines, x, y, (font == null ? fontRenderer : font));
    }

    protected void drawVanillaElements(int mouseX, int mouseY, float partialTicks) {
        for (Object guiButton : this.buttonList) {
            ((GuiButton) guiButton).drawButton(this.mc, mouseX, mouseY);
        }
        for (Object guiLabel : this.labelList) {
            ((GuiLabel) guiLabel).func_146159_a(this.mc, mouseX, mouseY);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        context.onClientTick();
        for (ModularWindow window : context.getOpenWindowsReversed()) {
            window.update();
        }
        context.getCursor().updateHovered();
        context.getCursor().onScreenUpdate();
    }

    private boolean isDoubleClick(long lastClick, long currentClick) {
        return currentClick - lastClick < 500;
    }

    //    @Override
    //    protected boolean hasClickedOutside(int p_193983_1_, int p_193983_2_, int p_193983_3_, int p_193983_4_) {
    //        for (ModularWindow window : context.getOpenWindows()) {
    //            if (Pos2d.isInside(p_193983_1_, p_193983_2_, window.getAbsolutePos(), window.getSize())) {
    //                return false;
    //            }
    //        }
    //        return super.hasClickedOutside(p_193983_1_, p_193983_2_, p_193983_3_, p_193983_4_);
    //    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        long time = Minecraft.getSystemTime();
        boolean doubleClick = isDoubleClick(lastClick, time);
        lastClick = time;
        // For reference: in 1.12 JEI handles drag-and-drop on MouseInputEvent.Pre event,
        // which is fired before GuiScreen#handleMouseInput call and able to dismiss it.
        // In contrast, NEI injects GuiContainerManager#mouseClicked at the start of GuiContainer#mouseClicked,
        // so at this point NEI has not handled drag-and-drop yet.
        // See also: PanelWidget#handleClickExt

        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            if (shouldSkipClick(interactable)) continue;
            interactable.onClick(mouseButton, doubleClick);
        }

        if (context.getCursor().onMouseClick(mouseButton)) {
            lastFocusedClick = time;
            return;
        }

        Object probablyClicked = null;
        boolean wasSuccess = false;
        doubleClick = isDoubleClick(lastFocusedClick, time);
        loop:
        for (Object hovered : getCursor().getAllHovered()) {
            if (shouldSkipClick(hovered)) break;
            if (context.getCursor().onHoveredClick(mouseButton, hovered)) {
                probablyClicked = hovered;
                break;
            }
            if (hovered instanceof Widget) {
                Widget widget = (Widget) hovered;
                if (ModularUI.isNEILoaded && widget.hasNEITransferRect()) {
                    if (mouseButton == 0) {
                        widget.handleTransferRectMouseClick(false);
                    } else if (mouseButton == 1) {
                        widget.handleTransferRectMouseClick(true);
                    }
                    probablyClicked = hovered;
                    break;
                }
            }
            if (hovered instanceof Interactable) {
                Interactable interactable = (Interactable) hovered;
                Interactable.ClickResult result =
                        interactable.onClick(mouseButton, doubleClick && lastClicked == interactable);
                switch (result) {
                    case IGNORE:
                        continue;
                    case ACKNOWLEDGED:
                        if (probablyClicked == null) {
                            probablyClicked = interactable;
                        }
                        continue;
                    case REJECT:
                        probablyClicked = null;
                        break loop;
                    case ACCEPT:
                        probablyClicked = interactable;
                        break loop;
                    case SUCCESS:
                        probablyClicked = interactable;
                        wasSuccess = true;
                        getCursor().updateFocused((Widget) interactable);
                        break loop;
                }
            }
        }
        if (probablyClicked instanceof Interactable) {
            this.lastClicked = (Interactable) probablyClicked;
        } else {
            this.lastClicked = null;
        }
        if (!wasSuccess) {
            getCursor().updateFocused(null);
        }
        if (probablyClicked == null) {
            // NEI injects GuiContainerManager#mouseClicked there
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        lastFocusedClick = time;
    }

    private boolean isNEIWantToHandleDragAndDrop() {
        return shouldShowNEI()
                && (ItemPanels.itemPanel.draggedStack != null || ItemPanels.bookmarkPanel.draggedStack != null);
    }

    private boolean shouldSkipClick(Object object) {
        return isNEIWantToHandleDragAndDrop() && object instanceof IDragAndDropHandler;
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onClickReleased(mouseButton);
        }
        if (!context.getCursor().onMouseReleased(mouseButton)
                && (lastClicked == null || !lastClicked.onClickReleased(mouseButton))) {
            super.mouseMovedOrUp(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick);
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onMouseDragged(mouseButton, timeSinceLastClick);
        }
        if (lastClicked != null) {
            lastClicked.onMouseDragged(mouseButton, timeSinceLastClick);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // debug mode C + CTRL + SHIFT
        if (keyCode == 46 && isCtrlKeyDown() && isShiftKeyDown()) {
            Config.debug = !Config.debug;
        }
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onKeyPressed(typedChar, keyCode);
        }

        Widget focused = getCursor().getFocused();
        if (focused instanceof Interactable && ((Interactable) focused).onKeyPressed(typedChar, keyCode)) {
            return;
        }
        for (Object hovered : getCursor().getAllHovered()) {
            if (focused != hovered
                    && hovered instanceof Interactable
                    && ((Interactable) hovered).onKeyPressed(typedChar, keyCode)) {
                return;
            }
        }

        if (keyCode == Keyboard.KEY_ESCAPE || this.mc.gameSettings.keyBindInventory.getKeyCode() == keyCode) {
            if (Config.closeWindowsAtOnce) {
                this.context.tryClose();
            } else {
                for (ModularWindow window : this.context.getOpenWindows()) {
                    window.tryClose();
                    break;
                }
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    public void mouseScroll(int direction) {
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onMouseScroll(direction);
        }
        Widget focused = getCursor().getFocused();
        if (focused instanceof Interactable && ((Interactable) focused).onMouseScroll(direction)) {
            return;
        }
        for (Object hovered : getCursor().getAllHovered()) {
            if (focused != hovered
                    && hovered instanceof Interactable
                    && ((Interactable) hovered).onMouseScroll(direction)) {
                return;
            }
        }
    }

    @Override
    public void onGuiClosed() {
        context.getCloseListeners().forEach(Runnable::run);
    }

    public boolean isDragSplitting() {
        return isDragSplittingInternal();
    }

    public Set<Slot> getDragSlots() {
        return getDragSplittingSlots();
    }

    public static RenderItem getItemRenderer() {
        return itemRender;
    }

    public static void setItemRenderer(RenderItem renderer) {
        itemRender = renderer;
    }

    public float getZ() {
        return zLevel;
    }

    public void setZ(float z) {
        this.zLevel = z;
    }

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    @SideOnly(Side.CLIENT)
    public static void drawBorder(float x, float y, float width, float height, int color, float border) {
        drawSolidRect(x - border, y - border, width + 2 * border, border, color);
        drawSolidRect(x - border, y + height, width + 2 * border, border, color);
        drawSolidRect(x - border, y, border, height, color);
        drawSolidRect(x + width, y, border, height, color);
    }

    @SideOnly(Side.CLIENT)
    public static void drawSolidRect(float x, float y, float width, float height, int color) {
        drawRect(x, y, x + width, y + height, color);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();
    }

    @SideOnly(Side.CLIENT)
    public static void drawText(String text, float x, float y, float scale, int color, boolean shadow) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        GlStateManager.disableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 0f);
        float sf = 1 / scale;
        fontRenderer.drawString(text, (int) (x * sf), (int) (y * sf), color, shadow);
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left < right) {
            float i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            float j = top;
            top = bottom;
            bottom = j;
        }

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.instance;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);
        tessellator.startDrawingQuads();
        //        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        tessellator.addVertex(left, bottom, 0.0D);
        tessellator.addVertex(right, bottom, 0.0D);
        tessellator.addVertex(right, top, 0.0D);
        tessellator.addVertex(left, top, 0.0D);
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private boolean shouldShowNEI() {
        return ModularUI.isNEILoaded && getContext().doShowNEI();
    }

    // === NEI ===

    @Override
    @Optional.Method(modid = ModularUI.MODID_NEI)
    public boolean handleDragNDrop(GuiContainer gui, int mousex, int mousey, ItemStack draggedStack, int button) {
        if (!(gui instanceof ModularGui) || NEIClientUtils.getHeldItem() != null) return false;
        Widget hovered = getContext().getCursor().getHovered();
        if (hovered instanceof IDragAndDropHandler) {
            return ((IDragAndDropHandler) hovered).handleDragAndDrop(draggedStack, button);
        }
        return false;
    }

    @Override
    @Optional.Method(modid = ModularUI.MODID_NEI)
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        if (!(gui instanceof ModularGui)) return false;
        Rectangle neiSlotRectangle = new Rectangle(x, y, w, h);

        for (ModularWindow window : getContext().getOpenWindows()) {
            if (window.getRectangle().intersects(neiSlotRectangle)) {
                return true;
            }
        }

        List<Widget> activeWidgets = new ArrayList<>();
        for (ModularWindow window : getContext().getOpenWindows()) {
            IWidgetParent.forEachByLayer(
                    window,
                    true,
                    // skip children search if parent does not respect NEI area
                    widget -> !widget.isRespectNEIArea(),
                    widget -> {
                        if (widget.isRespectNEIArea()) {
                            activeWidgets.add(widget);
                        }
                        return false;
                    });
        }
        for (Widget widget : activeWidgets) {
            Rectangle widgetAbsoluteRectangle = new Rectangle(
                    widget.getAbsolutePos().x,
                    widget.getAbsolutePos().y,
                    widget.getSize().width,
                    widget.getSize().height);
            if (widgetAbsoluteRectangle.intersects(neiSlotRectangle)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Optional.Method(modid = ModularUI.MODID_NEI)
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        return currentVisibility;
    }

    @Override
    @Optional.Method(modid = ModularUI.MODID_NEI)
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return Collections.emptyList();
    }

    @Override
    @Optional.Method(modid = ModularUI.MODID_NEI)
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return null;
    }
}
