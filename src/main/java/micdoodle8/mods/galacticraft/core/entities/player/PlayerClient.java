package micdoodle8.mods.galacticraft.core.entities.player;

import cpw.mods.fml.client.FMLClientHandler;
import micdoodle8.mods.galacticraft.api.entity.ICameraZoomEntity;
import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;
import micdoodle8.mods.galacticraft.core.dimension.WorldProviderMoon;
import micdoodle8.mods.galacticraft.core.dimension.WorldProviderOrbit;
import micdoodle8.mods.galacticraft.core.entities.EntityLanderBase;
import micdoodle8.mods.galacticraft.core.event.EventWakePlayer;
import micdoodle8.mods.galacticraft.core.proxy.ClientProxyCore;
import micdoodle8.mods.galacticraft.core.tick.TickHandlerClient;
import micdoodle8.mods.galacticraft.core.tile.TileEntityAdvanced;
import micdoodle8.mods.galacticraft.core.util.WorldUtil;
import micdoodle8.mods.galacticraft.core.wrappers.PlayerGearData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.MinecraftForge;

public class PlayerClient implements IPlayerClient
{
    @Override
    public void moveEntity(EntityPlayerSP player, double par1, double par3, double par5)
    {
        this.updateFeet(player, par1, par5);
    }

    @Override
    public boolean wakeUpPlayer(EntityPlayerSP player, boolean par1, boolean par2, boolean par3)
    {
        return this.wakeUpPlayer(player, par1, par2, par3, false);
    }

    @Override
    public void onUpdate(EntityPlayerSP player)
    {
        GCPlayerStatsClient stats = GCPlayerStatsClient.get(player);
        stats.tick++;

        if (stats.usingParachute && !player.capabilities.isFlying && !player.handleWaterMovement())
        {
            player.motionY = -0.5D;
            player.motionX *= 0.5F;
            player.motionZ *= 0.5F;
        }
    }

    @Override
    public boolean isEntityInsideOpaqueBlock(EntityPlayerSP player, boolean vanillaInside)
    {
        return !(player.ridingEntity instanceof EntityLanderBase) && vanillaInside;
    }

    @Override
    public void onLivingUpdatePre(EntityPlayerSP player)
    {
        GCPlayerStatsClient stats = GCPlayerStatsClient.get(player);

        if (player.worldObj.provider instanceof IGalacticraftWorldProvider)
        {
            //Test whether feet are on a block, also stops the login glitch
            boolean flag = true;
            int playerFeetOnY = (int) (player.boundingBox.minY - 0.001D);
            Block b = player.worldObj.getBlock(MathHelper.floor_double(player.posX), playerFeetOnY, MathHelper.floor_double(player.posZ));
            if (b.getMaterial() != Material.air && !(b instanceof BlockLiquid))
            {
                double blockYmax = playerFeetOnY + b.getBlockBoundsMaxY();
                if (player.boundingBox.minY - blockYmax < 0.001D && player.boundingBox.minY - blockYmax > -0.5D)
                {
                    player.onGround = true;
                    player.posY -= player.boundingBox.minY - blockYmax;
                    player.boundingBox.offset(0, blockYmax - player.boundingBox.minY, 0);
                    flag = false;
                }
            }

            if (player.worldObj.provider instanceof WorldProviderOrbit)
            {
                ((WorldProviderOrbit) player.worldObj.provider).spinUpdate(player, flag);
            }
        }

        if (player.boundingBox != null && stats.boundingBoxBefore == null)
        {
            stats.boundingBoxBefore = player.boundingBox;
            player.boundingBox.setBounds(stats.boundingBoxBefore.minX + 0.4, stats.boundingBoxBefore.minY + 0.9, stats.boundingBoxBefore.minZ + 0.4, stats.boundingBoxBefore.maxX - 0.4, stats.boundingBoxBefore.maxY - 0.9, stats.boundingBoxBefore.maxZ - 0.4);
        }
        else if (player.boundingBox != null && stats.boundingBoxBefore != null)
        {
            player.boundingBox.setBB(stats.boundingBoxBefore);
        }
    }

