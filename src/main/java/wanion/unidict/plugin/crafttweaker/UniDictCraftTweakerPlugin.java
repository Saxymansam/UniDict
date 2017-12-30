package wanion.unidict.plugin.crafttweaker;

/*
 * Created by WanionCane(https://github.com/WanionCane).
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import wanion.lib.recipe.RecipeAttributes;
import wanion.lib.recipe.RecipeHelper;
import wanion.unidict.UniDict;
import wanion.unidict.api.UniDictAPI;
import wanion.unidict.common.Reference;
import wanion.unidict.plugin.crafttweaker.RemovalByKind.AbstractRemovalByKind;
import wanion.unidict.plugin.crafttweaker.RemovalByKind.Crafting;
import wanion.unidict.resource.Resource;
import wanion.unidict.resource.UniResourceContainer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ZenRegister
@ZenClass("mods.unidict.api")
public final class UniDictCraftTweakerPlugin
{
	private static final List<ShapedRecipeTemplate> NEW_SHAPED_RECIPE_TEMPLATE_LIST = new ArrayList<>();
	private static final List<ShapelessRecipeTemplate> NEW_SHAPELESS_RECIPE_TEMPLATE_LIST = new ArrayList<>();
	private static final Map<Class<? extends AbstractRemovalByKind>, AbstractRemovalByKind> ABSTRACT_REMOVAL_BY_KIND_MAP = new HashMap<>();

	private UniDictCraftTweakerPlugin() { }

	@ZenMethod
	public static void newShapedRecipeTemplate(@Nonnull final String outputKind, final int outputSize, @Nonnull final String[][] inputKinds)
	{
		CraftTweakerAPI.apply(new ShapedRecipeTemplate(outputKind, outputSize, inputKinds));
	}

	@ZenMethod
	public static void newShapelessRecipeTemplate(@Nonnull final String outputKind, final int outputSize, @Nonnull final String[] inputKinds)
	{
		CraftTweakerAPI.apply(new ShapelessRecipeTemplate(outputKind, outputSize, inputKinds));
	}

	public static void preInit()
	{
		registerAbstractRemovalByKind(new Crafting());
	}

	private static void registerAbstractRemovalByKind(@Nonnull final AbstractRemovalByKind abstractRemovalByKind)
	{
		ABSTRACT_REMOVAL_BY_KIND_MAP.put(abstractRemovalByKind.getClass(), abstractRemovalByKind);
	}

	public static <R extends AbstractRemovalByKind> R getRemovalByKind(@Nonnull final Class<R> abstractRemovalByKindClass)
	{
		return abstractRemovalByKindClass.cast(ABSTRACT_REMOVAL_BY_KIND_MAP.get(abstractRemovalByKindClass));
	}

	public static void postInit(@Nonnull final FMLPostInitializationEvent event)
	{
		final UniDictAPI uniDictAPI = ABSTRACT_REMOVAL_BY_KIND_MAP.size() > 0 || NEW_SHAPED_RECIPE_TEMPLATE_LIST.size() > 0 || NEW_SHAPELESS_RECIPE_TEMPLATE_LIST.size() > 0 ? UniDict.getAPI() : null;
		if (uniDictAPI == null)
			return;
		ABSTRACT_REMOVAL_BY_KIND_MAP.values().forEach(removalByKind -> removalByKind.apply(uniDictAPI));
		final List<IRecipe> recipeList = new ArrayList<>();
		fetchShapedRecipeTemplates(uniDictAPI, recipeList);
		fetchShapelessRecipeTemplates(uniDictAPI, recipeList);
		final IForgeRegistry<IRecipe> recipeRegistry = ForgeRegistries.RECIPES;
		recipeList.forEach(recipe -> recipeRegistry.register(recipe.setRegistryName(new ResourceLocation(recipe.getGroup()))));
	}

	private static void fetchShapedRecipeTemplates(@Nonnull UniDictAPI uniDictAPI, @Nonnull final List<IRecipe> recipeList)
	{
		NEW_SHAPED_RECIPE_TEMPLATE_LIST.forEach(shapedRecipeTemplate -> {
			boolean badEntry = false;
			if (Resource.getKindFromName(shapedRecipeTemplate.outputKind) == 0)
				badEntry = true;
			for (final String[] subInputs : shapedRecipeTemplate.inputs)
				for (String input : subInputs)
					if (!input.isEmpty() && (shapedRecipeTemplate.outputKind.equals(input) || Resource.getKindFromName(input) == 0))
						badEntry = true;
			if (!badEntry) {
				final TObjectIntMap<String> nameKindMap = new TObjectIntHashMap<>();
				nameKindMap.put(shapedRecipeTemplate.outputKind, Resource.getKindFromName(shapedRecipeTemplate.outputKind));
				final String[] trueInputs = new String[9];
				for (int x = 0; x < 3; x++)
					for (int y = 0; y < 3; y++)
						if ((y * 3 + x) < trueInputs.length)
							trueInputs[y * 3 + x] = !shapedRecipeTemplate.inputs[x][y].equals("") ? shapedRecipeTemplate.inputs[x][y] : null;
				for (final String input : trueInputs)
					if (input != null && !nameKindMap.containsKey(input))
						nameKindMap.put(input, Resource.getKindFromName(input));
				final RecipeAttributes recipeAttributes = RecipeHelper.rawShapeToShape(trueInputs);
				final int outputKind = Resource.getKindFromName(shapedRecipeTemplate.outputKind);
				final List<Resource> resourceList = uniDictAPI.getResources(nameKindMap.values());
				resourceList.forEach(resource -> {
					final UniResourceContainer uniResourceContainer = resource.getChild(outputKind);
					final ItemStack itemStack = uniResourceContainer.getMainEntry();
					final int stackSize = MathHelper.clamp(shapedRecipeTemplate.outputSize, 1, itemStack.getMaxStackSize());
					itemStack.setCount(stackSize);
					recipeList.add(new ShapedOreRecipe(new ResourceLocation(Reference.MOD_ID, uniResourceContainer.name + ".x" + stackSize + "_shape." + recipeAttributes.shape + ".template"), itemStack, kindShapeToActualShape(recipeAttributes.actualShape, resource)));
				});
			}
		});
	}

	private static void fetchShapelessRecipeTemplates(@Nonnull UniDictAPI uniDictAPI, @Nonnull final List<IRecipe> recipeList)
	{
		NEW_SHAPELESS_RECIPE_TEMPLATE_LIST.forEach(shapelessRecipeTemplate -> {
			boolean badEntry = false;
			if (Resource.getKindFromName(shapelessRecipeTemplate.outputKind) == 0)
				badEntry = true;
			for (final String input : shapelessRecipeTemplate.inputs)
				if (!input.isEmpty() && (shapelessRecipeTemplate.outputKind.equals(input) || Resource.getKindFromName(input) == 0))
					badEntry = true;
			if (!badEntry) {
				final TObjectIntMap<String> nameKindMap = new TObjectIntHashMap<>();
				nameKindMap.put(shapelessRecipeTemplate.outputKind, Resource.getKindFromName(shapelessRecipeTemplate.outputKind));
				for (final String input : shapelessRecipeTemplate.inputs)
					if (!nameKindMap.containsKey(input))
						nameKindMap.put(input, Resource.getKindFromName(input));
				final int outputKind = Resource.getKindFromName(shapelessRecipeTemplate.outputKind);
				final List<Resource> resourceList = uniDictAPI.getResources(nameKindMap.values());
				resourceList.forEach(resource -> {
					final UniResourceContainer uniResourceContainer = resource.getChild(outputKind);
					final ItemStack itemStack = uniResourceContainer.getMainEntry();
					final int stackSize = MathHelper.clamp(shapelessRecipeTemplate.outputSize, 1, itemStack.getMaxStackSize());
					itemStack.setCount(stackSize);
					recipeList.add(new ShapelessOreRecipe(new ResourceLocation(Reference.MOD_ID, uniResourceContainer.name + ".x" + shapelessRecipeTemplate.outputSize + "_size." + shapelessRecipeTemplate.inputs.length + ".template"), itemStack, kindShapeToActualShape(shapelessRecipeTemplate.inputs, resource)));
				});
			}
		});
	}

	private static Object[] kindShapeToActualShape(@Nonnull final Object[] inputs, @Nonnull final Resource resource)
	{
		final Object[] newInputKinds = new Object[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			final Object input = inputs[i];
			if (input instanceof String) {
				final int kind = Resource.getKindFromName((String) input);
				if (kind != 0) {
					newInputKinds[i] = resource.getChild(kind).name;
					continue;
				}
			}
			newInputKinds[i] = input;
		}
		return newInputKinds;
	}

	private static class ShapedRecipeTemplate implements IAction
	{
		private final String outputKind;
		private final int outputSize;
		private final String[][] inputs;

		private ShapedRecipeTemplate(@Nonnull final String outputKind, final int outputSize, @Nonnull final String[][] inputs)
		{
			this.outputKind = outputKind;
			this.outputSize = outputSize;
			this.inputs = inputs;
		}

		@Override
		public void apply()
		{
			NEW_SHAPED_RECIPE_TEMPLATE_LIST.add(this);
		}

		@Override
		public String describe()
		{
			return "Trying to create a Shaped Recipe Template for kind: " + outputKind;
		}
	}

	private static class ShapelessRecipeTemplate implements IAction
	{
		private final String outputKind;
		private final int outputSize;
		private final String[] inputs;

		private ShapelessRecipeTemplate(@Nonnull final String output, final int outputSize, @Nonnull final String[] inputs)
		{
			this.outputKind = output;
			this.outputSize = outputSize;
			this.inputs = inputs;
		}

		@Override
		public void apply()
		{
			NEW_SHAPELESS_RECIPE_TEMPLATE_LIST.add(this);
		}

		@Override
		public String describe()
		{
			return "Trying to create a Shapeless Recipe Template for kind: " + outputKind;
		}
	}
}