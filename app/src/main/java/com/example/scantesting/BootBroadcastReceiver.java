package com.example.scantesting;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;


public class BootBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		// TODO Auto-generated method stub

		//Log.e("jiebao", "OemBootBroadcastReceiver " + Preference.getScanSelfopenSupport(BaseApplication.getAppContext(), true));

		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			if (Pref.getScanSelfopenSupport(BaseApplication.getAppContext(), true)) {
				Intent service = new Intent(context, ScanService.class);
				context.startService(service);
			}
		}

	}
}
