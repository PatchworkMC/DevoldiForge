/*
 * Minecraft Forge
 * Copyright (c) 2016-2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.client.gui;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.ChatUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

public class ForgeIngameGui extends InGameHud
{
    //private static final ResourceLocation VIGNETTE     = new ResourceLocation("textures/misc/vignette.png");
    //private static final ResourceLocation WIDGITS      = new ResourceLocation("textures/gui/widgets.png");
    //private static final ResourceLocation PUMPKIN_BLUR = new ResourceLocation("textures/misc/pumpkinblur.png");

    private static final int WHITE = 0xFFFFFF;

    //Flags to toggle the rendering of certain aspects of the HUD, valid conditions
    //must be met for them to render normally. If those conditions are met, but this flag
    //is false, they will not be rendered.
    public static boolean renderVignette = true;
    public static boolean renderHelmet = true;
    public static boolean renderPortal = true;
    public static boolean renderSpectatorTooltip = true;
    public static boolean renderHotbar = true;
    public static boolean renderCrosshairs = true;
    public static boolean renderBossHealth = true;
    public static boolean renderHealth = true;
    public static boolean renderArmor = true;
    public static boolean renderFood = true;
    public static boolean renderHealthMount = true;
    public static boolean renderAir = true;
    public static boolean renderExperiance = true;
    public static boolean renderJumpBar = true;
    public static boolean renderObjective = true;

    public static int left_height = 39;
    public static int right_height = 39;
    /*
     * If the Euclidian distance to the moused-over block in meters is less than this value, the "Looking at" text will appear on the debug overlay.
     */
    public static double rayTraceDistance = 20.0D;

    private TextRenderer fontrenderer = null;
    private RenderGameOverlayEvent eventParent;
    //private static final String MC_VERSION = MinecraftForge.MC_VERSION;
    private GuiOverlayDebugForge debugOverlay;

    public ForgeIngameGui(MinecraftClient mc)
    {
        super(mc);
        debugOverlay = new GuiOverlayDebugForge(mc);
    }

    @Override
    public void render(float partialTicks)
    {
        this.scaledWidth = this.client.getWindow().getScaledWidth();
        this.scaledHeight = this.client.getWindow().getScaledHeight();
        eventParent = new RenderGameOverlayEvent(partialTicks, this.client.getWindow());
        renderHealthMount = client.player.getVehicle() instanceof LivingEntity;
        renderFood = client.player.getVehicle() == null;
        renderJumpBar = client.player.hasJumpingMount();

        right_height = 39;
        left_height = 39;

        if (pre(ALL)) return;

        fontrenderer = client.textRenderer;
        //mc.entityRenderer.setupOverlayRendering();
        RenderSystem.enableBlend();
        if (renderVignette && MinecraftClient.isFancyGraphicsEnabled())
        {
            renderVignetteOverlay(client.getCameraEntity());
        }
        else
        {
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
        }

        if (renderHelmet) renderHelmet(partialTicks);

        if (renderPortal && !client.player.hasStatusEffect(StatusEffects.NAUSEA))
        {
            renderPortalOverlay(partialTicks);
        }

        if (this.client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
        {
            if (renderSpectatorTooltip) spectatorHud.render(partialTicks);
        }
        else if (!this.client.options.hudHidden)
        {
            if (renderHotbar) renderHotbar(partialTicks);
        }

        if (!this.client.options.hudHidden) {
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            setBlitOffset(-90);
            random.setSeed((long)(ticks * 312871));

            if (renderCrosshairs) renderCrosshair();
            if (renderBossHealth) renderBossHealth();

            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.client.interactionManager.hasStatusBars() && this.client.getCameraEntity() instanceof PlayerEntity)
            {
                if (renderHealth) renderHealth(this.scaledWidth, this.scaledHeight);
                if (renderArmor)  renderArmor(this.scaledWidth, this.scaledHeight);
                if (renderFood)   renderFood(this.scaledWidth, this.scaledHeight);
                if (renderHealthMount) renderHealthMount(this.scaledWidth, this.scaledHeight);
                if (renderAir)    renderAir(this.scaledWidth, this.scaledHeight);
            }

            if (renderJumpBar)
            {
                renderMountJumpBar(this.scaledWidth / 2 - 91);
            }
            else if (renderExperiance)
            {
                renderExperience(this.scaledWidth / 2 - 91);
            }
            if (this.client.options.heldItemTooltips && this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
                this.renderHeldItemTooltip();
             } else if (this.client.player.isSpectator()) {
                this.spectatorHud.render();
             }
        }

        renderSleepFade(this.scaledWidth, this.scaledHeight);

        renderHUDText(this.scaledWidth, this.scaledHeight);
        renderFPSGraph();
        renderStatusEffectOverlay();
        if (!client.options.hudHidden) {
            renderRecordOverlay(this.scaledWidth, this.scaledHeight, partialTicks);
            renderSubtitles();
            renderTitle(this.scaledWidth, this.scaledHeight, partialTicks);
        }


        Scoreboard scoreboard = this.client.world.getScoreboard();
        ScoreboardObjective objective = null;
        Team scoreplayerteam = scoreboard.getPlayerTeam(client.player.getEntityName());
        if (scoreplayerteam != null)
        {
            int slot = scoreplayerteam.getColor().getColorIndex();
            if (slot >= 0) objective = scoreboard.getObjectiveForSlot(3 + slot);
        }
        ScoreboardObjective scoreobjective1 = objective != null ? objective : scoreboard.getObjectiveForSlot(1);
        if (renderObjective && scoreobjective1 != null)
        {
            this.renderScoreboardSidebar(scoreobjective1);
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        RenderSystem.disableAlphaTest();

        renderChat(this.scaledWidth, this.scaledHeight);

        renderPlayerList(this.scaledWidth, this.scaledHeight);

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableAlphaTest();

        post(ALL);
    }

    @Override
    protected void renderCrosshair()
    {
        if (pre(CROSSHAIRS)) return;
        bind(DrawableHelper.GUI_ICONS_LOCATION);
        RenderSystem.enableBlend();
        RenderSystem.enableAlphaTest();
        super.renderCrosshair();
        post(CROSSHAIRS);
    }

    @Override
    protected void renderStatusEffectOverlay()
    {
        if (pre(POTION_ICONS)) return;
        super.renderStatusEffectOverlay();
        post(POTION_ICONS);
    }

    protected void renderSubtitles()
    {
        if (pre(SUBTITLES)) return;
        this.subtitlesHud.render();
        post(SUBTITLES);
    }

    protected void renderBossHealth()
    {
        if (pre(BOSSHEALTH)) return;
        bind(DrawableHelper.GUI_ICONS_LOCATION);
        RenderSystem.defaultBlendFunc();
        client.getProfiler().push("bossHealth");
        RenderSystem.enableBlend();
        this.bossBarHud.render();
        RenderSystem.disableBlend();
        client.getProfiler().pop();
        post(BOSSHEALTH);
    }

    @Override
    protected void renderVignetteOverlay(Entity entity)
    {
        if (pre(VIGNETTE))
        {
            // Need to put this here, since Vanilla assumes this state after the vignette was rendered.
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            return;
        }
        super.renderVignetteOverlay(entity);
        post(VIGNETTE);
    }

    private void renderHelmet(float partialTicks)
    {
        if (pre(HELMET)) return;

        ItemStack itemstack = this.client.player.inventory.getArmorStack(3);

        if (this.client.options.perspective == 0 && !itemstack.isEmpty())
        {
            Item item = itemstack.getItem();
            if (item == Blocks.CARVED_PUMPKIN.asItem())
            {
                renderPumpkinOverlay();
            }
            else
            {
                item.renderHelmetOverlay(itemstack, client.player, this.scaledWidth, this.scaledHeight, partialTicks);
            }
        }

        post(HELMET);
    }

    protected void renderArmor(int width, int height)
    {
        if (pre(ARMOR)) return;
        client.getProfiler().push("armor");

        RenderSystem.enableBlend();
        int left = width / 2 - 91;
        int top = height - left_height;

        int level = client.player.getArmor();
        for (int i = 1; level > 0 && i < 20; i += 2)
        {
            if (i < level)
            {
                blit(left, top, 34, 9, 9, 9);
            }
            else if (i == level)
            {
                blit(left, top, 25, 9, 9, 9);
            }
            else if (i > level)
            {
                blit(left, top, 16, 9, 9, 9);
            }
            left += 8;
        }
        left_height += 10;

        RenderSystem.disableBlend();
        client.getProfiler().pop();
        post(ARMOR);
    }

    @Override
    protected void renderPortalOverlay(float partialTicks)
    {
        if (pre(PORTAL)) return;

        float f1 = client.player.lastNauseaStrength + (client.player.nextNauseaStrength - client.player.lastNauseaStrength) * partialTicks;

        if (f1 > 0.0F)
        {
            super.renderPortalOverlay(f1);
        }

        post(PORTAL);
    }

    @Override
    protected void renderHotbar(float partialTicks)
    {
        if (pre(HOTBAR)) return;

        if (client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
        {
            this.spectatorHud.render(partialTicks);
        }
        else
        {
            super.renderHotbar(partialTicks);
        }

        post(HOTBAR);
    }

    @Override
    public void setOverlayMessage(Text component, boolean animateColor)
    {
        this.setOverlayMessage(component.asFormattedString(), animateColor);
    }

    protected void renderAir(int width, int height)
    {
        if (pre(AIR)) return;
        client.getProfiler().push("air");
        PlayerEntity player = (PlayerEntity)this.client.getCameraEntity();
        RenderSystem.enableBlend();
        int left = width / 2 + 91;
        int top = height - right_height;

        int air = player.getAir();
        if (player.isInFluid(FluidTags.WATER) || air < 300)
        {
            int full = MathHelper.ceil((double)(air - 2) * 10.0D / 300.0D);
            int partial = MathHelper.ceil((double)air * 10.0D / 300.0D) - full;

            for (int i = 0; i < full + partial; ++i)
            {
                blit(left - i * 8 - 9, top, (i < full ? 16 : 25), 18, 9, 9);
            }
            right_height += 10;
        }

        RenderSystem.disableBlend();
        client.getProfiler().pop();
        post(AIR);
    }

    public void renderHealth(int width, int height)
    {
        bind(GUI_ICONS_LOCATION);
        if (pre(HEALTH)) return;
        client.getProfiler().push("health");
        RenderSystem.enableBlend();

        PlayerEntity player = (PlayerEntity)this.client.getCameraEntity();
        int health = MathHelper.ceil(player.getHealth());
        boolean highlight = heartJumpEndTick > (long)ticks && (heartJumpEndTick - (long)ticks) / 3L %2L == 1L;

        if (health < this.lastHealthValue && player.timeUntilRegen > 0)
        {
            this.lastHealthCheckTime = Util.getMeasuringTimeMs();
            this.heartJumpEndTick = (long)(this.ticks + 20);
        }
        else if (health > this.lastHealthValue && player.timeUntilRegen > 0)
        {
            this.lastHealthCheckTime = Util.getMeasuringTimeMs();
            this.heartJumpEndTick = (long)(this.ticks + 10);
        }

        if (Util.getMeasuringTimeMs() - this.lastHealthCheckTime > 1000L)
        {
            this.lastHealthValue = health;
            this.renderHealthValue = health;
            this.lastHealthCheckTime = Util.getMeasuringTimeMs();
        }

        this.lastHealthValue = health;
        int healthLast = this.renderHealthValue;

        EntityAttributeInstance attrMaxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        float healthMax = (float)attrMaxHealth.getValue();
        float absorb = MathHelper.ceil(player.getAbsorptionAmount());

        int healthRows = MathHelper.ceil((healthMax + absorb) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);

        this.random.setSeed((long)(ticks * 312871));

        int left = width / 2 - 91;
        int top = height - left_height;
        left_height += (healthRows * rowHeight);
        if (rowHeight != 10) left_height += 10 - rowHeight;

        int regen = -1;
        if (player.hasStatusEffect(StatusEffects.REGENERATION))
        {
            regen = ticks % 25;
        }

        final int TOP =  9 * (client.world.getLevelProperties().isHardcore() ? 5 : 0);
        final int BACKGROUND = (highlight ? 25 : 16);
        int MARGIN = 16;
        if (player.hasStatusEffect(StatusEffects.POISON))      MARGIN += 36;
        else if (player.hasStatusEffect(StatusEffects.WITHER)) MARGIN += 72;
        float absorbRemaining = absorb;

        for (int i = MathHelper.ceil((healthMax + absorb) / 2.0F) - 1; i >= 0; --i)
        {
            //int b0 = (highlight ? 1 : 0);
            int row = MathHelper.ceil((float)(i + 1) / 10.0F) - 1;
            int x = left + i % 10 * 8;
            int y = top - row * rowHeight;

            if (health <= 4) y += random.nextInt(2);
            if (i == regen) y -= 2;

            blit(x, y, BACKGROUND, TOP, 9, 9);

            if (highlight)
            {
                if (i * 2 + 1 < healthLast)
                    blit(x, y, MARGIN + 54, TOP, 9, 9); //6
                else if (i * 2 + 1 == healthLast)
                    blit(x, y, MARGIN + 63, TOP, 9, 9); //7
            }

            if (absorbRemaining > 0.0F)
            {
                if (absorbRemaining == absorb && absorb % 2.0F == 1.0F)
                {
                    blit(x, y, MARGIN + 153, TOP, 9, 9); //17
                    absorbRemaining -= 1.0F;
                }
                else
                {
                    blit(x, y, MARGIN + 144, TOP, 9, 9); //16
                    absorbRemaining -= 2.0F;
                }
            }
            else
            {
                if (i * 2 + 1 < health)
                    blit(x, y, MARGIN + 36, TOP, 9, 9); //4
                else if (i * 2 + 1 == health)
                    blit(x, y, MARGIN + 45, TOP, 9, 9); //5
            }
        }

        RenderSystem.disableBlend();
        client.getProfiler().pop();
        post(HEALTH);
    }

    public void renderFood(int width, int height)
    {
        if (pre(FOOD)) return;
        client.getProfiler().push("food");

        PlayerEntity player = (PlayerEntity)this.client.getCameraEntity();
        RenderSystem.enableBlend();
        int left = width / 2 + 91;
        int top = height - right_height;
        right_height += 10;
        boolean unused = false;// Unused flag in vanilla, seems to be part of a 'fade out' mechanic

        HungerManager stats = client.player.getHungerManager();
        int level = stats.getFoodLevel();

        for (int i = 0; i < 10; ++i)
        {
            int idx = i * 2 + 1;
            int x = left - i * 8 - 9;
            int y = top;
            int icon = 16;
            byte background = 0;

            if (client.player.hasStatusEffect(StatusEffects.HUNGER))
            {
                icon += 36;
                background = 13;
            }
            if (unused) background = 1; //Probably should be a += 1 but vanilla never uses this

            if (player.getHungerManager().getSaturationLevel() <= 0.0F && ticks % (level * 3 + 1) == 0)
            {
                y = top + (random.nextInt(3) - 1);
            }

            blit(x, y, 16 + background * 9, 27, 9, 9);

            if (idx < level)
                blit(x, y, icon + 36, 27, 9, 9);
            else if (idx == level)
                blit(x, y, icon + 45, 27, 9, 9);
        }
        RenderSystem.disableBlend();
        client.getProfiler().pop();
        post(FOOD);
    }

    protected void renderSleepFade(int width, int height)
    {
        if (client.player.getSleepTimer() > 0)
        {
            client.getProfiler().push("sleep");
            RenderSystem.disableDepthTest();
            RenderSystem.disableAlphaTest();
            int sleepTime = client.player.getSleepTimer();
            float opacity = (float)sleepTime / 100.0F;

            if (opacity > 1.0F)
            {
                opacity = 1.0F - (float)(sleepTime - 100) / 10.0F;
            }

            int color = (int)(220.0F * opacity) << 24 | 1052704;
            fill(0, 0, width, height, color);
            RenderSystem.enableAlphaTest();
            RenderSystem.enableDepthTest();
            client.getProfiler().pop();
        }
    }

    protected void renderExperience(int x)
    {
        bind(GUI_ICONS_LOCATION);
        if (pre(EXPERIENCE)) return;
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        if (client.interactionManager.hasExperienceBar())
        {
            super.renderExperienceBar(x);
        }
        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        post(EXPERIENCE);
    }

    @Override
    public void renderMountJumpBar(int x)
    {
        bind(GUI_ICONS_LOCATION);
        if (pre(JUMPBAR)) return;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        super.renderMountJumpBar(x);

        RenderSystem.enableBlend();
        client.getProfiler().pop();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        post(JUMPBAR);
    }

    protected void renderHUDText(int width, int height)
    {
        client.getProfiler().push("forgeHudText");
        RenderSystem.defaultBlendFunc();
        ArrayList<String> listL = new ArrayList<String>();
        ArrayList<String> listR = new ArrayList<String>();

        if (client.isDemo())
        {
            long time = client.world.getTime();
            if (time >= 120500L)
            {
                listR.add(I18n.translate("demo.demoExpired"));
            }
            else
            {
                listR.add(I18n.translate("demo.remainingTime", ChatUtil.ticksToString((int)(120500L - time))));
            }
        }

        if (this.client.options.debugEnabled && !pre(DEBUG))
        {
            debugOverlay.update();
            listL.addAll(debugOverlay.getLeft());
            listR.addAll(debugOverlay.getRight());
            post(DEBUG);
        }

        RenderGameOverlayEvent.Text event = new RenderGameOverlayEvent.Text(eventParent, listL, listR);
        if (!MinecraftForge.EVENT_BUS.post(event))
        {
            int top = 2;
            for (String msg : listL)
            {
                if (msg == null) continue;
                fill(1, top - 1, 2 + fontrenderer.getStringWidth(msg) + 1, top + fontrenderer.fontHeight - 1, -1873784752);
                fontrenderer.drawWithShadow(msg, 2, top, 14737632);
                top += fontrenderer.fontHeight;
            }

            top = 2;
            for (String msg : listR)
            {
                if (msg == null) continue;
                int w = fontrenderer.getStringWidth(msg);
                int left = width - 2 - w;
                fill(left - 1, top - 1, left + w + 1, top + fontrenderer.fontHeight - 1, -1873784752);
                fontrenderer.drawWithShadow(msg, left, top, 14737632);
                top += fontrenderer.fontHeight;
            }
        }

        client.getProfiler().pop();
        post(TEXT);
    }

    protected void renderFPSGraph()
    {
        if (this.client.options.debugEnabled && this.client.options.debugTpsEnabled && !pre(FPS_GRAPH))
        {
            this.debugOverlay.render();
            post(FPS_GRAPH);
        }
    }

    protected void renderRecordOverlay(int width, int height, float partialTicks)
    {
        if (overlayRemaining > 0)
        {
            client.getProfiler().push("overlayMessage");
            float hue = (float)overlayRemaining - partialTicks;
            int opacity = (int)(hue * 256.0F / 20.0F);
            if (opacity > 255) opacity = 255;

            if (opacity > 0)
            {
                RenderSystem.pushMatrix();
                RenderSystem.translatef((float)(width / 2), (float)(height - 68), 0.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                int color = (overlayTinted ? MathHelper.hsvToRgb(hue / 50.0F, 0.7F, 0.6F) & WHITE : WHITE);
                fontrenderer.drawWithShadow(overlayMessage, -fontrenderer.getStringWidth(overlayMessage) / 2, -4, color | (opacity << 24));
                RenderSystem.disableBlend();
                RenderSystem.popMatrix();
            }

            client.getProfiler().pop();
        }
    }

    protected void renderTitle(int width, int height, float partialTicks)
    {
        if (titleTotalTicks > 0)
        {
            client.getProfiler().push("titleAndSubtitle");
            float age = (float)this.titleTotalTicks - partialTicks;
            int opacity = 255;

            if (titleTotalTicks > titleFadeOutTicks + titleRemainTicks)
            {
                float f3 = (float)(titleFadeInTicks + titleRemainTicks + titleFadeOutTicks) - age;
                opacity = (int)(f3 * 255.0F / (float)titleFadeInTicks);
            }
            if (titleTotalTicks <= titleFadeOutTicks) opacity = (int)(age * 255.0F / (float)this.titleFadeOutTicks);

            opacity = MathHelper.clamp(opacity, 0, 255);

            if (opacity > 8)
            {
                RenderSystem.pushMatrix();
                RenderSystem.translatef((float)(width / 2), (float)(height / 2), 0.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.pushMatrix();
                RenderSystem.scalef(4.0F, 4.0F, 4.0F);
                int l = opacity << 24 & -16777216;
                this.getFontRenderer().drawWithShadow(this.title, (float)(-this.getFontRenderer().getStringWidth(this.title) / 2), -10.0F, 16777215 | l);
                RenderSystem.popMatrix();
                RenderSystem.pushMatrix();
                RenderSystem.scalef(2.0F, 2.0F, 2.0F);
                this.getFontRenderer().drawWithShadow(this.subtitle, (float)(-this.getFontRenderer().getStringWidth(this.subtitle) / 2), 5.0F, 16777215 | l);
                RenderSystem.popMatrix();
                RenderSystem.disableBlend();
                RenderSystem.popMatrix();
            }

            this.client.getProfiler().pop();
        }
    }

    protected void renderChat(int width, int height)
    {
        client.getProfiler().push("chat");

        RenderGameOverlayEvent.Chat event = new RenderGameOverlayEvent.Chat(eventParent, 0, height - 48);
        if (MinecraftForge.EVENT_BUS.post(event)) return;

        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) event.getPosX(), (float) event.getPosY(), 0.0F);
        chatHud.render(ticks);
        RenderSystem.popMatrix();

        post(CHAT);

        client.getProfiler().pop();
    }

    protected void renderPlayerList(int width, int height)
    {
        ScoreboardObjective scoreobjective = this.client.world.getScoreboard().getObjectiveForSlot(0);
        ClientPlayNetworkHandler handler = client.player.networkHandler;

        if (client.options.keyPlayerList.isPressed() && (!client.isInSingleplayer() || handler.getPlayerList().size() > 1 || scoreobjective != null))
        {
            this.playerListHud.tick(true);
            if (pre(PLAYER_LIST)) return;
            this.playerListHud.render(width, this.client.world.getScoreboard(), scoreobjective);
            post(PLAYER_LIST);
        }
        else
        {
            this.playerListHud.tick(false);
        }
    }

    protected void renderHealthMount(int width, int height)
    {
        PlayerEntity player = (PlayerEntity)client.getCameraEntity();
        Entity tmp = player.getVehicle();
        if (!(tmp instanceof LivingEntity)) return;

        bind(GUI_ICONS_LOCATION);

        if (pre(HEALTHMOUNT)) return;

        boolean unused = false;
        int left_align = width / 2 + 91;

        client.getProfiler().swap("mountHealth");
        RenderSystem.enableBlend();
        LivingEntity mount = (LivingEntity)tmp;
        int health = (int)Math.ceil((double)mount.getHealth());
        float healthMax = mount.getMaximumHealth();
        int hearts = (int)(healthMax + 0.5F) / 2;

        if (hearts > 30) hearts = 30;

        final int MARGIN = 52;
        final int BACKGROUND = MARGIN + (unused ? 1 : 0);
        final int HALF = MARGIN + 45;
        final int FULL = MARGIN + 36;

        for (int heart = 0; hearts > 0; heart += 20)
        {
            int top = height - right_height;

            int rowCount = Math.min(hearts, 10);
            hearts -= rowCount;

            for (int i = 0; i < rowCount; ++i)
            {
                int x = left_align - i * 8 - 9;
                blit(x, top, BACKGROUND, 9, 9, 9);

                if (i * 2 + 1 + heart < health)
                    blit(x, top, FULL, 9, 9, 9);
                else if (i * 2 + 1 + heart == health)
                    blit(x, top, HALF, 9, 9, 9);
            }

            right_height += 10;
        }
        RenderSystem.disableBlend();
        post(HEALTHMOUNT);
    }

    //Helper macros
    private boolean pre(ElementType type)
    {
        return MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Pre(eventParent, type));
    }
    private void post(ElementType type)
    {
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(eventParent, type));
    }
    private void bind(Identifier res)
    {
        client.getTextureManager().bindTexture(res);
    }

    private class GuiOverlayDebugForge extends DebugHud
    {
        private MinecraftClient mc;
        private GuiOverlayDebugForge(MinecraftClient mc)
        {
            super(mc);
            this.mc = mc;
        }
        public void update()
        {
            Entity entity = this.mc.getCameraEntity();
            this.blockHit = entity.rayTrace(rayTraceDistance, 0.0F, false);
            this.fluidHit = entity.rayTrace(rayTraceDistance, 0.0F, true);
        }
        @Override protected void renderLeftText(){}
        @Override protected void renderRightText(){}
        private List<String> getLeft()
        {
            List<String> ret = this.getLeftText();
            ret.add("");
            ret.add("Debug: Pie [shift]: " + (this.mc.options.debugProfilerEnabled ? "visible" : "hidden") + " FPS [alt]: " + (this.mc.options.debugTpsEnabled ? "visible" : "hidden"));
            ret.add("For help: press F3 + Q");
            return ret;
        }
        private List<String> getRight(){ return this.getRightText(); }
    }
}
