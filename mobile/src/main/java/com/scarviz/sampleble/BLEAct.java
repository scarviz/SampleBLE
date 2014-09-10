package com.scarviz.sampleble;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.List;


public class BLEAct extends Activity {
	private BTService mBoundService;
	private boolean mIsBound;
	private BTServiceHandler mBTServiceHandler;

	private DeviceListAdapter mDeviceListAdapter;

	private Button mBtnBleSearch, mBtnTest;
	private Switch mSwService;
	private TextView mTextView;
	private ListView mBleList;

	private final static char NEW_LINE = '\n';
	private final static int MAX_LINE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

		mBtnBleSearch = (Button) findViewById(R.id.btnBleSearch);
		mSwService = (Switch) findViewById(R.id.swService);
		mTextView = (TextView) findViewById(R.id.text);
		mBleList = (ListView) findViewById(R.id.list);
		mBtnTest = (Button) findViewById(R.id.btnBleTest);

		mBtnBleSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!IsEnabledBluetooth()){
					return;
				}

				mDeviceListAdapter.clear();
				mDeviceListAdapter.notifyDataSetChanged();

				mBoundService.ScanDevice(mLeScanCallback);
			}
		});

		boolean isRunning = IsRunningBTService();
		mSwService.setChecked(isRunning);
		mSwService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
				if(isChecked){
					StartService();
				} else {
					StopService();
				}
			}
		});

		mBleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				if (!IsEnabledBluetooth()){
					return;
				}

				BluetoothDevice device = mDeviceListAdapter.getDevice(i);
				mBoundService.Connect(device.getAddress());
			}
		});

		mBtnTest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!IsEnabledBluetooth()){
					return;
				} else if (mBoundService.IsConnected()) {
					mBoundService.sendMessage("Test Message");
					Toast.makeText(BLEAct.this, "Send Message", Toast.LENGTH_SHORT).show();
				} else {
					SetText("BlueTooth Not Connected");
				}
			}
		});

		mDeviceListAdapter = new DeviceListAdapter(BLEAct.this);
		mBleList.setAdapter(mDeviceListAdapter);

		if(isRunning){
			StartService();
		}
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mIsBound) {
			unbindService(mConnection);
		}
	}

	private static final String mBTServiceName = BTService.class.getCanonicalName();
	/**
	 * BTServiceが起動中かどうか
	 * @return
	 */
	private boolean IsRunningBTService() {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

		for (ActivityManager.RunningServiceInfo info : services) {
			if (mBTServiceName.equals(info.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Bluetoothが有効かどうか
	 * @return
	 */
	private boolean IsEnabledBluetooth() {
		if (mBoundService == null){
			SetText("Service Not Bound");
			return false;
		} else if (!mBoundService.IsEnabledBluetooth()) {
			SetText("BlueTooth Not Enable");
			return false;
		}

		return true;
	}

	/**
	 * Serviceを開始する
	 */
	private void StartService(){
		// Serviceと接続
		Intent intent = new Intent(this, BTService.class);
		startService(intent);
		bindService(intent, mConnection, BIND_AUTO_CREATE);

		mIsBound = true;
	}

	/**
	 * Serviceを停止する
	 */
	private void StopService(){
		unbindService(mConnection);
		Intent intent = new Intent(this, BTService.class);
		stopService(intent);

		mIsBound = false;
	}

	/**
	 * Serviceと接続するためのコネクション
	 */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBoundService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBoundService = ((BTService.BTServicelBinder)service).getService();
			if(mBoundService == null) {
				SetText("Service Not Bound");
				return;
			}

			mBTServiceHandler = new BTServiceHandler(BLEAct.this);
			mBoundService.SetHandlerAct(mBTServiceHandler);
		}
	};

	/**
	 * メッセージを設定する
	 * @param mes
	 */
	private void SetText(String mes){
		StringBuilder sb = new StringBuilder();
		// 最初の行にメッセージを追加
		sb.append(mes);

		// 過去のメッセージ
		String text = mTextView.getText().toString();
		String[] textAry = text.split(String.valueOf(NEW_LINE));
		int len = textAry.length;
		// 行数が最大行以上の場合
		if(MAX_LINE <= len){
			len--;
		}

		// 過去のメッセージを追加していく
		for(int i = 0; i < len; i++){
			sb.append(NEW_LINE);
			sb.append(textAry[i]);
		}

		mTextView.setText(sb.toString());
	}

	/**
	 * BLE機器のスキャンのコールバック
	 */
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
			new BluetoothAdapter.LeScanCallback() {
				//　デバイスが発見された時
				@Override
				public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mDeviceListAdapter.addDevice(device);
							mDeviceListAdapter.notifyDataSetChanged();
						}
					});
				}
			};

	/**
	 * BTServiceのハンドラ
	 */
	private static class BTServiceHandler extends Handler {
		WeakReference<BLEAct> ref;

		public BTServiceHandler(BLEAct r) {
			ref = new WeakReference<BLEAct>(r);
		}

		@Override
		public void handleMessage(Message msg) {
			final BLEAct act = ref.get();
			if (act == null) {
				return;
			}

			switch (msg.what) {
				case BluetoothStatus.START_SCAN:
					act.SetText("Start Scan");
					break;
				case BluetoothStatus.STOP_SCAN:
					act.SetText("Stop Scan");
					break;
				case BluetoothStatus.CONNECTING:
					act.SetText("Connecting...");
					break;
				case BluetoothStatus.CONNECTED:
					act.SetText("Connected. Discover Services");
					break;
				case BluetoothStatus.DISCONNECTED:
					act.SetText("DisConnected");
					break;
				case BluetoothStatus.SUCCESS:
					if(msg.obj != null){
						act.SetText(msg.obj.toString());
					}
					break;
				case BluetoothStatus.FAILURE:
					if(msg.obj != null){
						act.SetText(msg.obj.toString());
					}
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ble, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
