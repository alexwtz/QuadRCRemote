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
import android.graphics.Point;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BtRemoteActivity extends Activity {


	
	private static final int REQUEST_ENABLE_BT = 10;
	//Default UUID used to connect to the HC-05 bluetooth module.
	//Do not change this UUID
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_SENT = 1;
    public static final int MESSAGE_RECEIVED = 2;
    public static final int POSX = 3;
    public static final int POSY = 4;
    public static final int POSZ = 5;
    public static final int CONNECTED = 6;

    private View v;

	private BroadcastReceiver mReceiver = null;
	
	private static boolean connected = false;
	private AlertDialog.Builder builder;

	private BluetoothAdapter mBluetoothAdapter;
	private ArrayList<String> btResult = new ArrayList<String>();
	private ArrayList<BluetoothDevice> btResultDevices = new ArrayList<BluetoothDevice>();
	private ArrayAdapter<String> adapter;
	private IntentFilter filter;
	
	private Button quit,send;
	private ListView btResultList;
	public static TextView receivedMessage,pos_x,pos_y,pos_z;
	private static Button searchDevices,askPosition;
	
	
	private ConnectThread ct;
	private ConnectedThread ctd;
	
	private Animation moveLeftToRigth, moveRigthToLeft,alphaDisapear,alphaApear;
	private ArrayList<View> buttonToHide,mrl;
	private static ImageView iv;
	
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
            	pos_x.setText("X "+msg.obj);
            	iv.setRotation((Integer)msg.obj);
            	break;
            case POSY:
            	pos_y.setText("Y "+msg.obj);
            	break;
            case POSZ:
            	pos_z.setText("Z "+msg.obj);
            	break;
            case CONNECTED:
            	searchDevices.setText("Blabla");
            	connected = true;
            	break;
            }
        }
    };
    
	private boolean direction = false;
    
	private VelocityTracker mVelocityTracker;
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(index);

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                if(mVelocityTracker == null) {
                    // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                    mVelocityTracker = VelocityTracker.obtain();
                }
                else {
                    // Reset the velocity tracker back to its initial state.
                    mVelocityTracker.clear();
                }
                // Add a user's movement to the tracker.
                mVelocityTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_MOVE:
                
                Log.d("TEST", "X velocity: " + event.getX());
                Log.d("TEST", "Y velocity: " + event.getY());
                iv.setX(event.getX());
                iv.setY(event.getY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Return a VelocityTracker object back to be re-used by others.
                mVelocityTracker.recycle();
                break;
        }
        return true;
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_bt_remote);
		
		Point size = new Point();
		getWindowManager().getDefaultDisplay().getSize(size);
		RCView rcv = (RCView) findViewById(R.id.RCView);
		rcv.setScreenSize(size);
		
    	iv = (ImageView)findViewById(R.id.imageView1);
    			
		//initAnimations();
    	
    	
    	
		pos_x = (TextView) findViewById(R.id.textView1);
		pos_y = (TextView) findViewById(R.id.textView2);
		pos_z = (TextView) findViewById(R.id.textView3);

		pos_x.setText("a");
		pos_y.setText("a");
		pos_z.setText("a");
		
	
		askPosition = (Button)findViewById(R.id.button_ask_position);
		askPosition.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String cmd;
				if(askPosition.getText().equals(getString(R.string.button_stop_position))){
					cmd = "C0";
					askPosition.setText(getString(R.string.button_get_position));
				}else{
					cmd = "C1";
					askPosition.setText(getString(R.string.button_stop_position));
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
		

		quit = (Button)findViewById(R.id.button_quit);
		quit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
	            System.exit(0);			
			}
		});
		
		
		adapter=new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1,
	            btResult);
		
		builder = new AlertDialog.Builder(this);
	    builder.setTitle(getString(R.string.discover));
	    builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
			public void onClick(DialogInterface dialog, int which) {
            // The 'which' argument contains the index position
            // of the selected item
            	Toast.makeText(getApplicationContext(),
						"Connection to :"+btResult.get(which), Toast.LENGTH_SHORT).show();
				ct = new ConnectThread(btResultDevices.get(which));
				ct.start();
            }
	    });
	    builder.setCancelable(true);
	    builder.create();
	    
	    
	    searchDevices = (Button)findViewById(R.id.button_search_devices);
		searchDevices.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(connected){
					ctd.cancel();
					connected = false;
					searchDevices.setText(getString(R.string.button_connect_devices));
				}else{
				if(mBluetoothAdapter.startDiscovery()){
					builder.show();
					Toast.makeText(getApplicationContext(),
							getString(R.string.discovery_begin), Toast.LENGTH_SHORT).show();
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
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		    	addDevice(device);
		    }
		}

		// Ask to start bluetooth if disabled
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}else{
			searchDevices.setEnabled(true);
		}
		