    @Override
    public void onLivingUpdatePost(EntityPlayerSP player)
    {
        GCPlayerStatsClient stats = GCPlayerStatsClient.get(player);

        if (stats.inFreefall)
        {
            player.limbSwing -= player.limbSwingAmount;
            player.limbSwingAmount = player.prevLimbSwingAmount;
        }

        boolean ridingThirdPersonEntity = player.ridingEntity instanceof ICameraZoomEntity && ((ICameraZoomEntity) player.ridingEntity).defaultThirdPerson();

        if (ridingThirdPersonEntity && !stats.lastRidingCameraZoomEntity)
        {
            FMLClientHandler.instance().getClient().gameSettings.thirdPersonView = 1;
        }

        if (player.ridingEntity != null && player.ridingEntity instanceof ICameraZoomEntity)
        {
            TickHandlerClient.zoom(((ICameraZoomEntity) player.ridingEntity).getCameraZoom());
        }
        else
        {
            TickHandlerClient.zoom(4.0F);
        }

        stats.lastRidingCameraZoomEntity = ridingThirdPersonEntity;

        if (stats.usingParachute)
        {
            player.fallDistance = 0.0F;
        }

        PlayerGearData gearData = ClientProxyCore.playerItemData.get(player.getCommandSenderName());

        stats.usingParachute = false;

        if (gearData != null)
        {
            stats.usingParachute = gearData.getParachute() != null;
        }

        if (stats.usingParachute && player.onGround)
        {
            stats.setParachute(false);
            FMLClientHandler.instance().getClient().gameSettings.thirdPersonView = stats.thirdPersonView;
        }

        if (!stats.lastUsingParachute && stats.usingParachute)
        {
            FMLClientHandler.instance().getClient().getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation(GalacticraftCore.TEXTURE_PREFIX + "player.parachute"), 0.95F + player.getRNG().nextFloat() * 0.1F, 1.0F, (float) player.posX, (float) player.posY, (float) player.posZ));
        }

        stats.lastUsingParachute = stats.usingParachute;
        stats.lastOnGround = player.onGround;
    }

    @Override
    public float getBedOrientationInDegrees(EntityPlayerSP player, float vanillaDegrees)
    {
        if (player.playerLocation != null)
        {
            int x = player.playerLocation.posX;
            int y = player.playerLocation.posY;
            int z = player.playerLocation.posZ;

            if (player.worldObj.getTileEntity(x, y, z) instanceof TileEntityAdvanced)
            {
//                int j = player.worldObj.getBlock(x, y, z).getBedDirection(player.worldObj, x, y, z);
                switch (player.worldObj.getBlockMetadata(x, y, z) - 4)
                {
                case 0:
                    return 90.0F;
                case 1:
                    return 270.0F;
                case 2:
                    return 180.0F;
                case 3:
                    return 0.0F;
                }
            }
            else
            {
                return vanillaDegrees;
            }
        }

        return vanillaDegrees;
    }

    private void updateFeet(EntityPlayerSP player, double motionX, double motionZ)
    {
        GCPlayerStatsClient stats = GCPlayerStatsClient.get(player);
        double motionSqrd = motionX * motionX + motionZ * motionZ;

        // If the player is on the moon, not airbourne and not riding anything
        if (motionSqrd > 0.001 && player.worldObj != null && player.worldObj.provider instanceof WorldProviderMoon && player.ridingEntity == null)
        {
            int iPosX = (int) Math.floor(player.posX);
            int iPosY = (int) Math.floor(player.posY - 2);
            int iPosZ = (int) Math.floor(player.posZ);

            // If the block below is the moon block
            if (player.worldObj.getBlock(iPosX, iPosY, iPosZ) == GCBlocks.blockMoon)
            {
                // And is the correct metadata (moon turf)
                if (player.worldObj.getBlockMetadata(iPosX, iPosY, iPosZ) == 5)
                {
                    // If it has been long enough since the last step
                    if (stats.distanceSinceLastStep > 0.35)
                    {
                        Vector3 pos = new Vector3(player);
                        // Set the footprint position to the block below and add random number to stop z-fighting
                        pos.y = MathHelper.floor_double(player.posY - 1) + player.getRNG().nextFloat() / 100.0F;

                        // Adjust footprint to left or right depending on step count
                        switch (stats.lastStep)
                        {
                        case 0:
                            pos.translate(new Vector3(Math.sin(Math.toRadians(-player.rotationYaw + 90)) * 0.25, 0, Math.cos(Math.toRadians(-player.rotationYaw + 90)) * 0.25));
                            break;
                        case 1:
                            pos.translate(new Vector3(Math.sin(Math.toRadians(-player.rotationYaw - 90)) * 0.25, 0, Math.cos(Math.toRadians(-player.rotationYaw - 90)) * 0.25));
                            break;
                        }

                        pos = WorldUtil.getFootprintPosition(player.worldObj, player.rotationYaw - 180, pos, new BlockVec3(player));

                        long chunkKey = ChunkCoordIntPair.chunkXZ2Int(pos.intX() >> 4, pos.intZ() >> 4);
                        ClientProxyCore.footprintRenderer.addFootprint(chunkKey, player.worldObj.provider.dimensionId, pos, player.rotationYaw);

                        // Increment and cap step counter at 1
                        stats.lastStep++;
                        stats.lastStep %= 2;
                        stats.distanceSinceLastStep = 0;
                    }
                    else
                    {
                        stats.distanceSinceLastStep += motionSqrd;
                    }
                }
            }
        }
    }

    public boolean wakeUpPlayer(EntityPlayerSP player, boolean par1, boolean par2, boolean par3, boolean bypass)
    {
        ChunkCoordinates c = player.playerLocation;

        if (c != null)
        {
            EventWakePlayer event = new EventWakePlayer(player, c.posX, c.posY, c.posZ, par1, par2, par3, bypass);
            MinecraftForge.EVENT_BUS.post(event);

            if (bypass || event.result == null || event.result == EntityPlayer.EnumStatus.OK)
            {
                return false;
            }
        }

        return true;
    }
}