import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.Companion.FullLine
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.Companion.SingleLane
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.coroutines.CoroutineContext

@Composable
@Preview
fun App() {
    MaterialTheme {
        RecipeScreen()
    }
}

private class RecipeState(val recipe: Recipe): Recipe {
    var isExpanded by mutableStateOf(false)
    override val id: Int
        get() = recipe.id
    override val title: String
        get() = recipe.title
    override val category: String
        get() = recipe.category
    override val link: String
        get() = recipe.link
    override val servings: Int
        get() = recipe.servings
    override val ingredients: Map<String, String>
        get() = recipe.ingredients
    override val steps: String
        get() = recipe.steps
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel = viewModel { RecipeViewModel() }
) {
    val scope = rememberCoroutineScope()
    val recipes by viewModel.recipes.map(scope) { it.map(::RecipeState) }.collectAsState()
    Column {
        var isActive by remember { mutableStateOf(false) }
        SearchBar(
            query = viewModel.searchText,
            onQueryChange = viewModel::updateSearchText,
            onSearch = viewModel::updateSearchText,
            active = isActive,
            onActiveChange = { isActive = it },
            placeholder = { Text("Filtra per ingredienti...") },
            trailingIcon = {
                Row {
                    AnimatedVisibility(viewModel.searchText.isNotEmpty()) {
                        IconButton({ viewModel.updateSearchText("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                    AnimatedVisibility(isActive) {
                        IconButton({ isActive = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                }
            },
            modifier = Modifier.padding(8.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                viewModel.ingredients.forEach {
                    val isSelected = it in viewModel.filterIngredients
                    FilterChip(
                        selected = isSelected,
                        label = { Text(it, maxLines = 1) },
                        onClick = {
                            if(isSelected) viewModel.removeIngredientFromFilter(it)
                            else viewModel.addIngredientToFilter(it)
                        },
                        trailingIcon = {
                            if(isSelected) Icon(Icons.Default.Clear, null)
                            else Icon(Icons.Default.Add, null)
                        }
                    )
                }
            }
        }
        AnimatedVisibility(viewModel.filterIngredients.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                items(viewModel.filterIngredients, key = { it }) {
                    FilterChip(
                        selected = true,
                        label = { Text(it, maxLines = 1) },
                        onClick = { viewModel.removeIngredientFromFilter(it) },
                        trailingIcon = { Icon(Icons.Default.Clear, null) }
                    )
                }
            }
        }
        LazyVerticalStaggeredGrid(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            columns = StaggeredGridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            items(
                recipes,
                key = Recipe::id,
                span = { if (it.isExpanded) FullLine else SingleLane }
            ) {
                Recipe(it)
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
private fun Recipe(
    recipe: RecipeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier
            .clickable { recipe.isExpanded = !recipe.isExpanded }
    ) {
        Text(
            text = recipe.category,
            style = typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
        )
        Text(
            text = buildAnnotatedString {
                append(recipe.title)
                pushLink(LinkAnnotation.Url(recipe.link, TextLinkStyles(SpanStyle(color = colorScheme.primary))))
            },
            style = typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        AnimatedVisibility(recipe.isExpanded) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if(recipe.ingredients.isNotEmpty()) {
                    Text(
                        text = recipe.ingredients
                            .map { "${it.key} ${it.value}" }
                            .joinToString(prefix = "Ingredienti per ${recipe.servings} persone:\n")
                    )
                }
                if(recipe.steps.isNotEmpty()) {
                    Text(
                        text = recipe.steps
                    )
                }
            }
        }
    }
}

class RecipeViewModel: ViewModel() {
    private val mutableRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes = mutableRecipes.asStateFlow()

    var ingredients by mutableStateOf(emptyList<String>())
        private set

    var filterIngredients by mutableStateOf(emptyList<String>())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var searchText by mutableStateOf("")
        private set

    private var ingredientsJob: Job? = null

    init {
        ingredientsJob = viewModelScope.launch {
            mutableRecipes.value = Repository.recipes.random(25)
        }
    }

    fun updateSearchText(text: String) {
        searchText = text
        ingredientsJob?.cancel()
        ingredientsJob = viewModelScope.launch {
            ingredients = Repository.recipes.ingredientsStartingWith(text, 10)
        }
    }

    fun removeIngredientFromFilter(ingredient: String) {
        filterIngredients -= ingredient
        viewModelScope.launch {
            mutableRecipes.value = Repository.recipes.withIngredients(filterIngredients, 40)
        }
    }

    fun addIngredientToFilter(ingredient: String) {
        filterIngredients += ingredient
        viewModelScope.launch {
            mutableRecipes.value = Repository.recipes.withIngredients(filterIngredients, 40)
        }
    }

    private inline fun CoroutineScope.launchWhileLoading(
        context: CoroutineContext = this.coroutineContext,
        crossinline block: suspend CoroutineScope.() -> Unit
    ): Job {
        return launch(context) {
            isLoading = true
            block()
            isLoading = false
        }
    }
}