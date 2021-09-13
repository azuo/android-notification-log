package org.hcilab.projects.nlogx.ui;

import android.app.AppOpsManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.hcilab.projects.nlogx.R;
import org.hcilab.projects.nlogx.misc.Const;
import org.hcilab.projects.nlogx.misc.Util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

@RequiresApi(28)
public class StandbyAppsActivity extends AppCompatActivity
		implements SwipeRefreshLayout.OnRefreshListener, SearchView.OnQueryTextListener {

	private SwipeRefreshLayout swipeRefreshLayout;
	private RecyclerView recyclerView;
	private SearchView searchView;
	private TextView emptyView;

	private final HashMap<String, Drawable> iconCache = new HashMap<>();

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browse);
		if (!Intent.ACTION_VIEW.equals(getIntent().getAction()))
			Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

		swipeRefreshLayout = findViewById(R.id.swiper);
		swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
		swipeRefreshLayout.setOnRefreshListener(this);

		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
		recyclerView = findViewById(R.id.list);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(layoutManager);

		emptyView = findViewById(R.id.empty);
		emptyView.setText(R.string.usage_access_disabled);
		emptyView.setOnClickListener(v -> startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 1));

		update();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		update();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.standby, menu);
		searchView = (SearchView)menu.findItem(R.id.menu_search).getActionView();
		searchView.setOnQueryTextListener(this);
		// remove space on the left side of SearchView
		View f = searchView.findViewById(androidx.appcompat.R.id.search_edit_frame);
		if (f != null) {
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)f.getLayoutParams();
			params.leftMargin = (int)(-8.0f * getResources().getDisplayMetrics().density + 0.5f);
			f.setLayoutParams(params);
		}
		// if launched from shortcut
		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			ActionBar bar = Objects.requireNonNull(getSupportActionBar());
			bar.setDisplayHomeAsUpEnabled(!searchView.isIconified());
			searchView.setOnSearchClickListener(v -> bar.setDisplayHomeAsUpEnabled(true));
			searchView.setOnCloseListener(() -> {
				bar.setDisplayHomeAsUpEnabled(false);
				return false;
			});
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			//case R.id.menu_refresh:
			//	update();
			//	return true;
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void update() {
		Adapter adapter = new Adapter();
		recyclerView.setAdapter(adapter);

		if (!adapter.isEnabled()) {
			recyclerView.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
		}
		else {
			recyclerView.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.GONE);
			if (searchView != null) {
				CharSequence query = searchView.getQuery();
				if (!TextUtils.isEmpty(query))
					adapter.applyFilter(query.toString());
			}
		}
	}

	@Override
	public void onRefresh() {
		update();
		swipeRefreshLayout.setRefreshing(false);
	}

	@Override
	public void onBackPressed() {
		if (!searchView.isIconified()) {
			//searchView.onActionViewCollapsed();
			if (!TextUtils.isEmpty(searchView.getQuery()))
				searchView.setQuery("", false);
			searchView.setIconified(true);
		}
		else
			super.onBackPressed();
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		((Adapter)Objects.requireNonNull(recyclerView.getAdapter())).applyFilter(newText);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		searchView.clearFocus();
		return true;
	}

	private class Adapter extends RecyclerView.Adapter<BrowseViewHolder> {
		private List<DataItem> data = null;
		private List<DataItem> filtered = null;
		private boolean enabled = false;

		@SuppressWarnings("unchecked")
		public Adapter() {
			setHasStableIds(true);

			AppOpsManager appOpsManager = (AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
			if (appOpsManager == null)
				return;
			int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
			if (mode != AppOpsManager.MODE_ALLOWED)
				return;

			enabled = true;
			Map<String, Integer> b = null;
			try {
				UsageStatsManager m = (UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
				b = (Map<String, Integer>)m.getClass().getMethod("getAppStandbyBuckets").invoke(m);
			}
			catch (Throwable e) {
				if (Const.DEBUG) e.printStackTrace();
				if (e instanceof InvocationTargetException)
					e = ((InvocationTargetException)e).getTargetException();
				Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
			}
			if (b == null)
				return;

			//TreeMap<String, Integer> m = new TreeMap<>(b);	// not work since b may contain null keys?!
			TreeMap<String, Integer> m = new TreeMap<>();
			for (Map.Entry<String, Integer> e : b.entrySet()) {
				if (e.getKey() != null)
					m.put(e.getKey(), e.getValue());
			}
			data = new ArrayList<>(m.size());
			for (Map.Entry<String, Integer> e : m.entrySet()) {
				DataItem item = new DataItem(data.size(), e.getKey());
				switch (e.getValue()) {
					case 5: //UsageStatsManager.STANDBY_BUCKET_EXEMPTED
						item.setBucket("EXEMPTED");
						break;
					case android.app.usage.UsageStatsManager.STANDBY_BUCKET_ACTIVE:
						item.setBucket("ACTIVE");
						break;
					case android.app.usage.UsageStatsManager.STANDBY_BUCKET_WORKING_SET:
						item.setBucket("WORKING_SET");
						break;
					case android.app.usage.UsageStatsManager.STANDBY_BUCKET_FREQUENT:
						item.setBucket("FREQUENT");
						break;
					case android.app.usage.UsageStatsManager.STANDBY_BUCKET_RARE:
						item.setBucket("RARE");
						break;
					case 50: //UsageStatsManager.STANDBY_BUCKET_NEVER
						item.setBucket("NEVER");
						break;
					default:
						item.setBucket("" + e.getValue());
						break;
				}
				data.add(item);
			}
			filtered = data;

			// lazy load appName in a background thread
			new Thread() {
				@Override
				public void run() {
					List<DataItem> d = data;
					if (d != null) {
						for (DataItem item : d)
							item.getAppName();
					}
				}
			}.start();
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void applyFilter(String query) {
			String q = query.trim().toLowerCase();
			if (q.length() == 0)
				filtered = data;
			else if (data != null){
				filtered = new ArrayList<>(data.size());
				for (DataItem item : data) {
					if (item.getPackageName().toLowerCase().contains(q) ||
						item.getAppName().toLowerCase().contains(q) ||
						item.getBucket().toLowerCase().contains(q)
					) {
						filtered.add(item);
					}
				}
			}
			notifyDataSetChanged();
		}

		@NonNull
		@Override
		public BrowseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_browse, parent, false);
			BrowseViewHolder vh = new BrowseViewHolder(view);
			vh.item.setOnClickListener(v -> {
				String packageName = (String)v.getTag();
				if (Util.getAppNameFromPackage(StandbyAppsActivity.this, packageName, true) != null) {
					Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					i.addCategory(Intent.CATEGORY_DEFAULT);
					i.setData(Uri.parse("package:" + packageName));
					startActivity(i);
				}
				else
					Toast.makeText(getApplicationContext(), R.string.app_not_found, Toast.LENGTH_LONG).show();
			});
			return vh;
		}

		@Override
		public void onBindViewHolder(@NonNull BrowseViewHolder vh, int position) {
			DataItem item  = filtered.get(position);

			String packageName = item.getPackageName();
			if (!iconCache.containsKey(packageName)) {
				iconCache.put(packageName, Util.getAppIconFromPackage(StandbyAppsActivity.this, packageName));
			}
			if (iconCache.get(packageName) != null) {
				vh.icon.setImageDrawable(iconCache.get(packageName));
			} else {
				vh.icon.setImageResource(android.R.mipmap.sym_def_app_icon);
			}

			vh.item.setTag(packageName);

			String appName = item.getAppName();
			vh.title.setText(appName);
			vh.text.setText(appName.equals(packageName) ? item.getBucket() : item.getBucket() + "\n" + packageName);
		}

		@Override
		public int getItemCount() {
			return filtered == null ? 0 : filtered.size();
		}

		@Override
		public long getItemId(int position) {
			return filtered.get(position).getId();
		}
	}

	private class DataItem {
		private final long id;
		private final String packageName;
		private String appName;
		private String bucket;

		public DataItem(long id, String packageName) {
			this.id = id;
			this.packageName = packageName;
		}

		public long getId() {
			return id;
		}

		public String getPackageName() {
			return packageName;
		}

		public String getAppName() {
			// lazy fetch appName
			if (appName == null)
				appName = Util.getAppNameFromPackage(StandbyAppsActivity.this, packageName, false);
			return appName;
		}

		public void setBucket(String bucket) {
			this.bucket = bucket;
		}

		public String getBucket() {
			return bucket;
		}
	}
}
