package org.hcilab.projects.nlogx.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.hcilab.projects.nlogx.R;
import org.hcilab.projects.nlogx.misc.Util;

import java.util.Objects;

public class BrowseActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

	private SwipeRefreshLayout swipeRefreshLayout;
	private RecyclerView recyclerView;
	private TextView emptyView;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browse);

		swipeRefreshLayout = findViewById(R.id.swiper);
		swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
		swipeRefreshLayout.setOnRefreshListener(this);

		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
		recyclerView = findViewById(R.id.list);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(layoutManager);

		emptyView = findViewById(R.id.empty);

		update();

		if (!Util.isNotificationAccessEnabled(this))
			startActivityForResult(new Intent(this, SettingsActivity.class), 1);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data == null)
			return;
		if (SettingsActivity.ACTION_REFRESH.equals(data.getStringExtra(SettingsActivity.EXTRA_ACTION))) {
			update();
		} else {
			int position = data.getIntExtra(DetailsActivity.EXTRA_DELETE, -1);
			if (position >= 0)
				((BrowseAdapter)Objects.requireNonNull(recyclerView.getAdapter())).remove(position);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.browse, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_settings:
				startActivityForResult(new Intent(this, SettingsActivity.class), 1);
				return true;
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
		BrowseAdapter adapter = new BrowseAdapter(this);
		recyclerView.setAdapter(adapter);

		if (adapter.getItemCount() == 0) {
			recyclerView.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
		}
		else {
			recyclerView.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.GONE);
		}
	}

	@Override
	public void onRefresh() {
		update();
		swipeRefreshLayout.setRefreshing(false);
	}
}
