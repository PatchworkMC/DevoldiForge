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

package net.minecraftforge.common;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.advancement.Advancement;
import net.minecraft.block.Block;
import net.minecraft.fluid.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.container.AnvilContainer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.item.Item;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.TippedArrowItem;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.tag.Tag;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Int2ObjectBiMap;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.IWorld;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifierManager;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.DifficultyChangeEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.registries.DataSerializerEntry;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.IRegistryDelegate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.util.TriConsumer;

public class ForgeHooks
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker FORGEHOOKS = MarkerManager.getMarker("FORGEHOOKS");

    public static boolean canContinueUsing(@Nonnull ItemStack from, @Nonnull ItemStack to)
    {
        if (!from.isEmpty() && !to.isEmpty())
        {
            return from.getItem().canContinueUsing(from, to);
        }
        return false;
    }

    public static boolean canHarvestBlock(@Nonnull BlockState state, @Nonnull PlayerEntity player, @Nonnull BlockView world, @Nonnull BlockPos pos)
    {
        //state = state.getActualState(world, pos);
        if (state.getMaterial().canBreakByHand())
        {
            return true;
        }

        ItemStack stack = player.getMainHandStack();
        ToolType tool = state.getHarvestTool();
        if (stack.isEmpty() || tool == null)
        {
            return player.isUsingEffectiveTool(state);
        }

        int toolLevel = stack.getItem().getHarvestLevel(stack, tool, player, state);
        if (toolLevel < 0)
        {
            return player.isUsingEffectiveTool(state);
        }

        return ForgeEventFactory.doPlayerHarvestCheck(player, state, toolLevel >= state.getHarvestLevel());
    }

    public static boolean canToolHarvestBlock(WorldView world, BlockPos pos, @Nonnull ItemStack stack)
    {
        BlockState state = world.getBlockState(pos);
        //state = state.getActualState(world, pos);
        ToolType tool = state.getHarvestTool();
        if (stack.isEmpty() || tool == null) return false;
        return stack.getHarvestLevel(tool, null, null) >= state.getHarvestLevel();
    }

    public static boolean isToolEffective(WorldView world, BlockPos pos, @Nonnull ItemStack stack)
    {
        BlockState state = world.getBlockState(pos);
        //state = state.getActualState(world, pos);
        for (ToolType type : stack.getToolTypes())
        {
            if (state.isToolEffective(type))
                return true;
        }
        return false;
    }

    private static boolean toolInit = false;
    static void initTools()
    {
        if (toolInit)
            return;
        toolInit = true;

        Set<Block> blocks = getPrivateValue(PickaxeItem.class, null, 0);
        blocks.forEach(block -> blockToolSetter.accept(block, ToolType.PICKAXE, 0));
        blocks = getPrivateValue(ShovelItem.class, null, 0);
        blocks.forEach(block -> blockToolSetter.accept(block, ToolType.SHOVEL, 0));
        blocks = getPrivateValue(AxeItem.class, null, 0);
        blocks.forEach(block -> blockToolSetter.accept(block, ToolType.AXE, 0));

        //This is taken from ItemAxe, if that changes update here.
        blockToolSetter.accept(Blocks.OBSIDIAN, ToolType.PICKAXE, 3);
        for (Block block : new Block[]{Blocks.DIAMOND_BLOCK, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.EMERALD_BLOCK, Blocks.GOLD_BLOCK, Blocks.GOLD_ORE, Blocks.REDSTONE_ORE})
            blockToolSetter.accept(block, ToolType.PICKAXE, 2);
        for (Block block : new Block[]{Blocks.IRON_BLOCK, Blocks.IRON_ORE, Blocks.LAPIS_BLOCK, Blocks.LAPIS_ORE})
            blockToolSetter.accept(block, ToolType.PICKAXE, 1);
    }

    /**
     * Called when a player uses 'pick block', calls new Entity and Block hooks.
     */
    public static boolean onPickBlock(HitResult target, PlayerEntity player, World world)
    {
        ItemStack result = ItemStack.EMPTY;
        boolean isCreative = player.abilities.creativeMode;
        BlockEntity te = null;

        if (target.getType() == HitResult.Type.BLOCK)
        {
            BlockPos pos = ((BlockHitResult)target).getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (state.isAir(world, pos))
                return false;

            if (isCreative && Screen.hasControlDown() && state.hasTileEntity())
                te = world.getBlockEntity(pos);

            result = state.getBlock().getPickBlock(state, target, world, pos, player);

            if (result.isEmpty())
                LOGGER.warn("Picking on: [{}] {} gave null item", target.getType(), state.getBlock().getRegistryName());
        }
        else if (target.getType() == HitResult.Type.ENTITY)
        {
            Entity entity = ((EntityHitResult)target).getEntity();
            result = entity.getPickedResult(target);

            if (result.isEmpty())
                LOGGER.warn("Picking on: [{}] {} gave null item", target.getType(), entity.getType().getRegistryName());
        }

        if (result.isEmpty())
            return false;

        if (te != null)
            MinecraftClient.getInstance().addBlockEntityNbt(result, te);

        if (isCreative)
        {
            player.inventory.addPickBlock(result);
            MinecraftClient.getInstance().interactionManager.clickCreativeStack(player.getStackInHand(Hand.MAIN_HAND), 36 + player.inventory.selectedSlot);
            return true;
        }
        int slot = player.inventory.getSlotWithStack(result);
        if (slot != -1)
        {
            if (PlayerInventory.isValidHotbarIndex(slot))
                player.inventory.selectedSlot = slot;
            else
                MinecraftClient.getInstance().interactionManager.pickFromInventory(slot);
            return true;
        }
        return false;
    }

    public static void onDifficultyChange(Difficulty difficulty, Difficulty oldDifficulty)
    {
        MinecraftForge.EVENT_BUS.post(new DifficultyChangeEvent(difficulty, oldDifficulty));
    }

    //Optifine Helper Functions u.u, these are here specifically for Optifine
    //Note: When using Optifine, these methods are invoked using reflection, which
    //incurs a major performance penalty.
    public static void onLivingSetAttackTarget(LivingEntity entity, LivingEntity target)
    {
        MinecraftForge.EVENT_BUS.post(new LivingSetAttackTargetEvent(entity, target));
    }

    public static boolean onLivingUpdate(LivingEntity entity)
    {
        return MinecraftForge.EVENT_BUS.post(new LivingUpdateEvent(entity));
    }

    public static boolean onLivingAttack(LivingEntity entity, DamageSource src, float amount)
    {
        return entity instanceof PlayerEntity || !MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(entity, src, amount));
    }

    public static boolean onPlayerAttack(LivingEntity entity, DamageSource src, float amount)
    {
        return !MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(entity, src, amount));
    }

    public static LivingKnockBackEvent onLivingKnockBack(LivingEntity target, Entity attacker, float strength, double ratioX, double ratioZ)
    {
        LivingKnockBackEvent event = new LivingKnockBackEvent(target, attacker, strength, ratioX, ratioZ);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    public static float onLivingHurt(LivingEntity entity, DamageSource src, float amount)
    {
        LivingHurtEvent event = new LivingHurtEvent(entity, src, amount);
        return (MinecraftForge.EVENT_BUS.post(event) ? 0 : event.getAmount());
    }

    public static float onLivingDamage(LivingEntity entity, DamageSource src, float amount)
    {
        LivingDamageEvent event = new LivingDamageEvent(entity, src, amount);
        return (MinecraftForge.EVENT_BUS.post(event) ? 0 : event.getAmount());
    }

    public static boolean onLivingDeath(LivingEntity entity, DamageSource src)
    {
        return MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(entity, src));
    }

    public static boolean onLivingDrops(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit)
    {
        return MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(entity, source, drops, lootingLevel, recentlyHit));
    }

    @Nullable
    public static float[] onLivingFall(LivingEntity entity, float distance, float damageMultiplier)
    {
        LivingFallEvent event = new LivingFallEvent(entity, distance, damageMultiplier);
        return (MinecraftForge.EVENT_BUS.post(event) ? null : new float[]{event.getDistance(), event.getDamageMultiplier()});
    }

    public static int getLootingLevel(Entity target, @Nullable Entity killer, DamageSource cause)
    {
        int looting = 0;
        if (killer instanceof LivingEntity)
            looting = EnchantmentHelper.getLooting((LivingEntity)killer);
        if (target instanceof LivingEntity)
            looting = getLootingLevel((LivingEntity)target, cause, looting);
        return looting;
    }

    public static int getLootingLevel(LivingEntity target, DamageSource cause, int level)
    {
        LootingLevelEvent event = new LootingLevelEvent(target, cause, level);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getLootingLevel();
    }

    public static double getPlayerVisibilityDistance(PlayerEntity player, double xzDistance, double maxXZDistance)
    {
        PlayerEvent.Visibility event = new PlayerEvent.Visibility(player);
        MinecraftForge.EVENT_BUS.post(event);
        double value = event.getVisibilityModifier() * xzDistance;
        return value >= maxXZDistance ? maxXZDistance : value;
    }

    public static boolean isLivingOnLadder(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull LivingEntity entity)
    {
        boolean isSpectator = (entity instanceof PlayerEntity && ((PlayerEntity)entity).isSpectator());
        if (isSpectator) return false;
        if (!ForgeConfig.SERVER.fullBoundingBoxLadders.get())
        {
            return state.isLadder(world, pos, entity);
        }
        else
        {
            Box bb = entity.getBoundingBox();
            int mX = MathHelper.floor(bb.x1);
            int mY = MathHelper.floor(bb.y1);
            int mZ = MathHelper.floor(bb.z1);
            for (int y2 = mY; y2 < bb.y2; y2++)
            {
                for (int x2 = mX; x2 < bb.x2; x2++)
                {
                    for (int z2 = mZ; z2 < bb.z2; z2++)
                    {
                        BlockPos tmp = new BlockPos(x2, y2, z2);
                        state = world.getBlockState(tmp);
                        if (state.isLadder(world, tmp, entity))
                        {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public static void onLivingJump(LivingEntity entity)
    {
        MinecraftForge.EVENT_BUS.post(new LivingJumpEvent(entity));
    }

    @Nullable
    public static ItemEntity onPlayerTossEvent(@Nonnull PlayerEntity player, @Nonnull ItemStack item, boolean includeName)
    {
        player.captureDrops(Lists.newArrayList());
        ItemEntity ret = player.dropItem(item, false, includeName);
        player.captureDrops(null);

        if (ret == null)
            return null;

        ItemTossEvent event = new ItemTossEvent(ret, player);
        if (MinecraftForge.EVENT_BUS.post(event))
            return null;

        if (!player.world.isClient)
            player.getEntityWorld().spawnEntity(event.getEntityItem());
        return event.getEntityItem();
    }

    @Nullable
    public static Text onServerChatEvent(ServerPlayNetworkHandler net, String raw, Text comp)
    {
        ServerChatEvent event = new ServerChatEvent(net.player, raw, comp);
        if (MinecraftForge.EVENT_BUS.post(event))
        {
            return null;
        }
        return event.getComponent();
    }


    static final Pattern URL_PATTERN = Pattern.compile(
            //         schema                          ipv4            OR        namespace                 port     path         ends
            //   |-----------------|        |-------------------------|  |-------------------------|    |---------| |--|   |---------------|
            "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"\u00A7 \n]|$))",
            Pattern.CASE_INSENSITIVE);

    public static Text newChatWithLinks(String string){ return newChatWithLinks(string, true); }
    public static Text newChatWithLinks(String string, boolean allowMissingHeader)
    {
        // Includes ipv4 and domain pattern
        // Matches an ip (xx.xxx.xx.xxx) or a domain (something.com) with or
        // without a protocol or path.
        Text ichat = null;
        Matcher matcher = URL_PATTERN.matcher(string);
        int lastEnd = 0;

        // Find all urls
        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();

            // Append the previous left overs.
            String part = string.substring(lastEnd, start);
            if (part.length() > 0)
            {
                if (ichat == null)
                    ichat = new LiteralText(part);
                else
                    ichat.append(part);
            }
            lastEnd = end;
            String url = string.substring(start, end);
            Text link = new LiteralText(url);

            try
            {
                // Add schema so client doesn't crash.
                if ((new URI(url)).getScheme() == null)
                {
                    if (!allowMissingHeader)
                    {
                        if (ichat == null)
                            ichat = new LiteralText(url);
                        else
                            ichat.append(url);
                        continue;
                    }
                    url = "http://" + url;
                }
            }
            catch (URISyntaxException e)
            {
                // Bad syntax bail out!
                if (ichat == null) ichat = new LiteralText(url);
                else ichat.append(url);
                continue;
            }

            // Set the click event and append the link.
            ClickEvent click = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
            link.getStyle().setClickEvent(click);
            link.getStyle().setUnderline(true);
            link.getStyle().setColor(Formatting.BLUE);
            if (ichat == null)
                ichat = new LiteralText("");
            ichat.append(link);
        }

        // Append the rest of the message.
        String end = string.substring(lastEnd);
        if (ichat == null)
            ichat = new LiteralText(end);
        else if (end.length() > 0)
            ichat.append(string.substring(lastEnd));
        return ichat;
    }

    public static int onBlockBreakEvent(World world, GameMode gameType, ServerPlayerEntity entityPlayer, BlockPos pos)
    {
        // Logic from tryHarvestBlock for pre-canceling the event
        boolean preCancelEvent = false;
        ItemStack itemstack = entityPlayer.getMainHandStack();
        if (!itemstack.isEmpty() && !itemstack.getItem().canMine(world.getBlockState(pos), world, pos, entityPlayer))
        {
            preCancelEvent = true;
        }

        if (gameType.shouldLimitWorldModification())
        {
            if (gameType == GameMode.SPECTATOR)
                preCancelEvent = true;

            if (!entityPlayer.canModifyWorld())
            {
                if (itemstack.isEmpty() || !itemstack.canDestroy(world.getTagManager(), new CachedBlockPosition(world, pos, false)))
                    preCancelEvent = true;
            }
        }

        // Tell client the block is gone immediately then process events
        if (world.getBlockEntity(pos) == null)
        {
            entityPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(DUMMY_WORLD, pos));
        }

        // Post the block break event
        BlockState state = world.getBlockState(pos);
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, state, entityPlayer);
        event.setCanceled(preCancelEvent);
        MinecraftForge.EVENT_BUS.post(event);

        // Handle if the event is canceled
        if (event.isCanceled())
        {
            // Let the client know the block still exists
            entityPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, pos));

            // Update any tile entity data for this block
            BlockEntity tileentity = world.getBlockEntity(pos);
            if (tileentity != null)
            {
                Packet<?> pkt = tileentity.toUpdatePacket();
                if (pkt != null)
                {
                    entityPlayer.networkHandler.sendPacket(pkt);
                }
            }
        }
        return event.isCanceled() ? -1 : event.getExpToDrop();
    }

    public static ActionResult onPlaceItemIntoWorld(@Nonnull ItemUsageContext context)
    {
        ItemStack itemstack = context.getStack();
        World world = context.getWorld();

        PlayerEntity player = context.getPlayer();
        if (player != null && !player.abilities.allowModifyWorld && !itemstack.canPlaceOn(world.getTagManager(), new CachedBlockPosition(world, context.getBlockPos(), false)))
            return ActionResult.PASS;

        // handle all placement events here
        Item item = itemstack.getItem();
        int size = itemstack.getCount();
        CompoundTag nbt = null;
        if (itemstack.getTag() != null)
            nbt = itemstack.getTag().copy();

        if (!(itemstack.getItem() instanceof BucketItem)) // if not bucket
            world.captureBlockSnapshots = true;

        ItemStack copy = itemstack.copy();
        ActionResult ret = itemstack.getItem().useOnBlock(context);
        if (itemstack.isEmpty())
            ForgeEventFactory.onPlayerDestroyItem(player, copy, context.getHand());

        world.captureBlockSnapshots = false;

        if (ret == ActionResult.SUCCESS)
        {
            // save new item data
            int newSize = itemstack.getCount();
            CompoundTag newNBT = null;
            if (itemstack.getTag() != null)
            {
                newNBT = itemstack.getTag().copy();
            }
            @SuppressWarnings("unchecked")
            List<BlockSnapshot> blockSnapshots = (List<BlockSnapshot>)world.capturedBlockSnapshots.clone();
            world.capturedBlockSnapshots.clear();

            // make sure to set pre-placement item data for event
            itemstack.setCount(size);
            itemstack.setTag(nbt);

            Direction side = context.getSide();

            boolean eventResult = false;
            if (blockSnapshots.size() > 1)
            {
                eventResult = ForgeEventFactory.onMultiBlockPlace(player, blockSnapshots, side);
            }
            else if (blockSnapshots.size() == 1)
            {
                eventResult = ForgeEventFactory.onBlockPlace(player, blockSnapshots.get(0), side);
            }

            if (eventResult)
            {
                ret = ActionResult.FAIL; // cancel placement
                // revert back all captured blocks
                for (BlockSnapshot blocksnapshot : Lists.reverse(blockSnapshots))
                {
                    world.restoringBlockSnapshots = true;
                    blocksnapshot.restore(true, false);
                    world.restoringBlockSnapshots = false;
                }
            }
            else
            {
                // Change the stack to its new content
                itemstack.setCount(newSize);
                itemstack.setTag(newNBT);

                for (BlockSnapshot snap : blockSnapshots)
                {
                    int updateFlag = snap.getFlag();
                    BlockState oldBlock = snap.getReplacedBlock();
                    BlockState newBlock = world.getBlockState(snap.getPos());
                    if (!newBlock.getBlock().hasTileEntity(newBlock)) // Containers get placed automatically
                    {
                        newBlock.onBlockAdded(world, snap.getPos(), oldBlock, false);
                    }

                    world.markAndNotifyBlock(snap.getPos(), null, oldBlock, newBlock, updateFlag);
                }
                player.incrementStat(Stats.USED.getOrCreateStat(item));
            }
        }
        world.capturedBlockSnapshots.clear();

        return ret;
    }

    public static boolean onAnvilChange(AnvilContainer container, @Nonnull ItemStack left, @Nonnull ItemStack right, Inventory outputSlot, String name, int baseCost)
    {
        AnvilUpdateEvent e = new AnvilUpdateEvent(left, right, name, baseCost);
        if (MinecraftForge.EVENT_BUS.post(e)) return false;
        if (e.getOutput().isEmpty()) return true;

        outputSlot.setInvStack(0, e.getOutput());
        container.setMaximumCost(e.getCost());
        container.repairItemUsage = e.getMaterialCost();
        return false;
    }

    public static float onAnvilRepair(PlayerEntity player, @Nonnull ItemStack output, @Nonnull ItemStack left, @Nonnull ItemStack right)
    {
        AnvilRepairEvent e = new AnvilRepairEvent(player, left, right, output);
        MinecraftForge.EVENT_BUS.post(e);
        return e.getBreakChance();
    }

    private static ThreadLocal<PlayerEntity> craftingPlayer = new ThreadLocal<PlayerEntity>();
    public static void setCraftingPlayer(PlayerEntity player)
    {
        craftingPlayer.set(player);
    }
    public static PlayerEntity getCraftingPlayer()
    {
        return craftingPlayer.get();
    }
    @Nonnull
    public static ItemStack getContainerItem(@Nonnull ItemStack stack)
    {
        if (stack.getItem().hasContainerItem(stack))
        {
            stack = stack.getItem().getContainerItem(stack);
            if (!stack.isEmpty() && stack.isDamageable() && stack.getDamage() > stack.getMaxDamage())
            {
                ForgeEventFactory.onPlayerDestroyItem(craftingPlayer.get(), stack, null);
                return ItemStack.EMPTY;
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public static boolean onPlayerAttackTarget(PlayerEntity player, Entity target)
    {
        if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, target))) return false;
        ItemStack stack = player.getMainHandStack();
        return stack.isEmpty() || !stack.getItem().onLeftClickEntity(stack, player, target);
    }

    public static boolean onTravelToDimension(Entity entity, DimensionType dimension)
    {
        EntityTravelToDimensionEvent event = new EntityTravelToDimensionEvent(entity, dimension);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
        {
            // Revert variable back to true as it would have been set to false
            if (entity instanceof StorageMinecartEntity)
            {
               ((StorageMinecartEntity) entity).dropContentsWhenDead(true);
            }
        }
        return !event.isCanceled();
    }

    public static ActionResult onInteractEntityAt(PlayerEntity player, Entity entity, HitResult ray, Hand hand)
    {
        Vec3d vec3d = ray.getPos().subtract(entity.getPos());
        return onInteractEntityAt(player, entity, vec3d, hand);
    }

    public static ActionResult onInteractEntityAt(PlayerEntity player, Entity entity, Vec3d vec3d, Hand hand)
    {
        PlayerInteractEvent.EntityInteractSpecific evt = new PlayerInteractEvent.EntityInteractSpecific(player, hand, entity, vec3d);
        MinecraftForge.EVENT_BUS.post(evt);
        return evt.isCanceled() ? evt.getCancellationResult() : null;
    }

    public static ActionResult onInteractEntity(PlayerEntity player, Entity entity, Hand hand)
    {
        PlayerInteractEvent.EntityInteract evt = new PlayerInteractEvent.EntityInteract(player, hand, entity);
        MinecraftForge.EVENT_BUS.post(evt);
        return evt.isCanceled() ? evt.getCancellationResult() : null;
    }

    public static ActionResult onItemRightClick(PlayerEntity player, Hand hand)
    {
        PlayerInteractEvent.RightClickItem evt = new PlayerInteractEvent.RightClickItem(player, hand);
        MinecraftForge.EVENT_BUS.post(evt);
        return evt.isCanceled() ? evt.getCancellationResult() : null;
    }

    public static PlayerInteractEvent.LeftClickBlock onLeftClickBlock(PlayerEntity player, BlockPos pos, Direction face)
    {
        PlayerInteractEvent.LeftClickBlock evt = new PlayerInteractEvent.LeftClickBlock(player, pos, face);
        MinecraftForge.EVENT_BUS.post(evt);
        return evt;
    }

    public static PlayerInteractEvent.RightClickBlock onRightClickBlock(PlayerEntity player, Hand hand, BlockPos pos, Direction face)
    {
        PlayerInteractEvent.RightClickBlock evt = new PlayerInteractEvent.RightClickBlock(player, hand, pos, face);
        MinecraftForge.EVENT_BUS.post(evt);
        return evt;
    }

    public static void onEmptyClick(PlayerEntity player, Hand hand)
    {
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickEmpty(player, hand));
    }

    public static void onEmptyLeftClick(PlayerEntity player)
    {
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickEmpty(player));
    }

    private static ThreadLocal<Deque<LootTableContext>> lootContext = new ThreadLocal<Deque<LootTableContext>>();
    private static LootTableContext getLootTableContext()
    {
        LootTableContext ctx = lootContext.get().peek();

        if (ctx == null)
            throw new JsonParseException("Invalid call stack, could not grab json context!"); // Should I throw this? Do we care about custom deserializers outside the manager?

        return ctx;
    }

    @Nullable
    public static LootTable loadLootTable(Gson gson, Identifier name, JsonObject data, boolean custom, LootManager lootTableManager)
    {
        Deque<LootTableContext> que = lootContext.get();
        if (que == null)
        {
            que = Queues.newArrayDeque();
            lootContext.set(que);
        }

        LootTable ret = null;
        try
        {
            que.push(new LootTableContext(name, custom));
            ret = gson.fromJson(data, LootTable.class);
            que.pop();
        }
        catch (JsonParseException e)
        {
            que.pop();
            throw e;
        }

        if (!custom)
            ret = ForgeEventFactory.loadLootTable(name, ret, lootTableManager);

        if (ret != null)
            ret.freeze();

        return ret;
    }

    public static FluidAttributes createVanillaFluidAttributes(Fluid fluid)
    {
        if (fluid instanceof EmptyFluid)
            return net.minecraftforge.fluids.FluidAttributes.builder(null, null)
                    .translationKey("block.minecraft.air")
                    .color(0).density(0).temperature(0).luminosity(0).viscosity(0).build(fluid);
        if (fluid instanceof WaterFluid)
            return net.minecraftforge.fluids.FluidAttributes.Water.builder(
                    new Identifier("block/water_still"),
                    new Identifier("block/water_flow"))
                    .overlay(new Identifier("block/water_overlay"))
                    .translationKey("block.minecraft.water")
                    .color(0xFF3F76E4).build(fluid);
        if (fluid instanceof LavaFluid)
            return net.minecraftforge.fluids.FluidAttributes.builder(
                    new Identifier("block/lava_still"),
                    new Identifier("block/lava_flow"))
                    .translationKey("block.minecraft.lava")
                    .luminosity(15).density(3000).viscosity(6000).temperature(1300).build(fluid);
        throw new RuntimeException("Mod fluids must override createAttributes.");
    }

    private static class LootTableContext
    {
        public final Identifier name;
        private final boolean vanilla;
        public final boolean custom;
        public int poolCount = 0;
        public int entryCount = 0;
        private HashSet<String> entryNames = Sets.newHashSet();

        private LootTableContext(Identifier name, boolean custom)
        {
            this.name = name;
            this.custom = custom;
            this.vanilla = "minecraft".equals(this.name.getNamespace());
        }

        private void resetPoolCtx()
        {
            this.entryCount = 0;
            this.entryNames.clear();
        }

        public String validateEntryName(@Nullable String name)
        {
            if (name != null && !this.entryNames.contains(name))
            {
                this.entryNames.add(name);
                return name;
            }

            if (!this.vanilla)
                throw new JsonParseException("Loot Table \"" + this.name.toString() + "\" Duplicate entry name \"" + name + "\" for pool #" + (this.poolCount - 1) + " entry #" + (this.entryCount-1));

            int x = 0;
            while (this.entryNames.contains(name + "#" + x))
                x++;

            name = name + "#" + x;
            this.entryNames.add(name);

            return name;
        }
    }

    public static String readPoolName(JsonObject json)
    {
        LootTableContext ctx = ForgeHooks.getLootTableContext();
        ctx.resetPoolCtx();

        if (json.has("name"))
            return JsonHelper.getString(json, "name");

        if (ctx.custom)
            return "custom#" + json.hashCode(); //We don't care about custom ones modders shouldn't be editing them!

        ctx.poolCount++;

        if (!ctx.vanilla)
            throw new JsonParseException("Loot Table \"" + ctx.name.toString() + "\" Missing `name` entry for pool #" + (ctx.poolCount - 1));

        return ctx.poolCount == 1 ? "main" : "pool" + (ctx.poolCount - 1);
    }

    public static String readLootEntryName(JsonObject json, String type)
    {
        LootTableContext ctx = ForgeHooks.getLootTableContext();
        ctx.entryCount++;

        if (json.has("entryName"))
            return ctx.validateEntryName(JsonHelper.getString(json, "entryName"));

        if (ctx.custom)
            return "custom#" + json.hashCode(); //We don't care about custom ones modders shouldn't be editing them!

        String name = null;
        if ("item".equals(type))
            name = JsonHelper.getString(json, "name");
        else if ("loot_table".equals(type))
            name = JsonHelper.getString(json, "name");
        else if ("empty".equals(type))
            name = "empty";

        return ctx.validateEntryName(name);
    }

    public static boolean onCropsGrowPre(World worldIn, BlockPos pos, BlockState state, boolean def)
    {
        BlockEvent ev = new BlockEvent.CropGrowEvent.Pre(worldIn,pos,state);
        MinecraftForge.EVENT_BUS.post(ev);
        return (ev.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || (ev.getResult() == net.minecraftforge.eventbus.api.Event.Result.DEFAULT && def));
    }

    public static void onCropsGrowPost(World worldIn, BlockPos pos, BlockState state)
    {
        MinecraftForge.EVENT_BUS.post(new BlockEvent.CropGrowEvent.Post(worldIn, pos, state, worldIn.getBlockState(pos)));
    }

    @Nullable
    public static CriticalHitEvent getCriticalHit(PlayerEntity player, Entity target, boolean vanillaCritical, float damageModifier)
    {
        CriticalHitEvent hitResult = new CriticalHitEvent(player, target, damageModifier, vanillaCritical);
        MinecraftForge.EVENT_BUS.post(hitResult);
        if (hitResult.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || (vanillaCritical && hitResult.getResult() == net.minecraftforge.eventbus.api.Event.Result.DEFAULT))
        {
            return hitResult;
        }
        return null;
    }

    public static void onAdvancement(ServerPlayerEntity player, Advancement advancement)
    {
        MinecraftForge.EVENT_BUS.post(new AdvancementEvent(player, advancement));
    }

    /**
     * Used as the default implementation of {@link Item#getCreatorModId}. Call that method instead.
     */
    @Nullable
    public static String getDefaultCreatorModId(@Nonnull ItemStack itemStack)
    {
        Item item = itemStack.getItem();
        Identifier registryName = item.getRegistryName();
        String modId = registryName == null ? null : registryName.getNamespace();
        if ("minecraft".equals(modId))
        {
            if (item instanceof EnchantedBookItem)
            {
                ListTag enchantmentsNbt = EnchantedBookItem.getEnchantmentTag(itemStack);
                if (enchantmentsNbt.size() == 1)
                {
                    CompoundTag nbttagcompound = enchantmentsNbt.getCompound(0);
                    Identifier resourceLocation = Identifier.tryParse(nbttagcompound.getString("id"));
                    if (resourceLocation != null && ForgeRegistries.ENCHANTMENTS.containsKey(resourceLocation))
                    {
                        return resourceLocation.getNamespace();
                    }
                }
            }
            else if (item instanceof PotionItem || item instanceof TippedArrowItem)
            {
                Potion potionType = PotionUtil.getPotion(itemStack);
                Identifier resourceLocation = ForgeRegistries.POTION_TYPES.getKey(potionType);
                if (resourceLocation != null)
                {
                    return resourceLocation.getNamespace();
                }
            }
            else if (item instanceof SpawnEggItem)
            {
                Identifier resourceLocation = ((SpawnEggItem)item).getEntityType(null).getRegistryName();
                if (resourceLocation != null)
                {
                    return resourceLocation.getNamespace();
                }
            }
        }
        return modId;
    }

    public static boolean onFarmlandTrample(World world, BlockPos pos, BlockState state, float fallDistance, Entity entity)
    {
        if (entity.canTrample(state, pos, fallDistance))
        {
            BlockEvent.FarmlandTrampleEvent event = new BlockEvent.FarmlandTrampleEvent(world, pos, state, fallDistance, entity);
            MinecraftForge.EVENT_BUS.post(event);
            return !event.isCanceled();
        }
        return false;
    }

    private static TriConsumer<Block, ToolType, Integer> blockToolSetter;
    //Internal use only Modders, this is specifically hidden from you, as you shouldn't be editing other people's blocks.
    public static void setBlockToolSetter(TriConsumer<Block, ToolType, Integer> setter)
    {
        blockToolSetter = setter;
    }
    @SuppressWarnings("unchecked")
    private static <T, E> T getPrivateValue(Class <? super E > classToAccess, @Nullable E instance, int fieldIndex)
    {
        try
        {
            Field f = classToAccess.getDeclaredFields()[fieldIndex];
            f.setAccessible(true);
            return (T) f.get(instance);
        }
        catch (Exception e)
        {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    private static final DummyBlockReader DUMMY_WORLD = new DummyBlockReader();
    private static class DummyBlockReader implements BlockView {

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return Blocks.AIR.getDefaultState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.getDefaultState();
        }

    }

    public static int onNoteChange(World world, BlockPos pos, BlockState state, int old, int _new) {
        NoteBlockEvent.Change event = new NoteBlockEvent.Change(world, pos, state, old, _new);
        if (MinecraftForge.EVENT_BUS.post(event))
            return -1;
        return event.getVanillaNoteId();
    }

    public static int canEntitySpawn(MobEntity entity, IWorld world, double x, double y, double z, MobSpawnerLogic spawner, SpawnType spawnReason) {
        Result res = ForgeEventFactory.canEntitySpawn(entity, world, x, y, z, null, spawnReason);
        return res == Result.DEFAULT ? 0 : res == Result.DENY ? -1 : 1;
    }

    @SuppressWarnings("deprecation")
    public static <T> void deserializeTagAdditions(Tag.Builder<T> builder, Function<Identifier, Optional<T>> valueGetter, JsonObject json)
    {
        if (json.has("optional"))
        {
            for (JsonElement entry : JsonHelper.getArray(json, "optional"))
            {
                String s = JsonHelper.asString(entry, "value");
                if (!s.startsWith("#"))
                    builder.addOptional(valueGetter, Collections.singleton(new Identifier(s)));
                else
                    builder.addOptionalTag(new Identifier(s.substring(1)));
            }
        }

        if (json.has("remove"))
        {
            for (JsonElement entry : JsonHelper.getArray(json, "remove"))
            {
                String s = JsonHelper.asString(entry, "value");
                if (!s.startsWith("#"))
                {
                    T value = valueGetter.apply(new Identifier(s)).orElse(null);
                    if (value != null)
                    {
                        Tag.Entry<T> dummyEntry = new Tag.CollectionEntry<>(Collections.singleton(value));
                        builder.remove(dummyEntry);
                    }
                } else
                {
                    Tag.Entry<T> dummyEntry = new Tag.TagEntry<>(new Identifier(s.substring(1)));
                    builder.remove(dummyEntry);
                }
            }
        }
    }

    private static final Map<TrackedDataHandler<?>, DataSerializerEntry> serializerEntries = GameData.getSerializerMap();
    //private static final ForgeRegistry<DataSerializerEntry> serializerRegistry = (ForgeRegistry<DataSerializerEntry>) ForgeRegistries.DATA_SERIALIZERS;
    // Do not reimplement this ^ it introduces a chicken-egg scenario by classloading registries during bootstrap

    @Nullable
    public static TrackedDataHandler<?> getSerializer(int id, Int2ObjectBiMap<TrackedDataHandler<?>> vanilla)
    {
        TrackedDataHandler<?> serializer = vanilla.get(id);
        if (serializer == null)
        {
            DataSerializerEntry entry = ((ForgeRegistry<DataSerializerEntry>)ForgeRegistries.DATA_SERIALIZERS).getValue(id);
            if (entry != null) serializer = entry.getSerializer();
        }
        return serializer;
    }

    public static int getSerializerId(TrackedDataHandler<?> serializer, Int2ObjectBiMap<TrackedDataHandler<?>> vanilla)
    {
        int id = vanilla.getId(serializer);
        if (id < 0)
        {
            DataSerializerEntry entry = serializerEntries.get(serializer);
            if (entry != null) id = ((ForgeRegistry<DataSerializerEntry>)ForgeRegistries.DATA_SERIALIZERS).getID(entry);
        }
        return id;
    }

    public static boolean canEntityDestroy(World world, BlockPos pos, LivingEntity entity)
    {
        BlockState state = world.getBlockState(pos);
        return ForgeEventFactory.getMobGriefingEvent(world, entity) && state.canEntityDestroy(world, pos, entity) && ForgeEventFactory.onEntityDestroyBlock(entity, pos, state);
    }

    private static final Map<IRegistryDelegate<Item>, Integer> VANILLA_BURNS = new HashMap<>();

    /**
     * Gets the burn time of this itemstack.
     */
    public static int getBurnTime(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return 0;
        }
        else
        {
            Item item = stack.getItem();
            int ret = stack.getBurnTime();
            return ForgeEventFactory.getItemBurnTime(stack, ret == -1 ? VANILLA_BURNS.getOrDefault(item.delegate, 0) : ret);
        }
    }

    @SuppressWarnings("deprecation")
    public static synchronized void updateBurns()
    {
        VANILLA_BURNS.clear();
        FurnaceBlockEntity.createFuelTimeMap().entrySet().forEach(e -> VANILLA_BURNS.put(e.getKey().delegate, e.getValue()));
    }

    /**
     * All loot table drops should be passed to this function so that mod added effects
     * (e.g. smelting enchantments) can be processed.
     * @param list The loot generated
     * @param context The loot context that generated that loot
     * @return The modified list
     */
    public static List<ItemStack> modifyLoot(List<ItemStack> list, LootContext context) {
        LootModifierManager man = context.getWorld().getServer().getLootModifierManager();
        for(IGlobalLootModifier mod : man.getAllLootMods()) {
            list = mod.apply(list, context);
        }
        return list;
    }

}
