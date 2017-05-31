package com.example.q.pocketmusic.module.song.bottom;

import android.content.Intent;
import android.database.SQLException;
import android.support.annotation.NonNull;

import com.example.q.pocketmusic.callback.ToastQueryListListener;
import com.example.q.pocketmusic.callback.ToastQueryListener;
import com.example.q.pocketmusic.callback.ToastSaveListener;
import com.example.q.pocketmusic.callback.ToastUpdateListener;
import com.example.q.pocketmusic.config.CommonString;
import com.example.q.pocketmusic.config.Constant;
import com.example.q.pocketmusic.model.bean.DownloadInfo;
import com.example.q.pocketmusic.model.bean.MyUser;
import com.example.q.pocketmusic.model.bean.Song;
import com.example.q.pocketmusic.model.bean.SongObject;
import com.example.q.pocketmusic.model.bean.ask.AskSongComment;
import com.example.q.pocketmusic.model.bean.collection.CollectionPic;
import com.example.q.pocketmusic.model.bean.collection.CollectionSong;
import com.example.q.pocketmusic.model.bean.local.Img;
import com.example.q.pocketmusic.model.bean.local.LocalSong;
import com.example.q.pocketmusic.model.db.LocalSongDao;
import com.example.q.pocketmusic.module.common.BaseActivity;
import com.example.q.pocketmusic.module.common.BasePresenter;
import com.example.q.pocketmusic.module.common.IBaseView;
import com.example.q.pocketmusic.module.song.SongActivity;
import com.example.q.pocketmusic.util.CheckUserUtil;
import com.example.q.pocketmusic.util.DownloadUtil;
import com.example.q.pocketmusic.util.MyToast;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.ForeignCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.datatype.BmobPointer;
import cn.bmob.v3.datatype.BmobRelation;

/**
 * Created by 鹏君 on 2017/5/31.
 */

public class SongMenuPresenter extends BasePresenter<SongMenuPresenter.IView> {
    private IView fragment;
    private Intent intent;
    private boolean isEnableAgree = true;//是否能够点赞
    private Song song;
    private int isFrom;

    public SongMenuPresenter(IView fragment) {
        attachView(fragment);
        this.fragment = getIViewRef();
    }


    public Song getSong() {
        return song;
    }

    public boolean isEnableAgree() {
        return isEnableAgree;
    }


    //下载
    public void download(final String name) {
        fragment.showLoading(true);
        DownloadUtil downloadUtil = new DownloadUtil(fragment.getCurrentContext());
        downloadUtil.setOnDownloadListener(new DownloadUtil.OnDownloadListener() {
                                               @Override
                                               public DownloadInfo onStart() {
                                                   fragment.dismissEditDialog();
                                                   return downloadStartCheck();
                                               }

                                               @Override
                                               public void onSuccess() {
                                                   fragment.showLoading(false);
                                                   fragment.downloadResult(Constant.SUCCESS, "下载成功");
                                               }

                                               @Override
                                               public void onFailed(String info) {
                                                   fragment.showLoading(false);
                                                   fragment.downloadResult(Constant.FAIL, info);
                                               }
                                           }
        ).downloadBatchPic(name, song.getIvUrl(), song.getTypeId());
    }

    //下载检测
    private DownloadInfo downloadStartCheck() {
        //如果无图
        if (song.getIvUrl() == null || song.getIvUrl().size() <= 0) {
            fragment.showLoading(false);
            return new DownloadInfo("没有图片", false);
        }
        //如果本地已经存在
        if (new LocalSongDao(fragment.getCurrentContext()).isExist(song.getName())) {
            fragment.showLoading(false);
            return new DownloadInfo("本地已存在", false);
        }

        //需要硬币
        if (song.isNeedGrade()) {
            MyUser user = CheckUserUtil.checkLocalUser((BaseActivity) fragment.getCurrentContext());
            //找不到用户
            if (user == null) {
                fragment.showLoading(false);
                return new DownloadInfo("找不到用户", false);
            }
            //硬币不足
            if (!CheckUserUtil.checkUserContribution(((BaseActivity) fragment.getCurrentContext()), Constant.REDUCE_COIN_UPLOAD)) {
                fragment.showLoading(false);
                return new DownloadInfo(CommonString.STR_NOT_ENOUGH_COIN, false);
            }
            //扣除硬币
            user.increment("contribution", -Constant.REDUCE_COIN_UPLOAD);
            user.update(new ToastUpdateListener(fragment) {
                @Override
                public void onSuccess() {
                    MyToast.showToast(fragment.getCurrentContext(), CommonString.REDUCE_COIN_BASE + (Constant.REDUCE_COIN_UPLOAD));
                }
            });
        }
        return new DownloadInfo("", true);
    }


