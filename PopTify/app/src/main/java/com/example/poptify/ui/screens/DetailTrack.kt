package com.example.poptify.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.Track
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.poptify.FavoritesRepository
import com.example.poptify.R
import com.example.poptify.SpotifyApiRequest
import com.example.poptify.ui.components.AlbumCard
import com.example.poptify.ui.components.ArtistCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun DetailTrack(
    favoritesRepository: FavoritesRepository = remember { FavoritesRepository() },
    trackId: String,
    navController: NavController? = null,
    spotifyApi: SpotifyApiRequest = remember { SpotifyApiRequest() }
) {
    var track by remember { mutableStateOf<Track?>(null) } //Inicialización variables que se rellenarán al recibir un Track
    var album by remember { mutableStateOf<SimpleAlbum?>(null) }
    val artists = remember { mutableStateListOf<Artist>() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val favoriteTracks = remember { mutableStateListOf<String>() }
    val favoriteArtists = remember { mutableStateListOf<String>() }
    val favoriteAlbums = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        favoritesRepository.getFavoriteTracks().collect { tracks ->
            favoriteTracks.clear()
            favoriteTracks.addAll(tracks.map { it.id })
        }
    }

    LaunchedEffect(Unit) {
        favoritesRepository.getFavoriteArtists().collect { artists ->
            favoriteArtists.clear()
            favoriteArtists.addAll(artists.map { it.id })
        }
    }

    LaunchedEffect(Unit) {
        favoritesRepository.getFavoriteAlbums().collect { albums ->
            favoriteAlbums.clear()
            favoriteAlbums.addAll(albums.map { it.id })
        }
    }

    fun onFavoriteClick(item: Any, isFavorite: Boolean) { //Accion de añadir y eliminar de Favoritos enlazada a Firestore
        coroutineScope.launch {
            when (item) {
                is Track -> {
                    if (isFavorite) {
                        favoritesRepository.addFavoriteTrack(item)
                        favoriteTracks.add(item.id)
                    } else {
                        favoritesRepository.removeFavoriteTrack(item.id)
                        favoriteTracks.remove(item.id)
                    }
                }
                is Artist -> {
                    if (isFavorite) {
                        favoritesRepository.addFavoriteArtist(item)
                        favoriteArtists.add(item.id)
                    } else {
                        favoritesRepository.removeFavoriteArtist(item.id)
                        favoriteArtists.remove(item.id)
                    }
                }
                is SimpleAlbum -> {
                    if (isFavorite) {
                        favoritesRepository.addFavoriteAlbum(item)
                        favoriteAlbums.add(item.id)
                    } else {
                        favoritesRepository.removeFavoriteAlbum(item.id)
                        favoriteAlbums.remove(item.id)
                    }
                }
            }
        }
    }

    LaunchedEffect(trackId) { //Rellenado del track y sus artistas y album al que pertenece
        try {
            spotifyApi.buildSearchAPI()
            track = spotifyApi.getTrack(trackId)
        } catch (e: Exception) {
            error = "Error al cargar la canción"
        } finally {
            isLoading = false
        }

        try {
            spotifyApi.buildSearchAPI()
            var art: Artist
            for (artist in track!!.artists){
                art = spotifyApi.getArtist(artist.id);
                artists.add(art)
            }
        } catch (e: Exception) {
            error = "Error al cargar los artistas"
        } finally {
            isLoading = false
        }

        try {
            spotifyApi.buildSearchAPI()
            album = track!!.album
        } catch (e: Exception) {
            error = "Error al cargar los artistas"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles Track") },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) { //Volver a la pantalla anterior
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (track != null) {
                        val isFavorite = favoriteTracks.contains(track!!.id)
                        IconButton(onClick = {
                            onFavoriteClick(track!!, !isFavorite) //Añadir o eliminar de Favoritos en su misma página
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error ?: "Error desconocido")
                }
            }
            track != null -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val context = LocalContext.current

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = track?.name ?: "Título desconocido",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlideImage(
                        model = track?.album?.images?.firstOrNull()?.url ?: R.drawable.ic_music_note,
                        contentDescription = "Portada del álbum",
                        modifier = Modifier
                            .size(300.dp)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop,
                        colorFilter = if (track?.album?.images?.firstOrNull()?.url == null) {
                            ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        } else null
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Popularidad",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator( //Barra que indica la popularidad del track del 0 al 100
                        progress = (track?.popularity ?: 0) / 100f,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${track?.popularity}/100",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    track?.durationMs?.let { duration ->
                        Text(
                            text = "Duración: ${(duration / 60000)}:${String.format("%02d", (duration % 60000) / 1000)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(track!!.externalUrls.spotify))
                                context.startActivity(intent)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Text(
                            text = "Tocar para abrir en Spotify",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Artistas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column {
                        artists.forEach{ artist ->
                            ArtistCard(
                                artist = artist,
                                isFavorite = favoriteArtists.contains(artist.id),
                                onFavoriteClick = { t, fav -> onFavoriteClick(t, fav) },
                                onClick = {
                                    navController?.navigate("detail-artist/${artist.id}") // Pasa el ID aquí
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Album",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column {
                        if (album != null) {
                            AlbumCard(
                                album = album!!,
                                isFavorite = favoriteAlbums.contains(album!!.id),
                                onFavoriteClick = {t, fav -> onFavoriteClick(t, fav)},
                                onClick = {
                                    navController?.navigate("detail-album/${album!!.id}") // Pasa el ID aquí
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}