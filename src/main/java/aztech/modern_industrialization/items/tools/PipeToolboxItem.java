package aztech.modern_industrialization.items.tools;

import aztech.modern_industrialization.MIIdentifier;
import aztech.modern_industrialization.fluid.MIFluid;
import aztech.modern_industrialization.items.FluidFuelItemHelper;
import aztech.modern_industrialization.pipes.MIPipes;
import aztech.modern_industrialization.pipes.PipeColor;
import aztech.modern_industrialization.pipes.api.PipeNetworkType;
import aztech.modern_industrialization.pipes.impl.PipeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.List;


public class PipeToolboxItem extends Item {
    public static final int CAPACITY = 4 * 81000;

    public static final PipeColor[] COLOR_CYCLE = PipeColor.values();
    private static final String COLOR_INDEX_TAG_NAME = "col_idx";

    public PipeToolboxItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && user.isShiftKeyDown()) {
            ItemStack stack = user.getItemInHand(hand);
            int colorIndex = getColorCycleIndex(stack);
            setColorCycleIndex(stack, (colorIndex + 1) % COLOR_CYCLE.length);
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
        }

        return super.use(world, user, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack toolboxStack = context.getItemInHand();
        PipeItem pipeItem = getPipeForColor(getSelectedPipeColor(toolboxStack));
        Player player = context.getPlayer();
        Inventory inventory = player.getInventory();
        if (inventory == null) { return InteractionResult.FAIL; }

        // check if player has required items in inventory

        int pipeSlot = inventory.findSlotMatchingItem(getPipeForColor(PipeColor.REGULAR).getDefaultInstance());
        if (pipeSlot == -1) { return InteractionResult.FAIL; }

        // check if item has dye fluid
        if (FluidFuelItemHelper.getAmount(toolboxStack) == 0) { return InteractionResult.FAIL; }

        PipeItem.PipePlacementResult result = pipeItem.tryPlacePipe(context);

        switch (result) {
            case PLACED_NEW -> {
                // remove one pipe from inventory and use fuel
                ItemStack placementStack = inventory.getItem(pipeSlot);
                if (!player.getAbilities().instabuild) {
                    placementStack.shrink(1);
                    FluidFuelItemHelper.decrement(toolboxStack);
                }

                return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
            }
            case MADE_CONNECTION -> {
                return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
            }
            case DID_NOTHING -> {
                return InteractionResult.FAIL;
            }
        }
        return super.useOn(context);
    }
    private PipeColor getSelectedPipeColor(ItemStack stack) {
        return COLOR_CYCLE[getColorCycleIndex(stack)];
    }
    private PipeItem getPipeForColor(PipeColor color) {
        String pipeId = color.prefix + "fluid_pipe";
        ResourceLocation loc = new MIIdentifier(pipeId);
        PipeNetworkType type = PipeNetworkType.get(loc);
        return MIPipes.INSTANCE.getPipeItem(type);
    }

    private static int getColorCycleIndex(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(COLOR_INDEX_TAG_NAME)) {
            return tag.getInt(COLOR_INDEX_TAG_NAME);
        } else {
            return 0;
        }
    }

    private static void setColorCycleIndex(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(COLOR_INDEX_TAG_NAME, value);
    }


    public int getCapacity() {return CAPACITY;}
    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
        FluidFuelItemHelper.appendTooltip(stack, tooltip, CAPACITY);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(getDurabilityBarProgress(stack) * 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        Fluid fluid = FluidFuelItemHelper.getFluid(stack).getFluid();

        if (fluid instanceof MIFluid cf) {
            return cf.color;
        } else {
            return 0;
        }
    }

    public double getDurabilityBarProgress(ItemStack stack) {
        return (double) FluidFuelItemHelper.getAmount(stack) / CAPACITY;
    }


}
