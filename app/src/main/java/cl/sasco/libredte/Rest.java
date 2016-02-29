package cl.sasco.libredte;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Clase para consumir servicios web basados en REST
 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]sasco.cl)
 * @version 2016-02-21
 */
public class Rest {

    private String url; ///< Dirección web base del servicio web (example.com)
    private String auth; ///< HTTP Basic Auth (usuario y contraseña en base64)
    private int status; ///< Código de estado entregado por el servicio web
    private String result; ///< Datos del resultado del consumo del servicio web

    /**
     * Constructor de la clase para el servicio web
     * @param url Dirección URL del servicio web
     * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]sasco.cl)
     * @version 2016-02-20
     */
    public Rest(String url) {
        this.url = url;
    }

    /**
     * Asigna los datos de autenticación al servicio web con HTTP Basic Auth
     * @param user Usuario del servicio web
     * @param pass Contraseña del servicio web
     * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]sasco.cl)
     * @version 2016-02-21
     */
    public void setAuth(String user, String pass) {
        this.auth = Base64.encodeToString((user + ":" + pass).getBytes(), Base64.DEFAULT);
    }

    /**
     * Asigna un token para autenticación (enviará una X como contraseña)
     * @param token Token de autorización para el servicio web
     * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]sasco.cl)
     * @version 2016-02-21
     */
    public void setAuth(String token) {
        this.setAuth(token, "X");
    }

    /**
     * Entrega el estado de la respuesta del servicio web
     * @return Código de respuesta HTTP (ok es 200)
     * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]sasco.cl)
     * @version 2016-02-20
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * Entrega el string con el resultado de la consulta del servicio web
     * @return String con el resultado de la consulta al servicio web
     * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]sasco.cl)
     * @version 2016-02-20
     */
    public String getResult() {
        return this.result;
    }

    /**
     * Entrega el objeto JSON con el resultado de la consulta del servicio web
     * @return Objeto JSON con el resultado de la consulta al servicio web
     * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]sasco.cl)
     * @version 2016-02-22
     */
    public JSONObject getJSON() {
        if (this.result!=null) {
            try {
                return new JSONObject(this.result);
            } catch (JSONException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Consumir recurso en el servicio web
     * @param resource Recurso que se desea consumir en el servicio web
     * @param data String JSON con los datos que se enviarán al servicio web
     * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]sasco.cl)
     * @version 2016-02-20
     */
    public void consume(String resource, String data) {
        this.result = "";
        URL url = null;
        HttpURLConnection conn = null;
        try {
            // crear conexión
            url = new URL(this.url+resource);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Basic " + this.auth);
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            // enviar datos
            conn.setRequestProperty("Content-Length", "" + Integer.toString(data.getBytes().length));
            OutputStream os = conn.getOutputStream();
            os.write(data.getBytes());
            os.flush();
            os.close();
            // obtener respuesta
            this.status = conn.getResponseCode();
            InputStream is = conn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            while((line = rd.readLine()) != null) {
                this.result += line;
            }
            // cerrar conexión
            conn.disconnect();
        } catch (FileNotFoundException e) {
            // si el código de estado fue diferente a 200 se caerá en esta excepción y se añade el
            // resultado de salida del servicio web
            InputStream error = conn.getErrorStream();
            try {
                int code = error.read();
                while (code != -1) {
                    this.result += (char)code;
                    code = error.read();
                }
                error.close();
            } catch (IOException ignored) {
            }
        }
        catch (Exception e) {
            this.status = 500;
            this.result = "\"" + e + "\"";
        }
    }

}
