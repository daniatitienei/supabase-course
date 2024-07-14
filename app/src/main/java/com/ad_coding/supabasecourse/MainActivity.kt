@file:OptIn(SupabaseExperimental::class)

package com.ad_coding.supabasecourse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil.compose.AsyncImage
import com.ad_coding.supabasecourse.ui.theme.SupabaseCourseTheme
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.selectAsFlow
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@OptIn(SupabaseInternal::class)
class MainActivity : ComponentActivity() {

    private val supabase = createSupabaseClient(
        supabaseUrl = "YOUR_URL",
        supabaseKey = "YOUR_KEY"
    ) {
        install(Postgrest)
        install(Realtime)
        httpConfig {
            this.install(WebSockets)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SupabaseCourseTheme {
                Scaffold { innerPadding ->
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = CafeListScreen) {
                        composable<CafeListScreen> {
                            CafeList(
                                onCafeClick = {
                                    navController.navigate(CafeScreen(id = it))
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        composable<CafeScreen> {
                            val args = it.toRoute<CafeScreen>()

                            CafeDetails(cafeID = args.id, modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data object CafeListScreen

    @Serializable
    data class CafeScreen(
        val id: Int
    )

    @Composable
    fun CafeList(
        onCafeClick: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var cafes by remember { mutableStateOf<List<Cafe>>(listOf()) }
        LaunchedEffect(Unit) {
            getCafeListRealtime()
                .collect {
                    cafes = it
                }
        }
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                cafes,
                key = { cafe -> cafe.id },
            ) { cafe ->
                CafeCard(
                    cafe = cafe,
                    onClick = { onCafeClick(cafe.id) }
                )
            }
        }
    }

    @Composable
    fun CafeDetails(cafeID: Int, modifier: Modifier = Modifier) {
        var cafe by remember { mutableStateOf<Cafe?>(null) }

        LaunchedEffect(Unit) {
            cafe = getCafeByID(cafeID)
        }

        Column {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(bottomEnd = 20.dp, bottomStart = 20.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = cafe?.image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 6.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = cafe?.name ?: "",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = cafe?.description ?: "",
                    fontSize = 18.sp
                )
            }
        }
    }

    @Composable
    fun CafeCard(cafe: Cafe, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = cafe.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )

            Text(
                text = cafe.name,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    fun getCafeListRealtime() =
        supabase.from("cafe").selectAsFlow(Cafe::id)

    suspend fun getCafeList() =
        supabase.from("cafe")
            .select()
            .decodeList<Cafe>()


    suspend fun getCafeByID(id: Int) =
        supabase.from("cafe")
            .select(columns = Columns.list("id", "name", "description", "image")) {
                filter {
                    Cafe::id eq id
                }
            }
            .decodeSingle<Cafe>()

    suspend fun updateCafe(cafe: Cafe) {
        supabase.from("cafe").update(
            {
                set("name", cafe.name)
                set("description", cafe.description)
            }
        ) {
            filter {
                eq("id", cafe.id)
            }
        }
    }

    suspend fun insertCafe(cafe: Cafe) {
        supabase.from("cafe").insert(cafe)
    }

    suspend fun upsertCafe(cafe: Cafe) {
        supabase.from("cafe").upsert(cafe)
    }

    suspend fun deleteCafe(cafe: Cafe) {
        supabase.from("cafe").delete {
            filter {
                Cafe::id eq cafe.id
            }
        }
    }
}

@Serializable
data class Cafe(
    val id: Int,
    val name: String,
    val description: String,
    val image: String
)

