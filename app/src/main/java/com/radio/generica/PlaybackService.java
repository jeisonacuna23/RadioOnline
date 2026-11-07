package com.radio.generica;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;
    private final String STREAM_URL = "https://radio.conectarservidor.com:8016/stream";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. Crear el reproductor (sin setRepeatMode en el builder)
        ExoPlayer player = new ExoPlayer.Builder(this)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(), true)
                .setHandleAudioBecomingNoisy(true)
                .build();

        // 2. Aplicar la configuración directamente al reproductor
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        player.setPauseAtEndOfMediaItems(true);

        mediaSession = new MediaSession.Builder(this, player)
                .setId("RadioOnlineSession")
                .build();
                
        Uri artworkUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.logo);
        
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(STREAM_URL)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle("A Voz en Cuello")
                        .setArtist("En Vivo")
                        .setArtworkUri(artworkUri)
                        .build())
                .build();
                
        player.setMediaItem(mediaItem);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
