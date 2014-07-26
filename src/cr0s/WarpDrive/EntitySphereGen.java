package cr0s.WarpDrive;

import cpw.mods.fml.common.FMLCommonHandler;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
/*
2014-06-07 21:41:45 [Infos] [STDOUT] Generating star (class 0) at -579 257 1162
2014-06-07 21:41:45 [Infos] [Minecraft-Client] [CHAT] /generate: generating star at -579, 257, 1162
2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saving blocks...
2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saved 310248 blocks
2014-06-07 21:41:45 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 95.646ms, total: 95.646ms
2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saving blocks...
2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saved 23706 blocks
2014-06-07 21:41:45 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 15.427ms, total: 15.427ms

2014-06-07 21:42:03 [Infos] [STDOUT] Generating star (class 1) at -554 257 1045
2014-06-07 21:42:03 [Infos] [Minecraft-Client] [CHAT] /generate: generating star at -554, 257, 1045
2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saving blocks...
2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saved 1099136 blocks
2014-06-07 21:42:03 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 37.404ms, total: 37.404ms
2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saving blocks...
2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saved 50646 blocks
2014-06-07 21:42:03 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 34.369ms, total: 34.369ms

2014-06-07 21:42:39 [Infos] [STDOUT] Generating star (class 2) at -404 257 978
2014-06-07 21:42:39 [Infos] [Minecraft-Client] [CHAT] /generate: generating star at -404, 257, 978
2014-06-07 21:42:39 [Infos] [STDOUT] [ESG] Saving blocks...
2014-06-07 21:42:39 [Infos] [STDOUT] [ESG] Saved 2144432 blocks
2014-06-07 21:42:39 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 85.523ms, total: 85.523ms
2014-06-07 21:42:39 [Infos] [STDOUT] [ESG] Saving blocks...
2014-06-07 21:42:40 [Infos] [STDOUT] [ESG] Saved 76699 blocks
2014-06-07 21:42:40 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 9.286ms, total: 9.286ms

 */
public final class EntitySphereGen extends Entity
{
	public int xCoord;
	public int yCoord;
	public int zCoord;

	private int radius;
	private int[] defaultBlock;
	private boolean hollow;
	private boolean fillingSphere; // new sphere blocks does not replace existing blocks (for gases & moons)
	private boolean generateOres; // generate random surface blocks (ores) or fixed blockID
	private int gasColor;

	private final int BLOCKS_PER_TICK = 5000;

	private final int STATE_SAVING = 0;
	private final int STATE_SETUP = 1;
	private final int STATE_DELETE = 2;
	private final int STATE_STOP = 3;
	private int state = STATE_DELETE;
	private int ticksDelay = 0;

	private int currentIndex = 0;
	private int pregenSize = 0;

	private ArrayList<JumpBlock> blocks;

	public EntitySphereGen(World world) {
		super(world);
	}

	public EntitySphereGen(World world, int x, int y, int z, int radius, int blockID, int blockMeta, boolean hollow, boolean fillingSphere, boolean generateOres) {
		super(world);
		this.xCoord = x;
		this.posX = (double) x;
		this.yCoord = y;
		this.posY = (double) y;
		this.zCoord = z;
		this.posZ = (double) z;
		this.radius = radius;
		this.hollow = hollow;
		this.fillingSphere = fillingSphere;
		this.generateOres = generateOres;
		this.gasColor = worldObj.rand.nextInt(12);
		this.state = STATE_SAVING;
		this.pregenSize = (int)Math.ceil(Math.PI * 4.0F / 3.0F * Math.pow(radius + 1, 3));
		blocks = new ArrayList<JumpBlock>(this.pregenSize);
		this.defaultBlock = new int[] {blockID, blockMeta};
		this.ticksDelay = world.rand.nextInt(60);
	}

	public void killEntity() {
		this.state = STATE_STOP;
		worldObj.removeEntity(this);
	}

	@Override
	public void onUpdate() {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			return;
		}
		
		if (ticksDelay > 0) {
			ticksDelay--;
			return;
		}
		
