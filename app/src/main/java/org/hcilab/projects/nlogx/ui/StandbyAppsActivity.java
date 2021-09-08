package org.hcilab.projects.nlogx.ui;

import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
		recyclerView.setLayoutManager(layoutManager);

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
		recyclerView.setAdapter(new Adapter());
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
		((Adapter)Objects.requireNonNull(recyclerView.getAdapter())).applyFilter(newText.trim());
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return true;
	}

	private class Adapter extends RecyclerView.Adapter<BrowseViewHolder> {
		private List<String[]> buckets = null;
		private List<String[]> data;

		@SuppressWarnings("unchecked")
		public Adapter() {
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
				finish();
			}
			if (b != null) {
				buckets = new ArrayList<>(b.size());
				for (Map.Entry<String, Integer> e : new TreeMap<>(b).entrySet()) {
					String[] v = new String[3];
					v[0] = e.getKey();
					//v[1] = Util.getAppNameFromPackage(StandbyAppsActivity.this, v[0], false);
					switch (e.getValue()) {
						case 5: //UsageStatsManager.STANDBY_BUCKET_EXEMPTED
							v[2] = "EXEMPTED";
							break;
						case android.app.usage.UsageStatsManager.STANDBY_BUCKET_ACTIVE:
							v[2] = "ACTIVE";
							break;
						case android.app.usage.UsageStatsManager.STANDBY_BUCKET_WORKING_SET:
							v[2] = "WORKING_SET";
							break;
						case android.app.usage.UsageStatsManager.STANDBY_BUCKET_FREQUENT:
							v[2] = "FREQUENT";
							break;
						case android.app.usage.UsageStatsManager.STANDBY_BUCKET_RARE:
							v[2] = "RARE";
							break;
						case 50: //UsageStatsManager.STANDBY_BUCKET_NEVER
							v[2] = "NEVER";
							break;
						default:
							v[2] = "" + e.getValue();
							break;
					}
					buckets.add(v);
				}
				// call getAppNameFromPackage in a background thread
				new Thread() {
					@Override
					public void run() {
						List<String[]> b = buckets;
						if (b != null) {
							for (String[] v : b) {
								if (v[1] == null)
									v[1] = Util.getAppNameFromPackage(StandbyAppsActivity.this, v[0], false);
							}
						}
					}
				}.start();
			}
			data = buckets;
		}

		public void applyFilter(String query) {
			if (query.length() == 0)
				data = buckets;
			else if (buckets != null){
				String q = query.toLowerCase();
				data = new ArrayList<>(buckets.size());
				for (String[] v : buckets) {
					if (v[1] == null)
						v[1] = Util.getAppNameFromPackage(StandbyAppsActivity.this, v[0], false);
					for (String s : v) {
						if (s != null && s.toLowerCase().contains(q)) {
							data.add(v);
							break;
						}
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
					Toast.makeText(getApplicationContext(), "AppInfo not found", Toast.LENGTH_LONG).show();
			});
			return vh;
		}

		@Override
		public void onBindViewHolder(@NonNull BrowseViewHolder vh, int position) {
			String[] v = data.get(position);

			if (!iconCache.containsKey(v[0])) {
				iconCache.put(v[0], Util.getAppIconFromPackage(StandbyAppsActivity.this, v[0]));
			}
			if (iconCache.get(v[0]) != null) {
				vh.icon.setImageDrawable(iconCache.get(v[0]));
			} else {
				vh.icon.setImageResource(android.R.mipmap.sym_def_app_icon);
			}

			vh.item.setTag(v[0]);

			if (v[1] == null)
				v[1] = Util.getAppNameFromPackage(StandbyAppsActivity.this, v[0], false);
			vh.title.setText(v[1]);
			vh.text.setText(v[1].equals(v[0]) ? v[2] : v[2] + "\n" + v[0]);
		}

		@Override
		public int getItemCount() {
			return data == null ? 0 : data.size();
		}
	}
}
