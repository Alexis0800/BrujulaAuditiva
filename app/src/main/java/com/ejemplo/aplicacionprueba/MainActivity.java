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
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
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
    private MediaPlayer mediaPlayerNorte, mediaPlayerSur, mediaPlayerEste, mediaPlayerOeste;
    private Vibrator vibrator;
    private TextView directionText;
    private ImageView compassImage;

    private static final float ALPHA = 0.25f;  // Ajuste del suavizado
    private float lastAzimuth = -999f;  // Último valor de azimuth
    private static final float MIN_DIFF = 1.0f;  // Diferencia mínima en grados para permitir una nueva activación
    private long lastSoundTime = 0;  // Última vez que se reprodujo el sonido
    private static final long SOUND_DELAY = 2000;  // Tiempo mínimo entre sonidos (2 segundos)
    private float currentDegree = 0f;  // Para rotar la brújula

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

        // Inicializa los MediaPlayers para cada punto cardinal
        mediaPlayerNorte = MediaPlayer.create(this, R.raw.sonido_norte);
        mediaPlayerSur = MediaPlayer.create(this, R.raw.sonido_sur);
        mediaPlayerEste = MediaPlayer.create(this, R.raw.sonido_este);
        mediaPlayerOeste = MediaPlayer.create(this, R.raw.sonido_oeste);

        // Inicializa la vibración
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Obtener referencia al TextView para mostrar la dirección
        directionText = findViewById(R.id.directionText);

        // Obtener referencia a la imagen de la brújula
        compassImage = findViewById(R.id.compassImage);

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

                // Asegurar que los valores estén entre 0 y 359
                float degree = (azimuth + 360) % 360;

                // Evitar múltiples activaciones repetitivas solo si el cambio es mayor que el umbral
                if (Math.abs(degree - lastAzimuth) >= MIN_DIFF) {
                    // Corregir la rotación de 359° a 0° para que sea continua
                    float deltaDegree = degree - currentDegree;
                    if (deltaDegree > 180) {
                        deltaDegree -= 360;
                    } else if (deltaDegree < -180) {
                        deltaDegree += 360;
                    }

                    lastAzimuth = degree;  // Actualiza el valor del azimuth

                    // Rotar la brújula de forma suave
                    RotateAnimation rotateAnimation = new RotateAnimation(
                            currentDegree, degree,  // Cambia currentDegree por degree para una rotación correcta
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f);
                    rotateAnimation.setDuration(210);
                    rotateAnimation.setFillAfter(true);
                    compassImage.startAnimation(rotateAnimation);
                    currentDegree = degree;

                    // Mostrar el valor entero en pantalla junto con la dirección cardinal
                    directionText.setText(String.valueOf(Math.round(degree)) + "° " + getCardinalDirection(degree));

                    long currentTime = System.currentTimeMillis();

                    // Detectar los puntos cardinales y activar sonido/vibración solo si ha pasado suficiente tiempo
                    if (degree >= 350 || degree <= 10) {
                        if (currentTime - lastSoundTime > SOUND_DELAY) {
                            performAction("Norte", mediaPlayerNorte);
                            lastSoundTime = currentTime;
                        }
                    } else if (degree >= 80 && degree <= 100) {
                        if (currentTime - lastSoundTime > SOUND_DELAY) {
                            performAction("Este", mediaPlayerEste);
                            lastSoundTime = currentTime;
                        }
                    } else if (degree >= 170 && degree <= 190) {
                        if (currentTime - lastSoundTime > SOUND_DELAY) {
                            performAction("Sur", mediaPlayerSur);
                            lastSoundTime = currentTime;
                        }
                    } else if (degree >= 260 && degree <= 280) {
                        if (currentTime - lastSoundTime > SOUND_DELAY) {
                            performAction("Oeste", mediaPlayerOeste);
                            lastSoundTime = currentTime;
                        }
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

    private void performAction(String direction, MediaPlayer mediaPlayer) {
        // Mostrar la dirección en pantalla (ya se hace en onSensorChanged)
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
        // Libera los reproductores de medios y desregistra los sensores
        if (mediaPlayerNorte != null) {
            mediaPlayerNorte.release();
            mediaPlayerNorte = null;
        }
        if (mediaPlayerSur != null) {
            mediaPlayerSur.release();
            mediaPlayerSur = null;
        }
        if (mediaPlayerEste != null) {
            mediaPlayerEste.release();
            mediaPlayerEste = null;
        }
        if (mediaPlayerOeste != null) {
            mediaPlayerOeste.release();
            mediaPlayerOeste = null;
        }
        sensorManager.unregisterListener(this);
    }

    // Obtener la dirección cardinal
    private String getCardinalDirection(float degree) {
        if (degree >= 350 || degree <= 10) return "N";
        if (degree >= 80 && degree <= 100) return "E";
        if (degree >= 170 && degree <= 190) return "S";
        if (degree >= 260 && degree <= 280) return "O";
        if (degree > 10 && degree < 80) return "NE";
        if (degree > 100 && degree < 170) return "SE";
        if (degree > 190 && degree < 260) return "SO";
        if (degree > 280 && degree < 350) return "NO";
        return "";
    }
}
