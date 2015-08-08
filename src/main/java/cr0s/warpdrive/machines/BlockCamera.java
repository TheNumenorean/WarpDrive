package cr0s.warpdrive.machines;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CamRegistryItem;

public class BlockCamera extends BlockContainer {
	private IIcon[] iconBuffer;

	private final int ICON_SIDE = 0;

	public BlockCamera(int texture, Material material) {
		super(material);
		setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
		this.setBlockName("warpdrive.machines.CloakingCoil");
		this.setBlockName("warpdrive.machines.Camera");
	}

	@Override
	public void registerBlockIcons(IIconRegister par1IconRegister) {
		iconBuffer = new IIcon[1];
		// Solid textures
		iconBuffer[ICON_SIDE] = par1IconRegister.registerIcon("warpdrive:cameraSide");
	}

	@Override
	public IIcon getIcon(int side, int metadata) {
		return iconBuffer[ICON_SIDE];
	}

	@Override
	public TileEntity createNewTileEntity(World parWorld, int i) {
		return new TileEntityCamera();
	}

	/**
	 * Returns the quantity of items to drop on block destruction.
	 */
	@Override
	public int quantityDropped(Random par1Random) {
		return 1;
	}

	/**
	 * Returns the ID of the items to drop on destruction.
	 */
	@Override
	public Item getItemDropped(int par1, Random par2Random, int par3) {
		return Item.getItemFromBlock(this);
	}

	/**
	 * Called upon block activation (right click on the block.)
	 */
	@Override
	public boolean onBlockActivated(World par1World, int x, int y, int z, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9) {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			return false;
		}

		// Get camera frequency
		TileEntity te = par1World.getTileEntity(x, y, z);
		if (te != null && te instanceof TileEntityCamera && (par5EntityPlayer.getHeldItem() == null)) {
			int frequency = ((TileEntityCamera)te).getFrequency();

			CamRegistryItem cam = WarpDrive.instance.cams.getCamByFrequency(par1World, frequency);
			if (cam == null) {
				WarpDrive.instance.cams.printRegistry(par1World);
				par5EntityPlayer.addChatMessage(new ChatComponentText(getLocalizedName() + " Frequency '" + frequency + "' is invalid!"));
			} else {
				par5EntityPlayer.addChatMessage(new ChatComponentText(getLocalizedName() + " Frequency '" + frequency + "' is valid for camera at " + cam.position.chunkPosX + ", " + cam.position.chunkPosY + ", " + cam.position.chunkPosZ));
			}
			return true;
		}

		return false;
	}
}