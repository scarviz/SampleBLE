package com.scarviz.sampleble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

/**
 * Bluetoothヘルパークラス
 */
public class BluetoothHelper {
	private final static String TAG = "BluetoothHelper";

	private Context mContext;
	private Handler mHandler;
	private BluetoothAdapter.LeScanCallback mLeScanCallback;

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;

	private boolean mIsScanning = false;
	private boolean mIsConnected = false;

	BluetoothGatt mBluetoothGatt;
	/** スキャンを始めてからSCAN_PERIOD ms後にスキャンを自動停止 */
	private static final long SCAN_PERIOD = 10000;

	/** 対象のサービスUUID */
	private static final String DEVICE_SERVICE_UUID = "9E672755-C622-49E0-93B8-4BE76A97208B";
	/** 対象のキャラクタリスティックUUID */
	private static final String DEVICE_CHARACTERISTIC_UUID = "E2CC9711-C6D2-464D-AC7C-25DC963F0BDE";
	/** Descriptor設定UUID */
	private static final String CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

	/**
	 * コンストラクタ
	 * @param context
	 * @param handler
	 */
	public BluetoothHelper(Context context, Handler handler){
		mContext = context;
		mHandler = handler;

		GenBluetoothAdapter();
	}

	/**
	 * BluetoothAdapterの生成
	 */
	private void GenBluetoothAdapter() {
		mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
	}

	/**
	 * 端末がBluetoothを使用できるかチェックする
	 * @return
	 */
	public boolean IsEnabledBluetooth() {
		if(mBluetoothAdapter == null) {
			GenBluetoothAdapter();
		}

		if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
			return false;
		}
		else{
			return true;
		}
	}

	/**
	 * Bluetooth機器のスキャン
	 * @param leScanCallback
	 */
	public void ScanDevice(BluetoothAdapter.LeScanCallback leScanCallback){
		mLeScanCallback = leScanCallback;
		// 一定時間後にスキャンを停止
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				StopScan();
			}
		}, SCAN_PERIOD);

		mBluetoothAdapter.startLeScan(mLeScanCallback);
		mIsScanning = true;
		SendHandlerMessage(BluetoothStatus.START_SCAN);
	}

	/**
	 * Bluetooth機器のスキャンを停止する
	 */
	public void StopScan(){
		if(mIsScanning) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			SendHandlerMessage(BluetoothStatus.STOP_SCAN);
		}
		mIsScanning = false;
	}

	/**
	 * Bluetooth機器の接続
	 * @param context
	 * @param autoConnect
	 * @param address
	 */
	public void Connect(Context context, boolean autoConnect, String address) {
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		mBluetoothGatt = device.connectGatt(context, autoConnect, mBleGattCallback);
		StopScan();
		SendHandlerMessage(BluetoothStatus.CONNECTING);
	}

	/**
	 * BLE 機器との接続を解除する
	 */
	public void DisConnect() {
		StopScan();
		if (mBluetoothGatt != null) {
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}
		mIsConnected = false;
		SendHandlerMessage(BluetoothStatus.DISCONNECTED);
	}

	/**
	 * GATTコールバック
	 */
	private BluetoothGattCallback mBleGattCallback = new BluetoothGattCallback() {
		/**
		 * 接続状態変更時処理
		 * @param gatt
		 * @param status
		 * @param newState
		 */
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			Log.d(TAG, "onConnectionStateChange: " + status + " -> " + newState);
			if (newState == BluetoothProfile.STATE_CONNECTED) {	// GATT接続成功
				Log.d(TAG, "Connected");
				// Serviceを検索する
				gatt.discoverServices();
				mIsConnected = true;
				SendHandlerMessage(BluetoothStatus.CONNECTED);
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {	// GATT通信が切断
				Log.d(TAG, "DisConnected");
				DisConnect();
			}
		}

		/**
		 * Service発見時処理
		 * @param gatt
		 * @param status
		 */
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Log.d(TAG, "onServicesDiscovered received: " + status);
			if (status != BluetoothGatt.GATT_SUCCESS) {
				Log.d(TAG, "onServicesDiscovered GATT failure");
				SendHandlerMessage(BluetoothStatus.FAILURE, "onServicesDiscovered GATT failure:" + status);
				return;
			}

			// サービス
			BluetoothGattService service = gatt.getService(UUID.fromString(DEVICE_SERVICE_UUID));
			// サービスが見つからなかった場合
			if (service == null) {
				Log.d(TAG, "service is null");
				SendHandlerMessage(BluetoothStatus.FAILURE, "service is null");
				return;
			}

			// キャラクタリスティック
			BluetoothGattCharacteristic characteristic =
					service.getCharacteristic(UUID.fromString(DEVICE_CHARACTERISTIC_UUID));
			// キャラクタリスティックが見つからなかった場合
			if (characteristic == null) {
				Log.d(TAG, "characteristic is null");
				SendHandlerMessage(BluetoothStatus.FAILURE, "characteristic is null");
				return;
			}

			// Notificationを要求する
			boolean registered = gatt.setCharacteristicNotification(characteristic, true);
			// Notificationを有効化
			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
					UUID.fromString(CHARACTERISTIC_CONFIG));
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			gatt.writeDescriptor(descriptor);

			// キャラクタリスティック通知設定が成功
			if (registered) {
				Log.d(TAG, "CharacteristicNotification success");
				SendHandlerMessage(BluetoothStatus.SUCCESS, "CharacteristicNotification Success");
			} else {
				Log.d(TAG, "CharacteristicNotification failure");
				SendHandlerMessage(BluetoothStatus.FAILURE, "CharacteristicNotification Failure");
			}
		}

		/**
		 * キャラクタリスティック変更時処理
		 * @param gatt
		 * @param characteristic
		 */
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Log.d(TAG, "onCharacteristicChanged");
			// Characteristicの値更新通知
			if (DEVICE_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString())) {
				Log.d(TAG, "device characteristic changed");

				final byte[] data = characteristic.getValue();
				if (data != null && data.length > 0) {
					final StringBuilder stringBuilder = new StringBuilder(data.length);
					for(byte byteChar : data) {
						stringBuilder.append(String.format("%02X ", byteChar));
					}

					String mes = stringBuilder.toString();
					if(mes == null || mes.isEmpty()) {
						mes = "Notify is empty";
					}
					Log.d(TAG, mes);
					Toast.makeText(mContext, mes, Toast.LENGTH_LONG).show();
				}
			}
		}

		/**
		 * 読み取り処理
		 * @param gatt
		 * @param characteristic
		 * @param status
		 */
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.d(TAG, "onCharacteristicRead");
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.d(TAG, "Characteristic read success");
			}
		}

		/**
		 * 書き込み処理
		 * @param gatt
		 * @param characteristic
		 * @param status
		 */
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.d(TAG, "onCharacteristicWrite");
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.d(TAG, "Characteristic write success");
			}
		}
	};

	/**
	 * 接続中かどうか
	 * @return
	 */
	public boolean IsConnected() {
		return mIsConnected;
	}

	/**
	 * HandlerMessageを送る
	 * @param id
	 */
	private void SendHandlerMessage(int id){
		SendHandlerMessage(id, null);
	}

	/**
	 * HandlerMessageを送る
	 * @param id
	 * @param obj
	 */
	private void SendHandlerMessage(int id, Object obj){
		Message mes = Message.obtain();
		mes.what = id;
		mes.obj = obj;
		mHandler.sendMessage(mes);
	}
}
