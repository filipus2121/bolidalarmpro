package ru.bolidalarmpro.device;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import ru.bolidalarmpro.BolidAlarmPro;
import ru.bolidalarmpro.ModCreativeTab;
import ru.bolidalarmpro.ModInfo;
import ru.bolidalarmpro.ModItems;
import ru.bolidalarmpro.sim.DeviceKind;
import ru.bolidalarmpro.sim.Pos;
import ru.bolidalarmpro.wiring.WiringWorldData;

/** One block with metadata = DeviceKind ordinal; the facing is stored in the tile entity. */
public final class DeviceBlock extends Block implements ITileEntityProvider {
    private IIcon placeholderIcon;

    public DeviceBlock() {
        super(Material.iron);
        setBlockName(ModInfo.MODID + ".device");
        setCreativeTab(ModCreativeTab.TAB);
        setHardness(0.6F);
        setResistance(2.0F);
        setStepSound(Block.soundTypeMetal);
    }

    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean renderAsNormalBlock() { return false; }

    @Override
    public int getRenderType() { return -1; }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
        return null;
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox((double) x, (double) y, (double) z,
                (double) x + 1.0, (double) y + 1.0, (double) z + 1.0);
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        placeholderIcon = reg.registerIcon(ModInfo.RES + "device_placeholder");
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return placeholderIcon;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        DeviceKind[] all = DeviceKind.values();
        for (int i = 0; i < all.length; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public int damageDropped(int meta) {
        return meta & 15;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        DeviceTileEntity te = new DeviceTileEntity();
        te.setKindFromMetadata(meta);
        return te;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int dir = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int facing = dir == 0 ? 2 : (dir == 1 ? 5 : (dir == 2 ? 3 : 4));
        TileEntity raw = world.getTileEntity(x, y, z);
        if (raw instanceof DeviceTileEntity) {
            DeviceTileEntity te = (DeviceTileEntity) raw;
            te.facing = facing;
            te.markDirty();
            world.markBlockForUpdate(x, y, z);
        }
        WiringWorldData.get(world).markTopologyDirty();
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        WiringWorldData.get(world).addDevice(new Pos(x, y, z));
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        WiringWorldData.get(world).removeDevice(new Pos(x, y, z));
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        WiringWorldData.get(world).markTopologyDirty();
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        DeviceKind kind = DeviceKind.RIP_12;
        int facing = 2;
        TileEntity raw = world.getTileEntity(x, y, z);
        if (raw instanceof DeviceTileEntity) {
            DeviceTileEntity te = (DeviceTileEntity) raw;
            kind = te.kind;
            facing = te.facing;
        }
        setBoundsFor(kind, facing);
    }

    private void setBoundsFor(DeviceKind kind, int facing) {
        if (kind.isPyro()) {
            setBlockBounds(0.35F, 0.0F, 0.35F, 0.65F, 0.55F, 0.65F);
            return;
        }
        boolean tablo = kind.isTablo();
        float depth = tablo ? 0.06F : (kind == DeviceKind.RIP_12 || kind == DeviceKind.ASPT_353 ? 0.25F : 0.13F);
        float x1 = tablo ? 0.03F : 0.12F;
        float x2 = tablo ? 0.97F : 0.88F;
        float y1 = tablo ? 0.34F : 0.12F;
        float y2 = tablo ? 0.66F : 0.88F;
        float z1, z2;
        if (facing == 2) { z1 = 0.0F; z2 = depth; }
        else if (facing == 3) { z1 = 1.0F - depth; z2 = 1.0F; }
        else if (facing == 4) {
            x1 = 1.0F - depth; x2 = 1.0F;
            z1 = tablo ? 0.34F : 0.12F; z2 = tablo ? 0.66F : 0.88F;
        } else {
            x1 = 0.0F; x2 = depth;
            z1 = tablo ? 0.34F : 0.12F; z2 = tablo ? 0.66F : 0.88F;
        }
        setBlockBounds(x1, y1, z1, x2, y2, z2);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                    int side, float hx, float hy, float hz) {
        if (world.isRemote) return true;
        TileEntity raw = world.getTileEntity(x, y, z);
        if (!(raw instanceof DeviceTileEntity)) return true;
        DeviceTileEntity te = (DeviceTileEntity) raw;
        ItemStack held = player.getHeldItem();

        if (ModItems.isHeld(held, ModItems.programmer)) {
            player.openGui(BolidAlarmPro.instance, ModInfo.GUI_DEVICE, world, x, y, z);
            return true;
        }
        if (ModItems.isHeld(held, ModItems.resetKey)) {
            te.requestReset();
            chat(player, StatCollector.translateToLocal("gui.bolid.reset.key"));
            return true;
        }
        if (ModItems.isHeld(held, ModItems.battery7)) {
            te.installBattery7();
            held.stackSize--;
            chat(player, "АКБ 12 В 7 А·ч установлена в РИП-12");
            return true;
        }
        if (ModItems.isHeld(held, ModItems.battery45)) {
            te.installBattery45();
            held.stackSize--;
            chat(player, "Комплект 2 x 4,5 А·ч установлен в С2000-АСПТ");
            return true;
        }
        if (held == null) {
            if (te.kind.isAddressable() && !player.isSneaking()) {
                te.press();
                chat(player, te.kind.displayName() + ": " + (te.kind == DeviceKind.UDP_513_3AM
                        ? "ПУСК ПОЖАРОТУШЕНИЯ" : "ПОЖАР"));
            } else {
                player.openGui(BolidAlarmPro.instance, ModInfo.GUI_DEVICE, world, x, y, z);
            }
            return true;
        }
        player.openGui(BolidAlarmPro.instance, ModInfo.GUI_DEVICE, world, x, y, z);
        return true;
    }

    private static void chat(EntityPlayer player, String text) {
        player.addChatComponentMessage(new ChatComponentText(text));
    }
}
