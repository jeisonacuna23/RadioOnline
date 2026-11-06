package com.radio.generica;

import android.content.ComponentName;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends AppCompatActivity {

    private FloatingActionButton fabPlayStop;
    private Button btnExit;
    private TextView tvSongTitle;
    private ProgressBar progressBar;
    private VisualizerView visualizerView;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    
    private final String RADIO_URL = "https://radio.conectarservidor.com:8016/stream";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fabPlayStop = findViewById(R.id.fabPlayStop);
        btnExit = findViewById(R.id.btnExit);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        progressBar = findViewById(R.id.progressBar);
        visualizerView = findViewById(R.id.visualizer);

        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                configurarControles();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());

        // Lógica del botón Apagar y Salir
        btnExit.setOnClickListener(v -> {
            if (mediaController != null) {
                mediaController.stop();
                mediaController.release();
            }
            // Cierra la app por completo
            finishAffinity();
            System.exit(0);
        });
    }

    private void configurarControles() {
        fabPlayStop.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.prepare();
                    mediaController.play();
                }
            }
        });
        
        mediaController.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                runOnUiThread(() -> {
                    if (playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
                        progressBar.setVisibility(View.VISIBLE);
                        fabPlayStop.setImageResource(R.drawable.ic_pause);
                        tvSongTitle.setText("Conectando...");
                    } else if (playbackState == androidx.media3.common.Player.STATE_READY) {
                        progressBar.setVisibility(View.GONE);
                    } else if (playbackState == androidx.media3.common.Player.STATE_IDLE) {
                        progressBar.setVisibility(View.GONE);
                        fabPlayStop.setImageResource(R.drawable.ic_play);
                        tvSongTitle.setText("Radio Detenida");
                        visualizerView.stopAnimation();
                    }
                });
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                runOnUiThread(() -> {
                    if (isPlaying) {
                        fabPlayStop.setImageResource(R.drawable.ic_pause);
                        fabPlayStop.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
                        visualizerView.startAnimation();
                    } else {
                        fabPlayStop.setImageResource(R.drawable.ic_play);
                        fabPlayStop.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#331A33")));
                        visualizerView.stopAnimation();
                    }
                });
            }

            @Override
            public void onMediaMetadataChanged(androidx.media3.common.MediaMetadata mediaMetadata) {
                runOnUiThread(() -> {
                    if (mediaMetadata.title != null) {
                        tvSongTitle.setText(mediaMetadata.title);
                    }
                });
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaController != null) {
            mediaController.release();
        }
    }
}
