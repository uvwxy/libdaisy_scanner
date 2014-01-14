package de.uvwxy.daisy.nodescanner;

import android.content.Intent;
import android.util.Log;
import de.uvwxy.daisy.proto.Messages;
import de.uvwxy.daisy.proto.Messages.NameTag;
import de.uvwxy.helper.IntentExtras;
import de.uvwxy.helper.URLTools;

public class NodeDataConverter {
	/**
	 * Use this method to convert the intent result of the node scanner to proto
	 * NodeLocationData object.
	 * 
	 * @param intent
	 *            the returned intent from the node scanner
	 * @return a proto NodeLocationData object containing the scan result
	 */
	public static Messages.NodeLocationData handleReturnIntent(Intent intent) {
		Messages.NodeLocationData.Builder nodeDataBuilder = Messages.NodeLocationData.newBuilder();

		if (intent == null) {
			return null;
		}

		String qrCodeData = intent.getStringExtra(IntentExtras.QRCODE_DATA);
		if (qrCodeData != null) {
			nodeDataBuilder.setQrCodeData(qrCodeData);
		}

		float[] f = intent.getFloatArrayExtra(IntentExtras.QRCODE_ORIENTATION_DATA);
		if (f != null && f.length >= 3) {
			nodeDataBuilder.setQrCodeOrientationX(f[0]);
			nodeDataBuilder.setQrCodeOrientationY(f[1]);
			nodeDataBuilder.setQrCodeOrientationZ(f[2]);
		}

		f = intent.getFloatArrayExtra(IntentExtras.PARALLEL_ORIENTATION_DATA);
		if (f != null && f.length >= 3) {
			nodeDataBuilder.setParallelOrientationX(f[0]);
			nodeDataBuilder.setParallelOrientationY(f[1]);
			nodeDataBuilder.setParallelOrientationZ(f[2]);
		}

		String[] paths = intent.getStringArrayExtra(IntentExtras.SELECTED_IMAGE_PATHS);
		if (paths != null) {
			for (String path : paths) {
				if (path != null) {
					nodeDataBuilder.addImagePath(path);
				}
			}
		}

		f = intent.getFloatArrayExtra(IntentExtras.BARO0);
		if (f != null && f.length >= 1) {
			nodeDataBuilder.setBaro0(f[0]);
		}

		f = intent.getFloatArrayExtra(IntentExtras.BARO1);
		if (f != null && f.length >= 1) {
			nodeDataBuilder.setBaro1(f[0]);
		}

		nodeDataBuilder.setQrCodeBearing(intent.getIntExtra(IntentExtras.QRCODE_BEARING, -1));
		nodeDataBuilder.setHeight(intent.getFloatExtra(IntentExtras.BARO_HEIGHT, -1F));

		String landmarkString = intent.getStringExtra(IntentExtras.LANDMARKS);
		if (landmarkString != null) {
			nodeDataBuilder.setLandmarkText(landmarkString);
		}

		// TODO: better check than using LOCATION_LAT?
		if (intent.hasExtra(IntentExtras.LOCATION_LAT)) {
			Log.i("SCAN", "ADDING LOCATION");
			Messages.Location.Builder locBuilder = Messages.Location.newBuilder();
			locBuilder.setLatitude(intent.getDoubleExtra(IntentExtras.LOCATION_LAT, 0.0d));
			locBuilder.setLongitude(intent.getDoubleExtra(IntentExtras.LOCATION_LON, 0.0d));
			locBuilder.setAltitude(intent.getDoubleExtra(IntentExtras.LOCATION_ALT, 0.0d));
			locBuilder.setBearing(intent.getFloatExtra(IntentExtras.LOCATION_BEARING, 0.0f));
			locBuilder.setAccuracy(intent.getFloatExtra(IntentExtras.LOCATION_ACC, 0.0f));
			locBuilder.setTime(intent.getLongExtra(IntentExtras.LOCATION_TIME, System.currentTimeMillis()));
			locBuilder.setSpeed(intent.getFloatExtra(IntentExtras.LOCATION_SPEED, 0.0f));
			locBuilder.setProvider(intent.getStringExtra(IntentExtras.LOCATION_PROV));

			nodeDataBuilder.setLocation(locBuilder.build());
		}

		String qrUrlID = qrCodeData == null ? "" + System.currentTimeMillis() : URLTools
				.getParamValue(qrCodeData, "id");
		String nodeID = qrUrlID;

		// if no node id was set try to parse from qrcode data
		int tempId = intent.getIntExtra(IntentExtras.INTENT_EXTRA_NODE_ID_INT, -1);
		if (tempId == -1) {
			int iNodeID = getNodeIDFromQRCodeURL(nodeID);
			nodeDataBuilder.setNodeId(iNodeID);
		} else {
			nodeDataBuilder.setNodeId(tempId);
		}

		// this is just to satisfy the protocol
		nodeDataBuilder.setTag(NameTag.newBuilder().setUuid("DUMMY").setSequenceNumber(0).build());
		nodeDataBuilder.setTimestamp(System.currentTimeMillis());
		return nodeDataBuilder.build();
	}

	public static int getNodeIDFromQRCodeURL(String nodeID) {
		int iNodeID = 0;

		if (nodeID != null) {
			//
			// remove all chars and _try_ to convert to integer:
			String strippedID = nodeID.replaceAll("[^0-9]", "");
			try {
				iNodeID = Integer.parseInt(strippedID);
			} catch (Exception e) {
			}
		}

		if (iNodeID == 0) {
			iNodeID = (int) System.currentTimeMillis();
		}
		return iNodeID;
	}
}
