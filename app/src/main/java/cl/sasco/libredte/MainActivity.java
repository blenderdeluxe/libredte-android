package cl.sasco.libredte;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {

    private Button buttonScan;
    private TextView textEstado;

    private static final int ACTIVITY_SCAN_AUTH = 1;
    private static final int ACTIVITY_SCAN_TIMBRE = 2;

    private View.OnClickListener buttonScanListener = new View.OnClickListener() {
        public void onClick(View v) {
            scanTimbre();
        }
    };
    private int codigo;

    private void scanAuth() {
        if (this.isAppInstalled("com.google.zxing.client.android")) {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, ACTIVITY_SCAN_AUTH);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Se requiere Barcode Scanner para continuar");
            builder.setPositiveButton("Instalar", dialogInstallBarcodeScanner);
            builder.setNegativeButton("Cancelar", dialogInstallBarcodeScanner);
            builder.show();
        }
    }

    private void scanTimbre()  {
        SharedPreferences p = this.getPreferences(MODE_PRIVATE);
        if (!p.contains("api_url") || !p.contains("api_token")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Primero escanea el código QR de tu perfil de usuario en LibreDTE");
            builder.setPositiveButton("Escanear", dialogScanAuth);
            builder.setNegativeButton("Cancelar", dialogScanAuth);
            builder.show();
        } else {
            if (this.isAppInstalled("com.google.zxing.client.android")) {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_FORMATS", "PDF_417");
                startActivityForResult(intent, ACTIVITY_SCAN_TIMBRE);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Se requiere Barcode Scanner para continuar");
                builder.setPositiveButton("Instalar", dialogInstallBarcodeScanner);
                builder.setNegativeButton("Cancelar", dialogInstallBarcodeScanner);
                builder.show();
            }
        }
    }

    DialogInterface.OnClickListener dialogScanAuth = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    scanAuth();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    private void setAuth(String auth) {
        String[] aux = auth.split(";");
        if (aux.length==2) {
            SharedPreferences p = this.getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = p.edit();
            editor.putString("api_url", aux[0]);
            editor.putString("api_token", aux[1]);
            if (editor.commit()) {
                Toast.makeText(this, "Datos guardados, ahora puedes usar LibreDTE", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No fue posible guardar tus datos", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Código QR incorrecto", Toast.LENGTH_LONG).show();
        }
    }

    private String getDocumento(int codigo) {
        switch (codigo) {
            case 33: return "Factura electrónica";
            case 34: return "Factura no afecta o exenta electrónica";
            case 39: return "Boleta electrónica";
            case 41: return "Boleta no afecta o exenta electrónica";
            case 43: return "Liquidación factura electrónica";
            case 46: return "Factura de compra electrónica";
            case 52: return "Guía de despacho electrónica";
            case 56: return "Nota de débito electrónica";
            case 61: return "Nota de crédito electrónica";
            case 110: return "Factura de exportación electrónica";
            case 111: return "Nota de débito de exportación electrónica";
            case 112: return "Nota de crédito de exportación electrónica";
            default: return "Documento " + codigo;
        }
    }

    private String dateFormat(String fecha) {
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-mm-dd");
        Date date = null;
        try {
            date = dt.parse(fecha);
        } catch (ParseException e) {
            return null;
        }
        SimpleDateFormat dt1 = new SimpleDateFormat("dd/mm/yy");
        return dt1.format(date);
    }

    private String getTimbreInfo(String timbre)
    {
        String info = "";
        DocumentBuilderFactory factory =
        DocumentBuilderFactory.newInstance();
        try {
            DecimalFormat n = new DecimalFormat("###,###.##");
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input =  new ByteArrayInputStream(timbre.getBytes());
            Document doc = builder.parse(input);
            Element root = doc.getDocumentElement();
            info += "Ambiente: " + (Integer.parseInt(root.getElementsByTagName("IDK").item(0).getTextContent()) == 100 ? "certificación" : "producción") + "\n\n";
            info += this.getDocumento(Integer.parseInt(root.getElementsByTagName("TD").item(0).getTextContent()));
            info += " N° " + root.getElementsByTagName("F").item(0).getTextContent();
            info += " del " + this.dateFormat(root.getElementsByTagName("FE").item(0).getTextContent());
            info += " emitida por " + root.getElementsByTagName("RS").item(0).getTextContent();
            info += " (" + root.getElementsByTagName("RE").item(0).getTextContent() + ")";
            info += " a " + root.getElementsByTagName("RSR").item(0).getTextContent();
            info += " (" + root.getElementsByTagName("RR").item(0).getTextContent() + ")";
            info += " por un monto total de $" + n.format(Integer.parseInt(root.getElementsByTagName("MNT").item(0).getTextContent())) + ".-";
        } catch (Exception e) {
            info = "" + e;
        }
        return info;
    }

    private void verificarTimbre(String timbre) {
        String msg = "\n\n";
        // agregar info del timbre
        msg += this.getTimbreInfo(timbre) + "\n\n";
        // consultar servicio web
        SharedPreferences p = this.getPreferences(MODE_PRIVATE);
        Rest rest = new Rest(p.getString("api_url", null));
        rest.setAuth(p.getString("api_token", null));
        rest.consume(
                "/api/dte/documentos/verificar_ted",
                "\"" + Base64.encodeToString(timbre.getBytes(), Base64.DEFAULT) + "\""
        );
        if (rest.getStatus()==200) {
            try {
                msg += rest.getJSON().getString("GLOSA_ERR") + "\n\n";
            } catch (JSONException e) {
                msg += e.getMessage() + "\n\n";
            }
        } else {
            msg += "No fue posible verificar el timbre: " + rest.getResult() + "\n\n";
        }
        // mostrar resultado
        textEstado.setText(msg);
        // copiar timbre al portapapeles
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("timbre", timbre);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Timbre copiado al portapapeles", Toast.LENGTH_LONG).show();
    }

    /**
     * Manejador de resultados de llamadas a aplicaciones
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            if (requestCode == ACTIVITY_SCAN_AUTH)
                this.setAuth(intent.getStringExtra("SCAN_RESULT"));
            else if (requestCode == ACTIVITY_SCAN_TIMBRE)
                this.verificarTimbre(intent.getStringExtra("SCAN_RESULT"));
        } else if (resultCode == RESULT_CANCELED) {
            // Handle cancel
        }
    }

    DialogInterface.OnClickListener dialogInstallBarcodeScanner = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    goToMarket("com.google.zxing.client.android");
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonScan = (Button) findViewById(R.id.buttonScan);
        textEstado = (TextView) findViewById(R.id.textEstado);
        buttonScan.setOnClickListener(buttonScanListener);
        textEstado.setMovementMethod(new ScrollingMovementMethod());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            scanAuth();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Método para abrir el market de Google con la aplicación indicada
     * @param appName
     */
    private void goToMarket(String appName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
        }
    }

    /**
     * Método que revisa si una aplicación se encuentra instalada
     * @param packageName Nombre del paquete que se está buscando
     * @return boolean, verdadero si existe
     */
    private boolean isAppInstalled(String packageName) {
        PackageManager pm = getPackageManager();
        boolean installed = false;
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

}
