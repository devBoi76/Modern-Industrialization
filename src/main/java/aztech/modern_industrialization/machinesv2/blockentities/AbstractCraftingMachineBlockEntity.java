/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.machinesv2.blockentities;

import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.impl.MachineTier;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import aztech.modern_industrialization.machinesv2.MachineBlockEntity;
import aztech.modern_industrialization.machinesv2.components.CrafterComponent;
import aztech.modern_industrialization.machinesv2.components.IsActiveComponent;
import aztech.modern_industrialization.machinesv2.components.MachineInventoryComponent;
import aztech.modern_industrialization.machinesv2.components.OrientationComponent;
import aztech.modern_industrialization.machinesv2.components.sync.AutoExtract;
import aztech.modern_industrialization.machinesv2.components.sync.ProgressBar;
import aztech.modern_industrialization.machinesv2.gui.MachineGuiParameters;
import aztech.modern_industrialization.machinesv2.helper.OrientationHelper;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.Direction;

public abstract class AbstractCraftingMachineBlockEntity extends MachineBlockEntity implements CrafterComponent.Behavior, Tickable {
    public AbstractCraftingMachineBlockEntity(BlockEntityType<?> type, MachineRecipeType recipeType, MachineInventoryComponent inventory,
            MachineGuiParameters guiParams, ProgressBar.Parameters progressBarParams, MachineTier tier) {
        super(type, guiParams);
        this.inventory = inventory;
        this.crafter = new CrafterComponent(inventory, this);
        this.orientation = new OrientationComponent(
                new OrientationComponent.Params(true, inventory.itemOutputCount > 0, inventory.fluidOutputCount > 0));
        this.type = recipeType;
        this.tier = tier;
        this.isActiveComponent = new IsActiveComponent();
        registerClientComponent(new AutoExtract.Server(orientation));
        registerClientComponent(new ProgressBar.Server(progressBarParams, crafter::getProgress));
        this.registerComponents(crafter, this.inventory, orientation, isActiveComponent);
    }

    private final MachineInventoryComponent inventory;
    protected final CrafterComponent crafter;
    protected final OrientationComponent orientation;

    private final MachineRecipeType type;
    protected final MachineTier tier;
    protected IsActiveComponent isActiveComponent;

    @Override
    public MachineRecipeType recipeType() {
        return type;
    }

    @Override
    public long getBaseRecipeEu() {
        return tier.getBaseEu();
    }

    @Override
    public long getMaxRecipeEu() {
        return tier.getMaxEu();
    }

    @Override
    public void tick() {
        if (!world.isClient) {
            boolean newActive = crafter.tickRecipe();
            if (newActive != isActiveComponent.isActive) {
                isActiveComponent.isActive = newActive;
                sync();
            }
            if (orientation.extractItems) {
                inventory.inventory.autoExtractItems(world, pos, orientation.outputDirection);
            }
            if (orientation.extractFluids) {
                inventory.inventory.autoExtractFluids(world, pos, orientation.outputDirection);
            }
            markDirty();
        }
    }

    @Override
    public MIInventory getInventory() {
        return inventory.inventory;
    }

    @Override
    protected ActionResult onUse(PlayerEntity player, Hand hand, Direction face) {
        return OrientationHelper.onUse(player, hand, face, orientation, this);
    }

    @Override
    public void onPlaced(LivingEntity placer, ItemStack itemStack) {
        orientation.onPlaced(placer, itemStack);
    }

}
