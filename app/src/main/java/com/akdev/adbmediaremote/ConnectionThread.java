package com.akdev.adbmediaremote;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.util.Log;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Thread to send an ADB command to host:port
 */
public class ConnectionThread implements Runnable{
    MainActivity ref;
    String command;
    String host;
    int port;

    String response = "";

    ConnectionThread(MainActivity ref, String command, String host, int port) {
        this.ref = ref;
        this.command = command;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run()
    {
        Log.d("ConnectionThread", "running");

        AdbCrypto crypto = getCrypto();

        try {
            Socket socket = new Socket(this.host, this.port);

            AdbConnection connection = AdbConnection.create(socket, crypto);
            connection.connect();

            AdbStream stream = connection.open("shell:"+command);
            Thread.sleep(1000);
            byte[] response_byte = stream.read();
            this.response = new String(response_byte, StandardCharsets.UTF_8);

            Log.d("ConnectionThread", "Response:" + response);

            connection.close();
            socket.close();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    AdbCrypto getCrypto()
    {
        AdbCrypto crypto;
        KeyPair keypair;

        SharedPreferences mPrefs = getDefaultSharedPreferences(ref);
        String keypair_str = mPrefs.getString("keypair", "");
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

                SharedPreferences.Editor prefsEditor = mPrefs.edit();
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
