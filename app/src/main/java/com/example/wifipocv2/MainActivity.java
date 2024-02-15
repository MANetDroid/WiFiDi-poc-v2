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
import android.net.wifi.p2p.WifiP2pGroup;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import javax.net.SocketFactory;


public class MainActivity extends AppCompatActivity {
    private final int fixedPort = 8888;
    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
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
    ListView listView;
    ListView socketListView;
    TextView read_msg_box, connectionStatus, socketStatus, selectedClientName;
    MultiServerThread selectedClient;
    EditText writeMsg;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    List<MultiServerThread> peerSocketList = new ArrayList<MultiServerThread>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    // Sockets
    Socket socket;
    ServerClass serverClass;
    ClientClass clientClass;

    Socket mySocket;
    VirtualRouterTest v;
    boolean isHost;
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            try {
                if (!wifiP2pDeviceList.equals(peers)) {
                    peers.clear();
                    peers.addAll(wifiP2pDeviceList.getDeviceList());
                    deviceArray = null;
                    deviceNameArray = null;
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
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            Log.w("A", "Device");
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                connectionStatus.setText("Host");
                Log.w("A", "IsServer");

                isHost = true;
                if (serverClass == null) {
                    serverClass = new ServerClass();
                    serverClass.start();
                }

            } else if (wifiP2pInfo.groupFormed) {
                Log.w("A", "IsClient" + groupOwnerAddress);
                connectionStatus.setText("Client");
                isHost = false;
                if (groupOwnerAddress != null) {
                    clientClass = new ClientClass(groupOwnerAddress);
                    clientClass.start();
                }

            }
        }
    };
    private Button btnConnect;
    private Button btnOnOff;
    private Button btnWifiDiOnOff;
    private Button btnDiscover;
    private Button btnSend;
    private IntentFilter intentFilter;
    private WifiManager wifiManager;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;

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
                Log.w("Info", "WIFIDI OFF");
