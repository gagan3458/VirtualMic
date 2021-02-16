package com.example.audiorecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 99;
    private static String SERVER_IP = "192.168.0.106";
    private static final int PORT = 44456;

    private AudioRecord recorder = null;
    private int internalBufferSize = 0;
    private boolean isStreaming = false;

    private static final int SAMPLING_RATE = 48000;

    View rootLayout;
    EditText editTextIP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        editTextIP = findViewById(R.id.editText);

        internalBufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        Log.d("MMM", "Audio Recorder's Internal Buffer size : " + internalBufferSize * 6);

        final Button mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStreaming) {
                    //Starting the Stream.

                    if (ContextCompat.checkSelfPermission(
                            MainActivity.this, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED) {

                        String s = editTextIP.getText().toString();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(rootLayout.getWindowToken(), 0);
                        }

                        if (validate(s)) {

                            SERVER_IP = s;

                            mButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.circular_button_streaming));
                            mButton.setText(R.string.STOP_STREAMING);
                            startStreaming();
                            Snackbar.make(rootLayout, "Audio Streaming Started.", Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(rootLayout, "Enter Valid IPV4 Address.", Snackbar.LENGTH_SHORT).show();
                        }
                    } else {
                            requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO},REQUEST_CODE);
                    }
                } else {
                    //Terminating the Stream.
                    mButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.circular_button_not_streaming));
                    mButton.setText(R.string.START_STREAMING);
                    stopStreaming();
                    Snackbar.make(rootLayout, "Audio Streaming Stopped.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void stopStreaming() {

        isStreaming = false;
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    private void startStreaming() {

        isStreaming = true;
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    DatagramSocket datagramSocket = new DatagramSocket();
                    final InetAddress inetAddress = InetAddress.getByName(SERVER_IP);
                    DatagramPacket reusableDatagramPacket;

                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, internalBufferSize * 6);

                    int readBufferSize = internalBufferSize;
                    byte[] readBuffer = new byte[readBufferSize];
                    Log.d("MMM", "Read buffer created of Size : " + readBufferSize);

                    recorder.startRecording();

                    while (isStreaming) {
                        int readBytes = recorder.read(readBuffer, 0, readBuffer.length);

                        reusableDatagramPacket = new DatagramPacket(readBuffer, 0, readBytes, inetAddress, PORT);

                        datagramSocket.send(reusableDatagramPacket);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        th.start();
    }

    private boolean validate(String s) {

        String pattern = "^\\d{1,3}[.]\\d{1,3}[.]\\d{1,3}[.]\\d{1,3}$";
        return s.matches(pattern);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(rootLayout, "Mic Permission is required for Audio Streaming", Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}