import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import recipes.composeapp.generated.resources.Res

@Database(entities = [Recipe::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipes(): RecipeDao
}

@Dao
interface RecipeDao {
    @Insert(Recipe::class, OnConflictStrategy.REPLACE)
    suspend fun set(recipes: List<Recipe>)

    @Query("SELECT * FROM Recipe ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandom(count: Int): List<Recipe>

    @Query("SELECT * FROM Recipe ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): Recipe?

    @Query("SELECT COUNT(id) FROM Recipe")
    suspend fun getRecipeCount(): Int
}

class RecipeRepository(
    database: AppDatabase
) {
    private val recipes = database.recipes()

    suspend fun random(count: Int): List<Recipe> =
        recipes.getRandom(count)

    suspend fun random(): Recipe? =
        recipes.getRandom()

    suspend fun count(): Int =
        recipes.getRecipeCount()
}

@Entity
data class Recipe(
    @PrimaryKey val id: Long,
    val title: String,
    val category: String,
    val link: String,
    val servings: String,
    val ingredients: String,
    val steps: String
)

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

private val roomDatabase = getDatabaseBuilder()
    .setDriver(BundledSQLiteDriver())
    .fallbackToDestructiveMigration(true)
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
    .also {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            if(it.recipes().getRecipeCount() == 0) {
                it.recipes().set(getRecipesFromResources())
            }
        }
    }

object Repository {
    val recipes = RecipeRepository(roomDatabase)
}

private suspend fun getRecipesFromResources(): List<Recipe> = withContext(Dispatchers.IO) {
    @OptIn(ExperimentalResourceApi::class)
    return@withContext Gson().fromJson(
        Res.readBytes("files/recipes.json").decodeToString(),
        object : TypeToken<List<Recipe>>() {}
    )
}