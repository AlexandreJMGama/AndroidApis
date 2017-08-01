package com.project.aj.notifyservice;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ale on 24/07/2017.
 */

public class NotificacaoService extends Service {

    private String accessToken;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class AutenticacaoTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Map<String, String> data = new HashMap<String, String>();
                data.put("grant_type", "client_credentials");
                String json = HttpRequest
                        .post("https://api.twitter.com/oauth2/token")
                        .authorization("Basic " + gerarChave())
                        .form(data)
                        .body();

                JSONObject token = new JSONObject(json);
                accessToken = token.getString("access_token");
            } catch (Exception e) {
                return null;
            }
            return null;
        }

        private String gerarChave() throws UnsupportedEncodingException {
            String key = "M7INN9k0IYk32hE2mWTJ5anmu";
            String secret = "27N9YFFSXpdrEVg0i3lrgk9JPDdg5pcmqTs88uhOzoqHDshGTm";
            String token = key + ":" + secret;
            String base64 = Base64.encodeToString(token.getBytes(), Base64.NO_WRAP);
            return base64;
        }

        @Override
        protected void onPostExecute(Void result) {
            ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1);
            long delayInicial = 0;
            long periodo = 10;
            TimeUnit unit = TimeUnit.MINUTES;
            pool.scheduleAtFixedRate(new Worker(), delayInicial, periodo, unit);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("::CHECK", "onCreate - Service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new AutenticacaoTask().execute();
        Log.i("::CHECK", "onStartCommand - Service");
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1);
        long delayInicial = 0;
        long periodo = 1;
        TimeUnit unit = TimeUnit.MINUTES;
        pool.scheduleAtFixedRate(new Worker(), delayInicial, periodo, unit);
        return START_STICKY;
    }

    class Worker implements Runnable {

        private String baseUrl = "https://api.twitter.com/1.1/search/tweets.json";
        private String refreshUrl = "?q=@android";

        @Override
        public void run() {
            if (!Conexao().isConnected()) {
                return;
            }
            try {
                String conteudo = HttpRequest.get(baseUrl + refreshUrl)
                        .authorization("Bearer " + accessToken)
                        .body();

                JSONObject jsonObject = new JSONObject(conteudo);
                refreshUrl = jsonObject.getJSONObject("search_metadata")
                        .getString("refresh_url");

                JSONArray resultados = jsonObject.getJSONArray("statuses");

                for (int i = 0; i < resultados.length(); i++) {
                    JSONObject tweet = resultados.getJSONObject(i);
                    String texto = tweet.getString("text");
                    String usuario = tweet.getJSONObject("user").getString("screen_name");
                    myNotify(usuario, texto, i);
                }
            } catch (Exception e) {
                Log.e(getPackageName(), e.getMessage(), e);
            }
        }
    }

    private void myNotify(String usuario, String texto, int id) {
        NotificationCompat.Builder mBuilder = (NotificationCompat.Builder)
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(usuario+" "+id)
                        .setContentText(texto)
                        .setAutoCancel(true);

        Intent resultIntent = new Intent(getApplicationContext(), TweetActivity.class);
        resultIntent.putExtra(TweetActivity.USUARIO, usuario.toString());
        resultIntent.putExtra(TweetActivity.TEXTO, texto.toString());

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        id,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(id, mBuilder.build());
    }

    private NetworkInfo Conexao() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
//        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//        NetworkInfo mb = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return info;
    }
}