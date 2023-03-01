package com.akdev.adbmediaremote;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ImageButton image_button_prev = findViewById(R.id.image_button_prev);
        final ImageButton image_button_rew = findViewById(R.id.image_button_rew);
        final ImageButton image_button_pause = findViewById(R.id.image_button_pause);
        final ImageButton image_button_ff = findViewById(R.id.image_button_ff);
        final ImageButton image_button_next = findViewById(R.id.image_button_next);
        final ImageButton image_button_conn = findViewById(R.id.image_button_conn);

        final SeekBar seekbar = findViewById(R.id.seekbar);

        image_button_prev.setOnClickListener(v -> {
            Thread t = new Thread(new ConnectionThread(this,
                    "media dispatch previous",
                    getHost(), getPort(), null));
            t.start();
        });
        image_button_rew.setOnClickListener(v -> {
            Thread t = new Thread(new ConnectionThread(this,
                    "media dispatch rewind",
                    getHost(), getPort(), null));
            t.start();
        });
        image_button_pause.setOnClickListener(v -> {
            Thread t = new Thread(new ConnectionThread(this,
                    "media dispatch play-pause",
                    getHost(), getPort(), null));
            t.start();
        });
        image_button_ff.setOnClickListener(v -> {
            Thread t = new Thread(new ConnectionThread(this,
                    "media dispatch fast-forward",
                    getHost(), getPort(), null));
            t.start();
        });
        image_button_next.setOnClickListener(v -> {
            Thread t = new Thread(new ConnectionThread(this,
                    "media dispatch next",
                    getHost(), getPort(), null));
            t.start();

        });
        image_button_conn.setOnClickListener(v -> {
            Thread t = new Thread(new ConnectionThread(this,
                    "getprop ro.product.name",
                    getHost(), getPort(), this::connCb));
            t.start();

            Thread t2 = new Thread(new ConnectionThread(this,
                    "media volume --get | tail -n1",
                    getHost(), getPort(), this::volCb));
            t2.start();
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
                Thread t = new Thread(new ConnectionThread(MainActivity.this,
                        "media volume --set " + seekbar.getProgress(),
                        getHost(), getPort(), null));
                t.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {}

            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
            }
        });
    }

    String getHost()
    {
        final EditText edit_text_host = findViewById(R.id.edit_text_host);
        return edit_text_host.getText().toString();
    }
    int getPort()
    {
        final EditText edit_text_port = findViewById(R.id.edit_text_port);
        return Integer.parseInt(edit_text_port.getText().toString());
    }

    void connCb(String response)
    {
        Log.d("callback", "response:" + response);
        String text = "connected to\n" + response;

        runOnUiThread(() -> Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show());
    }

    void volCb(String response)
    {
        Log.d("callback", "response:" + response);

        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(response);
        List<Integer> groups = new ArrayList<>();
        while (matcher.find()) {
            groups.add(Integer.parseInt(matcher.group()));
        }

        if (groups.size() != 3)
        {
            Log.w("callback", "could not parse response");
            return;
        }
        int min = groups.get(1);
        int max = groups.get(2);
        int val = groups.get(0);

        final SeekBar seekbar = findViewById(R.id.seekbar);
        seekbar.setMin(min);
        seekbar.setMax(max);
        seekbar.setProgress(val);
    }
}
