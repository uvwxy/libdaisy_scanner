package de.uvwxy.daisy.nodescanner;

import android.app.Activity;
import android.content.Intent;
import de.uvwxy.daisy.protocol.DaisyData;
import de.uvwxy.helper.IntentExtras;

public class NodeScanLauncher {
	public static void show(Activity act,DaisyData data,  int code){
		Intent my_intent = new Intent("de.uvwxy.daisy.SCAN_NODE");
		my_intent.addCategory(Intent.CATEGORY_DEFAULT);
		my_intent.putExtra(IntentExtras.INTENT_EXTRA_TIMESTAMP, data.getIdAndTimeStamp());
		act.startActivityForResult(my_intent, code);
	}
}
