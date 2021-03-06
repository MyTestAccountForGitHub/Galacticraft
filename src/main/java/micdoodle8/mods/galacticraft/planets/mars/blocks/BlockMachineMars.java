package micdoodle8.mods.galacticraft.planets.mars.blocks;

import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.core.GCBlocks;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.blocks.BlockTileGC;
import micdoodle8.mods.galacticraft.core.blocks.ISortableBlock;
import micdoodle8.mods.galacticraft.core.energy.tile.TileBaseUniversalElectrical;
import micdoodle8.mods.galacticraft.core.items.IShiftDescription;
import micdoodle8.mods.galacticraft.core.tile.IMultiBlock;
import micdoodle8.mods.galacticraft.core.util.EnumSortCategoryBlock;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import micdoodle8.mods.galacticraft.core.world.IChunkLoader;
import micdoodle8.mods.galacticraft.planets.GalacticraftPlanets;
import micdoodle8.mods.galacticraft.planets.GuiIdsPlanets;
import micdoodle8.mods.galacticraft.planets.mars.ConfigManagerMars;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityCryogenicChamber;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityTerraformer;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

public class BlockMachineMars extends BlockTileGC implements IShiftDescription, ISortableBlock
{
    public static final int TERRAFORMER_METADATA = 0;
    public static final int CRYOGENIC_CHAMBER_METADATA = 4;
    public static final int LAUNCH_CONTROLLER_METADATA = 8;

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyEnum TYPE = PropertyEnum.create("type", EnumMachineType.class);

    public enum EnumMachineType implements IStringSerializable
    {
        TERRAFORMER(0, "terraformer"),
        CRYOGENIC_CHAMBER(1, "cryogenic_chamber"),
        LAUNCH_CONTROLLER(2, "launch_controller");

        private final int meta;
        private final String name;

        private EnumMachineType(int meta, String name)
        {
            this.meta = meta;
            this.name = name;
        }

        public int getMeta()
        {
            return this.meta;
        }

        public static EnumMachineType byMetadata(int meta)
        {
            return values()[meta];
        }

        @Override
        public String getName()
        {
            return this.name;
        }
    }

    public BlockMachineMars(String assetName)
    {
        super(GCBlocks.machine);
        this.setStepSound(soundTypeMetal);
        this.setUnlocalizedName(assetName);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        final TileEntity var9 = worldIn.getTileEntity(pos);

        if (var9 instanceof IMultiBlock)
        {
            ((IMultiBlock) var9).onDestroy(var9);
        }

        super.breakBlock(worldIn, pos, state);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public CreativeTabs getCreativeTabToDisplayOn()
    {
        return GalacticraftCore.galacticraftBlocksTab;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        int metadata = getMetaFromState(state);

        final int angle = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        int change = EnumFacing.getHorizontal(angle).getOpposite().getHorizontalIndex();

        worldIn.setBlockState(pos, getStateFromMeta((metadata & 12) + change), 3);

        TileEntity var8 = worldIn.getTileEntity(pos);

        if (var8 instanceof IMultiBlock)
        {
            ((IMultiBlock) var8).onCreate(worldIn, pos);
        }

        if (metadata >= BlockMachineMars.LAUNCH_CONTROLLER_METADATA)
        {
            for (int dX = -2; dX < 3; dX++)
            {
                for (int dZ = -2; dZ < 3; dZ++)
                {
                    BlockPos pos1 = pos.add(dX, 0, dZ);
                    final Block id = worldIn.getBlockState(pos1).getBlock();

                    if (id == GCBlocks.landingPadFull)
                    {
                        worldIn.markBlockForUpdate(pos1);
                    }
                }
            }
        }

        if (var8 instanceof IChunkLoader && !var8.getWorld().isRemote && ConfigManagerMars.launchControllerChunkLoad && placer instanceof EntityPlayer)
        {
            ((IChunkLoader) var8).setOwnerName(((EntityPlayer) placer).getGameProfile().getName());
            ((IChunkLoader) var8).onTicketLoaded(ForgeChunkManager.requestTicket(GalacticraftCore.instance, var8.getWorld(), Type.NORMAL), true);
        }
        else if (var8 instanceof TileEntityLaunchController && placer instanceof EntityPlayer)
        {
            ((TileEntityLaunchController) var8).setOwnerName(((EntityPlayer) placer).getGameProfile().getName());
        }
    }

    @Override
    public boolean onUseWrench(World world, BlockPos pos, EntityPlayer entityPlayer, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        int metadata = getMetaFromState(world.getBlockState(pos));
        int change = world.getBlockState(pos).getValue(FACING).rotateY().getHorizontalIndex();

        world.setBlockState(pos, this.getStateFromMeta(metadata - (metadata % 4) + change), 3);

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileBaseUniversalElectrical)
        {
            ((TileBaseUniversalElectrical) te).updateFacing();
        }

        return true;
    }