		switch (this.state) {
			case STATE_SAVING:
				tickScheduleBlocks();
				this.state = STATE_SETUP;
				break;
			case STATE_SETUP:
				if (currentIndex >= blocks.size() - 1)
					this.state = STATE_DELETE;
				else
					tickPlaceBlocks();
				break;
			case STATE_DELETE:
				currentIndex = 0;
				killEntity();
				break;
		}
	}

	private void tickPlaceBlocks() {
		int blocksToMove = Math.min(BLOCKS_PER_TICK, blocks.size() - currentIndex);
//		LocalProfiler.start("[EntitySphereGen] Placing blocks: " + currentIndex + "/" + blocks.size());
		int notifyFlag;

		for (int index = 0; index < blocksToMove; index++) {
			if (currentIndex >= blocks.size())
				break;
			notifyFlag = (currentIndex % 1000 == 0 ? 2 : 0);
			JumpBlock jb = blocks.get(currentIndex);
			mySetBlock(worldObj, jb.x, jb.y, jb.z, jb.blockID, jb.blockMeta, notifyFlag);
			currentIndex++;
		}

//		LocalProfiler.stop();
	}

	private void tickScheduleBlocks() {
//		LocalProfiler.start("EntitySphereGen.tickScheduleBlocks");
		radius += 0.5D; // Radius from center of block
		double radiusSq = radius * radius; // Optimization to avoid square roots
		double radius1Sq = (radius - 1.0D) * (radius - 1.0D); // for hollow sphere
		int ceilRadius = (int) Math.ceil(radius);

		// Pass the cube and check points for sphere equation x^2 + y^2 + z^2 = r^2
		int[] block = defaultBlock;
		for (int x = 0; x <= ceilRadius; x++) {
			double x2 = (x + 0.5D) * (x + 0.5D); 
			for (int y = 0; y <= ceilRadius; y++) {
				double y2 = (y + 0.5D) * (y + 0.5D); 
				for (int z = 0; z <= ceilRadius; z++) {
					double z2 = (z + 0.5D) * (z + 0.5D); 
					double dSq = x2 + y2 + z2; // Distance from current position to center
					
					// Skip too far blocks
					if (dSq > radiusSq)
						continue;
					// Hollow sphere condition
					if ((hollow) && ((dSq < radius1Sq) || ((lengthSq(x + 1.5D, y + 0.5D, z + 0.5D) <= radiusSq) && (lengthSq(x + 0.5D, y + 1.5D, z + 0.5D) <= radiusSq) && (lengthSq(x + 0.5D, y + 0.5D, z + 1.5D) <= radiusSq))))
						continue;

					if (generateOres)
						block = WarpDriveConfig.getRandomSurfaceBlock(worldObj.rand, defaultBlock[0], defaultBlock[1], true);
					// Add blocks to memory
					addBlock(new JumpBlock(block[0], block[1], xCoord + x, yCoord + y, zCoord + z));
					if (generateOres)
						block = WarpDriveConfig.getRandomSurfaceBlock(worldObj.rand, defaultBlock[0], defaultBlock[1], true);
					addBlock(new JumpBlock(block[0], block[1], xCoord - x, yCoord + y, zCoord + z));
					if (generateOres)
						block = WarpDriveConfig.getRandomSurfaceBlock(worldObj.rand, defaultBlock[0], defaultBlock[1], true);
					addBlock(new JumpBlock(block[0], block[1], xCoord + x, yCoord - y, zCoord + z));
					if (generateOres)
						block = WarpDriveConfig.getRandomSurfaceBlock(worldObj.rand, defaultBlock[0], defaultBlock[1], true);
					addBlock(new JumpBlock(block[0], block[1], xCoord + x, yCoord + y, zCoord - z));
					if (generateOres)
						block = WarpDriveConfig.getRandomSurfaceBlock(worldObj.rand, defaultBlock[0], defaultBlock[1], true);
					addBlock(new JumpBlock(block[0], block[1], xCoord - x, yCoord - y, zCoord + z));
					if (generateOres)
						block = WarpDriveConfig.getRandomSurfaceBlock(worldObj.rand, defaultBlock[0], defaultBlock[1], true);
					addBlock(new JumpBlock(block[0], block[1], xCoord + x, yCoord - y, zCoord - z));
					if (generateOres)
						block = WarpDriveConfig.getRandomSurfaceBlock(worldObj.rand, defaultBlock[0], defaultBlock[1], true);
					addBlock(new JumpBlock(block[0], block[1], xCoord - x, yCoord + y, zCoord - z));
					if (generateOres)
						block = WarpDriveConfig.getRandomSurfaceBlock(worldObj.rand, defaultBlock[0], defaultBlock[1], true);
					addBlock(new JumpBlock(block[0], block[1], xCoord - x, yCoord - y, zCoord - z));
				}
			}
		}
		if (blocks != null) {
			WarpDrive.debugPrint("[EntitySphereGen] Saved " + blocks.size() + " blocks (estimated to " + pregenSize + ")");
		}
//		LocalProfiler.stop();
	}

	private void addBlock(JumpBlock jb) {
		if (blocks == null)
			return;
		// Replace water with random gas (ship in moon)
		if (worldObj.getBlockId(jb.x, jb.y, jb.z) == Block.leaves.blockID) {
			if (worldObj.rand.nextInt(50) != 1)
			{
				jb.blockID = WarpDriveConfig.gasID;
				jb.blockMeta = gasColor;
			}
			blocks.add(jb);
			return;
		}
		// Do not replace existing blocks if fillingSphere is true
		if (fillingSphere && !worldObj.isAirBlock(jb.x, jb.y, jb.z))
			return;
		blocks.add(jb);
	}

	private static double lengthSq(double x, double y, double z) {
		return (x * x) + (y * y) + (z * z);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
	}

	@Override
	protected void entityInit() {
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
	}

	// Own implementation of setting blocks without light recalculation in optimization purposes
	public boolean mySetBlock(World w, int x, int y, int z, int blockId, int blockMeta, int par6) {
		if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
			if (y < 0)
				return false;
			else if (y >= 256)
				return false;
			w.markBlockForUpdate(x, y, z);
			Chunk chunk = w.getChunkFromChunkCoords(x >> 4, z >> 4);
			return myChunkSBIDWMT(chunk, x & 15, y, z & 15, blockId, blockMeta);
		} else {
			return false;
		}
	}

	public boolean myChunkSBIDWMT(Chunk c, int x, int y, int z, int blockId, int blockMeta) {
		int j1 = z << 4 | x;
		if (y >= c.precipitationHeightMap[j1] - 1)
			c.precipitationHeightMap[j1] = -999;
		int l1 = c.getBlockID(x, y, z);
		int i2 = c.getBlockMetadata(x, y, z);
		if (l1 == blockId && i2 == blockMeta)
			return false;
		ExtendedBlockStorage[] storageArrays = c.getBlockStorageArray();
		ExtendedBlockStorage extendedblockstorage = storageArrays[y >> 4];
		if (extendedblockstorage == null)
		{
			if (blockId == 0)
				return false;
			extendedblockstorage = storageArrays[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4, !c.worldObj.provider.hasNoSky);
		}
		int j2 = c.xPosition * 16 + x;
		int k2 = c.zPosition * 16 + z;
		extendedblockstorage.setExtBlockID(x, y & 15, z, blockId);
		if (extendedblockstorage.getExtBlockID(x, y & 15, z) != blockId)
			return false;
		extendedblockstorage.setExtBlockMetadata(x, y & 15, z, blockMeta);
		if (blockId != 0)
			if (Block.blocksList[blockId] != null && Block.blocksList[blockId].hasTileEntity(blockMeta))
			{
				TileEntity tileentity = c.getChunkBlockTileEntity(x, y, z);
				if (tileentity == null)
				{
					tileentity = Block.blocksList[blockId].createTileEntity(c.worldObj, blockMeta);
					c.worldObj.setBlockTileEntity(j2, y, k2, tileentity);
				}
				else
				{
					tileentity.updateContainingBlockInfo();
					tileentity.blockMetadata = blockMeta;
				}
			}
		c.isModified = true;
		return true;
	}
	
    @Override
    public boolean shouldRenderInPass(int pass) {
        return false;
    }	
}