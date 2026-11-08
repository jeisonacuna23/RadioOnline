package com.radio.generica;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;
    private final String STREAM_URL = "https://radio.conectarservidor.com:8016/stream";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. Configurar la red para que falle rápido si no hay datos
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(5000)
                .setReadTimeoutMs(5000);

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpDataSourceFactory);

        // 2. Política de errores: Cero reintentos.
        LoadErrorHandlingPolicy policy = new DefaultLoadErrorHandlingPolicy() {
            @Override
            public long getRetryDelayMsFor(LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo) {
                return C.TIME_UNSET; // No reintentar nunca
            }
        };

        ExoPlayer player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory).setLoadErrorHandlingPolicy(policy))
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(), true)
                .setHandleAudioBecomingNoisy(true)
                .build();

        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                player.stop();
                player.clearMediaItems();
            }
        });

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
