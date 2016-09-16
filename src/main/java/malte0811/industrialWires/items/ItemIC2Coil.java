package malte0811.industrialWires.items;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.IWireCoil;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import blusunrize.immersiveengineering.common.IESaveData;
import blusunrize.immersiveengineering.common.util.IEAchievements;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import blusunrize.immersiveengineering.common.util.Utils;
import malte0811.industrialWires.IndustrialWires;
import malte0811.industrialWires.wires.IC2Wiretype;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ItemIC2Coil extends Item implements IWireCoil{
	public final static String[] subNames = {"tin", "copper", "gold", "hv"};

	public ItemIC2Coil() {
		setUnlocalizedName(IndustrialWires.MODID+".ic2wireCoil");
		setHasSubtypes(true);
		this.setCreativeTab(IndustrialWires.creativeTab);
		setMaxStackSize(64);
		ImmersiveEngineering.registerByFullName(this, IndustrialWires.MODID+":"+"ic2WireCoil");
	}
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
		for (int i = 0;i<subNames.length;i++) {
			subItems.add(new ItemStack(this, 1, i));
		}
	}
	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return this.getUnlocalizedName()+"."+subNames[stack.getMetadata()];
	}
	@Override
	public WireType getWireType(ItemStack stack) {
		return IC2Wiretype.IC2_TYPES[stack.getMetadata()];
	}
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean adv) {
		if(stack.getTagCompound()!=null && stack.getTagCompound().hasKey("linkingPos")) {
			int[] link = stack.getTagCompound().getIntArray("linkingPos");
			if(link!=null&&link.length>3) {
				list.add(I18n.format(Lib.DESC_INFO+"attachedToDim", link[1],link[2],link[3],link[0]));
			}
		}
	}

	//copied from "vanilla" IE
	@Override
	public EnumActionResult onItemUseFirst(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
		if(!world.isRemote) {
			TileEntity tileEntity = world.getTileEntity(pos);
			if(tileEntity instanceof IImmersiveConnectable && ((IImmersiveConnectable)tileEntity).canConnect()) {
				TargetingInfo target = new TargetingInfo(side, hitX,hitY,hitZ);
				WireType wire = getWireType(stack);
				BlockPos masterPos = ((IImmersiveConnectable)tileEntity).getConnectionMaster(wire, target);
				tileEntity = world.getTileEntity(masterPos);
				if( !(tileEntity instanceof IImmersiveConnectable) || !((IImmersiveConnectable)tileEntity).canConnect()) {
					return EnumActionResult.PASS;
				}

				if( !((IImmersiveConnectable)tileEntity).canConnectCable(wire, target)) {
					player.addChatMessage(new TextComponentTranslation(Lib.CHAT_WARN+"wrongCable"));
					return EnumActionResult.FAIL;
				}

				if(!ItemNBTHelper.hasKey(stack, "linkingPos")) {
					ItemNBTHelper.setIntArray(stack, "linkingPos", new int[]{world.provider.getDimension(),masterPos.getX(),masterPos.getY(),masterPos.getZ()});
					target.writeToNBT(stack.getTagCompound());
				} else {
					WireType type = getWireType(stack);
					int[] array = ItemNBTHelper.getIntArray(stack, "linkingPos");
					BlockPos linkPos = new BlockPos(array[1],array[2],array[3]);
					TileEntity tileEntityLinkingPos = world.getTileEntity(linkPos);
					int distanceSq = (int) Math.ceil( linkPos.distanceSq(masterPos) );
					if(array[0]!=world.provider.getDimension()) {
						player.addChatMessage(new TextComponentTranslation(Lib.CHAT_WARN+"wrongDimension"));
					} else if(linkPos.equals(masterPos)) {
						player.addChatMessage(new TextComponentTranslation(Lib.CHAT_WARN+"sameConnection"));
					} else if( distanceSq > (type.getMaxLength()*type.getMaxLength())) {
						player.addChatMessage(new TextComponentTranslation(Lib.CHAT_WARN+"tooFar"));
					} else if(!(tileEntityLinkingPos instanceof IImmersiveConnectable)) {
						player.addChatMessage(new TextComponentTranslation(Lib.CHAT_WARN+"invalidPoint"));
					} else {
						IImmersiveConnectable nodeHere = (IImmersiveConnectable)tileEntity;
						IImmersiveConnectable nodeLink = (IImmersiveConnectable)tileEntityLinkingPos;
						boolean connectionExists = false;
						Set<Connection> outputs = ImmersiveNetHandler.INSTANCE.getConnections(world, Utils.toCC(nodeHere));
						if(outputs!=null) {
							for(Connection con : outputs) {
								if(con.end.equals(Utils.toCC(nodeLink))) {
									connectionExists = true;
								}
							}
						}
						if(connectionExists) {
							player.addChatMessage(new TextComponentTranslation(Lib.CHAT_WARN+"connectionExists"));
						} else {
							Vec3d rtOff0 = nodeHere.getRaytraceOffset(nodeLink).addVector(masterPos.getX(), masterPos.getY(), masterPos.getZ());
							Vec3d rtOff1 = nodeLink.getRaytraceOffset(nodeHere).addVector(linkPos.getX(), linkPos.getY(), linkPos.getZ());
							Set<BlockPos> ignore = new HashSet<>();
							ignore.addAll(nodeHere.getIgnored(nodeLink));
							ignore.addAll(nodeLink.getIgnored(nodeHere));
							boolean canSee = Utils.rayTraceForFirst(rtOff0, rtOff1, world, ignore)==null;
							if(canSee) {
								TargetingInfo targetLink = TargetingInfo.readFromNBT(stack.getTagCompound());
								ImmersiveNetHandler.INSTANCE.addConnection(world, Utils.toCC(nodeHere), Utils.toCC(nodeLink), (int)Math.sqrt(distanceSq), type);

								nodeHere.connectCable(type, target, nodeLink);
								nodeLink.connectCable(type, targetLink, nodeHere);
								IESaveData.setDirty(world.provider.getDimension());
								player.addStat(IEAchievements.connectWire);

								if(!player.capabilities.isCreativeMode) {
									stack.stackSize--;
								}
								((TileEntity)nodeHere).markDirty();
								world.addBlockEvent(masterPos, ((TileEntity) nodeHere).getBlockType(), -1, 0);
								IBlockState state = world.getBlockState(masterPos);
								world.notifyBlockUpdate(masterPos, state,state, 3);
								((TileEntity)nodeLink).markDirty();
								world.addBlockEvent(linkPos, ((TileEntity) nodeLink).getBlockType(), -1, 0);
								state = world.getBlockState(linkPos);
								world.notifyBlockUpdate(linkPos, state,state, 3);
							} else {
								player.addChatMessage(new TextComponentTranslation(Lib.CHAT_WARN+"cantSee"));
							}
						}
					}
					ItemNBTHelper.remove(stack, "linkingPos");
					ItemNBTHelper.remove(stack, "side");
					ItemNBTHelper.remove(stack, "hitX");
					ItemNBTHelper.remove(stack, "hitY");
					ItemNBTHelper.remove(stack, "hitZ");
				}
				return EnumActionResult.SUCCESS;
			}
		}
		return EnumActionResult.PASS;
	}
}
