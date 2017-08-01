package com.project.aj.notifyservice;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class TweetActivity extends AppCompatActivity {

    public static final String USUARIO = "usuario";
    public static final String TEXTO = "texto";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweet);

        String usuario = getIntent().getStringExtra(USUARIO);
        String texto = getIntent().getStringExtra(TEXTO);

        TextView usuarioView = (TextView) findViewById(R.id.usuario);
        TextView textoView = (TextView) findViewById(R.id.texto);

        usuarioView.setText(usuario);
        textoView.setText(texto);

    }
}
