package com.example.scantesting;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Vibrator;


public class ScanBeepManager {

	private static final String TAG = "BeepManagerSdl";
	// 常量
	private final float BEEP_VOLUME = 0.3f;
	private final long VIBRATE_DURATION = 200L;

	// 变量
	private boolean playBeep = false;
	private boolean vibrate = false;

	// 控制器
	private Context mContext;
	private int loadId1;
	private SoundPool mSoundPool;
	private Vibrator mVibrator;

	public ScanBeepManager(Context context, boolean playBeep, boolean vibrate) {
		super();
		this.mContext = context;
		this.playBeep = playBeep;
		this.vibrate = vibrate;
		initial();
	}

	public ScanBeepManager(Context context) {
		super();
		this.mContext = context;
		this.playBeep = Pref.getIsPlaySound(context);
		this.vibrate = Pref.getVibrate(context);
		initial();
	}
	


	private void initial() {

		if (null == mSoundPool) {
			mSoundPool = new SoundPool(5, AudioManager.STREAM_RING, 0);
		}
		
		loadId1 = mSoundPool.load(mContext, getRawResIDByName(mContext, "beep"), 1);
		// initialVibrator
		mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
	}

	public static int getRawResIDByName(Context context, String name) {
		return context.getResources().getIdentifier(name, "raw",
				context.getPackageName());
	}
	
	public void play() {
		// playMusic
		// if (playBeep && !km.inKeyguardRestrictedInputMode()) {
		// mMediaPlayer.start();
		// }
		if (Pref.getIsPlaySound(mContext)) {
			// mMediaPlayer.start();
			// 参数1：播放特效加载后的ID值
			// 参数2：左声道音量大小(range = 0.0 to 1.0)
			// 参数3：右声道音量大小(range = 0.0 to 1.0)
			// 参数4：特效音乐播放的优先级，因为可以同时播放多个特效音乐
			// 参数5：是否循环播放，0只播放一次(0 = no loop, -1 = loop forever)
			// 参数6：特效音乐播放的速度，1F为正常播放，范围 0.5 到 2.0
			mSoundPool.play(loadId1,0.5f, 0.5f, 1, 0, 2.0f);
		}

		if (Pref.getVibrate(mContext)) {
			mVibrator.vibrate(VIBRATE_DURATION);
		}

	}

}