    @Override
    public boolean onMachineActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        int metadata = getMetaFromState(worldIn.getBlockState(pos));

        if (metadata >= BlockMachineMars.LAUNCH_CONTROLLER_METADATA)
        {
            playerIn.openGui(GalacticraftPlanets.instance, GuiIdsPlanets.MACHINE_MARS, worldIn, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        else if (metadata >= BlockMachineMars.CRYOGENIC_CHAMBER_METADATA)
        {
            ((IMultiBlock) worldIn.getTileEntity(pos)).onActivated(playerIn);
            return true;
        }
        else
        {
            playerIn.openGui(GalacticraftPlanets.instance, GuiIdsPlanets.MACHINE_MARS, worldIn, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public boolean isFullCube()
    {
        return false;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        int metadata = getMetaFromState(state);
        if (metadata >= BlockMachineMars.LAUNCH_CONTROLLER_METADATA)
        {
            return new TileEntityLaunchController();
        }
        if (metadata >= BlockMachineMars.CRYOGENIC_CHAMBER_METADATA)
        {
            return new TileEntityCryogenicChamber();
        }
        else
        {
            return new TileEntityTerraformer();
        }
    }

    @Override
    public boolean removedByPlayer(World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
    {
        if (getMetaFromState(world.getBlockState(pos)) >= BlockMachineMars.LAUNCH_CONTROLLER_METADATA)
        {
            for (int dX = -2; dX < 3; dX++)
            {
                for (int dZ = -2; dZ < 3; dZ++)
                {
                    BlockPos pos1 = pos.add(dX, 0, dZ);
                    final Block id = world.getBlockState(pos1).getBlock();

                    if (id == GCBlocks.landingPadFull)
                    {
                        world.markBlockForUpdate(pos1);
                    }
                }
            }
        }

        return super.removedByPlayer(world, pos, player, willHarvest);
    }

    public ItemStack getTerraformer()
    {
        return new ItemStack(this, 1, BlockMachineMars.TERRAFORMER_METADATA);
    }

    public ItemStack getChamber()
    {
        return new ItemStack(this, 1, BlockMachineMars.CRYOGENIC_CHAMBER_METADATA);
    }

    public ItemStack getLaunchController()
    {
        return new ItemStack(this, 1, BlockMachineMars.LAUNCH_CONTROLLER_METADATA);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List par3List)
    {
        par3List.add(this.getTerraformer());
        par3List.add(this.getChamber());
        par3List.add(this.getLaunchController());
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        int metadata = getMetaFromState(state);
        if (metadata >= BlockMachineMars.LAUNCH_CONTROLLER_METADATA)
        {
            return BlockMachineMars.LAUNCH_CONTROLLER_METADATA;
        }
        else if (metadata >= BlockMachineMars.CRYOGENIC_CHAMBER_METADATA)
        {
            return BlockMachineMars.CRYOGENIC_CHAMBER_METADATA;
        }
        else
        {
            return BlockMachineMars.TERRAFORMER_METADATA;
        }
    }

    @Override
    public boolean isBed(IBlockAccess world, BlockPos pos, Entity player)
    {
        return getMetaFromState(world.getBlockState(pos)) >= BlockMachineMars.CRYOGENIC_CHAMBER_METADATA;
    }

    @Override
    public BlockPos getBedSpawnPosition(IBlockAccess world, BlockPos pos, EntityPlayer player)
    {
        return pos.up();
    }

    @Override
    public void setBedOccupied(IBlockAccess world, BlockPos pos, EntityPlayer player, boolean occupied)
    {
        TileEntity tile = world.getTileEntity(pos);

        if (tile instanceof TileEntityCryogenicChamber)
        {
            ((TileEntityCryogenicChamber) tile).isOccupied = true;
        }
    }

    @Override
    public EnumFacing getBedDirection(IBlockAccess world, BlockPos pos)
    {
        return EnumFacing.DOWN;
    }

    @Override
    public String getShiftDescription(int meta)
    {
        switch (meta)
        {
        case CRYOGENIC_CHAMBER_METADATA:
            return GCCoreUtil.translate("tile.cryo_chamber.description");
        case LAUNCH_CONTROLLER_METADATA:
            return GCCoreUtil.translate("tile.launch_controller.description");
        case TERRAFORMER_METADATA:
            return GCCoreUtil.translate("tile.terraformer.description");
        }
        return "";
    }

    @Override
    public boolean showDescription(int meta)
    {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side)
    {
        return true;
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        EnumFacing enumfacing = EnumFacing.getHorizontal(meta % 4);
        EnumMachineType type = EnumMachineType.byMetadata((int) Math.floor(meta / 4.0));
        return this.getDefaultState().withProperty(FACING, enumfacing).withProperty(TYPE, type);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return (state.getValue(FACING)).getHorizontalIndex() + ((EnumMachineType) state.getValue(TYPE)).getMeta() * 4;
    }

    @Override
    protected BlockState createBlockState()
    {
        return new BlockState(this, FACING, TYPE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (state.getValue(TYPE) == EnumMachineType.CRYOGENIC_CHAMBER)
        {
            GalacticraftPlanets.spawnParticle("cryoFreeze", new Vector3(pos.getX() + 0.3 + rand.nextDouble() * 0.4, pos.getY(), pos.getZ() + 0.3 + rand.nextDouble() * 0.4), new Vector3(0.0, 0.05 + rand.nextDouble() * 0.01, 0.0));
            GalacticraftPlanets.spawnParticle("cryoFreeze", new Vector3(pos.getX() + 0.3 + rand.nextDouble() * 0.4, pos.getY(), pos.getZ() + 0.3 + rand.nextDouble() * 0.4), new Vector3(0.0, 0.05 + rand.nextDouble() * 0.01, 0.0));
            GalacticraftPlanets.spawnParticle("cryoFreeze", new Vector3(pos.getX() + 0.3 + rand.nextDouble() * 0.4, pos.getY(), pos.getZ() + 0.3 + rand.nextDouble() * 0.4), new Vector3(0.0, 0.05 + rand.nextDouble() * 0.01, 0.0));

            GalacticraftPlanets.spawnParticle("cryoFreeze", new Vector3(pos.getX() + 0.3 + rand.nextDouble() * 0.4, pos.getY() + 2.9F, pos.getZ() + 0.3 + rand.nextDouble() * 0.4), new Vector3(0.0, -0.05 - rand.nextDouble() * 0.01, 0.0));
            GalacticraftPlanets.spawnParticle("cryoFreeze", new Vector3(pos.getX() + 0.3 + rand.nextDouble() * 0.4, pos.getY() + 2.9F, pos.getZ() + 0.3 + rand.nextDouble() * 0.4), new Vector3(0.0, -0.05 - rand.nextDouble() * 0.01, 0.0));
            GalacticraftPlanets.spawnParticle("cryoFreeze", new Vector3(pos.getX() + 0.3 + rand.nextDouble() * 0.4, pos.getY() + 2.9F, pos.getZ() + 0.3 + rand.nextDouble() * 0.4), new Vector3(0.0, -0.05 - rand.nextDouble() * 0.01, 0.0));
        }
    }

    @Override
    public EnumSortCategoryBlock getCategory(int meta)
    {
        return EnumSortCategoryBlock.MACHINE;
    }
}
