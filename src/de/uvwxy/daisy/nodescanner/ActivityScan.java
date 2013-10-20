package de.uvwxy.daisy.nodescanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

import de.uvwxy.daisy.ViewTools;
import de.uvwxy.daisy.ViewTools.RemovePath;
import de.uvwxy.daisy.protocol.DaisyData;
import de.uvwxy.helper.BitmapTools;
import de.uvwxy.helper.FileTools;
import de.uvwxy.helper.IntentExtras;
import de.uvwxy.helper.IntentTools;
import de.uvwxy.sensors.BarometerReader;
import de.uvwxy.sensors.CompassReader;
import de.uvwxy.sensors.SensorReader.SensorResultCallback;
import de.uvwxy.sensors.location.GPSWIFIReader;
import de.uvwxy.sensors.location.LocationReader.LocationResultCallback;
import de.uvwxy.sensors.location.LocationReader.LocationStatusCallback;

public class ActivityScan extends Activity {

	private static int DISPLAY_ROTATION_DEGREES = 90;

	private LinearLayout llPhotoList = null;
	private RelativeLayout rlBottomBox = null;
	private RelativeLayout rlMain = null;
	private ScrollView scrollView1 = null;

	private Button btnBaro0 = null;
	private Button btnBaro1 = null;
	private Button btnCancel = null;
	private Button btnDone = null;
	private Button btnHelp = null;
	private Button btnId = null;
	private Button btnLocation = null;
	private Button btnNodeOrientation = null;
	private Button btnPhotos = null;
	private Button btnPhotoSelect = null;

	private EditText etLandmarks = null;
	private EditText etNodeId = null;
	private TextView lblBaro0 = null;
	private TextView lblBaro1 = null;
	private TextView lblId = null;
	private TextView lblLandmarks = null;
	private TextView lblLocation = null;
	private TextView lblNodeOrientation = null;
	private TextView lblPhotos = null;
	private TextView tvBaro0Info = null;
	private TextView tvBaro1Info = null;
	private TextView tvId = null;
	private TextView tvLocation = null;
	private TextView tvNodeOrientation = null;
	private TextView tvPhotos = null;

	ProgressDialog progressDialogBaro0;
	ProgressDialog progressDialogBaro1;
	ProgressDialog progressDialogNodeOrientation;

	protected Activity ctx;

	private static final int INTENT_PICK_IMAGES = 1;
	private static final int INTENT_TAKE_IMAGE = 2;
	private static final int INTENT_SCAN_QR_CODE = 3;

	private String mLastTakenImagePath;
	private String etLandmarksOnCreateText = null;

	ArrayList<String> imagePaths = new ArrayList<String>();
	private float[] parallelOrientation;
	private float[] baro0;
	private float[] baro1;
	protected float nodeHeight;
	private Location nodeLocation;

	private CompassReader cr;
	private BarometerReader br0;
	private BarometerReader br1;
	private GPSWIFIReader gps;

	private RemovePath removePathCallback = new RemovePath() {

		@Override
		public void removePath(String path) {
			imagePaths.remove(path);
			updateLblPhotoList();
		}
	};

	private String qrValue;

	private float[] qrOrientationData;

	private int qrBearing;