    //点赞
    public void agree() {
        if (isEnableAgree()) {
            BmobRelation relation = new BmobRelation();
            final MyUser user = MyUser.getCurrentUser(MyUser.class);
            relation.add(user);
            AskSongComment askSongComment = (AskSongComment) intent.getSerializableExtra(SongActivity.ASK_COMMENT);
            askSongComment.setAgrees(relation);
            askSongComment.increment("agreeNum");//原子操作，点赞数加一
            askSongComment.update(new ToastUpdateListener(fragment) {
                @Override
                public void onSuccess() {
                    MyToast.showToast(fragment.getCurrentContext(), "已点赞");
                    user.increment("contribution", Constant.ADD_CONTRIBUTION_AGREE);
                    user.update(new ToastUpdateListener(fragment) {
                        @Override
                        public void onSuccess() {
                            MyToast.showToast(fragment.getCurrentContext(), CommonString.ADD_COIN_BASE + Constant.ADD_CONTRIBUTION_AGREE);
                        }
                    });
                }
            });
        } else {
            MyToast.showToast(fragment.getCurrentContext(), "已经赞过了哦~");
        }


    }


    //判断当前的评论的图片是否可以点赞
    public void checkHasAgree() {
        BmobQuery<MyUser> query = new BmobQuery<>();
        final MyUser user = MyUser.getCurrentUser(MyUser.class);
        AskSongComment askSongComment = (AskSongComment) intent.getSerializableExtra(SongActivity.ASK_COMMENT);
        query.addWhereRelatedTo("agrees", new BmobPointer(askSongComment));
        query.findObjects(new ToastQueryListener<MyUser>(fragment) {
            @Override
            public void onSuccess(List<MyUser> list) {
                for (MyUser other : list) {
                    if (other.getObjectId().equals(user.getObjectId())) {
                        //已经点赞
                        isEnableAgree = false;
                        break;
                    }
                    isEnableAgree = true;
                }
            }
        });

    }

