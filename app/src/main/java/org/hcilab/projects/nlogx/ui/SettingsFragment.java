package org.hcilab.projects.nlogx.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.hcilab.projects.nlogx.BuildConfig;
import org.hcilab.projects.nlogx.R;
import org.hcilab.projects.nlogx.misc.Const;
import org.hcilab.projects.nlogx.misc.DatabaseHelper;
import org.hcilab.projects.nlogx.misc.Util;
import org.hcilab.projects.nlogx.service.NotificationHandler;

public class SettingsFragment extends PreferenceFragmentCompat {
	private BroadcastReceiver updateReceiver;

	private Preference prefStatus;
	private Preference prefBrowse;
	private Preference prefText;
	private Preference prefOngoing;

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.preferences);

		PreferenceManager pm = getPreferenceManager();

		prefStatus = pm.findPreference(Const.PREF_STATUS);
		if(prefStatus != null) {
			prefStatus.setOnPreferenceClickListener(preference -> {
				startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
				return true;
			});
		}

		prefBrowse = pm.findPreference(Const.PREF_BROWSE);
		if(prefBrowse != null) {
			prefBrowse.setOnPreferenceClickListener(preference -> {
				requireActivity().finish();
				return true;
			});
		}

		prefText    = pm.findPreference(Const.PREF_TEXT);
		prefOngoing = pm.findPreference(Const.PREF_ONGOING);

		Preference prefAbout = pm.findPreference(Const.PREF_ABOUT);
		if(prefAbout != null) {
			prefAbout.setOnPreferenceClickListener(preference -> {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://github.com/azuo/android-notification-log"));
				startActivity(intent);
				return true;
			});
		}

		Preference prefVersion = pm.findPreference(Const.PREF_VERSION);
		if(prefVersion != null) {
			prefVersion.setSummary(BuildConfig.VERSION_NAME + (Const.DEBUG ? " dev" : ""));
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		updateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				update();
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();

		if(Util.isNotificationAccessEnabled(getActivity())) {
			prefStatus.setSummary(R.string.settings_notification_access_enabled);
			prefBrowse.setEnabled(true);
			prefText.setEnabled(true);
			prefOngoing.setEnabled(true);
		} else {
			prefStatus.setSummary(R.string.settings_notification_access_disabled);
			prefBrowse.setEnabled(false);
			prefText.setEnabled(false);
			prefOngoing.setEnabled(false);
		}

		IntentFilter filter = new IntentFilter();
		filter.addAction(NotificationHandler.BROADCAST);
		LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(updateReceiver, filter);

		update();
	}

	@Override
	public void onPause() {
		LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(updateReceiver);
		super.onPause();
	}

	private void update() {
		try {
			DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			long numRowsPosted = DatabaseUtils.queryNumEntries(db, DatabaseHelper.PostedEntry.TABLE_NAME);
			db.close();
			dbHelper.close();
			prefBrowse.setSummary(String.valueOf(numRowsPosted));
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
	}

}