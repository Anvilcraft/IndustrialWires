/*
 * This file is part of Industrial Wires.
 * Copyright (C) 2016-2017 malte0811
 *
 * Industrial Wires is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Industrial Wires is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Industrial Wires.  If not, see <http://www.gnu.org/licenses/>.
 */

package malte0811.industrialWires;

import malte0811.industrialWires.hv.MarxOreHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

import javax.annotation.Nonnull;

//TODO register
public class IWSaveData extends WorldSavedData {
	private final static String MARX_ORES = "marxOres";

	public IWSaveData() {
		super(IndustrialWires.MODID);
	}

	@Override
	public void readFromNBT(@Nonnull NBTTagCompound nbt) {
		NBTTagCompound ores = nbt.getCompoundTag(MARX_ORES);
		MarxOreHandler.load(ores);
	}

	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
		compound.setTag(MARX_ORES, MarxOreHandler.save());
		return compound;
	}
}