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
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private TextView directionText;

    // Factor de suavizado para el filtro de paso bajo
    private static final float ALPHA = 0.25f;  // Ajuste del suavizado
    private float lastAzimuth = -999f;  // Último valor de azimuth, para evitar múltiples activaciones
    private static final float MIN_DIFF = 1.0f;  // Diferencia mínima en grados para permitir una nueva activación
    private long lastSoundTime = 0;  // Última vez que se reprodujo el sonido
    private static final long SOUND_DELAY = 2000;  // Tiempo mínimo entre sonidos (2 segundos)

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
        mediaPlayer = MediaPlayer.create(this, R.raw.sonar);

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
            gravity = applyLowPassFilter(event.values, gravity);  // Aplica el filtro de paso bajo
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = applyLowPassFilter(event.values, geomagnetic);  // Aplica el filtro de paso bajo
        }
        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]); // orientación en grados

                // Redondear azimuth a enteros
                int roundedAzimuth = Math.round(azimuth);

                // Evitar múltiples activaciones repetitivas solo si el cambio es mayor que el umbral
                if (Math.abs(roundedAzimuth - lastAzimuth) >= MIN_DIFF) {
                    lastAzimuth = roundedAzimuth;  // Actualiza el valor del azimuth

                    // Mostrar el valor entero en pantalla
                    directionText.setText(String.valueOf(roundedAzimuth));

                    long currentTime = System.currentTimeMillis();

                    // Detectar los puntos cardinales y activar sonido/vibración solo si ha pasado suficiente tiempo
                    if (roundedAzimuth >= -10 && roundedAzimuth <= 10) {
                        if (currentTime - lastSoundTime > SOUND_DELAY) {
                            performAction("Norte", true);
                            lastSoundTime = currentTime;
                        }
                    } else if (roundedAzimuth >= 80 && roundedAzimuth <= 100) {
                        if (currentTime - lastSoundTime > SOUND_DELAY) {
                            performAction("Este", true);
                            lastSoundTime = currentTime;
                        }
                    } else if (roundedAzimuth >= 170 && roundedAzimuth <= 190) {
                        if (currentTime - lastSoundTime > SOUND_DELAY) {
                            performAction("Sur", true);
                            lastSoundTime = currentTime;
                        }
                    } else if (roundedAzimuth >= -100 && roundedAzimuth <= -80) {
                        if (currentTime - lastSoundTime > SOUND_DELAY) {
                            performAction("Oeste", true);
                            lastSoundTime = currentTime;
                        }
                    } else {
                        performAction(valueOf(roundedAzimuth), false);  // Sin sonido
                    }
                }
            }
        }
    }

    // Filtro de paso bajo para suavizar los valores
    private float[] applyLowPassFilter(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);  // Ajuste del suavizado
        }
        return output;
    }

    private void performAction(String direction, Boolean sonido) {
        // Mostrar la dirección en pantalla (ya se hace en onSensorChanged)
        if (sonido) {
            // Vibrar y sonar al apuntar a un punto cardinal
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
            if (vibrator != null) {
                vibrator.vibrate(500); // Vibrar por 500ms
            }
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
