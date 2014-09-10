package com.scarviz.sampleble;

/**
 * Created by satoshi on 2014/09/10.
 */
public class BluetoothStatus {
	/** Scan開始 */
	public final static int START_SCAN = 0x1001;
	/** Scan停止 */
	public final static int STOP_SCAN = 0x1002;
	/** 接続中 */
	public final static int CONNECTING = 0x1003;
	/** 接続 */
	public final static int CONNECTED = 0x1004;
	/** 切断 */
	public final static int DISCONNECTED = 0x1005;

	/** 成功 */
	public final static int SUCCESS = 0x0000;
	/** 失敗 */
	public final static int FAILURE = 0x9999;
}
