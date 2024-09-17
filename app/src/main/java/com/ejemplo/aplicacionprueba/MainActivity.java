package com.ejemplo.aplicacionprueba;

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

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor magnetometer;
    private TextView directionText;
    private ImageView compassImage;
    private float currentDegree = 0f;  // Para rotar la brújula
    private float lastAzimuth = -999f;  // Último valor del azimuth

    private MediaPlayer mediaPlayerNorte, mediaPlayerSur, mediaPlayerEste, mediaPlayerOeste;
    private Vibrator vibrator;
    private long lastSoundTime = 0;
    private static final long SOUND_DELAY = 2000;  // 2 segundos entre sonidos
    private static final float MIN_DIFF = 1.5f;  // Diferencia mínima para actualizar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa el SensorManager y el sensor magnético
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Registra el listener del sensor magnético
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);

        // Obtener referencia al TextView para mostrar la dirección
        directionText = findViewById(R.id.directionText);

        // Obtener referencia a la imagen de la brújula
        compassImage = findViewById(R.id.compassImage);

        // Inicializar MediaPlayers para los sonidos de cada punto cardinal
        mediaPlayerNorte = MediaPlayer.create(this, R.raw.sonido_norte);
        mediaPlayerSur = MediaPlayer.create(this, R.raw.sonido_sur);
        mediaPlayerEste = MediaPlayer.create(this, R.raw.sonido_este);
        mediaPlayerOeste = MediaPlayer.create(this, R.raw.sonido_oeste);

        // Inicializar vibrador
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Ajustar barra de estado (StatusBar) para que siga los colores de la app
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // Obtener el azimuth a partir del valor magnético
            float azimuth = (float) Math.toDegrees(Math.atan2(event.values[0], event.values[1]));  // Obtener el azimuth en grados

            // Invertir el azimuth para corregir la inversión de dirección
            azimuth = -azimuth;

            // Convertir de -180/+180 a 0-360 grados
            float degree = (azimuth + 360) % 360;

            // Evitar actualizaciones si la diferencia es menor al umbral mínimo
            if (Math.abs(degree - lastAzimuth) >= MIN_DIFF) {
                lastAzimuth = degree;
                updateCompassDisplay(Math.round(degree));  // Redondear el valor del azimuth
                checkPlaySound(Math.round(degree));  // Verificar si se debe reproducir sonido
            }
        }
    }

    private void updateCompassDisplay(int degree) {
        // Calcular la diferencia de rotación
        float deltaDegree = degree - currentDegree;
        if (deltaDegree > 180) {
            deltaDegree -= 360;  // Ajuste para evitar la rotación completa
        } else if (deltaDegree < -180) {
            deltaDegree += 360;  // Ajuste para evitar la rotación completa
        }

        // Rotar la imagen de la brújula de forma suave
        RotateAnimation rotateAnimation = new RotateAnimation(
                -currentDegree, -currentDegree - deltaDegree,  // Manejamos el deltaDegree para una rotación suave
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(250);
        rotateAnimation.setFillAfter(true);
        compassImage.startAnimation(rotateAnimation);

        // Actualizar el valor del texto con el grado actual y la dirección cardinal
        directionText.setText(degree + "° " + getCardinalDirection(degree));

        // Actualizar el valor actual del grado
        currentDegree = degree;
    }


    private void checkPlaySound(int degree) {
        long currentTime = System.currentTimeMillis();

        // Reproducir sonido solo si ha pasado suficiente tiempo desde el último sonido
        if (currentTime - lastSoundTime > SOUND_DELAY) {
            if (degree >= 350 || degree <= 10) {
                playSound(mediaPlayerNorte);  // Norte
            } else if (degree >= 80 && degree <= 100) {  // Este
                playSound(mediaPlayerEste);  // Este
            } else if (degree >= 170 && degree <= 190) {
                playSound(mediaPlayerSur);  // Sur
            } else if (degree >= 260 && degree <= 280) {  // Oeste
                playSound(mediaPlayerOeste);  // Oeste
            }
            lastSoundTime = currentTime;  // Actualizar tiempo del último sonido
        }
    }

    private void playSound(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        if (vibrator != null) {
            vibrator.vibrate(500);  // Vibrar por 500ms
        }
    }

    private String getCardinalDirection(int degree) {
        if (degree >= 350 || degree <= 10) return "N";  // Norte
        if (degree >= 80 && degree <= 100) return "E";  // Este
        if (degree >= 170 && degree <= 190) return "S";  // Sur
        if (degree >= 260 && degree <= 280) return "O";  // Oeste
        if (degree > 10 && degree < 80) return "NE";  // Noreste
        if (degree > 100 && degree < 170) return "SE";  // Sureste
        if (degree > 190 && degree < 260) return "SO";  // Suroeste
        if (degree > 280 && degree < 350) return "NO";  // Noroeste
        return "";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se requiere implementar
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libera los reproductores de medios y desregistra los sensores
        if (mediaPlayerNorte != null) mediaPlayerNorte.release();
        if (mediaPlayerSur != null) mediaPlayerSur.release();
        if (mediaPlayerEste != null) mediaPlayerEste.release();
        if (mediaPlayerOeste != null) mediaPlayerOeste.release();
        sensorManager.unregisterListener(this);
    }
}
