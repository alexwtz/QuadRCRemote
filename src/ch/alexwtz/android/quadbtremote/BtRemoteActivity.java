package ch.alexwtz.android.quadbtremote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

public class BtRemoteActivity extends Activity {

	private static final int REQUEST_ENABLE_BT = 10;
	// Default UUID used to connect to the HC-05 bluetooth module.
	// Do not change this UUID
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// Message types sent from the Bluetooth Handler
	public static final int MESSAGE_SENT = 1;
	public static final int MESSAGE_RECEIVED = 2;
	public static final int POSX = 3;
	public static final int POSY = 4;
	public static final int POSZ = 5;
	public static final int CONNECTED = 6;
	public static final int LEFT_CONTROL = 7;
	public static final int RIGHT_CONTROL = 8;

	static Point leftControl, rightControl;
	public boolean controlRunning = false;
	private static String deviceBt;

	private BroadcastReceiver mReceiver = null;

	//Variable used to determine if the device is connected to the bt device
	private static boolean connected = false;
	private AlertDialog.Builder builder;

	private BluetoothAdapter mBluetoothAdapter;
	private ArrayList<String> btResult = new ArrayList<String>();
	private ArrayList<BluetoothDevice> btResultDevices = new ArrayList<BluetoothDevice>();
	private ArrayAdapter<String> adapter;
	private IntentFilter filter;

	private Button quit;
	public static TextView receivedMessage, pos_x, pos_y, pos_z,ctrL,ctrR;
	private static Button searchDevices, askPosition,activateBt,speed;

	private ConnectThread ct;
	private ConnectedThread ctd;
	
	private NumberPicker pk1,pk2,pk3,pk4;

	private static ImageView iv;
	
	private Thread getControl;

	private static final Handler mHandler = new Handler() {
		@Override
		public synchronized void handleMessage(Message msg) {

			switch (msg.what) {
			case MESSAGE_RECEIVED:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				receivedMessage.append(readMessage);
				break;
			case POSX:
				pos_x.setText("X " + msg.obj);
				iv.setRotation((Integer) msg.obj);
				break;
			case POSY:
				pos_y.setText("Y " + msg.obj);
				break;
			case POSZ:
				pos_z.setText("Z " + msg.obj);
				break;
			case CONNECTED:
				searchDevices.setText("Disconnect from :"+deviceBt.split("\n")[0]);
				connected = true;
				break;
			case LEFT_CONTROL:
				ctrL.setText("("+leftControl.x+" - "+leftControl.y+")");
				break;
			case RIGHT_CONTROL:
				ctrR.setText("("+rightControl.x+" - "+rightControl.y+")");
				break;
			}
		}
	};

