package com.radio.generica;

import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnPlayStop;
    private TextView tvStatus;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    // URL DE PRUEBA (Luego la cambiamos por la de MagoHost)
    private final String STREAM_URL = "https://ice1.somafm.com/groovesalad-128-mp3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPlayStop = findViewById(R.id.btnPlayStop);
        tvStatus = findViewById(R.id.tvStatus);

        btnPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    iniciarRadio();
                } else {
                    detenerRadio();
                }
            }
        });
    }

    private void iniciarRadio() {
        tvStatus.setText(R.string.status_online);
        tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Verde
        btnPlayStop.setText("CARGANDO...");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        try {
            mediaPlayer.setDataSource(STREAM_URL);
            mediaPlayer.prepareAsync(); // Preparar en segundo plano

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    isPlaying = true;
                    btnPlayStop.setText(R.string.btn_stop);
                    btnPlayStop.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark, getTheme()));
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    detenerRadio();
                    tvStatus.setText("ERROR DE CONEXIÓN");
                    return true;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            detenerRadio();
            tvStatus.setText("ERROR");
        }
    }

    private void detenerRadio() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        tvStatus.setText(R.string.status_offline);
        tvStatus.setTextColor(Color.parseColor("#FF5252")); // Rojo
        btnPlayStop.setText(R.string.btn_play);
        btnPlayStop.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_dark, getTheme()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isPlaying) {
            detenerRadio();
        }
    }
}