	private void initGUI() {
		rlMain = (RelativeLayout) findViewById(R.id.rlMain);
		scrollView1 = (ScrollView) findViewById(R.id.scrollView1);
		llPhotoList = (LinearLayout) findViewById(R.id.llPhotoList);
		rlBottomBox = (RelativeLayout) findViewById(R.id.rlBottomBox);

		lblBaro0 = (TextView) findViewById(R.id.lblBaro0);
		lblBaro1 = (TextView) findViewById(R.id.lblBaro1);
		lblId = (TextView) findViewById(R.id.lblId);
		lblNodeOrientation = (TextView) findViewById(R.id.lblNodeOrientation);
		lblLocation = (TextView) findViewById(R.id.lblLocation);
		lblLandmarks = (TextView) findViewById(R.id.lblLandmarks);
		lblPhotos = (TextView) findViewById(R.id.lblPhotos);

		tvBaro0Info = (TextView) findViewById(R.id.tvBaro0Info);
		tvBaro1Info = (TextView) findViewById(R.id.tvBaro1Info);
		tvId = (TextView) findViewById(R.id.tvId);
		tvNodeOrientation = (TextView) findViewById(R.id.tvNodeOrientation);
		tvLocation = (TextView) findViewById(R.id.tvLocation);
		tvPhotos = (TextView) findViewById(R.id.tvPhotos);

		etLandmarks = (EditText) findViewById(R.id.etLandmarks);
		etNodeId = (EditText) findViewById(R.id.etNodeId);

		btnBaro0 = (Button) findViewById(R.id.btnBaro0);
		btnBaro1 = (Button) findViewById(R.id.btnBaro1);
		btnId = (Button) findViewById(R.id.btnId);
		btnNodeOrientation = (Button) findViewById(R.id.btnNodeOrientation);
		btnLocation = (Button) findViewById(R.id.btnLocation);
		btnPhotoSelect = (Button) findViewById(R.id.btnPhotoSelect);
		btnPhotos = (Button) findViewById(R.id.btnPhotos);
		btnCancel = (Button) findViewById(R.id.btnCancel);
		btnDone = (Button) findViewById(R.id.btnDone);
		btnHelp = (Button) findViewById(R.id.btnHelp);

		etLandmarksOnCreateText = etLandmarks.getText().toString();

		PackageManager PM = this.getPackageManager();
		boolean hasBaro = PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
		boolean hasAcc = PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
		boolean hasComp = PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
		// boolean hasCamera =
		// PM.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
		boolean hasLocation = PM.hasSystemFeature(PackageManager.FEATURE_LOCATION);

		if (!hasBaro) {
			btnBaro0.setEnabled(false);
			btnBaro1.setEnabled(false);
		}

		if (!hasComp || !hasAcc) {
			btnNodeOrientation.setEnabled(false);
		}

		// Asus TF201 reports "no camera" although both are present
		// if (!hasCamera) {
		// btnPhotos.setEnabled(false);
		// btnId.setEnabled(false);
		// }

		if (!hasLocation) {
			btnLocation.setEnabled(false);
		}

		btnPhotos.setEnabled(false);
		btnPhotoSelect.setEnabled(false);

	}

