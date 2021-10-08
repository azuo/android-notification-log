package org.hcilab.projects.nlogx.service;

import android.annotation.TargetApi;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.hcilab.projects.nlogx.misc.Const;

public class NotificationListener extends NotificationListenerService {

	private static NotificationListener instance = null;

	@Override
	public void onCreate() {
		super.onCreate();
		if (Build.VERSION.SDK_INT < 21) {
			instance = this;
		}
	}

	@Override
	public void onDestroy() {
		if (Build.VERSION.SDK_INT < 21) {
			instance = null;
		}
		super.onDestroy();
	}

	@Override
	public void onListenerConnected() {
		super.onListenerConnected();
		if (Build.VERSION.SDK_INT >= 21) {
			instance = this;
		}
	}

	@Override
	public void onListenerDisconnected() {
		if (Build.VERSION.SDK_INT >= 21) {
			instance = null;
		}
		super.onListenerDisconnected();
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		try {
			NotificationHandler notificationHandler = new NotificationHandler(this);
			notificationHandler.handlePosted(sbn);
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		try {
			NotificationHandler notificationHandler = new NotificationHandler(this);
			notificationHandler.handleRemoved(sbn, -1);
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
		try {
			NotificationHandler notificationHandler = new NotificationHandler(this);
			notificationHandler.handleRemoved(sbn, reason);
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
	}

	public static StatusBarNotification[] getAllActiveNotifications() {
		if(instance != null) {
			try {
				return instance.getActiveNotifications();
			} catch (Exception e) {
				if(Const.DEBUG) e.printStackTrace();
			}
		}
		return null;
	}

	@TargetApi(Build.VERSION_CODES.O)
	public static StatusBarNotification[] getAllSnoozedNotifications() {
		if(instance != null) {
			try {
				return instance.getSnoozedNotifications();
			} catch (Exception e) {
				if(Const.DEBUG) e.printStackTrace();
			}
		}
		return null;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static int getInterruptionFilter() {
		if(instance != null) {
			try {
				return instance.getCurrentInterruptionFilter();
			} catch (Exception e) {
				if(Const.DEBUG) e.printStackTrace();
			}
		}
		return -1;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static int getListenerHints() {
		if(instance != null) {
			try {
				return instance.getCurrentListenerHints();
			} catch (Exception e) {
				if(Const.DEBUG) e.printStackTrace();
			}
		}
		return -1;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static NotificationListenerService.RankingMap getRanking() {
		if(instance != null) {
			try {
				return instance.getCurrentRanking();
			} catch (Exception e) {
				if(Const.DEBUG) e.printStackTrace();
			}
		}
		return null;
	}
}
