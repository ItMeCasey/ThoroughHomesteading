package com.itmecasey.thoroughhomesteading.block;

import com.itmecasey.thoroughhomesteading.BarelyQuestingBridge;
import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.HomeCheck;
import com.itmecasey.thoroughhomesteading.HomeScanner;
import com.itmecasey.thoroughhomesteading.ModContent;
import com.itmecasey.thoroughhomesteading.ScriptEvents;
import com.itmecasey.thoroughhomesteading.Settings;
import com.itmecasey.thoroughhomesteading.SettlingEntry;
import com.itmecasey.thoroughhomesteading.SettlingRecipes;
import com.itmecasey.thoroughhomesteading.Settler;
import com.itmecasey.thoroughhomesteading.script.ScriptHost;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * The Hearth's state: nine slots of food (and the extra items recipes spend), the owner, the settlers it brought and the countdown. A lit Hearth
 * in a loaded chunk runs the home check every {@code scan_interval} ticks. It is a container for hoppers only: items go in from above and
 * the sides, nothing comes out.
 */
public class HearthBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int SLOTS = 9;
    private static final int[] INSERT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private UUID owner;
    private final List<Settler.Ref> settlers = new ArrayList<>();
    private int progress;
    private boolean keep;
    private HomeScanner.Result last;
    private long lastScanTime = Long.MIN_VALUE;
    private String lastBlocked = "";

    public HearthBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.HEARTH, pos, state);
    }

    public HearthTier tier() {
        return getBlockState().getBlock() instanceof HearthBlock block ? block.tier() : HearthTier.STONE;
    }

    public boolean lit() {
        return getBlockState().getValue(HearthBlock.LIT);
    }

    // ---- Ownership ----

    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    /** The owner, and their Barely Questing team when that mod is installed, may feed, upgrade and break a Hearth. A Hearth with no owner is open to everyone. */
    public boolean canManage(Player player) {
        return owner == null || owner.equals(player.getUUID()) || BarelyQuestingBridge.sameTeam(owner, player.getUUID()) || player.hasInfiniteMaterials();
    }

    // ---- Food ----

    /** Total nutrition of the food in the Hearth. */
    public int nutrition() {
        int sum = 0;
        for (ItemStack s : items) {
            sum += nutritionOf(s) * s.getCount();
        }
        return sum;
    }

    public static int nutritionOf(ItemStack stack) {
        var food = stack.get(DataComponents.FOOD);
        return food == null ? 0 : food.nutrition();
    }

    public int count(String itemId) {
        int n = 0;
        for (ItemStack s : items) {
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(itemId)) {
                n += s.getCount();
            }
        }
        return n;
    }

    public boolean hasExtras(SettlingEntry recipe) {
        for (SettlingEntry.Extra x : recipe.extra()) {
            if (count(x.item()) < x.count()) {
                return false;
            }
        }
        return true;
    }

    /** Spends food worth at least {@code nutrition} and the recipe's extra items. Call only after the check passed. */
    public void spend(int nutrition, SettlingEntry recipe) {
        for (SettlingEntry.Extra x : recipe.extra()) {
            int left = x.count();
            for (ItemStack s : items) {
                if (left > 0 && !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(x.item())) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
        }
        int left = nutrition;
        for (ItemStack s : items) {
            int each = nutritionOf(s);
            while (left > 0 && each > 0 && !s.isEmpty()) {
                s.shrink(1);
                left -= each;
            }
        }
        setChanged();
    }

    /** Puts a stack into the Hearth by hand or from a hopper; returns what did not fit. */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty() || !acceptable(stack)) {
            return stack;
        }
        ItemStack rest = stack.copy();
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, rest)) {
                int room = Math.min(s.getMaxStackSize(), getMaxStackSize()) - s.getCount();
                int moved = Math.min(room, rest.getCount());
                if (moved > 0) {
                    s.grow(moved);
                    rest.shrink(moved);
                }
            }
        }
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                int moved = Math.min(rest.getMaxStackSize(), rest.getCount());
                items.set(i, rest.split(moved));
            }
        }
        setChanged();
        return rest;
    }

    private boolean acceptable(ItemStack stack) {
        if (nutritionOf(stack) > 0) {
            return true;
        }
        if (level instanceof ServerLevel server) {
            return SettlingRecipes.extraItems(server.getServer()).contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        return false;
    }

    // ---- Settlers ----

    public List<String> settlerIds() {
        List<String> out = new ArrayList<>();
        for (Settler.Ref r : settlers) {
            out.add(r.id().toString());
        }
        return out;
    }

    public int settlerCount() {
        return settlers.size();
    }

    public void addSettler(Settler.Ref ref) {
        settlers.add(ref);
        last = null;
        setChanged();
    }

    /** Forgets settlers that died or lost their bed or workstation, and notes new trade levels. */
    private void prune(ServerLevel server) {
        for (int i = settlers.size() - 1; i >= 0; i--) {
            Settler.Ref ref = settlers.get(i);
            Entity e = server.getEntity(ref.id());
            if (e == null) {
                if (server.hasChunkAt(ref.pos())) {
                    settlers.remove(i);
                }
                continue;
            }
            if (!(e instanceof Villager v) || !v.isAlive() || !Settler.keepsHome(v)) {
                settlers.remove(i);
                continue;
            }
            settlers.set(i, Settler.update(server, v, ref));
        }
    }

    // ---- Scanning and the countdown ----

    /** The newest scan result, or a fresh one when the last is older than a second. Never advances the countdown. */
    public HomeScanner.Result result(ServerLevel server) {
        if (last == null || server.getGameTime() - lastScanTime > 20) {
            prune(server);
            last = HomeScanner.scan(server, worldPosition, tier(), settlers.size(), this, "");
            lastScanTime = server.getGameTime();
        }
        return last;
    }

    /** Called every tick on the server. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, HearthBlockEntity hearth) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        ScriptHost.touch(level);
        int interval = Math.max(1, Settings.getInt("scan_interval"));
        if (Math.floorMod(level.getGameTime() + pos.asLong(), interval) != 0) {
            return;
        }
        hearth.tickScan(server, interval);
    }

    /** One scan of the home check: advances the countdown by {@code interval} ticks while lit and passing, and brings the settler when it ends. */
    public void tickScan(ServerLevel server, int interval) {
        boolean lit = lit();
        if (!lit && !Settings.getBool("allow_unlit_hearth_scan")) {
            last = null;
            return;
        }
        prune(server);
        HomeScanner.Result r = HomeScanner.scan(server, worldPosition, tier(), settlers.size(), this, "");
        last = r;
        lastScanTime = server.getGameTime();
        String reason = r.report().reason().id();
        if (!r.passes()) {
            if (lit && !reason.equals(lastBlocked)) {
                ScriptEvents.fire("settling_blocked", "x", worldPosition.getX(), "y", worldPosition.getY(), "z", worldPosition.getZ(),
                        "dimension", ScriptHost.dimension(server), "tier", tier().id(), "reason", reason);
            }
            lastBlocked = reason;
        } else {
            lastBlocked = "";
        }
        if (lit) {
            progress = HomeCheck.advance(progress, r.passes(), interval);
            if (HomeCheck.done(progress, tier().settleTicks()) && r.passes()) {
                Settler.arrive(server, this, r);
                progress = 0;
            }
            setChanged();
        }
    }

    /** Skips the countdown and brings a settler now, if the Hearth is lit and the home check passes. */
    public boolean settleNow(ServerLevel server, String profession) {
        if (!lit()) {
            return false;
        }
        prune(server);
        HomeScanner.Result r = HomeScanner.scan(server, worldPosition, tier(), settlers.size(), this, profession);
        last = r;
        lastScanTime = server.getGameTime();
        if (!r.passes()) {
            return false;
        }
        boolean ok = Settler.arrive(server, this, r);
        if (ok) {
            progress = 0;
            setChanged();
        }
        return ok;
    }

    /** Every requirement with its current value, the timer and the settlers: what the scripting read returns. */
    public Map<String, Object> report(ServerLevel server) {
        HomeScanner.Result r = result(server);
        HomeCheck.Inputs in = r.report().in();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tier", tier().id());
        out.put("lit", lit());
        out.put("owner", owner == null ? "" : owner.toString());
        out.put("settlers", in.settlers());
        out.put("population", in.population());
        out.put("beds", in.freeBeds());
        out.put("litBeds", in.litBeds());
        out.put("light", in.bestLight());
        out.put("lightMin", in.lightMin());
        out.put("workstations", in.freeWorkstations());
        out.put("food", in.food());
        out.put("foodRequired", in.foodRequired());
        out.put("progress", progress);
        out.put("settleTicks", tier().settleTicks());
        out.put("blocked", r.report().reason().id());
        out.put("profession", r.recipe() == null ? "" : r.recipe().profession());
        out.put("settlerIds", settlerIds());
        return out;
    }

    public int progress() {
        return progress;
    }

    // ---- Container ----

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SLOTS ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = ContainerHelper.removeItem(items, slot, count);
        if (!out.isEmpty()) {
            setChanged();
        }
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SLOTS) {
            items.set(slot, stack);
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return acceptable(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[0] : INSERT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction face) {
        return face != Direction.DOWN && acceptable(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return false;
    }

    /** The comparator reads how full the nine slots are. */
    public int comparatorSignal() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }

    // ---- Upgrade and removal ----

    /** Keeps the contents when this block entity is removed because the block is replaced by an upgrade. */
    public void keepOnRemove() {
        keep = true;
    }

    /** Takes over everything of another Hearth (the upgrade path). */
    public void adopt(HearthBlockEntity other) {
        for (int i = 0; i < SLOTS; i++) {
            items.set(i, other.items.get(i));
            other.items.set(i, ItemStack.EMPTY);
        }
        owner = other.owner;
        settlers.clear();
        settlers.addAll(other.settlers);
        progress = other.progress;
        lastBlocked = other.lastBlocked;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (keep || level == null) {
            return;
        }
        // The base class drops the Hearth's inventory because this block entity is a container.
        super.preRemoveSideEffects(pos, state);
    }

    // ---- Save and load ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        if (owner != null) {
            output.putString("owner", owner.toString());
        }
        output.putInt("progress", progress);
        output.putString("blocked", lastBlocked);
        List<String> refs = new ArrayList<>();
        for (Settler.Ref r : settlers) {
            refs.add(r.encode());
        }
        output.putString("settlers", String.join(";", refs));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < SLOTS; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        for (net.minecraft.world.ItemStackWithSlot s : input.listOrEmpty("Items", net.minecraft.world.ItemStackWithSlot.CODEC)) {
            if (s.slot() >= 0 && s.slot() < SLOTS) {
                items.set(s.slot(), s.stack());
            }
        }
        String o = input.getStringOr("owner", "");
        try {
            owner = o.isEmpty() ? null : UUID.fromString(o);
        } catch (IllegalArgumentException e) {
            owner = null;
        }
        progress = input.getIntOr("progress", 0);
        lastBlocked = input.getStringOr("blocked", "");
        settlers.clear();
        String text = input.getStringOr("settlers", "");
        if (!text.isEmpty()) {
            for (String part : text.split(";")) {
                Settler.Ref ref = Settler.Ref.decode(part);
                if (ref != null) {
                    settlers.add(ref);
                }
            }
        }
        last = null;
    }
}
