package org.hcilab.projects.nlogx.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import org.hcilab.projects.nlogx.misc.Const;
import org.hcilab.projects.nlogx.misc.Util;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Arrays;

class NotificationObject {
	private final JSONObject json;

	NotificationObject(Context context, StatusBarNotification sbn, final boolean LOG_TEXT, int reason) {
		JSONObject o = new JSONObject();
		try {
			o.put("id", sbn.getId());
			String key = null;
			if (Build.VERSION.SDK_INT >= 20) {
				key = sbn.getKey();
				o.put("key", str(key));
			}
			Notification n = sbn.getNotification();
			if (n.extras != null) {
				o.putOpt("title", n.extras.getCharSequence(NotificationCompat.EXTRA_TITLE));
				o.putOpt("text", n.extras.getCharSequence(NotificationCompat.EXTRA_TEXT));
			}
			String packageName = sbn.getPackageName();
			if (packageName != null) {
				o.putOpt("appName", Util.getAppNameFromPackage(context, packageName, true));
				o.put("packageName", packageName);
			}
			if (Build.VERSION.SDK_INT >= 29) o.put("opPkg", str(sbn.getOpPkg()));
			if (Build.VERSION.SDK_INT >= 29) o.put("uid", sbn.getUid());
			if (Build.VERSION.SDK_INT >= 21) o.put("user", str(sbn.getUser()));
			else                             o.put("userId", sbn.getUserId());
			if (Build.VERSION.SDK_INT >= 24) o.put("group", sbn.isGroup());
			if (Build.VERSION.SDK_INT >= 30) o.put("appGroup", sbn.isAppGroup());
			if (Build.VERSION.SDK_INT >= 21) o.put("groupKey", str(sbn.getGroupKey()));
			if (Build.VERSION.SDK_INT >= 24) o.put("overrideGroupKey", str(sbn.getOverrideGroupKey()));
			o.put("ongoing", sbn.isOngoing());
			o.put("clearable", sbn.isClearable());
			o.put("tag", str(sbn.getTag()));
			o.put("postTime", sbn.getPostTime());
			o.put("logTime", System.currentTimeMillis());

			JSONObject no = new JSONObject();
			o.put("notification", no);
			no.put("when", n.when);
			no.put("iconLevel", n.iconLevel);
			if (Build.VERSION.SDK_INT < 23) {
				no.put("icon", n.icon);
				no.put("largeIcon", str(n.largeIcon));
			}
			else {
				no.put("smallIcon", str(n.getSmallIcon()));
				no.put("largeIcon", str(n.getLargeIcon()));
			}
			if (Build.VERSION.SDK_INT >= 21) no.put("color", n.color);
			if (Build.VERSION.SDK_INT >= 26) no.put("badgeIconType", n.getBadgeIconType());
			if (Build.VERSION.SDK_INT >= 29) no.put("bubbleMetadata", str(n.getBubbleMetadata()));
			if (Build.VERSION.SDK_INT >= 20) no.put("group", str(n.getGroup()));
			if (Build.VERSION.SDK_INT >= 21) no.put("category", str(n.category));
			if (Build.VERSION.SDK_INT >= 21) no.put("visibility", n.visibility);
			if (Build.VERSION.SDK_INT >= 26) no.put("channelId", str(n.getChannelId()));
			no.put("flags", n.flags);
			no.put("groupSummary", NotificationCompat.isGroupSummary(n));
			no.put("localOnly", NotificationCompat.getLocalOnly(n));
			//no.put("actions", n.actions == null ? JSONObject.NULL : Arrays.toString(n.actions));
			no.put("actionCount", NotificationCompat.getActionCount(n));
			no.put("contentIntent", str(n.contentIntent));
			no.put("deleteIntent", str(n.deleteIntent));
			no.put("fullScreenIntent", str(n.fullScreenIntent));
			if (Build.VERSION.SDK_INT < 24) {
				no.put("contentView", str(n.contentView));
				no.put("bigContentView", str(n.bigContentView));
				if (Build.VERSION.SDK_INT >= 21)
					no.put("headsUpContentView", str(n.headsUpContentView));
				else
					no.put("tickerView", str(n.tickerView));
			}
			if (LOG_TEXT) {
				no.put("tickerText", str(n.tickerText));
			}
			no.put("number", n.number);
			if (Build.VERSION.SDK_INT < 26) {
				no.put("priority", n.priority);
				no.put("defaults", n.defaults);
				no.put("sound", str(n.sound));
				if (Build.VERSION.SDK_INT < 21)
					no.put("audioStreamType", n.audioStreamType);
				else
					no.put("audioAttributes", str(n.audioAttributes));
				no.put("vibrate", str(n.vibrate == null ? null : Arrays.toString(n.vibrate)));
				no.put("ledARGB", n.ledARGB);
				no.put("ledOnMS", n.ledOnMS);
				no.put("ledOffMS", n.ledOffMS);
			}
			if (Build.VERSION.SDK_INT >= 20) no.put("sortKey", str(n.getSortKey()));
			if (Build.VERSION.SDK_INT >= 26) no.put("timeoutAfter", n.getTimeoutAfter());
			if (Build.VERSION.SDK_INT >= 26) no.put("groupAlertBehavior", n.getGroupAlertBehavior());
			if (Build.VERSION.SDK_INT >= 26) no.put("shortcutId", str(n.getShortcutId()));
			if (Build.VERSION.SDK_INT >= 29) no.put("locusId", str(n.getLocusId()));
			if (LOG_TEXT) {
				if (Build.VERSION.SDK_INT >= 26) no.put("settingsText", str(n.getSettingsText()));
			}
			if (n.extras != null) {
				JSONObject eo = new JSONObject();
				no.put("extras", eo);
				String[] tmp = n.extras.getStringArray(NotificationCompat.EXTRA_PEOPLE);
				eo.putOpt("people", tmp != null ? Arrays.toString(tmp) : null);
				eo.putOpt("template", n.extras.getString(NotificationCompat.EXTRA_TEMPLATE));
				if (LOG_TEXT) {
					eo.putOpt("title", n.extras.getCharSequence(NotificationCompat.EXTRA_TITLE));
					eo.putOpt("title.big", n.extras.getCharSequence(NotificationCompat.EXTRA_TITLE_BIG));
					eo.putOpt("text", n.extras.getCharSequence(NotificationCompat.EXTRA_TEXT));
					eo.putOpt("subText", n.extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT));
					eo.putOpt("infoText", n.extras.getCharSequence(NotificationCompat.EXTRA_INFO_TEXT));
					eo.putOpt("summaryText", n.extras.getCharSequence(NotificationCompat.EXTRA_SUMMARY_TEXT));
					eo.putOpt("bigText", n.extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT));
					CharSequence[] lines = n.extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES);
					if(lines != null) {
						StringBuilder sb = new StringBuilder();
						for (CharSequence line : lines)
							sb.append(line).append('\n');
						eo.put("textLines", sb.toString());
					}
				}
			}

			if (Build.VERSION.SDK_INT >= 21) {
				JSONObject so = new JSONObject();
				o.put("service", so);
				so.put("listenerHints", NotificationListener.getListenerHints());
				so.put("interruptionFilter", NotificationListener.getInterruptionFilter());
				NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
				NotificationListenerService.RankingMap rankingMap = NotificationListener.getRanking();
				if (rankingMap != null && rankingMap.getRanking(key, ranking)) {
					JSONObject ro = new JSONObject();
					o.put("ranking", ro);
					ro.put("key", str(ranking.getKey()));
					ro.put("rank", ranking.getRank());
					ro.put("ambient", ranking.isAmbient());
					if (Build.VERSION.SDK_INT >= 24) {
						int importance = ranking.getImportance();
						try {
							Method m = NotificationListenerService.Ranking.class.getMethod("importanceToString", Integer.TYPE);
							ro.put("importance", str(m.invoke(null, importance)));
						}
						catch (Exception e) {
							ro.put("importance", importance);
						}
					}
					if (Build.VERSION.SDK_INT >= 24) ro.put("suppressedVisualEffects", ranking.getSuppressedVisualEffects());
					if (Build.VERSION.SDK_INT >= 24) ro.put("overrideGroupKey", str(ranking.getOverrideGroupKey()));
					if (Build.VERSION.SDK_INT >= 26) ro.put("showBadge", ranking.canShowBadge());
					if (Build.VERSION.SDK_INT >= 28) ro.put("userSentiment", ranking.getUserSentiment());
					if (Build.VERSION.SDK_INT >= 28) ro.put("suspended", ranking.isSuspended());
					if (Build.VERSION.SDK_INT >= 29) ro.put("bubble", ranking.canBubble());
					if (Build.VERSION.SDK_INT >= 26) {
						NotificationChannel channel = ranking.getChannel();
						if (channel != null) {
							try {
								Method m = NotificationChannel.class.getMethod("toJson");
								ro.put("channel", new JSONObject(m.invoke(channel).toString()));
							}
							catch (Exception e) {
								ro.put("channel", channel);
							}
						}
						else
							ro.put("channel", JSONObject.NULL);
					}
				}
			}

			if (reason >= 0)
				o.put("removeReason", reason);
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
		json = o;
	}

	private static Object str(Object s) {
		return s == null ? JSONObject.NULL : s.toString();
	}

	@Override
	public String toString() {
		return json.toString();
	}
}
