package cr0s.warpdrive.data;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;

public class JumpBlock {
	public Block block;
	public int blockMeta;
	public TileEntity blockTileEntity;
	public NBTTagCompound blockNBT;
	public int x;
	public int y;
	public int z;

	public JumpBlock() {
	}

	public JumpBlock(Block block, int blockMeta, int x, int y, int z) {
		this.block = block;
		this.blockMeta = blockMeta;
		blockTileEntity = null;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public JumpBlock(Block block, int blockMeta, TileEntity tileEntity, int x, int y, int z) {
		this.block = block;
		this.blockMeta = blockMeta;
		blockTileEntity = tileEntity;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public boolean deploy(World targetWorld, int offsetX, int offsetY, int offsetZ) {
		try {
			int newX = x + offsetX;
			int newY = y + offsetY;
			int newZ = z + offsetZ;
			setBlockNoLight(targetWorld, newX, newY, newZ, block, blockMeta, 2);

			// Re-schedule air blocks update
			if (block == WarpDrive.airBlock) {
				targetWorld.markBlockForUpdate(newX, newY, newZ);
				targetWorld.scheduleBlockUpdate(newX, newY, newZ, block, 40 + targetWorld.rand.nextInt(20));
			}

			NBTTagCompound oldnbt = new NBTTagCompound();
			if (blockTileEntity != null) {
				blockTileEntity.writeToNBT(oldnbt);
				oldnbt.setInteger("x", newX);
				oldnbt.setInteger("y", newY);
				oldnbt.setInteger("z", newZ);

				if (oldnbt.hasKey("mainX") && oldnbt.hasKey("mainY") && oldnbt.hasKey("mainZ")) { // Mekanism 6.0.4.44
					WarpDrive.debugPrint("[JUMP] moveBlockSimple: TileEntity from Mekanism detected");
					oldnbt.setInteger("mainX", oldnbt.getInteger("mainX") + offsetX);
					oldnbt.setInteger("mainY", oldnbt.getInteger("mainY") + offsetY);
					oldnbt.setInteger("mainZ", oldnbt.getInteger("mainZ") + offsetZ);
				}

				TileEntity newTileEntity = null;
				boolean isForgeMultipart = false;
				if (oldnbt.hasKey("id") && oldnbt.getString("id") == "savedMultipart" && WarpDriveConfig.isForgeMultipartLoaded) {
					isForgeMultipart = true;
					newTileEntity = (TileEntity) WarpDriveConfig.forgeMultipart_helper_createTileFromNBT.invoke(null, targetWorld, oldnbt);

				} else if (block == WarpDriveConfig.CC_Computer || block == WarpDriveConfig.CC_peripheral || block == WarpDriveConfig.CCT_Turtle || block == WarpDriveConfig.CCT_Upgraded
						|| block == WarpDriveConfig.CCT_Advanced) {
					newTileEntity = TileEntity.createAndLoadEntity(oldnbt);
					newTileEntity.invalidate();

				} /*else if (block == WarpDriveConfig.AS_Turbine) {
					if (oldnbt.hasKey("zhuYao")) {
						NBTTagCompound nbt1 = oldnbt.getCompoundTag("zhuYao");
						nbt1.setDouble("x", newX);
						nbt1.setDouble("y", newY);
						nbt1.setDouble("z", newZ);
						oldnbt.setTag("zhuYao", nbt1);
					}
					newTileEntity = TileEntity.createAndLoadEntity(oldnbt);
					}*///No 1.7.10 version

				if (newTileEntity == null) {
					newTileEntity = TileEntity.createAndLoadEntity(oldnbt);
				}

				if (newTileEntity != null) {
					newTileEntity.setWorldObj(targetWorld);
					newTileEntity.validate();

					targetWorld.setTileEntity(newX, newY, newZ, newTileEntity);
					if (isForgeMultipart) {
						WarpDriveConfig.forgeMultipart_tileMultipart_onChunkLoad.invoke(newTileEntity);
						WarpDriveConfig.forgeMultipart_helper_sendDescPacket.invoke(null, targetWorld, newTileEntity);
					}
				} else {
					WarpDrive.logger.info(" moveBlockSimple failed to create new tile entity at " + x + ", " + y + ", " + z + " blockId " + block + ":" + blockMeta);
					WarpDrive.logger.info("NBT data was " + ((oldnbt == null) ? "null" : oldnbt.toString()));
				}
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			String coordinates = "";
			try {
				coordinates = " at " + x + ", " + y + ", " + z + " blockId " + block + ":" + blockMeta;
			} catch (Exception dropMe) {
				coordinates = " (unknown coordinates)";
			}
			WarpDrive.logger.info("moveBlockSimple exception at " + coordinates);
			return false;
		}

		return true;
	}

	// This code is a straight copy from Vanilla net.minecraft.world.World.setBlock to remove lighting computations
	public static boolean setBlockNoLight(World w, int x, int y, int z, Block block, int blockMeta, int par6) {
		if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
			if (y < 0) {
				return false;
			} else if (y >= 256) {
				return false;
			} else {
				Chunk chunk = w.getChunkFromChunkCoords(x >> 4, z >> 4);
				Block block1 = null;
				// net.minecraftforge.common.util.BlockSnapshot blockSnapshot = null;

				if ((par6 & 1) != 0) {
					block1 = chunk.getBlock(x & 15, y, z & 15);
				}

				// Disable rollback on item use
				// if (w.captureBlockSnapshots && !w.isRemote) {
				// 	blockSnapshot = net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(w, x, y, z, par6);
				// 	w.capturedBlockSnapshots.add(blockSnapshot);
				// }

				boolean flag = myChunkSBIDWMT(chunk, x & 15, y, z & 15, block, blockMeta);

				// Disable rollback on item use
				// if (!flag && blockSnapshot != null) {
				//	w.capturedBlockSnapshots.remove(blockSnapshot);
				//	blockSnapshot = null;
				// }

				// Remove light computations
				// w.theProfiler.startSection("checkLight");
				// w.func_147451_t(x, y, z);
				// w.theProfiler.endSection();

				// Disable rollback on item use
				// if (flag && blockSnapshot == null) {// Don't notify clients or update physics while capturing blockstates
					// Modularize client and physic updates
					w.markAndNotifyBlock(x, y, z, chunk, block1, block, par6);
				// }

				return flag;
			}
		} else {
			return false;
		}
	}

	// This code is a straight copy from Vanilla net.minecraft.world.Chunk.func_150807_a to remove lighting computations
	public static boolean myChunkSBIDWMT(Chunk c, int x, int y, int z, Block block, int blockMeta) {
		int i1 = z << 4 | x;

		if (y >= c.precipitationHeightMap[i1] - 1) {
			c.precipitationHeightMap[i1] = -999;
		}

		// Removed light recalculations
		// int j1 = c.heightMap[i1];
		Block block1 = c.getBlock(x, y, z);
		int k1 = c.getBlockMetadata(x, y, z);

		if (block1 == block && k1 == blockMeta) {
			return false;
		} else {
			ExtendedBlockStorage[] storageArrays = c.getBlockStorageArray();
			ExtendedBlockStorage extendedblockstorage = storageArrays[y >> 4];
			// Removed light recalculations
			// boolean flag = false;

			if (extendedblockstorage == null) {
				if (block == Blocks.air) {
					return false;
				}

				extendedblockstorage = storageArrays[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4, !c.worldObj.provider.hasNoSky);
				// Removed light recalculations
				// flag = y >= j1;
			}

			int l1 = c.xPosition * 16 + x;
			int i2 = c.zPosition * 16 + z;

			// Removed light recalculations
			// int k2 = block1.getLightOpacity(c.worldObj, l1, y, i2);

			// Removed preDestroy event
			// if (!c.worldObj.isRemote) {
			// 	block1.onBlockPreDestroy(c.worldObj, l1, y, i2, k1);
			// }

			extendedblockstorage.func_150818_a(x, y & 15, z, block);
			extendedblockstorage.setExtBlockMetadata(x, y & 15, z, blockMeta); // This line duplicates the one below, so breakBlock fires with valid worldstate

			// Skip air at destination
			if (block1 != Blocks.air) {
				if (!c.worldObj.isRemote) {
					block1.breakBlock(c.worldObj, l1, y, i2, block1, k1);
					// After breakBlock a phantom TE might have been created with incorrect meta. This attempts to kill that phantom TE so the normal one can be create properly later
					TileEntity te = c.getTileEntityUnsafe(x & 0x0F, y, z & 0x0F);
					if (te != null && te.shouldRefresh(block1, c.getBlock(x & 0x0F, y, z & 0x0F), k1, c.getBlockMetadata(x & 0x0F, y, z & 0x0F), c.worldObj, l1, y, i2)) {
						c.removeTileEntity(x & 0x0F, y, z & 0x0F);
					}
				} else if (block1.hasTileEntity(k1)) {
					TileEntity te = c.getTileEntityUnsafe(x & 0x0F, y, z & 0x0F);
					if (te != null && te.shouldRefresh(block1, block, k1, blockMeta, c.worldObj, l1, y, i2)) {
						c.worldObj.removeTileEntity(l1, y, i2);
					}
				}
			}

			if (extendedblockstorage.getBlockByExtId(x, y & 15, z) != block) {
				return false;
			} else {
				extendedblockstorage.setExtBlockMetadata(x, y & 15, z, blockMeta);
				// Removed light recalculations
				/*
				if (flag) {
					c.generateSkylightMap();
				} else {
					int j2 = block.getLightOpacity(c.worldObj, l1, y, i2);

					if (j2 > 0) {
						if (y >= j1) {
							c.relightBlock(x, y + 1, z);
						}
					} else if (y == j1 - 1) {
						c.relightBlock(x, y, z);
					}

					if (j2 != k2 && (j2 < k2 || c.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) > 0 || c.getSavedLightValue(EnumSkyBlock.Block, x, y, z) > 0)) {
						c.propagateSkylightOcclusion(x, z);
					}
				}
				/**/

				TileEntity tileentity;

				// Removed onBlockAdded event
				// if (!c.worldObj.isRemote) {
				//	block.onBlockAdded(c.worldObj, l1, y, i2);
				// }

				// Skip air at destination
				if (block1 != Blocks.air) {
					if (block.hasTileEntity(blockMeta)) {
						tileentity = c.func_150806_e(x, y, z);
	
						if (tileentity != null) {
							tileentity.updateContainingBlockInfo();
							tileentity.blockMetadata = blockMeta;
						}
					}
				}

				c.isModified = true;
				return true;
			}
		}
	}
}
