package com.ejemplo.aplicacionprueba;

import static java.lang.String.valueOf;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;
    private float[] gravity, geomagnetic;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private TextView directionText; // Agrega esta variable para el TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Inicializa el SensorManager y los sensores
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Registra los listeners de los sensores
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);

        // Inicializa el reproductor de sonidos (archivo en res/raw)
        mediaPlayer = MediaPlayer.create(this, R.raw.sonar);  // coloca tu sonido en res/raw

        // Inicializa la vibración
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Obtener referencia al TextView para mostrar la dirección
        directionText = findViewById(R.id.directionText);

        // Ajusta las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }
        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]); // orientación en grados

                // Detectar los puntos cardinales (aproximadamente)
                performAction(valueOf(azimuth));
                if (azimuth >= -10 && azimuth <= 10) {
                    // Norte
                    performAction(valueOf(azimuth));
                } else if (azimuth >= 80 && azimuth <= 100) {
                    // Este
                    performAction(valueOf(azimuth));
                } else if (azimuth >= 170 && azimuth <= 190) {
                    // Sur
                    performAction(valueOf(azimuth));
                } else if (azimuth >= -100 && azimuth <= -80) {
                    // Oeste
                    performAction(valueOf(azimuth));
                }
            }
        }
    }

    private void performAction(String direction) {
        // Mostrar la dirección en pantalla
        directionText.setText(direction);

        // Vibrar y sonar al apuntar a un punto cardinal
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        if (vibrator != null) {
            vibrator.vibrate(500); // Vibrar por 500ms
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No es necesario implementar
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libera el reproductor de medios y desregistra los sensores
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        sensorManager.unregisterListener(this);
    }
}
