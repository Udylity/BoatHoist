package com.udylity.boathoist;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

enum HoistMode {
    RAISE, LOWER, STOP
}

public class ControlsFragment extends Fragment implements View.OnClickListener {

    private OnFragmentInteractionListener mListener;
    private static final String ARG_PARAM_NAME = "param1";

    private String name;


    private Button btnRaise, btnLower, btnStop;

    public ControlsFragment() {
    }

    public static ControlsFragment newInstance(String modelName) {
        ControlsFragment fragment = new ControlsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_NAME, modelName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            name = getArguments().getString(ARG_PARAM_NAME);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controls, container, false);

        btnRaise = view.findViewById(R.id.btnRaise);
        btnLower = view.findViewById(R.id.btnLower);
        btnStop = view.findViewById(R.id.btnStop);


        btnRaise.setOnClickListener(this);
        btnLower.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        return view;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        ControlHoist controlHoistTask;

        switch (v.getId()) {
            case R.id.btnRaise:
                controlHoistTask = new ControlHoist(HoistMode.RAISE);
                break;
            case R.id.btnLower:
                controlHoistTask = new ControlHoist(HoistMode.LOWER);
                break;
            case R.id.btnStop:
                controlHoistTask = new ControlHoist(HoistMode.STOP);
                break;
            default:
                controlHoistTask = null;
                break;
        }

        controlHoistTask.execute();
    }

    public static class ControlHoist extends AsyncTask<Void, Integer, Void> {

        private String data;

        ControlHoist(HoistMode value) {
            switch (value) {
                case RAISE:
                    this.data = "Raising Hoist";
                    break;
                case LOWER:
                    this.data = "Lowering Hoist";
                    break;
                case STOP:
                    this.data = "Stopping Hoist";
                    break;
            }
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            try {

                Socket socket = new Socket(MainActivity.dstAddress, MainActivity.PORT);

                BufferedWriter bufferedWriter = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())
                );


                bufferedWriter.write(data);
                bufferedWriter.flush();

                socket.close();

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            MainActivity.UpdateHoistConnection hoistConnection = new MainActivity.UpdateHoistConnection();
            hoistConnection.execute();
        }

    }

}
