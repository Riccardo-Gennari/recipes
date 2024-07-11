import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        RecipeScreen()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel = viewModel { RecipeViewModel() }
) {
    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.filterIngredients.forEach {
                Ingredient(it) {
                    viewModel.removeIngredientFromFilter(it)
                }
            }
            IconButton(
                onClick = { TODO() }
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize().padding(4.dp)
        ) {
            items(viewModel.recipes, key = Recipe::id) {
                Recipe(it, modifier = Modifier.padding(4.dp))
            }
        }
        AnimatedVisibility(viewModel.isLoading) {
            LoadingDialog()
        }
    }
}

@Composable
fun LoadingDialog() {
    Dialog(onDismissRequest = {}) {
        Box(modifier = Modifier.padding(20.dp)) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun Ingredient(name: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        label = { Text(name, maxLines = 1) },
        onClick = onClick,
        trailingIcon = { Icon(Icons.Default.Delete, null) }
    )
}

@Composable
fun Recipe(
    recipe: Recipe,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Text(
            text = recipe.category,
            style = typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
        )
        Text(
            text = recipe.title,
            style = typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp)
        )
    }
}

class RecipeViewModel: ViewModel() {
    var recipes by mutableStateOf(emptyList<Recipe>())
        private set

    var ingredients by mutableStateOf(emptySet<String>())
        private set

    var filterIngredients by mutableStateOf(emptySet<String>())
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            whileLoading {
                recipes = Repository.recipes.random(100)
                ingredients = emptySet() // TODO
            }
        }
    }

    fun removeIngredientFromFilter(ingredient: String) {
        filterIngredients -= ingredient
    }

    fun addIngredientToFilter(ingredient: String) {
        filterIngredients += ingredient
    }

    private inline fun <T> whileLoading(block: () -> T) {
        isLoading = true
        block()
        isLoading = false
    }
}