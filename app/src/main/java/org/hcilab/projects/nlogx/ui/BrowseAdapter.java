package org.hcilab.projects.nlogx.ui;

import android.app.Activity;
import android.content.Context;
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
import java.util.Locale;

class BrowseAdapter extends RecyclerView.Adapter<BrowseViewHolder> {

	private final static int LIMIT = Integer.MAX_VALUE;
	private final static String PAGE_SIZE = "20";

	private DateFormat format = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());

	private Activity context;
	private ArrayList<DataItem> data = new ArrayList<>();
	private HashMap<String, Drawable> iconCache = new HashMap<>();
	private Handler handler = new Handler();

	private String lastDate = "";
	private DataItem lastItem = null;
	private boolean shouldLoadMore = true;

	BrowseAdapter(Activity context) {
		this.context = context;
		loadMore(Integer.MAX_VALUE);
	}

	@NonNull
	@Override
	public BrowseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_browse, parent, false);
		BrowseViewHolder vh = new BrowseViewHolder(view);
		vh.item.setOnClickListener(v -> {
			String id = (String) v.getTag();
			if(id != null) {
				Intent intent = new Intent(context, DetailsActivity.class);
				intent.putExtra(DetailsActivity.EXTRA_ID, id);
				if(Build.VERSION.SDK_INT >= 21) {
					Pair<View, String> p1 = Pair.create(vh.icon, "icon");
					@SuppressWarnings("unchecked") ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(context, p1);
					context.startActivityForResult(intent, 1, options.toBundle());
				} else {
					context.startActivityForResult(intent, 1);
				}
			}
		});
		return vh;
	}

	@Override
	public void onBindViewHolder(@NonNull BrowseViewHolder vh, int position) {
		DataItem item = data.get(position);

		String packageName = item.getPackageName();
		if(!iconCache.containsKey(packageName)) {
			iconCache.put(packageName, Util.getAppIconFromPackage(context, packageName));
		}
		if(iconCache.get(packageName) != null) {
			vh.icon.setImageDrawable(iconCache.get(packageName));
		} else {
			vh.icon.setImageResource(R.mipmap.ic_launcher);
		}

		vh.item.setTag("" + item.getId());
		vh.title.setText(item.getTitle());
		vh.text.setText(item.getText());

		if(item.shouldShowDate()) {
			vh.date.setVisibility(View.VISIBLE);
			vh.date.setText(item.getDate());
		} else {
			vh.date.setVisibility(View.GONE);
		}

		if(position == getItemCount() - 1) {
			loadMore(item.getId());
		}
	}

	@Override
	public int getItemCount() {
		return data.size();
	}

	private void loadMore(long afterId) {
		if(!shouldLoadMore) {
			if(Const.DEBUG) System.out.println("not loading more items");
			return;
		}

		if(Const.DEBUG) System.out.println("loading more items");
		int before = getItemCount();
		try {
			DatabaseHelper databaseHelper = new DatabaseHelper(context);
			SQLiteDatabase db = databaseHelper.getReadableDatabase();

			Cursor cursor = db.query(DatabaseHelper.PostedEntry.TABLE_NAME,
					new String[] {
							DatabaseHelper.PostedEntry._ID,
							DatabaseHelper.PostedEntry.COLUMN_NAME_CONTENT
					},
					DatabaseHelper.PostedEntry._ID + " < ?",
					new String[] {""+afterId},
					null,
					null,
					DatabaseHelper.PostedEntry._ID + " DESC",
					PAGE_SIZE);

			if(cursor != null && cursor.moveToFirst()) {
				for(int i = 0; i < cursor.getCount(); i++) {
					DataItem dataItem = new DataItem(context, cursor.getLong(0), cursor.getString(1));

					if(TextUtils.equals(lastDate, dataItem.getDate()))
						dataItem.setShowDate(false);
					else
						lastDate = dataItem.getDate();

					if (lastItem == null ||
						!TextUtils.equals(dataItem.getPackageName(), lastItem.getPackageName()) ||
						!TextUtils.equals(dataItem.getTitle(), lastItem.getTitle()) ||
						!TextUtils.equals(dataItem.getText(), lastItem.getText()) ||
						!TextUtils.equals(dataItem.getDate(), lastItem.getDate())) {
						data.add(dataItem);
						lastItem = dataItem;
					}

					cursor.moveToNext();
				}
				cursor.close();
			}

			db.close();
			databaseHelper.close();
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
		int after = getItemCount();

		if(before == after) {
			if(Const.DEBUG) System.out.println("no new items loaded: " + getItemCount());
			shouldLoadMore = false;
		}

		if(getItemCount() > LIMIT) {
			if(Const.DEBUG) System.out.println("reached the limit, not loading more items: " + getItemCount());
			shouldLoadMore = false;
		}

		handler.post(() -> notifyDataSetChanged());
	}

	private class DataItem {

		private long id;
		private String packageName;
		private String title;
		private String text;
		private String date;
		private boolean showDate;

		DataItem(Context context, long id, String str) {
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
				date = format.format(json.optLong("postTime"));
				showDate = true;
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
			return date;
		}

		public boolean shouldShowDate() {
			return showDate;
		}

		public void setShowDate(boolean showDate) {
			this.showDate = showDate;
		}

	}

}
