package com.akdev.adbmediaremote;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * Thread to send an ADB command to host:port.
 * If a callback is given, the response will be passed back
 */
public class ConnectionThread implements Runnable{
    MainActivity ref;
    String command;
    String host;
    int port;

    Consumer<String> callback;
    String response;

    ConnectionThread(MainActivity ref, String command, String host, int port, Consumer<String> callback) {
        this.ref = ref;
        this.command = command;
        this.host = host;
        this.port = port;
        this.callback = callback;
    }

    @Override
    public void run()
    {
        Log.d("ConnectionThread", "running");

        AdbCrypto crypto = getCrypto();

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(this.host, this.port), 1000);

            Log.d("ConnectionThread", "hi");

            AdbConnection connection = AdbConnection.create(socket, crypto);
            connection.connect();

            Log.d("ConnectionThread", host + ":" + port + " $ "+command);
            AdbStream stream = connection.open("shell:"+command);

            if (callback != null) {
                byte[] response_byte = stream.read();
                this.response = new String(response_byte, StandardCharsets.UTF_8);
                Log.d("ConnectionThread", "Response:" + response);
                callback.accept(response);
            }

            Thread.sleep(5000); // always wait before closing again
            connection.close();
            socket.close();

            Log.d("ConnectionThread", "exit");

        } catch (SocketTimeoutException e) {
            ref.runOnUiThread(() -> Toast.makeText(ref.getApplicationContext(), "Timeout" , Toast.LENGTH_SHORT).show());
            e.printStackTrace();
        } catch (IOException | InterruptedException e) {
            ref.runOnUiThread(() -> Toast.makeText(ref.getApplicationContext(), "Exception" , Toast.LENGTH_SHORT).show());
            e.printStackTrace();
        }
    }

    AdbCrypto getCrypto()
    {
        AdbCrypto crypto;
        KeyPair keypair;

        SharedPreferences prefs = getDefaultSharedPreferences(ref);
        String keypair_str = prefs.getString("keypair", "");
        if (keypair_str.equals("")) // no stored keys
        {
            Log.d("ConnectionThread", "generate KeyPair");
            try {
                KeyPairGenerator rsaKeyPg = KeyPairGenerator.getInstance("RSA");
                rsaKeyPg.initialize(AdbCrypto.KEY_LENGTH_BITS);
                keypair = rsaKeyPg.genKeyPair();

                // gson cannot serialize keypair, do it manually
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream oo = new ObjectOutputStream(bo);
                oo.writeObject(keypair);
                keypair_str = Base64.getEncoder().encodeToString(bo.toByteArray());
                oo.close();
                bo.close();

                SharedPreferences.Editor prefsEditor = prefs.edit();
                prefsEditor.putString("keypair", keypair_str);
                prefsEditor.apply();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else
        {
            Log.d("ConnectionThread", "load KeyPair");
            try {
                // manually deserialize KeyPair
                byte[] keypair_bytes = Base64.getDecoder().decode(keypair_str);
                ByteArrayInputStream bi = new ByteArrayInputStream(keypair_bytes);
                ObjectInputStream oi = new ObjectInputStream(bi);
                keypair = (KeyPair) oi.readObject();
                oi.close();
                bi.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // create crypto object with known keypair
        crypto = AdbCrypto.loadAdbKeyPair(data -> Base64.getEncoder().encodeToString(data), keypair);

        return crypto;
    }
}
