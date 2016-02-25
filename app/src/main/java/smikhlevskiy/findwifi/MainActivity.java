/**
 *
application show list of enabled wifi connevtion with user mask
user can select coonection and do connect
 */

package smikhlevskiy.findwifi;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static String TAG = MainActivity.class.getSimpleName();
    private WifiManager mWifiManager;
    private ListView listView;
    private static String searchMask = "";
    private List<ScanResult> mScanResults;

    /**
     * BroadcastResiver  for getting Wifi list
     */
    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            Log.i(TAG, "On Recive");

            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

                mScanResults = mWifiManager.getScanResults();
                if (!searchMask.equals("")) //searchMask not empty
                    for (int i = mScanResults.size() - 1; i >= 0; i--) {

                        if (!mScanResults.get(i).SSID.toLowerCase().contains(searchMask.toLowerCase()))
                            mScanResults.remove(i);
                    }

                Log.i(TAG, "count=" + mScanResults.size());
                String sarray[] = new String[mScanResults.size()];
                for (int i = 0; i < mScanResults.size(); i++)
                    sarray[i] = mScanResults.get(i).SSID;
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, sarray);
                listView.setAdapter(arrayAdapter);


            }
        }
    };

    /**
     * start getting wifi networks list
     */
    private void startWifiScan() {

        searchMask = ((EditText) findViewById(R.id.searchText)).getText().toString();//get search mask
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiManager.setWifiEnabled(true);
        registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
    }


    /**
     * Connect to WifiNetwork
     *
     * @param scanResult  ConnectionNetwork
     * @param networkPass password
     */
    public void connectToWifi(ScanResult scanResult, String networkPass) {
        Log.i(TAG, "connectToWifi");

        WifiConfiguration wifiConfiguration = new WifiConfiguration();


        String securityMode = getScanResultSecurity(scanResult);

        if (securityMode.equalsIgnoreCase(getString(R.string.securOpen))) {

            wifiConfiguration.SSID = "\"" + scanResult.SSID + "\"";
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            int res = mWifiManager.addNetwork(wifiConfiguration);

            boolean b = mWifiManager.enableNetwork(res, true);

            mWifiManager.setWifiEnabled(true);

        } else if (securityMode.equalsIgnoreCase(getString(R.string.securWEP))) {

            wifiConfiguration.SSID = "\"" + scanResult.SSID + "\"";
            wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
            wifiConfiguration.wepTxKeyIndex = 0;
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            int res = mWifiManager.addNetwork(wifiConfiguration);


            boolean b = mWifiManager.enableNetwork(res, true);


            mWifiManager.setWifiEnabled(true);
        }

        wifiConfiguration.SSID = "\"" + scanResult.SSID + "\"";
        wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
        wifiConfiguration.hiddenSSID = true;
        wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

        int res = mWifiManager.addNetwork(wifiConfiguration);


        mWifiManager.enableNetwork(res, true);

        boolean changeHappen = mWifiManager.saveConfiguration();

        if (res != -1 && changeHappen) {
            Toast.makeText(MainActivity.this, R.string.connection_ok, Toast.LENGTH_LONG).show();


        } else {
            Toast.makeText(MainActivity.this, R.string.connection_bad, Toast.LENGTH_LONG).show();
        }

        mWifiManager.setWifiEnabled(true);


    }

    /**
     * calculate sequruty of wifi network
     */
    public String getScanResultSecurity(ScanResult scanResult) {
        Log.i(TAG, "getScanResultSecurity");

        final String cap = scanResult.capabilities;
        final String[] securityModes = {getString(R.string.securWEP), "PSK", "EAP"};

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return getString(R.string.securOpen);
    }


    /**
     * show diolog for connection
     *
     * @param mScanResult ConnectionNetwork
     */
    public void showConnectionDialog(final ScanResult mScanResult) {

        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_password, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        ((TextView) promptsView.findViewById(R.id.wifiname)).setText(getString(R.string.connection)+mScanResult.SSID);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.password);


        alertDialogBuilder
                .setCancelable(false)
                .setNegativeButton(R.string.button_connect,// connect
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                String password = (userInput.getText()).toString();

                                connectToWifi(mScanResult, password);

                            }
                        })
                .setPositiveButton(R.string.button_cancel,// cancel
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }

                        }

                );

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.wifi_list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "Pos=" + position);
                showConnectionDialog(mScanResults.get(position));
            }
        });

        //searchButton
        Button searchButton = (Button) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startWifiScan();

            }
        });
    }


    @Override
    protected void onResume() {
        startWifiScan();
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mWifiScanReceiver);
        super.onPause();
    }

}