	private void initGUIClicks() {

		etNodeId.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (etNodeId.getText().toString().length() >= 1) {
					btnPhotos.setEnabled(true);
					btnPhotoSelect.setEnabled(true);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		btnPhotos.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mLastTakenImagePath = FileTools.getAndCreateExternalFolder(CREATE_IMAGES_FOLDER) + etNodeId.getText().toString() + "_"
						+ System.currentTimeMillis() + ".jpg";
				IntentTools.captureImage(ctx, mLastTakenImagePath, INTENT_TAKE_IMAGE);
			}
		});

		btnPhotoSelect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				IntentTools.selectImage(ctx, INTENT_PICK_IMAGES);
			}
		});

		btnNodeOrientation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (cr == null) {
					cr = new CompassReader(ctx, 4000, new SensorResultCallback() {

						@Override
						public void result(float[] f) {
							parallelOrientation = f.clone();
							String format = String.format("Values: %.2f° / %.2f° / %.2f° ", f[0], f[1], f[2]);
							ViewTools.updateViewOnUIThread(ctx, tvNodeOrientation, format);
							progressDialogNodeOrientation.dismiss();
						}
					});
				}

				progressDialogNodeOrientation = ProgressDialog.show(ctx, "Please wait",
						"Point your device parallel to the node.\n\nRecording measurement after 4 seconds", true);
				cr.startReading();

			}
		});

		btnBaro0.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (br0 == null) {
					br0 = new BarometerReader(ctx, 4000, new SensorResultCallback() {

						@Override
						public void result(float[] f) {
							String format = String.format("Value: %.2f mBar", f[0]);
							ViewTools.updateViewOnUIThread(ctx, tvBaro0Info, format);
							baro0 = f.clone();
							progressDialogBaro0.dismiss();
						}
					});
					// br.addFilter(new LowPassFilter());
				}

				progressDialogBaro0 = ProgressDialog.show(ctx, "Please wait",
						"Hold your device as close as possible to the ground.\n\nRecording measurement after 4 seconds", true);
				br0.startReading();
			}
		});

		btnBaro1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				nodeHeight = -1f;
				if (br1 == null) {
					br1 = new BarometerReader(ctx, 4000, new SensorResultCallback() {

						@Override
						public void result(float[] f) {

							baro1 = f.clone();
							if (baro0 != null && baro1 != null && baro0.length >= 1 && baro1.length >= 1) {
								nodeHeight = BarometerReader.getHeightFromDiff(baro1[0], baro0[0]);
							}

							String format = String.format("Value: %.2f mBar \nDevice height above ground: %.2f m", f[0], nodeHeight);
							ViewTools.updateViewOnUIThread(ctx, tvBaro1Info, format);
							progressDialogBaro1.dismiss();
						}
					});
					// br.addFilter(new LowPassFilter());
				}

				progressDialogBaro1 = ProgressDialog.show(ctx, "Please wait",
						"Hold your device at the height of the node.\n\nRecording measurement after 4 seconds", true);
				br1.startReading();
			}
		});

		// remove preset text on click
		etLandmarks.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				resetEditTextLandmarks();
			}

		});
		// remove preset text on focus, which ever is first
		etLandmarks.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				resetEditTextLandmarks();

			}
		});

		btnLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				LocationStatusCallback cbStatus = new LocationStatusCallback() {

					@Override
					public void status(Location l) {
						String format = String.format("Waiting: lat/lon: %.2f / %.2f\nAccuracy: %.2f\nAltitude: %.2f", l.getLatitude(), l.getLongitude(),
								l.getAccuracy(), l.getAltitude());
						ViewTools.updateViewOnUIThread(ctx, tvLocation, format);
						nodeLocation = l;
					}
				};
				LocationResultCallback cbResult = new LocationResultCallback() {

					@Override
					public void result(Location l) {
						nodeLocation = l;
						if (l != null) {
							String format = String.format("Location: lat/lon: %.2f / %.2f\nAccuracy: %.2f\nAltitude: %.2f", l.getLatitude(), l.getLongitude(),
									l.getAccuracy(), l.getAltitude());
							ViewTools.updateViewOnUIThread(ctx, tvLocation, format);
						} else {
							ViewTools.updateViewOnUIThread(ctx, tvLocation, "Failed to obtain location fix");
						}
					}
				};
				if (gps == null) {
					gps = new GPSWIFIReader(ctx, 20000, 15, cbStatus, cbResult, true, true);
				}
				ViewTools.updateViewOnUIThread(ctx, tvLocation, "Trying to obtain location within the next 20 seconds");
				gps.startReading();
			}
		});

		btnId.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent(ctx, CaptureActivity.class);
					intent.setAction("com.google.zxing.client.android.SCAN");
					// Intent intent = new
					// Intent("com.google.zxing.client.android.SCAN");
					// intent.setPackage("com.google.zxing.client.android");
					intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
					intent.putExtra("RESULT_DISPLAY_DURATION_MS", 10000);
					startActivityForResult(intent, INTENT_SCAN_QR_CODE);
					ViewTools.updateViewOnUIThread(ctx, tvId, "Scanning QR Code");
				} catch (Exception e) {

					ViewTools.updateViewOnUIThread(ctx, tvId, "Failed to launch QRCode Reader");

				}

			}
		});

		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		btnDone.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				returnIntent();
			}
		});

		btnHelp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String helpText = "With this Activity you can collect information about a sensor node."
						+ "\n\nWith the first two buttons you can calculate the height of the sensor node from the ground."
						+ " First measure the pressure as close to the ground as possible."
						+ "Then measure the pressure as close to the node as possible."
						+ " From the pressure difference the height of the node is estimated"
						+ "\n\nWith the scan button you can scan the ID of a node from a QRCode."
						+ " The QRCode content is returned as text via the returned Intent."
						+ " From the QR Code result points the orientation is estimated by reading the orientation of the compass."
						+ "\n\nWith the node orientation button you can hold your device parallel to the node to record the orientation of the node directly, without a QRCode"
						+ "\n\nWith the next set button you can obtain a GPS fix for your node location"
						+ "\n\nDescribe important landmarks to help find your node in the Landmarks text box"
						+ "\n\nTake or select photos which should be associated with this node.";

				IntentTools.showHelpDialog(ctx, "Node Scanner Help", "Back", helpText);
			}
		});
	}

	private void resetEditTextLandmarks() {
		if (etLandmarksOnCreateText.equals(etLandmarks.getText().toString())) {
			etLandmarks.setText("");
			TypedValue tv = new TypedValue();
			ctx.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
			int primaryColor = ctx.getResources().getColor(tv.resourceId);
			etLandmarks.setTextColor(primaryColor);
		}
	}

	private void updateLblPhotoList() {
		ViewTools.updateViewOnUIThread(this, tvPhotos, "Number of photos: " + imagePaths.size());
	}

	private String CREATE_IMAGES_FOLDER;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);
		ctx = this;

		initGUI();
		initGUIClicks();

		rlBottomBox.requestFocus();

		long idAndTimeStamp = getIntent().getLongExtra(IntentExtras.INTENT_EXTRA_TIMESTAMP, System.currentTimeMillis());
		CREATE_IMAGES_FOLDER = DaisyData.IMAGES_FOLDER + idAndTimeStamp + "/";

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		if (br0 != null) {
			br0.stopReading();
		}
		if (br1 != null) {
			br1.stopReading();
		}
		if (cr != null) {
			cr.stopReading();
		}
		if (gps != null) {
			gps.stopReading();
		}
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == RESULT_CANCELED) {
			return;
		}

		switch (requestCode) {
		case INTENT_PICK_IMAGES:
			if (resultCode == RESULT_OK) {
				Uri imagePath = intent.getData();
				if (imagePath != null) {
					String originalFilePath = FileTools.getRealPathFromURI(ctx, imagePath);
					String copyOfFilePath = FileTools.getAndCreateExternalFolder(CREATE_IMAGES_FOLDER) //
							+ etNodeId.getText().toString() + "_" + System.currentTimeMillis() + ".jpg";

					try {
						FileTools.copy(new File(originalFilePath), new File(copyOfFilePath));
					} catch (IOException e) {
						e.printStackTrace();
						Toast.makeText(ctx, "Failed to copy file to deployment folder:\n" + copyOfFilePath, Toast.LENGTH_LONG).show();
						return;
					}

					Bitmap bmp = BitmapTools.loadScaledBitmap(ctx, copyOfFilePath, 128, 128);
					if (bmp != null) {
						ViewTools.addToLinearLayout(ctx, llPhotoList, bmp, copyOfFilePath, removePathCallback);
						imagePaths.add((new File(copyOfFilePath)).getName());
						updateLblPhotoList();
					}

				}
			}
			break;
		case INTENT_TAKE_IMAGE:
			if (resultCode == RESULT_OK) {

				Bitmap bmp = BitmapTools.loadScaledBitmap(ctx, mLastTakenImagePath, 128, 128);
				if (bmp != null) {
					ViewTools.addToLinearLayout(ctx, llPhotoList, bmp, mLastTakenImagePath, removePathCallback);
					imagePaths.add((new File(mLastTakenImagePath)).getName());
					updateLblPhotoList();
				}

			}
			break;
		case INTENT_SCAN_QR_CODE:
			String contents = intent.getStringExtra("SCAN_RESULT");
			String format = intent.getStringExtra(Intents.Scan.RESULT_ORIENTATION);

			// if (!format.equals("QR_CODE")) {
			// ViewTools.updateViewOnUIThread(ctx, tvId,
			// "Scan result was not a QR Code");
			// } else {
			qrValue = contents;

			qrOrientationData = intent.getFloatArrayExtra("COMPASS");
			float[] points = intent.getFloatArrayExtra("POINTS");
			int rotation = intent.getIntExtra(Intents.Scan.RESULT_ORIENTATION, -1);
			// value is in the range [0,360).
			qrBearing = ((int) (rotation + DISPLAY_ROTATION_DEGREES + qrOrientationData[0]));
			qrBearing = qrBearing % 360;

			ResultPoint[] resultPoints;
			String pts = "";
			if (points != null && points.length > 0) {
				resultPoints = new ResultPoint[points.length / 2];
				for (int i = 0; i < points.length; i += 2) {
					resultPoints[i / 2] = new ResultPoint(points[i], points[i + 1]);
					pts += "\n" + points[i] + "/" + points[i + 1];
				}
			}

			int nodeId = NodeDataConverter.getNodeIDFromQRCodeURL(qrValue);
			etNodeId.setText("" + nodeId);

			btnPhotos.setEnabled(true);
			btnPhotoSelect.setEnabled(true);

			String strComp = String.format("Values: %.0f°", qrOrientationData[0]);

			ViewTools.updateViewOnUIThread(ctx, tvId, "Content: " + qrValue + "\nRotation: " + qrBearing + "\nCompass: " + strComp);

			break;
		default:
			// not our request...
		}

	}

	public double getQrRotation(int qrRotation, float[] compassValue) {
		// +90 as zxing lib is not portrait mode
		double d = qrRotation + compassValue[0] + 90;
		d = d % 360;
		return d;
	}

	private void returnIntent() {
		if (nodeLocation == null) {
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

			alertDialog.setPositiveButton("GPS Fix", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					btnLocation.callOnClick();
				}
			});

			alertDialog.setNegativeButton("Cancel", null);
			alertDialog.setMessage("Node without location will not be returned");
			alertDialog.setTitle("Node Scanner");
			alertDialog.show();
			return;
		}
		if (etNodeId.getText().toString().equals("")) {
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

			alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					etNodeId.requestFocus();
				}
			});
			alertDialog.setMessage("Node without a valid ID will not be returned\n\nEither scan a node id or enter one manually.");
			alertDialog.setTitle("Node Scanner");
			alertDialog.show();
			return;
		}

		Intent result = new Intent("de.uvwxy.daisy.SCAN_NODE_RESULT");

		result.putExtra(IntentExtras.QRCODE_DATA, qrValue);
		result.putExtra(IntentExtras.QRCODE_BEARING, qrBearing);
		result.putExtra(IntentExtras.QRCODE_ORIENTATION_DATA, qrOrientationData);
		result.putExtra(IntentExtras.PARALLEL_ORIENTATION_DATA, parallelOrientation);
		result.putExtra(IntentExtras.SELECTED_IMAGE_PATHS, imagePaths.toArray(new String[imagePaths.size()]));
		result.putExtra(IntentExtras.BARO0, baro0);
		result.putExtra(IntentExtras.BARO1, baro1);
		result.putExtra(IntentExtras.BARO_HEIGHT, nodeHeight);
		result.putExtra(IntentExtras.LANDMARKS, getLandmarksText());

		result.putExtra(IntentExtras.LOCATION_LAT, nodeLocation.getLatitude());
		result.putExtra(IntentExtras.LOCATION_LON, nodeLocation.getLongitude());
		result.putExtra(IntentExtras.LOCATION_ALT, nodeLocation.getAltitude());
		result.putExtra(IntentExtras.LOCATION_BEARING, nodeLocation.getBearing());
		result.putExtra(IntentExtras.LOCATION_ACC, nodeLocation.getAccuracy());
		result.putExtra(IntentExtras.LOCATION_TIME, nodeLocation.getTime());
		result.putExtra(IntentExtras.LOCATION_SPEED, nodeLocation.getSpeed());
		result.putExtra(IntentExtras.LOCATION_PROV, nodeLocation.getProvider());

		int tempId = -1;

		try {
			tempId = Integer.parseInt(etNodeId.getText().toString());
		} catch (Exception e) {

		}

		result.putExtra(IntentExtras.INTENT_EXTRA_NODE_ID_INT, tempId);

		setResult(Activity.RESULT_OK, result);
		finish();
	}

	private String getLandmarksText() {
		if (etLandmarks.getText().toString().equals(etLandmarksOnCreateText)) {
			return "";
		} else {
			return etLandmarks.getText().toString();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}
