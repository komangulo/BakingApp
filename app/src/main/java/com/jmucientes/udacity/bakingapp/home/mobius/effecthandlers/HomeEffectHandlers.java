package com.jmucientes.udacity.bakingapp.home.mobius.effecthandlers;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.jmucientes.udacity.bakingapp.data.RecipeRepository;
import com.jmucientes.udacity.bakingapp.home.mobius.domain.HomeEffect;
import com.jmucientes.udacity.bakingapp.home.mobius.domain.HomeEffect.ShowFeedback;
import com.jmucientes.udacity.bakingapp.home.mobius.domain.HomeEvent;
import com.jmucientes.udacity.bakingapp.model.Recipe;
import com.spotify.mobius.rx2.RxMobius;

import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

import static com.jmucientes.udacity.bakingapp.home.mobius.domain.HomeEffect.NavigateToRecipeDetailsList;
import static com.jmucientes.udacity.bakingapp.home.mobius.domain.HomeEffect.RequestRecipes;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

public class HomeEffectHandlers {

    private static final String TAG = HomeEffectHandlers.class.getName();

    private HomeEffectHandlers() {
    }

    public static ObservableTransformer<HomeEffect, HomeEvent> provideEffectHandler(
            RecipeRepository recipeRepository,
            Consumer<Recipe> navigateToDetailsCommand,
            Action showErrorView) {
        Log.d(TAG, "setUp -> provideEffectHandler()");
        return RxMobius.<HomeEffect, HomeEvent>subtypeEffectHandler()
                .addTransformer(RequestRecipes.class, handleRequestRecipes(recipeRepository))
                .addConsumer(NavigateToRecipeDetailsList.class, handleNavigateToRecipe(navigateToDetailsCommand), mainThread())
                .addConsumer(ShowFeedback.class, handleShowFeedback(showErrorView))
                .build();
    }

    @VisibleForTesting
    static ObservableTransformer<RequestRecipes, HomeEvent> handleRequestRecipes(
            RecipeRepository recipeRepository) {
        return requests ->
                requests.flatMap(request ->
                        recipeRepository.getRecipesMaybe()
                                .doAfterSuccess(result -> Log.d(TAG, "handleRequestRecipes() Got response from server. Size: " + result.size() ))
                                .toObservable()
                                .map(result -> HomeEvent.recipesLoaded(ImmutableList.copyOf(result)))
                                .onErrorReturnItem(HomeEvent.taskLoadingFailed())
                                .doOnError(error -> Log.w("HomeEffectHandler", "Failed to load recipes: " + error.getMessage())));
    }

    @VisibleForTesting
    static Consumer<NavigateToRecipeDetailsList> handleNavigateToRecipe(Consumer<Recipe> command) {
        return navigationEffect -> command.accept(navigationEffect.recipe());
    }

    @VisibleForTesting
    static Consumer<ShowFeedback> handleShowFeedback(Action showErrorView) {

        return showFeedback -> {
            switch (showFeedback.type()) {
                case LOADING_ERROR:
                    showErrorView.run();
                    break;
            }
        };
    }

}
