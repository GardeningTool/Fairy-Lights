package com.pau101.fairylights.util.crafting.ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.pau101.fairylights.util.crafting.GenericRecipe.MatchResultRegular;
import com.pau101.fairylights.util.crafting.ingredient.behavior.Behavior;
import com.pau101.fairylights.util.crafting.ingredient.behavior.BehaviorRegular;
import net.minecraft.item.ItemStack;

public class IngredientRegularList extends IngredientRegular {
	private final ImmutableList<IngredientRegular> ingredients;

	public IngredientRegularList(IngredientRegular... ingredients) {
		this(ImmutableList.copyOf(ingredients));
	}

	public IngredientRegularList(ImmutableList<IngredientRegular> ingredients) {
		this(ImmutableList.of(), EMPTY_TOOLTIP, ImmutableList.of(), ingredients);
	}

	public IngredientRegularList(ImmutableList<Behavior> behaviors, Consumer<List<String>> tooltip, ImmutableList<BehaviorRegular> regularBehaviors, ImmutableList<IngredientRegular> ingredients) {
		super(behaviors, tooltip, regularBehaviors);
		this.ingredients = Objects.requireNonNull(ingredients, "ingredients");
	}

	@Override
	public final MatchResultRegular matches(ItemStack input, ItemStack output) {
		MatchResultRegular matchResult = null;
		List<MatchResultRegular> supplementaryResults = new ArrayList<>(ingredients.size());
		for (IngredientRegular ingredient : ingredients) {
			MatchResultRegular result = ingredient.matches(input, output);
			if (result.doesMatch() && matchResult == null) {
				matchResult = result;
			} else {
				supplementaryResults.add(result);
			}
		}
		if (matchResult == null) {
			return new MatchResultRegular(this, input, false, supplementaryResults);
		}
		return matchResult.withParent(new MatchResultRegular(this, input, true, supplementaryResults));
	}

	@Override
	public ImmutableList<ItemStack> getInputs() {
		ImmutableList.Builder<ItemStack> inputs = ImmutableList.builder();
		for (IngredientRegular ingredient : ingredients) {
			inputs.addAll(ingredient.getInputs());
		}
		return inputs.build();
	}

	@Override
	public ImmutableList<ImmutableList<ItemStack>> getInput(ItemStack output) {
		List<List<ItemStack>> inputs = new ArrayList<>();
		for (IngredientRegular ingredient : ingredients) {
			ImmutableList<ImmutableList<ItemStack>> subInputs = ingredient.getInput(output);
			for (int i = 0; i < subInputs.size(); i++) {
				List<ItemStack> stacks;
				if (i < inputs.size()) {
					stacks = inputs.get(i);
				} else {
					inputs.add(stacks = new ArrayList<>());
				}
				stacks.addAll(subInputs.get(i));
			}
		}
		ImmutableList.Builder<ImmutableList<ItemStack>> inputsImm = ImmutableList.builder();
		for (List<ItemStack> list : inputs) {
			inputsImm.add(ImmutableList.copyOf(list));
		}
		return inputsImm.build();
	}
}
