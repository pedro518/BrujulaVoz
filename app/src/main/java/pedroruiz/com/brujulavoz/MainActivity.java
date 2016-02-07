/**
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pedroruiz.com.brujulavoz;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * <p>Clase que se encarga de asociar las acciones del usuario con la ejecución de las ordenes correspondientes</p>
 *
 * @author Pedro Antonio Ruiz Cuesta
 * @author Ignacio Martín Requena
 * Última modificación: 7/2/2016
 *
 */

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    //Variables para la interacción gráfica
    private ImageView coordenadasView;
    private ImageView flecha;
    private TextView texto;
    private Spinner mSpinner;
    private EditText editPorcentaje;

    //Variables para controlar la orientación deseada
    private String orienta; //Variable para almacenar la coordenada hacia donde queremos orientarnos
    private Float porcentaje = 0f; // Porcentaje de error que toleraremos para orientarnos
    private Float diferencia = 0f;//Esta variable se actualizará para controlar en qué orientación queremos ponernos
    private float currentDegree = 0f;

    //Variables para controlar la interacción por voz // guarda el ángulo (grado) actual del compass
    private final static int numberRecoResults = 1;
    private final static String languageModel = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
    private static int ASR_CODE = 123;

    // El sensor manager del dispositivo
    private SensorManager mSensorManager;

    // Los dos sensores necesarios
    private Sensor accelerometer;
    private Sensor magnetometer;

    float[] mGravity;
    float[] mGeomagnetic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        orienta = getString(R.string.norte);

        //Asocio las variables a los distintos elementos gráficos
        coordenadasView = (ImageView) findViewById(R.id.coordenadas);
        texto = (TextView) findViewById(R.id.text);
        flecha = (ImageView) findViewById(R.id.flecha);
        mSpinner = (Spinner) findViewById(R.id.spinner);
        editPorcentaje = (EditText) findViewById(R.id.porcentaje);

        //Le doy los valores que va a tener el spinner ("Norte", "Sur", "Este", "Oeste")
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.coordenadas_array, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        // Se inicializa los sensores del dispositivo android
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mGravity = null;
        mGeomagnetic = null;

        //Muestro los valores que tiene la aplicación por defecto
        texto.setText(getString(R.string.orientando) + " " + orienta+ "\n" + getString(R.string.porcentaje)+ " " + String.valueOf(porcentaje) + "%");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Se registra un listener para los sensores del accelerometer y el magnetometer
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Se detiene el listener para no malgastar la bateria
        mSensorManager.unregisterListener(this);
    }

    /**
     * Lanza la activity para el reconocimiento de voz.
     */
    public void Escuchar(View view) throws Exception {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Especifico el modelo de lenguaje
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);

        // Especificio cuantos resultados quiero recibir del reconocimiento de voz
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, numberRecoResults);

        // Comienzo a escuchar
        startActivityForResult(intent, ASR_CODE);

    }

    /**
     * Controla cuando hay algún resultado en el reconocimiento de voz.
     * @param requestCode Código de respuesta
     * @param resultCode Código del resultado
     * @param data Datos camtados por la activity
     */
    @SuppressLint("InlinedApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //Si he obtenido resultados de la escucha
        if (requestCode == ASR_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    ArrayList<String> nBestList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String[] res = nBestList.get(0).split(" ", 2);

                    //Si es una orden reconocida, actualizo los parámetros para la orientación
                    if (res.length > 1) {
                        if(isNumeric(res[1])) {
                            Float por = Float.parseFloat(res[1]);
                            if (orden_correcta(res[0], por)) {
                                orienta = res[0].toLowerCase();
                                porcentaje = Float.parseFloat(res[1]);

                                switch (orienta) {
                                    case "norte":
                                        diferencia = 0f;
                                        break;

                                    case "este":
                                        diferencia = 90f;
                                        break;

                                    case "oeste":
                                        diferencia = -90f;
                                        break;

                                    case "sur":
                                        diferencia = 180f;
                                        break;
                                }

                                texto.setText(getString(R.string.orientando) + " " + orienta + "\n" + getString(R.string.porcentaje) + " " + String.valueOf(porcentaje) + "%");
                            } else { // Sino se lo comunico al usuario
                                Toast.makeText(getApplicationContext(), getString(R.string.orden_incorrecta), Toast.LENGTH_LONG).show();
                            }
                        } else { // Sino se lo comunico al usuario
                            Toast.makeText(getApplicationContext(), getString(R.string.orden_incorrecta), Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(), getString(R.string.orden_incorrecta), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    /**
     * Controla los cambios del sensor.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        float degree;
        float azimut = 0;

        // Se comprueba que tipo de sensor está activo en cada momento
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mGravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeomagnetic = event.values.clone();
                break;
        }

        //Si hay resultados, calculo cuánto ha cambiado el ángulo desde la última actualización
        if ((mGravity != null) && (mGeomagnetic != null)) {

            float RotationMatrix[] = new float[16];
            boolean success = SensorManager.getRotationMatrix(RotationMatrix, null, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(RotationMatrix, orientation);
                azimut = orientation[0] * (180 / (float) Math.PI);
            }
        }
        degree = azimut;

        // se crea la animación de la rotación (se revierte el giro en grados, negativo)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        // el tiempo durante el cual la animación se llevará a cabo
        ra.setDuration(1000);
        // establecer la animación después del final del estado de reserva
        ra.setFillAfter(true);
        // Inicio de la animación
        coordenadasView.startAnimation(ra);

        currentDegree = -degree;


        // Actualizo el color de la flecha (Verde cuando estamos bien orientados, negra en el caso contrario)
        if(orienta.equals(getString(R.string.sur))){
            degree = Math.abs(degree);
        }

        if(degree < diferencia + 360*porcentaje/2/100 && degree > diferencia - 360*porcentaje/2/100){
            flecha.setImageDrawable(getDrawable(R.drawable.arrow_green));
        }else{
            flecha.setImageDrawable(getDrawable(R.drawable.arrow));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Comprueba si se han introducido los valores correctos.
     * @param orientacion Palabra que debería de ser la orientación
     * @param porcentaje Porcentaje de error que debería estar en el rango [0-100]
     */
    private boolean orden_correcta(String orientacion, Float porcentaje){
        boolean orden_correcta = false;

        String orien = orientacion.toLowerCase();

        if (orien.equals("norte"))
            orden_correcta = true;
        if(orien.equals("sur"))
            orden_correcta = true;
        if(orien.equals("este"))
            orden_correcta = true;
        if(orien.equals("oeste"))
            orden_correcta = true;

        if(porcentaje < 0 || porcentaje > 100)
            orden_correcta = false;

        return orden_correcta;
    }

    /**
     * Actualiza los valores de la orientación a partir de los introducidos por escrito.
     */
    public void Send(View view){
        String val = editPorcentaje.getText().toString();

        //Si se ha introducido bien el porcentaje, actualizo los valores
        if(!val.equals("")) {
            Float por = Float.parseFloat(String.valueOf(val));
            if (por >= 0 && por <= 100) {
                int selectedItemPosition = mSpinner.getSelectedItemPosition();
                String selected = String.valueOf(selectedItemPosition);

                switch (selectedItemPosition) {
                    case 0:
                        orienta = "norte";
                        break;

                    case 1:
                        orienta = "sur";
                        break;

                    case 2:
                        orienta = "este";
                        break;

                    case 3:
                        orienta = "oeste";
                        break;
                }

                switch (orienta){
                    case "norte":
                        diferencia = 0f;
                        break;

                    case "este":
                        diferencia = 90f;
                        break;

                    case "oeste":
                        diferencia = -90f;
                        break;

                    case "sur":
                        diferencia = 180f;
                        break;
                }

                porcentaje = Float.parseFloat(String.valueOf(editPorcentaje.getText()));

                texto.setText(getString(R.string.orientando) + " " + orienta+ "\n" + getString(R.string.porcentaje)+ " " + String.valueOf(porcentaje) + "%");
            }else{
                Toast.makeText(getApplicationContext(), getText(R.string.introducePorcentaje), Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(getApplicationContext(), getText(R.string.introducePorcentaje), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Comprueba si un string es numérico.
     * @param str String a comprobar
     */
    public static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }
}