	private RCView rcv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window ww = getWindow();
		ww.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		ww.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_bt_remote);

		Point size = new Point();
		getWindowManager().getDefaultDisplay().getSize(size);
		rcv = (RCView) findViewById(R.id.RCView);
		rcv.setScreenSize(size);

		//Control
		leftControl = new Point();
		rightControl = new Point();
		leftControl.x = 0;
		leftControl.y = 0;
		rightControl.x = 0;
		rightControl.y = 0;
		
		//User infterface
		iv = (ImageView) findViewById(R.id.imageView1);
		
		ctrL = (TextView) findViewById(R.id.textView4);
		ctrR = (TextView) findViewById(R.id.textView5);
		
		pos_x = (TextView) findViewById(R.id.textView1);
		pos_y = (TextView) findViewById(R.id.textView2);
		pos_z = (TextView) findViewById(R.id.textView3);

		pos_x.setText("a");
		pos_y.setText("a");
		pos_z.setText("a");

		speed = (Button)findViewById(R.id.button_speed);
		speed.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(builder==null)builder =  new AlertDialog.Builder(BtRemoteActivity.this);
				builder.setTitle(getString(R.string.speed));
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int which) {
				        
				    	return;
				    } }); 
				builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int which) {
				        return;
				    } }); 
				View view = getLayoutInflater().inflate(R.layout.popup, null);
				builder.setView (view);

				final AlertDialog dialog = builder.create ();
				pk1 = (NumberPicker) view.findViewById(R.id.myNumber1);
				pk1.setMinValue(0);
				pk1.setMaxValue(10);
				pk1.setValue(5);

				pk2 = (NumberPicker) view.findViewById(R.id.myNumber2);
				pk2.setMinValue(0);
				pk2.setMaxValue(10);
				pk2.setValue(5);
				
				pk3 = (NumberPicker) view.findViewById(R.id.myNumber3);
				pk3.setMinValue(0);
				pk3.setMaxValue(10);
				pk3.setValue(5);
				
				pk4 = (NumberPicker) view.findViewById(R.id.myNumber4);
				pk4.setMinValue(0);
				pk4.setMaxValue(10);
				pk4.setValue(5);
				
				dialog.getWindow().
				    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
				dialog.show(); 
				
			}
		});
		
		askPosition = (Button) findViewById(R.id.button_ask_position);
		askPosition.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				
				
				String cmd;
				if (askPosition.getText().equals(
						getString(R.string.button_stop_position))) {
					cmd = "C0";
					askPosition
							.setText(getString(R.string.button_get_position));
				} else {
					cmd = "C1";
					askPosition
							.setText(getString(R.string.button_stop_position));
				}
				ctd.write(cmd.getBytes());
				
			}
		});

		// Devices discovery
		// Create a BroadcastReceiver for ACTION_FOUND
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				// When discovery finds a device
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					// Get the BluetoothDevice object from the Intent
					BluetoothDevice device = intent
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					// Add the name and address to an array adapter to show in a
					// ListView
					addDevice(device);
					adapter.notifyDataSetChanged();
				}
			}
		};
		// Register the BroadcastReceiver
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter); // Don't forget to unregister
												// during onDestroy

		quit = (Button) findViewById(R.id.button_quit);
		quit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				quitApp();
			}
		});

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, btResult);

		builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.discover));
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// The 'which' argument contains the index position
				// of the selected item
				deviceBt = btResult.get(which);
				Toast.makeText(getApplicationContext(),
						"Connection to :" + deviceBt,
						Toast.LENGTH_SHORT).show();
				ct = new ConnectThread(btResultDevices.get(which));
				ct.start();
			}
		});
		builder.setCancelable(true);
		builder.create();

		searchDevices = (Button) findViewById(R.id.button_search_devices);
		searchDevices.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (connected) {
					ctd.cancel();
					connected = false;
					searchDevices
							.setText(getString(R.string.button_connect_devices));
				} else {
					if (mBluetoothAdapter.startDiscovery()) {
						builder.show();
						Toast.makeText(getApplicationContext(),
								getString(R.string.discovery_begin),
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
		searchDevices.setEnabled(false);

		// Test if bluetooth is available on the device
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth
		}

		// firstSearch for all devices already paired
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				// Add the name and address to an array adapter to show in a
				// ListView
				addDevice(device);
			}
		}

		activateBt = (Button)findViewById(R.id.button_bt);
		activateBt.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// Ask to start bluetooth if disabled
				
					Intent enableBtIntent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		});
		if (!mBluetoothAdapter.isEnabled()) {
			displayBtButtons(false);
		}else{
			displayBtButtons(true);
			searchDevices.setEnabled(true);
		}
		
		
		getControl = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					while (controlRunning) {
						Thread.sleep(100);
						Point pt = rcv.getLeftValues();
						String tmpCmd;
						// We send left update only if necessary
						if (connected) {
							if (leftControl.x != pt.x) {
								tmpCmd = "LX" + (pt.x==100?pt.x:("0"+pt.x))+"n";
								ctd.write(tmpCmd.getBytes());
							}
							if (leftControl.y != pt.y) {
								tmpCmd = "LY" + (pt.y==100?pt.y:("0"+pt.y))+"n";
								ctd.write(tmpCmd.getBytes());
							}
						}
						leftControl.x = pt.x;
						leftControl.y = pt.y;
						Message msg = mHandler.obtainMessage(LEFT_CONTROL, pt);
						mHandler.sendMessage(msg);
						pt = rcv.getRightValues();
						// We send right update only if necessary
						if (connected) {
							if (rightControl.x != pt.x) {
								tmpCmd = "RX" + pt.x;
								ctd.write(tmpCmd.getBytes());
							}
							if (rightControl.y != pt.y) {
								tmpCmd = "RY" + pt.y;
								ctd.write(tmpCmd.getBytes());
							}
						}
						rightControl.x = pt.x;
						rightControl.y = pt.y;
						msg = mHandler.obtainMessage(RIGHT_CONTROL,
								rcv.getRightValues());
						mHandler.sendMessage(msg);
					}
				} catch (Throwable t) {
				}
			}
		});
		
		
		// if(mBluetoothAdapter.startDiscovery()){
		// Toast.makeText(getApplicationContext(),
		// getString(R.string.discovery_begin), Toast.LENGTH_SHORT).show();
		// }
	}
	
	/**
	 * Method used to display the buttons relative to the bluetooth function
	 * @param btConnected
	 */
	public void displayBtButtons(boolean btConnected){
		if (!btConnected) {
			activateBt.setVisibility(View.VISIBLE);
			searchDevices.setVisibility(View.GONE);
			askPosition.setEnabled(false);
		}else{
			activateBt.setVisibility(View.GONE);
			searchDevices.setVisibility(View.VISIBLE);
			askPosition.setEnabled(true);
		}
	}

	/**
	 * Method called just before leaving the app to ask the user to turn on or off the bluetooth connectivity
	 */
	public void quitApp(){
		//Ask to switch off Bluetooth
	    AlertDialog.Builder builder = new AlertDialog.Builder(BtRemoteActivity.this);
        builder.setMessage(R.string.quitBt)
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   mBluetoothAdapter.disable();
                	   finish();
       				   System.exit(0);
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog
                	   finish();
       				System.exit(0);
                   }
               })
               .setCancelable(true);
        // Create the AlertDialog object and return it
        builder.create().show();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
	        quitApp();
	    }
	    return super.onKeyDown(keyCode, event);
	}
		
	/**
	 * Method used to avoid duplicate entry in the bt list
	 * @param device
	 */
	public void addDevice(BluetoothDevice device) {
		// First we check if the device already exists
		int index = -1;
		for (int i = 0; i < btResultDevices.size(); i++) {
			System.out.println();
			if (btResultDevices.get(i).getAddress().equals(device.getAddress())) {
				index = i;
				break;
			}
		}
		if (index < 0) {
			btResultDevices.add(device);
			btResult.add(device.getName() + "\n" + device.getAddress());
		} else {
			btResultDevices.remove(index);
			btResultDevices.add(index, device);
			btResult.remove(index);
			btResult.add(index, device.getName() + "\n" + device.getAddress());
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == RESULT_CANCELED){
				Toast.makeText(getApplicationContext(),
						getString(R.string.bluetooth_disabled),
						Toast.LENGTH_SHORT).show();
				displayBtButtons(false);
			}else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.bluetooth_enabled),
						Toast.LENGTH_SHORT).show();
				searchDevices.setEnabled(true);
				//Thread control running
				controlRunning = true;
				getControl.start();
				displayBtButtons(true);
			}
			break;
		}
	}

	@Override
	public void onStop() {
		controlRunning = false;
		if (mReceiver != null)
			unregisterReceiver(mReceiver);
		if (ctd != null && ctd.isAlive())
			ctd.cancel();
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
			}
			mmSocket = tmp;
		}

		@Override
		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException closeException) {
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			// manageConnectedSocket(mmSocket);
			// connected();
			Message mes = mHandler.obtainMessage(CONNECTED, null);
			mHandler.sendMessage(mes);
			ctd = new ConnectedThread(mmSocket);
			ctd.start();
		}

		/** Will cancel an in-progress connection, and close the socket */
		@SuppressWarnings("unused")
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[1024]; // buffer store for the stream
			Message mes;
			int index = 0;
			int pos;
			int c1 = 0, c2 = 0;
			boolean loop = true;

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					c2 = c1;
					c1 = mmInStream.read();
					switch (c2) {
					case ('C'):
						switch (c1) {
						case ('1'):
							c1 = mmInStream.read();
							pos = c1 << 8;
							c1 = mmInStream.read();
							pos += c1;
							if (pos > 128)
								pos -= 256;

							mes = mHandler.obtainMessage(POSX, pos);
							mHandler.sendMessage(mes);
							c1 = mmInStream.read();
							pos = c1 << 8;
							c1 = mmInStream.read();
							pos += c1;
							mes = mHandler.obtainMessage(POSY, pos);
							mHandler.sendMessage(mes);
							c1 = mmInStream.read();
							pos = c1 << 8;
							c1 = mmInStream.read();
							pos += c1;
							mes = mHandler.obtainMessage(POSZ, pos);
							mHandler.sendMessage(mes);
							break;
						case ('2'):
							loop = true;
							index = 0;
							while (loop) {
								c2 = c1;
								c1 = mmInStream.read();
								buffer[index++] = (byte) c1;
								if (c2 == '\r' && c1 == '\n') {
									mes = mHandler
											.obtainMessage(MESSAGE_RECEIVED,
													index, -1, buffer);
									mHandler.sendMessage(mes);
								}
							}
							break;
						}
						break;
					}

				} catch (IOException e) {
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				if(connected)mmOutStream.write(bytes);
			} catch (IOException e) {
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

}
