package wanion.unidict;

/*
 * Created by WanionCane(https://github.com/WanionCane).
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.google.common.collect.Sets;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.lang3.text.WordUtils;
import wanion.unidict.common.Reference;
import wanion.unidict.resource.Resource;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static net.minecraftforge.fml.common.Loader.isModLoaded;
import static wanion.unidict.common.Reference.SLASH;

public final class Config implements UniDict.IDependency
{
	// general configs
	public final boolean libraryMode;
	public final boolean keepOneEntry;
	public final boolean registerTheIngredientsOfTheNewRecipesAsItemStacks;
	public final boolean inputReplacementFurnace;
	public final boolean inputReplacementIC2;
	public final boolean inputReplacementMekanism;
	public final Set<String> keepOneEntryModBlackSet;
	public final boolean autoHideInJEI;
	public final Set<String> hideInJEIBlackSet;
	public final boolean kindDebugMode;
	public final boolean enableSpecificKindSort;
	public final TObjectIntMap<String> ownerOfEveryThing;
	public final Set<String> metalsToUnify;
	public final Set<String> childrenOfMetals;
	public final List<String> resourceBlackList;
	public final Map<String, Set<String>> customUnifiedResources;
	// userRegisteredOreDictEntries
	public final List<String> userRegisteredOreDictEntries;
	// modules
	public final boolean integrationModule;
	//public final boolean processingModule;
	// config
	private final Configuration config;
	// resource related stuff
	private final String resources = "resources";

	private Config()
	{
		boolean deleted = false;
		config = new Configuration(new File("." + SLASH + "config" + SLASH + Reference.MOD_ID + SLASH + Reference.MOD_NAME + ".cfg"), Reference.MOD_VERSION);
		try {
			if (!config.getDefinedConfigVersion().equals(config.getLoadedConfigVersion()))
				deleted = config.getConfigFile().delete();

			// config
			// general configs
			final String general = Configuration.CATEGORY_GENERAL;
			libraryMode = config.getBoolean("libraryMode", general, false, "Enable this if you have mods that depends on UniDict but you don't like the unification.");
			keepOneEntry = config.getBoolean("keepOneEntry", general, false, "keep only one entry per ore dict entry?");
			keepOneEntryModBlackSet = Collections.unmodifiableSet(Sets.newLinkedHashSet(Arrays.asList(config.getStringList("keepOneEntryModBlackList", general, new String[]{}, "mods listed here will be blacklisted in keepOneEntry.\nmust be the exact modID."))));
			registerTheIngredientsOfTheNewRecipesAsItemStacks = config.getBoolean("registerTheIngredientsOfTheNewRecipesAsItemStacks", general, false, "If Enabled, the inputs of the new recipes created by Crafting Integration will be registered as ItemStacks. Enable this if you don't like the cycling through the possibilities of JEI.");
			autoHideInJEI = config.getBoolean("autoHideInJEI", general, true, "auto hide items in JEI?") && isModLoaded("JEI");
			hideInJEIBlackSet = Collections.unmodifiableSet(Sets.newLinkedHashSet(Arrays.asList(config.getStringList("autoHideInJEIBlackList", general, new String[]{"ore"}, "put here things that you don't want to hide in JEI.\nonly works if keepOneEntry is false."))));
			kindDebugMode = config.getBoolean("kindDebugMode", general, false, "Enable this to keep track of all the kinds.\nthe output will be in logs folder.");
			// input replacement
			inputReplacementFurnace = config.getBoolean("furnace", "inputReplacement", false, "Enabling this will remove all non-standard items as input of the Furnace.");
			inputReplacementIC2 = config.getBoolean("ic2", "inputReplacement", false, "Enabling this will remove all non-standard items as input of IC2 Machine Recipes.\nNote: this will only affect recipes that doesn't uses OreDictionary.");
			inputReplacementMekanism = config.getBoolean("mekanism", "inputReplacement", false, "Enabling this will remove all non-standard items as input of Mekanism Machine Recipes.");
			// resource related stuff
			enableSpecificKindSort = config.getBoolean("enableSpecificKindSort", resources, false, "enabling this allow you to specify the \"owner\" of each kind.\nalso will make \"S:ownerOfEveryThing\" be ignored.");
			ownerOfEveryThing = new TObjectIntHashMap<>((!enableSpecificKindSort) ? getOwnerOfEveryThingMap() : new TObjectIntHashMap<>());
			metalsToUnify = Collections.unmodifiableSet(Sets.newLinkedHashSet(Arrays.asList(config.getStringList("metalsToUnify", resources, new String[]{"Iron", "Gold", "Copper", "Tin", "Silver", "Lead", "Nickel", "Platinum", "Zinc", "Aluminium", "Aluminum", "Alumina", "Chromium", "Chrome", "Uranium", "Iridium", "Osmium", "Bronze", "Steel", "Brass", "Invar", "Electrum", "Cupronickel", "Constantan"}, "list of things to do unifying things.\n"))));
			childrenOfMetals = Collections.unmodifiableSet(Sets.newLinkedHashSet(Arrays.asList(config.getStringList("childrenOfMetals", resources, new String[]{"ore", "dustTiny", "dustSmall", "chunk", "dust", "nugget", "ingot", "block", "plate", "gear", "rod"}, "what kind of child do you want to make a standard?\n"))));
			resourceBlackList = Arrays.asList(config.getStringList("resourceBlackList", resources, new String[]{"Aluminum", "Alumina", "Chrome", "Constantan"}, "resources to be black-listed.\nthis exists to avoid duplicates.\nthis affect the API."));
			customUnifiedResources = Collections.unmodifiableMap(getCustomUnifiedResourcesMap());
			// userRegisteredOreDictEntries
			userRegisteredOreDictEntries = Arrays.asList(config.getStringList("userRegisteredOreDictEntries", general, new String[]{}, "This allows to the user register their own ore entries before the Unification happen.\nthis is mainly useful when the user is trying to unify things that aren't registered previously in the Ore Dictionary.\n\nFormat:\nweirdStone+minecraft:stone#1\nThe example above will register Granite as weirdStone."));
			// modules
			integrationModule = config.getBoolean("integration", "modules", true, "Integration Module.\nif false all the Integrations will be disabled.\n");
			//processingModule = config.getBoolean("processing", "modules", false, "Processing Module.\nif false all the Processing Addons will be disabled.\n");
		} catch (Exception e) {
			throw new RuntimeException("Something went wrong on " + config.getConfigFile() + " loading. " + e);
		}
		if (config.hasChanged() || deleted)
			config.save();
	}

	public void saveIfHasChanged()
	{
		if (config.hasChanged())
			config.save();
	}

	public TObjectIntMap<String> getOwnerOfEveryKindMap(final int kind)
	{
		final String kindName = WordUtils.capitalize(Resource.getNameOfKind(kind));
		final String[] ownerOfEveryKind = config.getStringList("ownerOfEvery" + kindName, resources, new String[]{"minecraft", "substratum", "ic2", "mekanism", "immersiveengineering", "techreborn"}, "entries of kind \"" + kindName + "\" will be sorted according to the modID list below\nmust be the exact modID.\n");
		final TObjectIntMap<String> ownerOfEveryThingMap = new TObjectIntHashMap<>(10, 1, Integer.MAX_VALUE);
		for (int i = 0; i < ownerOfEveryKind.length; i++)
			ownerOfEveryThingMap.put(ownerOfEveryKind[i], i);
		return ownerOfEveryThingMap;
	}

	private TObjectIntMap<String> getOwnerOfEveryThingMap()
	{
		final String[] ownerOfEveryThing = config.getStringList("ownerOfEveryThing", resources, new String[]{"minecraft", "substratum", "ic2", "mekanism", "immersiveengineering", "techreborn"}, "all the entries will be sorted according to the modID list below\nmust be the exact modID.\n");
		final TObjectIntMap<String> ownerOfEveryThingMap = new TObjectIntHashMap<>(10, 1, Integer.MAX_VALUE);
		for (int i = 0; i < ownerOfEveryThing.length; i++)
			ownerOfEveryThingMap.put(ownerOfEveryThing[i], i);
		return ownerOfEveryThingMap;
	}

	private Map<String, Set<String>> getCustomUnifiedResourcesMap()
	{
		final Map<String, Set<String>> customUnifiedResources = new THashMap<>();
		final Pattern splitPattern = Pattern.compile("\\|");
		for (String customUnifiedResource : config.getStringList("customUnifiedResources", resources, new String[]{"Obsidian:dustTiny|dust", "Stone:dust", "Obsidian:dust|dustSmall", "Coal:dust|dustSmall", "Sulfur:dust|dustSmall", "Salt:dust"}, "Here you can put a list to custom unify them.\nmust be in this format \"ResourceName:kind1|kind2|...\".")) {
			final int baseSeparatorIndex;
			final Set<String> kindSet;
			if ((baseSeparatorIndex = customUnifiedResource.indexOf(':')) != -1 && !(kindSet = Sets.newLinkedHashSet(Arrays.asList(splitPattern.split(customUnifiedResource.substring(baseSeparatorIndex + 1, customUnifiedResource.length()))))).isEmpty())
				customUnifiedResources.put(customUnifiedResource.substring(0, baseSeparatorIndex), kindSet);
		}
		return customUnifiedResources;
	}
}