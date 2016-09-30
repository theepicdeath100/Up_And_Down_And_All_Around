package uk.co.mysterymayhem.gravitymod.capabilities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.util.GravityAxisAlignedBB;

import java.util.concurrent.Callable;

/**
 * Created by Mysteryem on 2016-08-14.
 */
public class GravityDirectionCapability {
    public static final EnumGravityDirection DEFAULT_GRAVITY = EnumGravityDirection.DOWN;

    public static void registerCapability() {
        CapabilityManager.INSTANCE.register(IGravityDirectionCapability.class, new Storage(), new Factory());
        MinecraftForge.EVENT_BUS.register(new GravityCapabilityEventHandler());
    }

    @CapabilityInject(IGravityDirectionCapability.class)
    public static Capability<IGravityDirectionCapability> GRAVITY_CAPABILITY_INSTANCE = null;

    public static final String RESOURCE_NAME = "IGravityCapability";
    public static final ResourceLocation CAPABILITY_RESOURCE_LOCATION  = new ResourceLocation(GravityMod.MOD_ID, RESOURCE_NAME);

    private static IGravityDirectionCapability getGravityCapability(String playerName, World world) {
        world.getPlayerEntityByName(playerName);
        EntityPlayer playerByUsername = world.getPlayerEntityByName(playerName);
        return playerByUsername == null ? null : getGravityCapability(playerByUsername);
    }

    public static IGravityDirectionCapability getGravityCapability(EntityPlayer player) {
        return player.getCapability(GRAVITY_CAPABILITY_INSTANCE, null);
    }

    public static EnumGravityDirection getGravityDirection(String playerName, World world) {
        return getGravityDirection(getGravityCapability(playerName, world));
    }

    public static EnumGravityDirection getGravityDirection(EntityPlayer player) {
        return getGravityDirection(getGravityCapability(player));
    }

    public static EnumGravityDirection getGravityDirection(IGravityDirectionCapability capability) {
        return capability == null ? DEFAULT_GRAVITY : capability.getDirection();
    }

    public static AxisAlignedBB newGravityAxisAligned(EntityPlayer player, AxisAlignedBB old) {
        IGravityDirectionCapability gravityCapability = getGravityCapability(player);
        if (gravityCapability == null) {
            // Occurs during construction of players (<init>)
            // Once the capability is added, we'll make sure the player's bounding box is a GravityAxisAlignedBB
            return old;
        }
        else {

        }
        return new GravityAxisAlignedBB(gravityCapability, old);
    }

    private static void setGravityDirection(String playerName, EnumGravityDirection direction, World world) {
        world.getPlayerEntityByName(playerName);
        EntityPlayer playerByUsername = world.getPlayerEntityByName(playerName);
        if (playerByUsername != null) {
            setGravityDirection(playerByUsername, direction);
        }
//        //DEBUG:
//        else {
//            FMLLog.info("Could not set gravity for %s, player could not be found in %s", playerName, world.getWorldInfo().getWorldName());
//        }
    }

    public static void setGravityDirection(EntityPlayer player, EnumGravityDirection direction) {
        IGravityDirectionCapability capability = getGravityCapability(player);
        EnumGravityDirection oldDirection = capability.getDirection();
        oldDirection.preModifyPlayerOnGravityChange(player, direction);
        setGravityDirection(capability, direction);
        direction.postModifyPlayerOnGravityChange(player, oldDirection);
//        FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer()
//                ((EntityPlayerWithGravity_DEPRECATED) player).setSize(player.width, player.height);
        //AxisAlignedBB axisalignedbb = player.getEntityBoundingBox();
        //player.setEntityBoundingBox(new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ, axisalignedbb.minX + (double) player.width, axisalignedbb.minY + (double) player.height, axisalignedbb.minZ + (double) player.width));
    }

    private static void setGravityDirection(IGravityDirectionCapability capability, EnumGravityDirection direction) {
        if (capability != null) {
            capability.setDirection(direction);
        }
    }

    public interface IGravityDirectionCapability {
        EnumGravityDirection getDirection();
        void setDirection(EnumGravityDirection direction);
    }

    public static class GravityDirectionCapabilityImpl implements IGravityDirectionCapability {
        private EnumGravityDirection direction;

        public GravityDirectionCapabilityImpl() {
            this.direction = DEFAULT_GRAVITY;
        }

        public GravityDirectionCapabilityImpl(EnumGravityDirection direction) {
            this.direction = direction;
        }

        public EnumGravityDirection getDirection() {
            return direction;
        }

        public void setDirection(EnumGravityDirection direction) {
            this.direction = direction;
        }
    }

    private static class Storage implements Capability.IStorage<IGravityDirectionCapability> {

        @Override
        public NBTBase writeNBT(Capability<IGravityDirectionCapability> capability, IGravityDirectionCapability instance, EnumFacing side) {
            return new NBTTagInt(instance.getDirection().ordinal());
        }

        @Override
        public void readNBT(Capability<IGravityDirectionCapability> capability, IGravityDirectionCapability instance, EnumFacing side, NBTBase nbt) {
            instance.setDirection(EnumGravityDirection.values()[((NBTPrimitive) nbt).getInt()]);
        }
    }

    private static class Factory implements Callable<IGravityDirectionCapability> {

        @Override
        public IGravityDirectionCapability call() throws Exception {
            return new GravityDirectionCapabilityImpl();
        }
    }

    /**
     * Created by Mysteryem on 2016-08-14.
     */
    public static class GravityCapabilityEventHandler {

        @SubscribeEvent
        public void onEntityContruct(AttachCapabilitiesEvent.Entity event) {
            if (event.getEntity() instanceof EntityPlayer) {
                final EntityPlayer player = (EntityPlayer)event.getEntity();
                event.addCapability(CAPABILITY_RESOURCE_LOCATION, new ICapabilitySerializable<NBTPrimitive>() {

                    IGravityDirectionCapability instance = GRAVITY_CAPABILITY_INSTANCE.getDefaultInstance();
                    //TODO: This should work right?
                    {
                        player.setEntityBoundingBox(new GravityAxisAlignedBB(instance, player.getEntityBoundingBox()));
                    }

                    @Override
                    public NBTPrimitive serializeNBT() {
                        return (NBTPrimitive) GRAVITY_CAPABILITY_INSTANCE.getStorage().writeNBT(GRAVITY_CAPABILITY_INSTANCE, instance, null);
                    }

                    @Override
                    public void deserializeNBT(NBTPrimitive nbt) {
                        GRAVITY_CAPABILITY_INSTANCE.getStorage().readNBT(GRAVITY_CAPABILITY_INSTANCE, instance, null, nbt);
                    }

                    @Override
                    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
                        return GRAVITY_CAPABILITY_INSTANCE == capability;
                    }

                    @Override
                    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
                        return capability == GRAVITY_CAPABILITY_INSTANCE ? GRAVITY_CAPABILITY_INSTANCE.<T>cast(instance) : null;
                    }
                });
                //
            }
        }
    }
}