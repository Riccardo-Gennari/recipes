import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.ext.toRealmDictionary
import io.realm.kotlin.query.Sort
import io.realm.kotlin.types.RealmDictionary
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import recipes.composeapp.generated.resources.Res

class RealmRecipe : RealmObject, Recipe {
    @PrimaryKey
    override var id: Int = 0
    override var title: String = ""
    override var category: String = ""
    override var link: String = ""
    override var servings: Int = 0
    override var ingredients: RealmDictionary<String> = realmDictionaryOf()
    override var steps: String = ""
}

class JsonRecipe(
    private val id: Int,
    private val title: String,
    private val category: String,
    private val link: String,
    private val servings: Int,
    private val ingredients: String,
    private val steps: String
) {
    fun toRealmRecipe(): RealmRecipe {
        return RealmRecipe().also { recipe ->
            recipe.id = id
            recipe.title = title
            recipe.category = category
            recipe.link = link
            recipe.servings = servings
            recipe.steps = steps
            recipe.ingredients = Gson().fromJson(
                ingredients,
                object : TypeToken<List<List<String>>>() {}
            ).associate { it[0] to it[1] }.toRealmDictionary()
        }
    }
}

interface Recipe {
    val id: Int
    val title: String
    val category: String
    val link: String
    val servings: Int
    val ingredients: Map<String, String>
    val steps: String
}

class RealmIngredientName() : RealmObject {
    @PrimaryKey var name: String = ""
        set(value) {
            field = value
            nameLength = value.length
        }

    var nameLength: Int = name.length

    constructor(name: String): this() {
        this.name = name
    }
}

private val realm: Realm by lazy {
    Realm.open(
        RealmConfiguration
            .Builder(schema = setOf(RealmRecipe::class, RealmIngredientName::class))
            .deleteRealmIfMigrationNeeded()
            .initialData {
                val ingredients = mutableSetOf<String>()
                runBlocking(Dispatchers.IO) {
                    getRecipesFromResources()
                }.forEach { jsonRecipe ->
                    val recipe = jsonRecipe.toRealmRecipe()
                    copyToRealm(recipe)
                    ingredients.addAll(recipe.ingredients.keys)
                }
                ingredients.forEach {
                    copyToRealm(RealmIngredientName(it))
                }
            }
            .build()
    )
}

class RecipeRepository(private val realm: Realm) {
    suspend fun random(limit: Int): List<Recipe> {
        return withContext(Dispatchers.IO) {
            realm.query<RealmRecipe>().limit(limit).find()
        }
    }

    suspend fun ingredientsStartingWith(prefix: String, limit: Int): List<String> {
        return withContext(Dispatchers.IO) {
            if(prefix.isEmpty()) {
                emptyList()
            } else {
                realm
                    .query<RealmIngredientName>("name BEGINSWITH[c] '$prefix'")
                    .sort("nameLength", Sort.ASCENDING)
                    .limit(limit)
                    .find()
                    .map(RealmIngredientName::name)
            }
        }
    }

    suspend fun withIngredients(ingredients: List<String>, limit: Int): List<Recipe> {
        return withContext(Dispatchers.IO) {
            if(ingredients.isNotEmpty()) {
                val query = buildString {
                    ingredients.forEachIndexed { index, s ->
                        if(index != 0) {
                            append(" AND ")
                        }
                        append("ANY ingredients.@keys == '$s'")
                    }
                }
                realm.query<RealmRecipe>(query)
                    .limit(limit)
                    .find()
            } else {
                random(limit)
            }
        }
    }
}

object Repository {
    val recipes = RecipeRepository(realm)
}

private suspend fun getRecipesFromResources(): List<JsonRecipe> = withContext(Dispatchers.IO) {
    @OptIn(ExperimentalResourceApi::class)
    return@withContext Gson()
        .fromJson(
            Res.readBytes("files/recipes.json").decodeToString(),
            object : TypeToken<List<JsonRecipe>>() {}
        )
}