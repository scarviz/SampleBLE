package com.scarviz.sampleble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

public class BTService extends Service {
	private final static String TAG = "BTService";

	private BluetoothHelper mBtHelper;
	private BtProcHandler mBtProcHandler;
	private Handler mHandlerAct;

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");

		if(mBtProcHandler == null) {
			mBtProcHandler = new BtProcHandler(this);
		}
		if(mBtHelper == null) {
			mBtHelper = new BluetoothHelper(this, mBtProcHandler);
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		DisConnect();
	}

	/**
	 * Bind処理
	 * @param intent
	 * @return
	 */
	@Override
    public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
        return mBinder;
    }

	/**
	 * Binder
	 */
	private final IBinder mBinder = new BTServicelBinder();
	public class BTServicelBinder extends Binder {
		/**
		 * サービスの取得
		 */
		BTService getService() {
			return BTService.this;
		}
	}

	/**
	 * Handlerの設定
	 * @param handlerAct
	 */
	public void SetHandlerAct(Handler handlerAct){
		mHandlerAct = handlerAct;
	}

	/**
	 * 端末がBluetoothを使用できるかチェックする
	 * @return
	 */
	public boolean IsEnabledBluetooth(){
		return mBtHelper.IsEnabledBluetooth();
	}

	/**
	 * Bluetooth機器のスキャン
	 * @param mLeScanCallback
	 */
	public void ScanDevice(final BluetoothAdapter.LeScanCallback mLeScanCallback){
		mBtHelper.ScanDevice(mLeScanCallback);
	}

	/**
	 * Bluetooth機器の接続
	 * @param address
	 */
	public void Connect(String address){
		// 手動で接続するのでautoConnectはfalse
		mBtHelper.Connect(this, false, address);
	}

	/**
	 * Bluetooth機器の切断
	 */
	public void DisConnect(){
		Log.d(TAG, "DisConnect");
		if(mBtHelper != null) {
			mBtHelper.DisConnect();
		}
	}

	/**
	 * 接続中かどうか
	 * @return
	 */
	public boolean IsConnected() {
		return mBtHelper.IsConnected();
	}

	/**
	 * メッセージを送信する
	 * @param message
	 */
	public void sendMessage(String message) {
		//mBtHelper.sendMessage(message);
	}

	/**
	 * Bluetooth通信処理のハンドラ
	 */
	private static class BtProcHandler extends Handler {
		WeakReference<BTService> ref;

		public BtProcHandler(BTService r) {
			ref = new WeakReference<BTService>(r);
		}

		@Override
		public void handleMessage(Message msg) {
			final BTService btSrv = ref.get();
			if (btSrv == null) {
				return;
			}

			switch (msg.what) {
				case BluetoothStatus.START_SCAN:
				case BluetoothStatus.STOP_SCAN:
				case BluetoothStatus.CONNECTING:
				case BluetoothStatus.CONNECTED:
				case BluetoothStatus.DISCONNECTED:
					if(btSrv.mHandlerAct != null) {
						btSrv.mHandlerAct.sendMessage(btSrv.GetMessage(msg.what));
					}
					break;
				case BluetoothStatus.SUCCESS:
				case BluetoothStatus.FAILURE:
					if(btSrv.mHandlerAct != null) {
						btSrv.mHandlerAct.sendMessage(btSrv.GetMessage(msg.what, msg.obj));
					}
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
	}

	/**
	 * Messageを取得する
	 * @param id
	 */
	private Message GetMessage(int id){
		return GetMessage(id, null);
	}

	/**
	 * Messageを取得する
	 * @param id
	 * @param obj
	 */
	private Message GetMessage(int id, Object obj){
		Message mes = Message.obtain();
		mes.what = id;
		mes.obj = obj;
		return mes;
	}
}
