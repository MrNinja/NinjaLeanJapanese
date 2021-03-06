package gdg.ninja.ui;

import gdg.nat.ninjalearnjapanese.R;
import gdg.ninja.adapter.ListCategoriesAdapter;
import gdg.ninja.framework.BaseFragment;
import gdg.ninja.gameinfo.CategoriesInfo;
import gdg.ninja.navigate.NavigationBar.BTN_LEFT_MODE;
import gdg.ninja.navigate.NavigationBar.BTN_RIGHT_MODE;
import gdg.ninja.navigate.NavigationBar.INavigationBarListener;
import gdg.ninja.util.App;
import gdg.ninja.util.SoundUtils;
import gdg.ninja.util.SoundUtils.SOUND_NAME;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ListGameFragment extends BaseFragment implements
		INavigationBarListener{
	private ListView mLtvCategories;
	
	private ListCategoriesAdapter mAdapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fg_list_game, container, false);
		initView(view);
		initData();
		return view;
	}
	
	private void initView(View view){
		mLtvCategories = (ListView) view.findViewById(R.id.ltv_list);
		mLtvCategories.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id){
				CategoriesInfo item = mAdapter.getItem(position);
				if(position != 0){
					CategoriesInfo previousItem = mAdapter
							.getItem(position - 1);
					if (previousItem.getStt() < 3) {
						SoundUtils.getInstance().play(SOUND_NAME.WRONG);
						return;
					}
				}

				SoundUtils.getInstance().play(SOUND_NAME.OTHER_BTN);
				ListQuestFragment fragment = ListQuestFragment.getInstance(item
						.getCateId());
				mNaviManager.showPage(fragment, "");
			}
		});
	}
	
	private void initData(){
		mAdapter = new ListCategoriesAdapter(App.getListCategories(),
				getActivity());
		mLtvCategories.setAdapter(mAdapter);
	}
	
	@Override
	public String getTitle(){
		return getString(R.string.sc_list_category_title);
	}
	
	@Override
	public BTN_LEFT_MODE getButtonLeftMode(){
		return BTN_LEFT_MODE.BACK;
	}
	
	@Override
	public void onLeftClicked(){
		mNaviManager.goBack();
	}
	
	@Override
	public BTN_RIGHT_MODE getButtonRightMode(){
		return BTN_RIGHT_MODE.SETTING;
	}
	
	@Override
	public void onRightClicked(){
		Intent intent = new Intent(getActivity(), HelpActivity.class);
		startActivity(intent);
	}
}