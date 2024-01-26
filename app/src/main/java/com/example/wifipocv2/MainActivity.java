package com.example.wifipocv2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private Button btnConnect;
    private Button btnOnOff;
    private Button btnWifiDiOnOff;
    private Button btnDiscover;
    private Button btnSend;

    ListView listView;
    TextView read_msg_box, connectionStatus;
    EditText writeMsg;


    private IntentFilter intentFilter;
    private WifiManager wifiManager;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;


    // Sockets
    Socket socket;

    ServerClass serverClass;
    ClientClass clientClass;

    boolean isHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();

    }

    private void exqListener() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivityForResult(intent, 1);
            }
        });
        btnWifiDiOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.w("Info","WIFIDI OFF");
//                wifiManager.setWifiEnabled(false);
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery Not Started " + i);
                    }
                });
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice device = deviceArray[position];

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                Log.w("device Config", String.valueOf(config));
                manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        try {
                            if (device != null) {
                                Log.w("A", device.deviceAddress.toString());
                                connectionStatus.setText("Connected: " + device.deviceAddress);
                            }
                        } catch (Exception e) {

                        }


                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Failed to Connect");
                    }
                });
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is a blocking method there concurrency is used
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                String msg = writeMsg.getText().toString();
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (msg != null && isHost == true && serverClass!=null) {
                            serverClass.write(msg.getBytes());
                        } else if (msg != null && !isHost && clientClass!=null) {
                            clientClass.write(msg.getBytes());
                        }else{
                            Log.w("Error", String.valueOf(serverClass));
                            Log.w("Error", String.valueOf(clientClass));
                        }
                    }
                });

                writeMsg.setText("");
            }
        });
    }

    private void initialWork() {
        Log.w("Initialize", "Initialize App");
        // Init UI
        btnOnOff = (Button) findViewById(R.id.onOff);
        btnWifiDiOnOff = (Button) findViewById(R.id.wifi_di);
        btnDiscover = (Button) findViewById(R.id.discover);
        btnSend = (Button) findViewById(R.id.sendButton);
        listView = (ListView) findViewById(R.id.peerListView);
        read_msg_box = (TextView) findViewById(R.id.readMsg);
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        writeMsg = (EditText) findViewById(R.id.writeMsg);

        // Init WiFi manager
//        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), android.Manifest.permission.NEARBY_WIFI_DEVICES) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
//            performAction(...);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.NEARBY_WIFI_DEVICES)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected, and what
            // features are disabled if it's declined. In this UI, include a
            // "cancel" or "no thanks" button that lets the user continue
            // using your app without granting the permission.
//            showInContextUI(...);
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(
                    android.Manifest.permission.NEARBY_WIFI_DEVICES);
        }

    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your
            // app.
        } else {
            // Explain to the user that the feature is unavailable because the
            // feature requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their
            // decision.
        }
    });

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            try {
                if (!wifiP2pDeviceList.equals(peers)) {
                    peers.clear();
                    peers.addAll(wifiP2pDeviceList.getDeviceList());

                    deviceNameArray = new String[wifiP2pDeviceList.getDeviceList().size()];
                    deviceArray = new WifiP2pDevice[wifiP2pDeviceList.getDeviceList().size()];

                    int index = 0;

                    for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                        deviceNameArray[index] = device.deviceName;
                        Log.w("deviceName", device.deviceName);
                        Log.w("device", String.valueOf(device));
                        deviceArray[index] = device;
                        index++;
                    }

                    if (peers.size() == 0) {
                        connectionStatus.setText("No Device Found");
                        return;
                    }


                    try {
                        Log.w("deviceArray", Arrays.toString(deviceArray));
                        Log.w("DeviceNameArray", Arrays.toString(deviceNameArray));
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);

                        listView.setAdapter(adapter);
                    } catch (Exception e) {
                        Log.w("Error", e.toString());
                    }


                }
            } catch (Exception e) {
                System.out.println(e);
            }

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            Log.w("A", "Device");
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                connectionStatus.setText("Host");
                Log.w("A", "IsServer");

                isHost = true;
//                serverClass = new ServerClass();
//                serverClass.start();
            } else if (wifiP2pInfo.groupFormed) {
                Log.w("A", "IsClient"+groupOwnerAddress);
                InetSocketAddress test = new InetSocketAddress(groupOwnerAddress, (int) Math.random());
                Log.w("A", test.toString());
                connectionStatus.setText("Client");
                isHost = false;
//                clientClass = new ClientClass(groupOwnerAddress);
//                clientClass.start();

            }
        }
    };


    // Client class
    public class ClientClass extends Thread {
        String hostAdd;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ClientClass(InetAddress hostAddress) {
            this.hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            try {
                Log.w("HostAddress",hostAdd);
                socket.connect(new InetSocketAddress(hostAdd, 8988), 500);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];
                    int bytes;

                    while (socket != null) {
                        try {
                            Log.w("Buffer", Arrays.toString(buffer));
                            Log.w("IStream", inputStream.toString());
                            bytes = inputStream.read(buffer);
                            if (bytes > 0) {
                                int finalBytes = bytes;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        String tempMSG = new String(buffer, 0, finalBytes);
                                        read_msg_box.setText(tempMSG);
                                    }
                                });
                            }
                        } catch (Exception e) {
//                            throw new RuntimeException(e);
                        }
                    }
                }
            });

        }
    }

    public class ServerClass extends Thread {
        ServerSocket serverSocket;
        InetSocketAddress serverSocket2;
        private InputStream inputStream;
        private OutputStream outputStream;

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8988);
                serverSocket2 = new InetSocketAddress("192.2.2.2",1234);
                socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];
                    int bytes;

                    while (socket != null) {
                        try {
                            Log.w("Buffer", Arrays.toString(buffer));
                            Log.w("IStream", inputStream.toString());
                            bytes = inputStream.read(buffer);
                            if (bytes > 0) {
                                int finalBytes = bytes;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        String tempMSG = new String(buffer, 0, finalBytes);
                                        read_msg_box.setText(tempMSG);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });

        }
    }
}

