package dev.ftb.mods.sluice.recipe;

import dev.ftb.mods.sluice.SluiceMod;
import dev.ftb.mods.sluice.block.MeshType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SluiceModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SluiceMod.MOD_ID);

    public static final RegistryObject<RecipeSerializer<?>> SLUICE = REGISTRY.register("sluice", SluiceRecipeSerializer::new);
    public static final RecipeType<SluiceRecipe> SLUICE_TYPE = RecipeType.register(SluiceMod.MOD_ID + ":sluice");

    public static final RegistryObject<RecipeSerializer<?>> HAMMER = REGISTRY.register("hammer", HammerRecipeSerializer::new);
    public static final RecipeType<HammerRecipe> HAMMER_TYPE = RecipeType.register(SluiceMod.MOD_ID + ":hammer");
    public static final List<Ingredient> hammerableCache = new ArrayList<>();
    private static final Map<Triple<Fluid, Item, MeshType>, InputRecipeResult> sluiceCache = new HashMap<>();
    private static final Map<Item, List<ItemStack>> hammerCache = new HashMap<>();

    public static void clearCache() {
        sluiceCache.clear();
        hammerCache.clear();
        hammerableCache.clear();
    }

    /**
     * Runs through all sluice recipes to find matching mesh -> resulting items.
     *
     * @param world level
     * @param mesh  the type of mesh for the sluice
     * @param input an input item to find results for
     * @return A list of items with the chances.
     */
    public static InputRecipeResult getSluiceRecipes(Fluid fluid, Level world, MeshType mesh, ItemStack input) {
        return sluiceCache.computeIfAbsent(Triple.of(fluid, input.getItem(), mesh), key -> {
            world.getRecipeManager().getRecipesFor(SLUICE_TYPE, NoInventory.INSTANCE, world).forEach(e -> System.out.println(e.fluid));
            List<ItemWithWeight> list = new ArrayList<>();

            int max = -1;
            for (SluiceRecipe recipe : world.getRecipeManager().getRecipesFor(SLUICE_TYPE, NoInventory.INSTANCE, world)) {
                if (recipe.meshes.contains(mesh) && recipe.ingredient.test(input) && fluid.isSame(recipe.fluid)) {
                    // Only set based on the first min max we see.
                    if (max == -1) {
                        max = recipe.max;
                    }

                    recipe.results.forEach(e -> list.add(new ItemWithWeight(e.item, e.weight)));
                }
            }

            return new InputRecipeResult(list, max);
        });
    }

    /**
     * Checks that a given input has any result.
     */
    public static boolean itemHasSluiceResults(Fluid fluid, Level level, MeshType mesh, ItemStack input) {
        return !getSluiceRecipes(fluid, level, mesh, input).getItems().isEmpty();
    }

    /**
     * Computes a list of resulting output items based on an input. We get the outputting items from the
     * custom recipe.
     */
    public static List<ItemStack> getRandomResult(Fluid fluid, Level world, MeshType mesh, ItemStack input) {
        List<ItemStack> outputResults = new ArrayList<>();
        InputRecipeResult recipe = getSluiceRecipes(fluid, world, mesh, input);

        for (ItemWithWeight result : recipe.getItems()) {
            float number = world.getRandom().nextFloat();
            if (number <= result.weight) {
                if (outputResults.size() >= recipe.getMaxDrops()) {
                    break;
                }

                outputResults.add(result.item.copy());
            }
        }

        return outputResults;
    }

    public static List<ItemStack> getHammerDrops(Level level, ItemStack input) {
        return hammerCache.computeIfAbsent(input.getItem(), key -> {
            List<ItemStack> drops = new ArrayList<>();
            for (HammerRecipe recipe : level.getRecipeManager().getRecipesFor(HAMMER_TYPE, NoInventory.INSTANCE, level)) {
                if (recipe.ingredient.test(input)) {
                    recipe.results.forEach(e -> drops.add(e.copy()));
                }
            }

            return drops;
        });
    }

    public static boolean hammerable(BlockState state) {
        return hammerableCache.stream().anyMatch(e -> {
            ItemStack stack = new ItemStack(state.getBlock());
            return e.test(stack);
        });
    }
}
