package org.hcilab.projects.nlogx.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.hcilab.projects.nlogx.R;
import org.hcilab.projects.nlogx.misc.Const;
import org.hcilab.projects.nlogx.misc.DatabaseHelper;
import org.hcilab.projects.nlogx.misc.Util;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Objects;

public class DetailsActivity extends AppCompatActivity {

	public static final String EXTRA_ID = "id";
	public static final String EXTRA_MIN_ID = "minId";
	public static final String EXTRA_DELETE = "delete";

	private static final boolean SHOW_RELATIVE_DATE_TIME = true;

	private long id;
	private long minId;
	private int delete;
	private String packageName;
	private int appUid;
	private AlertDialog dialog;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_details);
		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		if (intent != null) {
			id = intent.getLongExtra(EXTRA_ID, -1);
			minId = intent.getLongExtra(EXTRA_MIN_ID, id);
			delete = intent.getIntExtra(EXTRA_DELETE, -1);
			if (id >= minId && minId >= 0 && delete >= 0) {
				loadDetails(id);
			} else {
				finishWithToast();
			}
		} else {
			finishWithToast();
		}
	}

	@Override
	protected void onPause() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
			dialog = null;
		}
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_delete:
				confirmDelete();
				return true;
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadDetails(long id) {
		JSONObject json = null;
		String str = "error";
		try {
			DatabaseHelper databaseHelper = new DatabaseHelper(this);
			SQLiteDatabase db = databaseHelper.getReadableDatabase();

			Cursor cursor = db.query(DatabaseHelper.PostedEntry.TABLE_NAME,
				new String[] { DatabaseHelper.PostedEntry.COLUMN_NAME_CONTENT },
				DatabaseHelper.PostedEntry._ID + " = ?",
				new String[] { "" + id },
				null,
				null,
				null,
				"1");

			if( cursor != null) {
				if (cursor.getCount() == 1 && cursor.moveToFirst()) {
					try {
						json = new JSONObject(cursor.getString(0));
						str = json.toString(2);
					}
					catch (JSONException e) {
						if (Const.DEBUG) e.printStackTrace();
					}
				}
				cursor.close();
			}

			db.close();
			databaseHelper.close();
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
		TextView tvJSON = findViewById(R.id.json);
		if (Build.VERSION.SDK_INT >= 25)
			tvJSON.setRevealOnFocusHint(false);
		tvJSON.setText(str);

		CardView card = findViewById(R.id.card);
		CardView buttons = findViewById(R.id.buttons);
		if (json != null) {
			packageName = json.optString("packageName");
			if (!TextUtils.isEmpty(packageName)) {
				card.setVisibility(View.VISIBLE);
				ImageView ivIcon = findViewById(R.id.icon);
				Drawable icon = Util.getAppIconFromPackage(this, packageName);
				if (icon != null)
					ivIcon.setImageDrawable(icon);
				else
					ivIcon.setImageResource(android.R.mipmap.sym_def_app_icon);
				TextView tvTitle = findViewById(R.id.title);
				String title   = json.optString("title").trim();
				tvTitle.setText(title.length() == 0 ? "-" : title);
				TextView tvText = findViewById(R.id.text);
				String text = json.optString("text").trim();
				tvText.setText(text.length() == 0 ? "-" : text);
				TextView tvDate = findViewById(R.id.date);
				String appName = Util.getAppNameFromPackage(this, packageName, true);
				StringBuilder sb = new StringBuilder();
				if (appName != null && !appName.equals(packageName))
					sb.append(appName);
				long time = json.optLong("postTime");
				if (time > 0) {
					if (sb.length() > 0)
						sb.append(" · ");
					if (SHOW_RELATIVE_DATE_TIME) {
						sb.append(DateUtils.getRelativeDateTimeString(
							this,
							time,
							DateUtils.MINUTE_IN_MILLIS,
							DateUtils.WEEK_IN_MILLIS,
							DateUtils.FORMAT_ABBREV_RELATIVE | DateUtils.FORMAT_SHOW_TIME)
						);
					} else {
						DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);
						sb.append(format.format(time));
					}
				}
				tvDate.setText(sb.toString());

				try {
					ApplicationInfo app = this.getPackageManager().getApplicationInfo(packageName, 0);
					buttons.setVisibility(View.VISIBLE);
					appUid = app.uid;
				} catch (PackageManager.NameNotFoundException e) {
					if(Const.DEBUG) e.printStackTrace();
					buttons.setVisibility(View.GONE);
				}
			} else {
				card.setVisibility(View.GONE);
			}
		} else {
			card.setVisibility(View.GONE);
			buttons.setVisibility(View.GONE);
		}
	}

	private void finishWithToast() {
		Toast.makeText(getApplicationContext(), R.string.details_error, Toast.LENGTH_SHORT).show();
		finish();
	}

	private void confirmDelete() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}

		dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.delete_dialog_title)
				.setMessage(R.string.delete_dialog_text)
				.setPositiveButton(R.string.delete_dialog_yes, doDelete)
				.setNegativeButton(R.string.delete_dialog_no, null)
				.show();
	}

	private final DialogInterface.OnClickListener doDelete = (dialog, which) -> {
		int affectedRows = 0;
		try {
			DatabaseHelper databaseHelper = new DatabaseHelper(this);
			SQLiteDatabase db = databaseHelper.getWritableDatabase();
			affectedRows = db.delete(DatabaseHelper.PostedEntry.TABLE_NAME,
				DatabaseHelper.PostedEntry._ID + " between ? and ?",
				new String[] { "" + minId, "" + id });
			db.close();
			databaseHelper.close();
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}

		if (affectedRows > 0) {
			Intent data = new Intent();
			data.putExtra(EXTRA_DELETE, delete);
			setResult(RESULT_OK, data);
			finish();
		}
	};

	public void openNotificationSettings(View v) {
		try {
			Intent intent = new Intent();
			if (Build.VERSION.SDK_INT > 25) {
				intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
				intent.putExtra("android.provider.extra.APP_PACKAGE", packageName);
			} else if(Build.VERSION.SDK_INT >= 21) {
				intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
				intent.putExtra("app_package", packageName);
				intent.putExtra("app_uid", appUid);
			} else {
				intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				intent.setData(Uri.parse("package:" + packageName));
			}
			startActivity(intent);
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
	}

}