//                wifiManager.setWifiEnabled(false);
                disconnect();
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                peers.clear();
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
                                Log.w("A", device.deviceAddress);
                                connectionStatus.setText("Connected: " + device.deviceAddress);
                            }
                        } catch (Exception e) {
                            Log.e("A", e.toString());
                        }
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Failed to Connect");
                    }
                });
            }
        });

        socketListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedClient = peerSocketList.get(position);
                selectedClientName.setText(selectedClient.getName());
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
                        if (msg != null && isHost && serverClass != null) {
                            serverClass.write(msg.getBytes());
                        } else if (msg != null && !isHost && clientClass != null) {
                            clientClass.write(msg.getBytes());
                        } else {
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
        btnOnOff = findViewById(R.id.onOff);
        btnWifiDiOnOff = findViewById(R.id.wifi_di);
        btnDiscover = findViewById(R.id.discover);
        btnSend = findViewById(R.id.sendButton);
        listView = findViewById(R.id.peerListView);
        socketListView = findViewById(R.id.peerSocketListView);
        read_msg_box = findViewById(R.id.readMsg);
        connectionStatus = findViewById(R.id.connectionStatus);
        socketStatus = findViewById(R.id.socketStatus);
        selectedClientName = findViewById(R.id.selectedDevice);
        writeMsg = findViewById(R.id.writeMsg);

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

    private void setSocketListView(MultiServerThread client) {
        peerSocketList.add(client);
        Log.v("S", peerSocketList.toString() + " " + client.getName());
        String[] socketArray = new String[peerSocketList.size()];
        int i = 0;
        for (MultiServerThread socket : peerSocketList
        ) {
            socketArray[i] = socket.getName();
            i++;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, socketArray);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                socketListView.setAdapter(adapter);
            }
        });
    }

    private void setSocketStatus(final TextView text, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }

    public void disconnect() {
        if (this.manager != null && this.channel != null) {
            this.manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                    if (wifiP2pGroup != null && manager != null && channel != null) {
                        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d("A", "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d("A", "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }
    }

    // Client class
    public class ClientClass extends Thread {
        String hostAdd;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ClientClass(InetAddress hostAddress) {
            this.hostAdd = hostAddress.getHostAddress();
//            socket = new Socket();
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // connect with fixed port-> receive port number from Server ie group owner
        @Override
        public void run() {
            try {
                Log.w("HostAddress", hostAdd);
                Socket kkSocket = new Socket(hostAdd, 8888);
                socket = kkSocket;
//                socket.connect(new InetSocketAddress(hostAdd, 8888), 500);
                outputStream = kkSocket.getOutputStream();
                inputStream = kkSocket.getInputStream();
//                inputStream = socket.getInputStream();
//                outputStream = socket.getOutputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//                Integer myInt = ConnectionUtils.getPort(getApplicationContext());
//                Log.w("Socket", "created client Here " + myInt);


//                byte myByte = myInt.byteValue();
//                outputStream.write(myByte);
                setSocketStatus(socketStatus, "Socket Connected: Client");
                int receive = inputStream.read();
                Log.w("P", String.valueOf(receive));
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
//                            Log.w("IStream", inputStream.toString());
                            bytes = inputStream.read(buffer);
                            if (bytes > 0) {
                                int finalBytes = bytes;
                                Log.w("V", new String(buffer, 0, finalBytes));
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        String tempMSG = new String(buffer, 0, finalBytes);
                                        read_msg_box.setText(tempMSG);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            setSocketStatus(socketStatus, "Socket Disconnected: Timeout");
                            break;
//                            throw new RuntimeException(e);
                        }
                    }
                }
            });

        }
    }

    public class MultiServerThread extends Thread {
        private Socket socket = null;
        private InputStream inputStream;
        private OutputStream outputStream;

        public MultiServerThread(Socket socket, int name) {
            super(String.valueOf(name));
//            super("MultiServerThread");
            Log.w("S", "Client ID: " + String.valueOf(socket.getLocalPort()) + " " + String.valueOf(socket.getPort()));
            this.socket = socket;
        }

        public OutputStream getOutputStream() {
            return outputStream;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void run() {

            try {
                Log.v("T", "MultithreadStarted");
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader in = new BufferedReader(inputStreamReader);

                String outputLine;


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
                                    int test = new Integer(finalBytes);
                                    Log.w("V", String.valueOf(test));
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            String tempMSG = new String(buffer, 0, finalBytes);
                                            read_msg_box.setText(socket.getPort() + ": " + tempMSG);
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                setSocketStatus(socketStatus, "Socket Disconnected: Timeout");
                                break;
//                                throw new RuntimeException(e);
                            }
                        }
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ServerClass extends Thread {
        ServerSocket serverSocket;
        SocketFactory socketFactory;
        InetSocketAddress serverSocket2;
        private InputStream inputStream;
        private OutputStream outputStream;

        public void write(byte[] bytes) {
            try {
//                outputStream.write(bytes);
                if (selectedClient.getOutputStream() != null) {
                    selectedClient.getOutputStream().write(bytes);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Get dynamic port from fixed port -> array of threads for connecting each client
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);

                while (true) {
                    Log.v("S", "Server Socket ready...");
                    Socket clientSocket = serverSocket.accept();
                    setSocketStatus(socketStatus, "Socket Active");
                    Log.v("S", "Client accepted..." + clientSocket.getInetAddress() + " " + clientSocket.isConnected() + " " + clientSocket.getPort());
                    MultiServerThread multiServerThread = new MultiServerThread(clientSocket, clientSocket.getPort());
                    multiServerThread.start();
                    setSocketListView(multiServerThread);
                    Log.v("S", "Thread Started");

//                    break;
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

//            ExecutorService executorService = Executors.newSingleThreadExecutor();
//            Handler handler = new Handler(Looper.getMainLooper());

//            executorService.execute(new Runnable() {
//                @Override
//                public void run() {
//                    byte[] buffer = new byte[1024];
//                    int bytes;
//
//                    while (socket != null) {
//                        try {
//                            Log.w("Buffer", Arrays.toString(buffer));
//                            Log.w("IStream", inputStream.toString());
//                            bytes = inputStream.read(buffer);
//
//                            if (bytes > 0) {
//                                int finalBytes = bytes;
//                                int test = new Integer(finalBytes);
//                                Log.w("V", String.valueOf(test));
//                                handler.post(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        String tempMSG = new String(buffer, 0, finalBytes);
//                                        read_msg_box.setText(tempMSG);
//                                    }
//                                });
//                            }
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                }
//            });

        }
    }
}
