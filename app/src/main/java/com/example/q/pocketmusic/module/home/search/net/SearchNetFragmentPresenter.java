package com.example.q.pocketmusic.module.home.search.net;

import android.content.Intent;

import com.example.q.pocketmusic.config.Constant;
import com.example.q.pocketmusic.model.bean.Song;
import com.example.q.pocketmusic.model.bean.SongObject;
import com.example.q.pocketmusic.model.net.LoadSearchSongList;
import com.example.q.pocketmusic.module.common.BasePresenter;
import com.example.q.pocketmusic.module.common.IBaseView;
import com.example.q.pocketmusic.module.song.SongActivity;

import java.util.List;

/**
 * Created by 鹏君 on 2017/4/14.
 */

public class SearchNetFragmentPresenter extends BasePresenter<SearchNetFragmentPresenter.IView> {
    private IView fragment;
    private int mPage;

    public SearchNetFragmentPresenter(IView fragment) {
        attachView(fragment);
        this.fragment = getIViewRef();
    }

    public int getmPage() {
        return mPage;
    }

    //这里有问题，最好是能够先搜Bmob再搜全网
    public void getList(final String query) {
        new LoadSearchSongList(mPage) {
            @Override
            protected void onPostExecute(final List<Song> list) {
                //默认是刷新操作，主线程adapter.clear,所以要保证list不为空
                fragment.setList(list);
            }
        }.execute(query);
    }


    public void setPage(int page) {
        this.mPage = page;
    }

    public void enterSongActivity(Song song) {
        Intent intent = new Intent(fragment.getCurrentContext(), SongActivity.class);
        SongObject object = new SongObject(song, song.getSearchFrom(), Constant.MENU_DOWNLOAD_COLLECTION_SHARE, Constant.NET);
        intent.setExtrasClassLoader(getClass().getClassLoader());
        intent.putExtra(SongActivity.PARAM_SONG_OBJECT_SERIALIZABLE, object);
        fragment.getCurrentContext().startActivity(intent);
    }

    public interface IView extends IBaseView {
        void setList(List<Song> list);
    }
}