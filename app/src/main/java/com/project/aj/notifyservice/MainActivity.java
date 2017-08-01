package com.project.aj.notifyservice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.inputmethodservice.Keyboard;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    TextView texto;
    ListView lista;
    Button buscar;
    private String accessToken;
    AlertDialog alerta;
    View v = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent it = new Intent(getApplicationContext(), NotificacaoService.class);
        getApplicationContext().startService(it);
        Log.i("::CHECK", "onCreate - Main");

        texto = (TextView) findViewById(R.id.texto);
        lista = (ListView) findViewById(R.id.lista);
        buscar = (Button) findViewById(R.id.buscar);

        if (Conexao() != null && Conexao().isConnected()){
            //Verifica se a uma Conexao e se esta conectato
            new AutenticacaoTask().execute();
            Toast.makeText(getApplicationContext(), "Conectado", Toast.LENGTH_SHORT).show();
        }else {
            //Não esta conectado
        }

        buscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filtro = texto.getText().toString().trim();
                if (Conexao() != null && Conexao().isConnected() && accessToken != null){
                    //Verifica se a uma Conexao e se esta conectato
                    new TwitterTask().execute(filtro);
                }else {
                    //Não esta conectado
                    Toast.makeText(getApplicationContext(), "Verify connection!", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private class AutenticacaoTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Map<String, String> data = new HashMap<String, String>();
                data.put("grant_type", "client_credentials");
                String json = HttpRequest
                        .post("https://api.twitter.com/oauth2/token")
                        .authorization("Basic "+ gerarChave())
                        .form(data)
                        .body();
                JSONObject token = new JSONObject(json);
                //Toast.makeText(getApplicationContext(), "Token gerado", Toast.LENGTH_LONG).show();
                accessToken = token.getString("access_token");
            }catch (Exception e){
                return null;
            }
            return null;
        }
    }

    private class TwitterTask extends AsyncTask<String, Void, String[]> {


        @Override
        protected void onPreExecute(){
            alertLoad();
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null){
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, result);
                lista.setAdapter(adapter);
            }
            alerta.cancel();
        }

        @Override
        protected String[] doInBackground(String... params) {
            try {
                String filtro = params[0];

                if (TextUtils.isEmpty(filtro)){
                    return null;
                }

                String urlTwitter = "https://api.twitter.com/1.1/search/tweets.json?q=";
                String url = Uri.parse(urlTwitter + filtro).toString();
                String conteudo = HttpRequest.get(url)
                        .authorization("Bearer " + accessToken)
                        .body();
                JSONObject jsonObject = new JSONObject(conteudo);
                JSONArray resultados =
                        jsonObject.getJSONArray("statuses");

                String[] tweets = new String[resultados.length()];

                for (int i = 0; i < resultados.length(); i++){
                    JSONObject tweet = resultados.getJSONObject(i);
                    String texto = tweet.getString("text");
                    String usuario = tweet.getJSONObject("user").getString("screen_name");
                    tweets[i] = usuario + " - " + texto;
                }


                return tweets;

            }catch (Exception e){
                Log.e(getPackageName(), e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private NetworkInfo Conexao() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
//        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//        NetworkInfo mb = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return info;
    }

    private String gerarChave() throws UnsupportedEncodingException{

        String key = "M7INN9k0IYk32hE2mWTJ5anmu";
        String secret = "27N9YFFSXpdrEVg0i3lrgk9JPDdg5pcmqTs88uhOzoqHDshGTm";
        String token = key + ":" + secret;
        String base64 = Base64.encodeToString(token.getBytes(), Base64.NO_WRAP);

        return base64;
    }

    public void alertLoad() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.loading_layout, null));
        
        builder.setCancelable(false);

        alerta = builder.create();
        alerta.show();
    }
}
