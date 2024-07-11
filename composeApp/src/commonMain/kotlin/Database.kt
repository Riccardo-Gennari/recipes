import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import recipes.composeapp.generated.resources.Res

private class MutableRecipe: RealmObject, Recipe {
    override var id: Int = 0
    override var title: String = ""
    override var category: String = ""
    override var link: String = ""
    override var servings: Int = 0
    override var ingredients: String = ""
    override var steps: String = ""
}

interface Recipe {
    val id: Int
    val title: String
    val category: String
    val link: String
    val servings: Int
    val ingredients: String
    val steps: String
}

private val realm: Realm by lazy {
    Realm.open(
        RealmConfiguration
            .Builder(schema = setOf(MutableRecipe::class))
            .initialData {
                runBlocking(Dispatchers.IO) {
                    getRecipesFromResources()
                }.forEach(::copyToRealm)
            }
            .build()
    )
}

class RecipeRepository(private val realm: Realm) {
    fun random(count: Int): List<Recipe> {
        return realm.query<MutableRecipe>().limit(count).find()
    }
}

object Repository {
    val recipes = RecipeRepository(realm)
}

private suspend fun getRecipesFromResources(): List<MutableRecipe> = withContext(Dispatchers.IO) {
    @OptIn(ExperimentalResourceApi::class)
    return@withContext Gson().fromJson(
        Res.readBytes("files/recipes.json").decodeToString(),
        object : TypeToken<List<MutableRecipe>>() {}
    )
}