//		if(mBluetoothAdapter.startDiscovery()){
//			Toast.makeText(getApplicationContext(),
//					getString(R.string.discovery_begin), Toast.LENGTH_SHORT).show();
//		}
	}
	
	
	/**
	 * Method used to avoid duplicate entry in the bt list 
	 * @param device
	 */
	public void addDevice(BluetoothDevice device){
		//First we check if the device already exists
		int index = -1;
		for(int i = 0; i<btResultDevices.size();i++){
			System.out.println();
			if(btResultDevices.get(i).getAddress().equals(device.getAddress())){
				index = i;
				break;
			}
		}
		if(index < 0){
			btResultDevices.add(device);
			btResult.add(device.getName() + "\n" + device.getAddress());
		}else{
			btResultDevices.remove(index);
			btResultDevices.add(index, device);
			btResult.remove(index);
			btResult.add(index,device.getName() + "\n" + device.getAddress());
		}
		
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == RESULT_CANCELED)
				Toast.makeText(getApplicationContext(),
						getString(R.string.bluetooth_disabled), Toast.LENGTH_SHORT).show();
			else{
				Toast.makeText(getApplicationContext(), getString(R.string.bluetooth_enabled),
						Toast.LENGTH_SHORT).show();
				searchDevices.setEnabled(true);
		}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bt_remote, menu);
		return true;
	}
	
	@Override
	public void onStop(){
		if (mReceiver != null)
			unregisterReceiver(mReceiver);
		if(ctd!=null && ctd.isAlive())
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
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
	        } catch (IOException e) { }
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
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        //manageConnectedSocket(mmSocket);
	        //connected();
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
	        } catch (IOException e) { }
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
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    @Override
		public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream	        
	        Message mes;
	        int index = 0;
	        int count = 0,pos;
	        int c1 = 0,c2 = 0;
	        boolean loop = true;
	        
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	            	c2 = c1;
	            	c1 = mmInStream.read();
	            	switch(c2){
	            	case('C'):
	            		switch(c1){
	            		case('1'):
	            				c1 = mmInStream.read();
	            				pos = c1<<8;
	            				c1 = mmInStream.read();
	            				pos += c1;
	            				if(pos>128)pos-=256;
	            				
	            				mes = mHandler.obtainMessage(POSX, pos);
	            				mHandler.sendMessage(mes);
	            				c1 = mmInStream.read();
	            				pos = c1<<8;
	            				c1 = mmInStream.read();
	            				pos += c1;
	            				mes = mHandler.obtainMessage(POSY, pos);
	            				mHandler.sendMessage(mes);
	            				c1 = mmInStream.read();
	            				pos = c1<<8;
	            				c1 = mmInStream.read();
	            				pos += c1;
	            				mes = mHandler.obtainMessage(POSZ, pos);
	            				mHandler.sendMessage(mes);
	            				break;
	            		case('2'):
	            			loop = true;
	            			index = 0;
	            			while(loop){
	            				c2 = c1;
	        	            	c1 = mmInStream.read();
	        	            	buffer[index++]= (byte)c1;
	        	                if(c2 == '\r' && c1 == '\n'){
	        	                	mes = mHandler.obtainMessage(MESSAGE_RECEIVED, index, -1,buffer);
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
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}


}
