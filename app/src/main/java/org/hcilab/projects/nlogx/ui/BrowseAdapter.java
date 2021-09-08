package org.hcilab.projects.nlogx.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import org.hcilab.projects.nlogx.R;
import org.hcilab.projects.nlogx.misc.Const;
import org.hcilab.projects.nlogx.misc.DatabaseHelper;
import org.hcilab.projects.nlogx.misc.Util;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;

class BrowseAdapter extends RecyclerView.Adapter<BrowseViewHolder> {

	private final static int LIMIT = Integer.MAX_VALUE;
	private final static String PAGE_SIZE = "20";

	private final Activity context;
	private final ArrayList<DataItem> data = new ArrayList<>();
	private final HashMap<String, Drawable> iconCache = new HashMap<>();
	private final Handler handler = new Handler();

	BrowseAdapter(Activity context) {
		this.context = context;
		setHasStableIds(true);
		loadMore();
	}

	@NonNull
	@Override
	public BrowseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_browse, parent, false);
		BrowseViewHolder vh = new BrowseViewHolder(view);
		vh.item.setOnClickListener(v -> {
			Intent intent = new Intent(context, DetailsActivity.class);
			intent.putExtra(DetailsActivity.EXTRA_ID, vh.getItemId());
			intent.putExtra(DetailsActivity.EXTRA_DELETE, vh.getBindingAdapterPosition());
			if (Build.VERSION.SDK_INT >= 21) {
				Pair<View, String> p1 = Pair.create(vh.icon, "icon");
				@SuppressWarnings("unchecked") ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(context, p1);
				context.startActivityForResult(intent, 1, options.toBundle());
			} else {
				context.startActivityForResult(intent, 1);
			}
		});
		return vh;
	}

	@Override
	public void onBindViewHolder(@NonNull BrowseViewHolder vh, int position) {
		DataItem item = data.get(position);

		String packageName = item.getPackageName();
		if (!iconCache.containsKey(packageName)) {
			iconCache.put(packageName, Util.getAppIconFromPackage(context, packageName));
		}
		if (iconCache.get(packageName) != null) {
			vh.icon.setImageDrawable(iconCache.get(packageName));
		} else {
			vh.icon.setImageResource(R.mipmap.ic_launcher);
		}

		vh.title.setText(item.getTitle());
		vh.text.setText(item.getText());

		if (item.shouldShowDate()) {
			vh.date.setVisibility(View.VISIBLE);
			vh.date.setText(item.getDate());
		} else {
			vh.date.setVisibility(View.GONE);
		}

		if (position == getItemCount() - 1) {
			handler.post(this::loadMore);
		}
	}

	@Override
	public int getItemCount() {
		return data.size();
	}

	@Override
	public long getItemId(int position) {
		return data.get(position).getId();
	}

	public void remove(int position) {
		data.remove(position);
		notifyItemRemoved(position);
	}

	private void loadMore() {
		if (getItemCount() > LIMIT) {
			if(Const.DEBUG) System.out.println("reached the limit, not loading more items: " + getItemCount());
			return;
		}

		int before = data.size();
		try {
			DatabaseHelper databaseHelper = new DatabaseHelper(context);
			SQLiteDatabase db = databaseHelper.getReadableDatabase();

			DateFormat format = DateFormat.getDateInstance();
			DataItem lastItem = data.isEmpty() ? null : data.get(data.size() - 1);
			while (true) {
				Cursor cursor = db.query(DatabaseHelper.PostedEntry.TABLE_NAME,
					new String[] {
						DatabaseHelper.PostedEntry._ID,
						DatabaseHelper.PostedEntry.COLUMN_NAME_CONTENT
					},
					DatabaseHelper.PostedEntry._ID + " < ?",
					new String[] { "" + (lastItem == null ? Long.MAX_VALUE : lastItem.getId()) },
					null,
					null,
					DatabaseHelper.PostedEntry._ID + " DESC",
					PAGE_SIZE);

				if (cursor == null)
					break;

				if (!cursor.moveToFirst()) {
					cursor.close();
					break;
				}

				do {
					DataItem dataItem = new DataItem(cursor.getLong(0), cursor.getString(1), format);
					if (lastItem == null ||
						!TextUtils.equals(dataItem.getDate(), lastItem.getDate())) {
						dataItem.setShowDate(true);
					}
					if (dataItem.shouldShowDate() ||
						!TextUtils.equals(dataItem.getPackageName(), lastItem.getPackageName()) ||
						!TextUtils.equals(dataItem.getTitle(), lastItem.getTitle()) ||
						!TextUtils.equals(dataItem.getText(), lastItem.getText())) {
						data.add(dataItem);
						lastItem = dataItem;
					} else {
						lastItem.id = dataItem.getId();
					}
				} while (cursor.moveToNext());
				cursor.close();

				if (data.size() > before)
					break;
			}

			db.close();
			databaseHelper.close();
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}

		notifyItemRangeInserted(before, data.size() - before);
	}

	private static class DataItem {

		private long id;
		private String packageName;
		private String title;
		private String text;
		private String date;
		private boolean showDate;

		DataItem(long id, String str, DateFormat format) {
			this.id = id;
			try {
				JSONObject json = new JSONObject(str);
				packageName = json.getString("packageName");
				title = json.optString("title").trim();
				if (title.length() > 100)
					title = title.substring(0, 100);
				text = json.optString("text").trim();
				if (text.length() > 200)
					text = text.substring(0, 200);
				long time = json.optLong("postTime");
				date = time > 0 ? format.format(time) : "";
				showDate = false;
			} catch (JSONException e) {
				if(Const.DEBUG) e.printStackTrace();
			}
		}

		public long getId() {
			return id;
		}

		public String getPackageName() {
			return packageName;
		}

		public String getTitle() {
			return title.length() == 0 ? "-" : title;
		}

		public String getText() {
			return text.length() == 0 ? "-" : text;
		}

		public String getDate() {
			return date.length() == 0 ? "-" : date;
		}

		public boolean shouldShowDate() {
			return showDate;
		}

		public void setShowDate(boolean showDate) {
			this.showDate = showDate;
		}

	}

}
