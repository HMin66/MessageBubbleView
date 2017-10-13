package com.huangmin66.messagebubbleview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements BubbleMessageTouchListener.BubbleDisappearListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MessageBubbleView.attach(findViewById(R.id.text), this);
    }

    @Override
    public void dismiss(View view) {

    }
}
