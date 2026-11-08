package com.radio.generica;

import android.Manifest;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.messaging.FirebaseMessaging;

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
    
    private Handler watchdogHandler = new Handler(Looper.getMainLooper());
    private long lastKnownPosition = 0;
    private Runnable watchdogRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fabPlayStop = findViewById(R.id.fabPlayStop);
        btnExit = findViewById(R.id.btnExit);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        progressBar = findViewById(R.id.progressBar);
        visualizerView = findViewById(R.id.visualizer);

        // 1. Pedir permisos de notificaciones (Android 13+)
        pedirPermisoNotificaciones();

        // 2. Suscribirse al canal de Firebase
        FirebaseMessaging.getInstance().subscribeToTopic("radio_en_vivo")
                .addOnCompleteListener(task -> {
                    // Se ejecuta cuando la suscripción termina (opcional para debug)
                });

        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                configurarControles();
                iniciarWatchdog();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());

        btnExit.setOnClickListener(v -> {
            if (mediaController != null) {
                mediaController.stop();
                mediaController.release();
            }
            finishAffinity();
            System.exit(0);
        });
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void iniciarWatchdog() {
        watchdogRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaController != null && mediaController.isPlaying()) {
                    long currentPosition = mediaController.getCurrentPosition();
                    
                    if (currentPosition == lastKnownPosition) {
                        mediaController.stop();
                        runOnUiThread(() -> {
                            tvSongTitle.setText("Señal perdida");
                            visualizerView.stopAnimation();
                        });
                    }
                    lastKnownPosition = currentPosition;
                } else {
                    lastKnownPosition = 0;
                }
                watchdogHandler.postDelayed(this, 5000);
            }
        };
        watchdogHandler.post(watchdogRunnable);
    }

    private void configurarControles() {
        fabPlayStop.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.prepare();
                    mediaController.play();
                    lastKnownPosition = 0;
                }
            }
        });
        
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                runOnUiThread(() -> {
                    if (playbackState == Player.STATE_BUFFERING) {
                        progressBar.setVisibility(View.VISIBLE);
                        fabPlayStop.setImageResource(R.drawable.ic_pause);
                        tvSongTitle.setText("Conectando...");
                    } else if (playbackState == Player.STATE_READY) {
                        progressBar.setVisibility(View.GONE);
                    } else if (playbackState == Player.STATE_IDLE) {
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
            public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
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
        watchdogHandler.removeCallbacks(watchdogRunnable);
    }
}
