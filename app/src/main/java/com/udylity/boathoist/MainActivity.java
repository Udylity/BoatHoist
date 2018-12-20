package com.udylity.boathoist;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;


public class MainActivity extends AppCompatActivity implements ControlsFragment.OnFragmentInteractionListener {

    private static TextView display;
    private EditText etAddress;

    public static String dstAddress;
    public static final int PORT = 8888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = findViewById(R.id.textView);
        etAddress = findViewById(R.id.etAddress);

        dstAddress = etAddress.getText().toString();
        if (TextUtils.isEmpty(dstAddress)) {
            dstAddress = getResources().getString(R.string.default_address);
        }

        UpdateHoistConnection mHoistConnection = new UpdateHoistConnection();
        mHoistConnection.execute();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.settings:
                Intent setingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(setingsIntent);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public static class UpdateHoistConnection extends AsyncTask<Void, Void, String>{

        private Socket socket;

        UpdateHoistConnection(){
        }

        @Override
        protected String doInBackground(Void... voids) {

            String data = null;

            try {
                socket = new Socket(MainActivity.dstAddress, MainActivity.PORT);


                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );

                BufferedWriter bufferedWriter = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())
                );

                bufferedWriter.write("Get Data");
                bufferedWriter.flush();

                data = "";
                StringBuilder sb = new StringBuilder(data);

                char buffer[] = new char[100];
                int numChar = bufferedReader.read(buffer);
                sb.append(buffer, 0, numChar);

                data = sb.toString();

                socket.close();
            } catch (IOException e) {

            }

            return data;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            display.setText(data);
        }


    }

}
