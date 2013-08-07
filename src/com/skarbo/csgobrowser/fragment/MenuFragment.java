package com.skarbo.csgobrowser.fragment;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.activity.MainActivity;

public class MenuFragment extends Fragment {

	private static final String TAG = MenuFragment.class.getSimpleName();
	private ListView menuListView;
	private MainActivity mainActivity;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.frame_menu, null);

		menuListView = (ListView) view.findViewById(R.id.frame_menu_list);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mainActivity = (MainActivity) getActivity();

		List<Item> menuList = new ArrayList<Item>();
		menuList.add(new SectionItem("Servers"));
		menuList.add(new MenuItem(ServersFragment.class, android.R.drawable.btn_star, "Servers"));
//		menuList.add(new SectionItem("Profile"));
//		menuList.add(new MenuItem(ProfileFragment.class, android.R.drawable.btn_star, "Profile"));
//		menuList.add(new SectionItem("Match"));
//		menuList.add(new MenuItem(MatchFragment.class, android.R.drawable.btn_star, "Match"));

		final MenuAdapter menuAdapter = new MenuAdapter(getActivity(), R.id.frame_menu_list, menuList);

		if (menuListView != null) {
			menuListView.setAdapter(menuAdapter);

			menuListView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if (!menuAdapter.getItem(position).isSection()) {
						doMenuSwitch((MenuItem) menuAdapter.getItem(position));
					}
				}
			});
		}
	}

	private void doMenuSwitch(MenuItem menu) {
		if (mainActivity == null) {
			Log.e(TAG, "Do menu switch: MainActivity is null");
			return;
		}
		try {
			mainActivity.doSwitchContent(menu.getFragmentClass().newInstance());
		} catch (java.lang.InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	// ... CLASS

	interface Item {
		public boolean isSection();

		public String getTitle();
	}

	private class SectionItem implements Item {

		private String title;

		public SectionItem(String title) {
			this.title = title;
		}

		@Override
		public boolean isSection() {
			return true;
		}

		@Override
		public String getTitle() {
			return title;
		}

	}

	private class MenuItem implements Item {

		private String title;
		private int imageResource;
		private Class<? extends Fragment> fragmentClass;

		public MenuItem(Class<? extends Fragment> fragmentClass, int imageResource, String title) {
			this.fragmentClass = fragmentClass;
			this.imageResource = imageResource;
			this.title = title;
		}

		@Override
		public boolean isSection() {
			return false;
		}

		public Class<? extends Fragment> getFragmentClass() {
			return fragmentClass;
		}

		public int getImageResource() {
			return imageResource;
		}

		@Override
		public String getTitle() {
			return title;
		}

	}

	private class MenuAdapter extends ArrayAdapter<Item> {

		private LayoutInflater layoutInfalter;

		public MenuAdapter(Context context, int textViewResourceId, List<Item> objects) {
			super(context, textViewResourceId, objects);
			layoutInfalter = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			final Item item = getItem(position);
			if (item != null) {
				if (item.isSection()) {
					SectionItem section = (SectionItem) item;
					v = layoutInfalter.inflate(R.layout.list_item_section, null);

					final TextView sectionView = (TextView) v.findViewById(R.id.list_item_section_text);
					sectionView.setText(section.getTitle());
				} else {
					MenuItem menu = (MenuItem) item;
					v = layoutInfalter.inflate(R.layout.list_item_entry, null);

					final TextView titleView = (TextView) v.findViewById(R.id.list_item_entry_title);
					final ImageView imageView = (ImageView) v.findViewById(R.id.list_item_entry_drawable);

					titleView.setText(menu.getTitle());
					imageView.setImageResource(menu.getImageResource());
				}
			}

			return v;
		}

	}

	// ... /CLASS

}
