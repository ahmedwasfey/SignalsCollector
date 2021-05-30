package com.example.signalscollector;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    static final int STORAGE = 1;
    static final String FILE_NAME = "RSSIs.txt";
    private static final int CELL = 1;
    private ArrayList<Integer> wifi = new ArrayList<>();
//    private ArrayList<Integer> bluetooth = new ArrayList<>();
    Set<BluetoothDevice > bluetooth ;
    private ArrayList<Integer> cellular = new ArrayList<>();
    private int numberOfSamples = 0;
    private boolean collecting = false;
    TelephonyManager mTelMgr ;
    TextView samples;
    Button buttonStart, buttonClear, buttonSave, buttonStop;
    int delay = 500; //milliseconds
// WiFi variables

    WifiManager wifiManager;
    WifiInfo wifiInfo;
    Runnable allRunnable ;
    Handler handler = new Handler();
    BluetoothAdapter myAdapter = BluetoothAdapter.getDefaultAdapter();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        create();
        checkPermissions();

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void create() {
        mTelMgr =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        // Create textviews
        samples =  findViewById(R.id.textViewSamples);

        // Create buttons
        buttonStart = findViewById(R.id.buttonStart);
        buttonClear =  findViewById(R.id.buttonClear);
        buttonSave =  findViewById(R.id.buttonSave);
        buttonStop = findViewById(R.id.buttonStop);


        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        allRunnable = () -> {
            collectWifi();
            collectBluetooth();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                collectCell();
            }
            numberOfSamples++;
            samples.setText("Number of samples: " + numberOfSamples);
            handler.postDelayed(allRunnable, delay);
        };
        if (myAdapter == null){
            Toast.makeText(this  , "No Bluetooth adapter available !" , Toast.LENGTH_LONG).show();
        }
        else {
            if (!myAdapter.isEnabled()){
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent , 1);
            }
        }

        };


    public void startClicked(View v) {
        if (!collecting) {
            collecting = true;
            handler.postDelayed(allRunnable, delay);
            Log.d("Runnable ", "Runnable Started");
            Toast.makeText(this, "Started Collecting ! \n please stop collecting before any other action !", Toast.LENGTH_SHORT).show();
            buttonClear.setEnabled(false);
            buttonSave.setEnabled(false);
            buttonStart.setEnabled(false);
        } else {
            Toast.makeText(this, "You are collecting ! \n please press stop first before any other action !", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopClicked(View v) {
        if (collecting) {
            collecting = false;
            handler.removeCallbacks(allRunnable);
            Log.d("Runnable", "Runnable Stopped !");
            Toast.makeText(this, " Now you can save or delete or continue collecting ", Toast.LENGTH_SHORT).show();
            buttonStart.setEnabled(true);
            buttonSave.setEnabled(true);
            buttonClear.setEnabled(true);

        } else {
            Toast.makeText(this, "Started collecting first !!", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveClicked(View v) {
        String result = "wifi RSSIs: " + wifi.toString() + "\ncellular RSSIs :" + cellular.toString() +"\nBluetooth Devices:\n";
        for (BluetoothDevice dev : bluetooth){
            result += dev.getName();
            result+="\n";
            result += dev.getAddress();
            result+="\n\n";

        }
        result+="\n=========\n";

        save(result);
        Toast.makeText(this, "Saved Successfully !", Toast.LENGTH_LONG).show();
        clearAll();
    }

    public void deleteClicked(View v) {
        Toast.makeText(this, "DELETED !!", Toast.LENGTH_LONG).show();
        clearAll();
    }


    private void clearAll() {
        numberOfSamples = 0;
        wifi.clear();
        cellular.clear();
        //bluetooth.clear();
        samples.setText("Number of samples: " + numberOfSamples);

    }

    private void collectWifi() {
        wifiInfo = wifiManager.getConnectionInfo();
        //       values.setText(String.valueOf(wifiInfo.getRssi()));
        wifi.add(wifiInfo.getRssi());
    }

    private void collectBluetooth() {
        bluetooth= myAdapter.getBondedDevices();
    }







    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void collectCell() {
        int s = getCellSignalStrength();
        cellular.add(s);
    }


    @SuppressLint("Range")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int getCellSignalStrength() {

        int strength = Integer.MIN_VALUE;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
        }
        List<CellInfo> cellInfos = mTelMgr.getAllCellInfo(); //This will give info of all sims present inside your mobile
        if (cellInfos != null) {
            for (int i = 0; i < cellInfos.size(); i++) {
                if (cellInfos.get(i).isRegistered()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        if (cellInfos.get(i) instanceof CellInfoWcdma) {
                            CellInfoWcdma cellInfowcdma = (CellInfoWcdma)
                                    mTelMgr.getAllCellInfo().get(0);
                            CellSignalStrengthWcdma cellSignalStrengthwcdma = cellInfowcdma.getCellSignalStrength();
                            strength = cellSignalStrengthwcdma.getLevel();
                        } else if (cellInfos.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfolte = (CellInfoLte) mTelMgr.getAllCellInfo().get(0);
                            CellSignalStrengthLte cellSignalStrengthlte = cellInfolte.getCellSignalStrength();
                            strength = cellSignalStrengthlte.getLevel();
                        } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfogsm = (CellInfoGsm) mTelMgr.getAllCellInfo().get(0);
                            CellSignalStrengthGsm cellsignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                            strength = cellsignalStrengthGsm.getLevel();
                        }
                    }
                }
                return strength;
            }
        }
        //Toast.makeText(this, "YOU MUST ENABLE LOCATION SERVICES !" , Toast.LENGTH_LONG).show();

        return 0;
    }
    public void save(String result) {

        File path = Environment.getExternalStorageDirectory();
        File file = new File(path,FILE_NAME);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(result.getBytes());
            Toast.makeText(this, "Saved to " + path + "/" + FILE_NAME, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void   checkPermissions(){
        // check if you need wifi and bluetooth permissions .
        if(ContextCompat.checkSelfPermission(MainActivity.this
                , Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED ) {
            //Toast.makeText(MainActivity.this, "You have the permission to write to a file !", Toast.LENGTH_SHORT).show();
        }else {
            requestStoragePermission();
        }

        if(ContextCompat.checkSelfPermission(MainActivity.this
                , Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            Toast.makeText(MainActivity.this, "You have the permission to read fine location !", Toast.LENGTH_SHORT).show();
        }else {
            ActivityCompat.requestPermissions(this , new String []{Manifest.permission.ACCESS_FINE_LOCATION} , CELL );
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this
                , Manifest.permission.READ_PHONE_STATE ) == PackageManager.PERMISSION_GRANTED ) {
           // Toast.makeText(MainActivity.this, "You have the permission to read phone state !", Toast.LENGTH_SHORT).show();
        }else {
            ActivityCompat.requestPermissions(this , new String []{Manifest.permission.READ_PHONE_STATE} , CELL );
        }
        Toast.makeText(MainActivity.this, "DON'T FORGET TO ENABLE LOCATION SERVICES ON YOUR PHONE !", Toast.LENGTH_LONG).show();

    }
    private void  requestStoragePermission(){
        ActivityCompat.requestPermissions(this , new String []{Manifest.permission.WRITE_EXTERNAL_STORAGE} , STORAGE );
    }
}