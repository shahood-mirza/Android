package identity.identityapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean btConnected = false;

    private BluetoothAdapter btAdpt;
    private BluetoothSocket btSckt;
    private DataOutputStream os;

    private TextView statusTxt;

    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTxt = new TextView(this);
        statusTxt = (TextView)findViewById(R.id.statusText);

        btAdpt = BluetoothAdapter.getDefaultAdapter();

        btStateCheck();

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void btStateCheck() {
        // Check that device supports Bluetooth and ensure it is turned on

        // If it doesn't support Bluetooth a null value is returned
        if (btAdpt == null) {
            //errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else if (!btAdpt.isEnabled()) {
            //Prompt user to turn on Bluetooth if it is disabled
            Intent enableBtIntent = new Intent(btAdpt.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void btConnCheck() {
        btAdpt = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> remoteDevices = btAdpt.getBondedDevices();

        if(remoteDevices.size() != 0 && !btConnected)
            btConnect();
    }

    private void btConnect() {
        BluetoothDevice remoteDevice = null;
        //String remoteDeviceName = "";

        btAdpt = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> remoteDevices = btAdpt.getBondedDevices();

        for (BluetoothDevice device : remoteDevices) {
            if(device.getName().equals("HC-06")) {
                remoteDevice = device;
                //remoteDeviceName = device.getName();
            }
        }

        //Toast.makeText(getApplicationContext(), "Discovered: " + remoteDeviceName + " address " + remoteDevice.getAddress(), Toast.LENGTH_SHORT).show();

        try {
            BluetoothDevice device = btAdpt.getRemoteDevice(remoteDevice.getAddress());
            Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            btSckt =  (BluetoothSocket) m.invoke(device, 1);
            if(!btSckt.isConnected()) {
                btSckt.connect();
                //showToast("Identity has Been Paired");
                statusTxt.setText("C O N N E C T E D");
                btConnected = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("BLUETOOTH", e.getMessage());
            //showToast("Identity has NOT Been Paired");
            statusTxt.setText("N O T  C O N N E C T E D");
            btConnected = false;
        }

    }

    public void onPairClicked(View v) {
        startActivityForResult(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), 0);
    }

    public void onOpenLockClicked(View v) {
        //btConnect();
        try{
            os = new DataOutputStream(btSckt.getOutputStream());
            os.writeBytes("x"); //we send 'x' to unlock the door
            os.flush();
            showToast("Unlocking Door...");
            statusTxt.setText("C O N N E C T E D");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("BLUETOOTH", e.getMessage());
            showToast("Unable to Connect to Identity");
            statusTxt.setText("N O T  C O N N E C T E D");
        }
    }

    public void onResetClicked(View v) {
       new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Secure Database Reset")
            .setMessage("Erasing the database requires the door to be unlocked first!\n\nTHIS WILL CLEAR ALL PRINTS!")
               .setPositiveButton("Reset Database", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // continue with delete
                    try {
                        os = new DataOutputStream(btSckt.getOutputStream());
                        os.writeBytes("r"); //we send 'r' to reset the system
                        os.flush();
                        showToast("Attempting System Reset...");
                        statusTxt.setText("C O N N E C T E D");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("BLUETOOTH", e.getMessage());
                        showToast("Unable to Connect to Identity");
                        statusTxt.setText("N O T  C O N N E C T E D");
                    }
                }
            })
               .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       // do nothing
                   }
               })
               .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    public void showToast(String message) {
        //Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Toast toast = new Toast(getApplicationContext());
        ImageView view = new ImageView(getApplicationContext());
        view.setImageResource(R.drawable.app_icon_w);
        toast.setView(view);
        toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        btConnCheck(); //when the application resumes (startup or return from pair) we make sure its connected
    }

        @Override
    public void onDestroy() {
        try {
            btSckt.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();

        // implement the App Indexing API.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
                // Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://identity.identityapp/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // implement the App Indexing API.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Main Page", 
                Uri.parse("http://host/path"),
                // Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://identity.identityapp/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