    //添加收藏
    public void addCollection() {
        fragment.showLoading(true);
        final MyUser user = CheckUserUtil.checkLocalUser((BaseActivity) fragment.getCurrentContext());
        if (user == null) {
            fragment.showLoading(false);
            MyToast.showToast(fragment.getCurrentContext(), "请先登录~");
            return;
        }
        if (song.getIvUrl() == null || song.getIvUrl().size() <= 0) {
            fragment.showLoading(false);
            MyToast.showToast(fragment.getCurrentContext(), "图片为空");
            return;
        }
        //检测是否已经收藏
        BmobQuery<CollectionSong> query = new BmobQuery<>();
        query.order("-updatedAt");
        query.addWhereRelatedTo("collections", new BmobPointer(user));//在user表的Collections找user
        query.findObjects(new ToastQueryListener<CollectionSong>(fragment) {
            @Override
            public void onSuccess(List<CollectionSong> list) {
                //是否已收藏
                for (CollectionSong collectionSong : list) {
                    if (collectionSong.getName().equals(song.getName())) {
                        fragment.showLoading(false);
                        MyToast.showToast(fragment.getCurrentContext(), "已收藏");
                        return;
                    }
                }
                //贡献度是否足够
                if (!CheckUserUtil.checkUserContribution(((BaseActivity) fragment.getCurrentContext()), Constant.REDUCE_CONTRIBUTION_COLLECTION)) {
                    fragment.showLoading(false);
                    MyToast.showToast(fragment.getCurrentContext(), "贡献值不够~");
                    return;
                }


                //添加收藏记录
                final CollectionSong collectionSong = new CollectionSong();
                collectionSong.setName(song.getName());
                collectionSong.setNeedGrade(song.isNeedGrade());//是否需要积分
                collectionSong.setIsFrom(((SongObject) intent.getParcelableExtra(SongActivity.PARAM_SONG_OBJECT_PARCEL)).getFrom());
                collectionSong.setContent(song.getContent());
                collectionSong.save(new ToastSaveListener<String>(fragment) {

                    @Override
                    public void onSuccess(String s) {
                        final int numPic = song.getIvUrl().size();
                        List<BmobObject> collectionPics = new ArrayList<BmobObject>();
                        for (int i = 0; i < numPic; i++) {
                            CollectionPic collectionPic = new CollectionPic();
                            collectionPic.setCollectionSong(collectionSong);
                            collectionPic.setUrl(song.getIvUrl().get(i));
                            collectionPics.add(collectionPic);
                        }
                        //批量修改
                        new BmobBatch().insertBatch(collectionPics).doBatch(new ToastQueryListListener<BatchResult>(fragment) {
                            @Override
                            public void onSuccess(List<BatchResult> list) {
                                BmobRelation relation = new BmobRelation();
                                relation.add(collectionSong);
                                user.setCollections(relation);//添加用户收藏
                                user.update(new ToastUpdateListener(fragment) {
                                    @Override
                                    public void onSuccess() {
                                        MyToast.showToast(fragment.getCurrentContext(), "已收藏");
                                        user.increment("contribution", -Constant.REDUCE_CONTRIBUTION_COLLECTION);//贡献值-1
                                        user.update(new ToastUpdateListener(fragment) {
                                            @Override
                                            public void onSuccess() {
                                                fragment.showLoading(false);
                                                MyToast.showToast(fragment.getCurrentContext(), CommonString.REDUCE_COIN_BASE + Constant.REDUCE_CONTRIBUTION_COLLECTION);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

    }

    //分享乐谱,本地和网络
    public void share() {
        List<String> list = null;
        SongObject songObject = intent.getParcelableExtra(SongActivity.PARAM_SONG_OBJECT_PARCEL);
        int loadingWay = songObject.getLoadingWay();
        switch (loadingWay) {
            case Constant.NET:
                list = song.getIvUrl();
                break;
            case Constant.LOCAL:
                list = getLocalImgs();
                break;
        }
        if (list == null || list.size() <= 0) {
            MyToast.showToast(fragment.getCurrentContext(), "没有图片");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String url : list) {
            sb.append(url).append(",");
        }

        Intent intent = new Intent("android.intent.action.SEND");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "推荐一首歌：" + "<<" + song.getName() + ">>:" + sb.toString());
        if (intent.resolveActivity(fragment.getCurrentContext().getPackageManager()) != null) {
            fragment.getCurrentContext().startActivity(intent);
        } else {
            MyToast.showToast(fragment.getCurrentContext(), "你的手机不支持分享~");
        }
    }

    //得到本地图片
    @NonNull
    private ArrayList<String> getLocalImgs() {
        LocalSong localsong = (LocalSong) intent.getSerializableExtra(SongActivity.LOCAL_SONG);
        LocalSongDao localSongDao = new LocalSongDao(fragment.getAppContext());
        ArrayList<String> imgUrls = new ArrayList<>();
        LocalSong localSong = localSongDao.findBySongId(localsong.getId());
        if (localSong == null) {
            MyToast.showToast(fragment.getCurrentContext(), "曲谱消失在了异次元。");
            fragment.finish();
            return new ArrayList<>();
        }
        ForeignCollection<Img> imgs = localSong.getImgs();
        CloseableIterator<Img> iterator = imgs.closeableIterator();
        try {
            while (iterator.hasNext()) {
                Img img = iterator.next();
                imgUrls.add(img.getUrl());
            }
        } finally {
            try {
                iterator.close();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        return imgUrls;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public void init() {
        SongObject songObject = intent.getParcelableExtra(SongActivity.PARAM_SONG_OBJECT_PARCEL);
        isFrom = songObject.getFrom();
        song=songObject.getSong();

        //求谱，检测是否可以点赞,
        if (isFrom == Constant.FROM_ASK) {
            checkHasAgree();
        }
    }

    public int getShowMenuFlag() {
        return ((SongObject) intent.getParcelableExtra(SongActivity.PARAM_SONG_OBJECT_PARCEL)).getShowMenu();
    }


    interface IView extends IBaseView {

        void dismissEditDialog();

        void downloadResult(Integer success, String str);
    }
}