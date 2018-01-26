package com.example.q.pocketmusic.module.home.local;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.q.pocketmusic.R;
import com.example.q.pocketmusic.module.common.BaseFragment;
import com.example.q.pocketmusic.module.home.local.lead.LeadSongActivity;
import com.example.q.pocketmusic.module.home.profile.gift.GiftModel;
import com.example.q.pocketmusic.view.widget.view.TopTabView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


/**
 * Created by 鹏君 on 2016/8/28.
 */
public class HomeLocalFragment extends BaseFragment<HomeLocalFragmentPresenter.IView, HomeLocalFragmentPresenter>
        implements HomeLocalFragmentPresenter.IView, TopTabView.TopTabListener {
    @BindView(R.id.add_local_iv)
    ImageView addLocalIv;
    @BindView(R.id.top_tab_view)
    TopTabView topTabView;
    @BindView(R.id.home_local_content)
    FrameLayout homeLocalContent;
    @BindView(R.id.activity_audio_record)
    LinearLayout activityAudioRecord;

    @Override
    protected HomeLocalFragmentPresenter createPresenter() {
        return new HomeLocalFragmentPresenter(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int setContentResource() {
        return R.layout.fragment_home_local;
    }


    @Override
    public void initView() {
        presenter.setFragmentManager(getChildFragmentManager());
        topTabView.setListener(this);
        topTabView.setCheck(HomeLocalFragmentPresenter.TabType.SONG);
        presenter.clickBottomTab(HomeLocalFragmentPresenter.TabType.SONG);
    }


    @Override
    public void finish() {
        getActivity().finish();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LeadSongActivity.REQUEST_LEAD && resultCode == LeadSongActivity.RESULT_OK) {
            topTabView.setCheck(HomeLocalFragmentPresenter.TabType.SONG);
            presenter.clickBottomTab(HomeLocalFragmentPresenter.TabType.SONG);
        }
    }

    @Override
    public void setTopTabCheck(int position) {
        switch (position) {
            case HomeLocalFragmentPresenter.TabType.SONG:
                presenter.clickBottomTab(HomeLocalFragmentPresenter.TabType.SONG);
                break;
            case HomeLocalFragmentPresenter.TabType.RECORD:
                presenter.clickBottomTab(HomeLocalFragmentPresenter.TabType.RECORD);
                break;
        }
    }

    @Override
    public void onSelectRecord() {
        topTabView.setCheck(HomeLocalFragmentPresenter.TabType.RECORD);
    }

    @Override
    public void onSelectSong() {
        topTabView.setCheck(HomeLocalFragmentPresenter.TabType.SONG);
    }


    @OnClick({R.id.add_local_iv})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.add_local_iv:
                presenter.enterLeadActivity();
                break;
        }
    }


}
