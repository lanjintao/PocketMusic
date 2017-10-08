package com.example.q.pocketmusic.module.user.notify.gift;

import com.example.q.pocketmusic.callback.ToastQueryListener;
import com.example.q.pocketmusic.callback.ToastUpdateListener;
import com.example.q.pocketmusic.config.BmobConstant;
import com.example.q.pocketmusic.config.CommonString;
import com.example.q.pocketmusic.config.Constant;
import com.example.q.pocketmusic.model.bean.bmob.Gift;
import com.example.q.pocketmusic.module.common.BasePresenter;
import com.example.q.pocketmusic.module.common.IBaseView;
import com.example.q.pocketmusic.util.UserUtil;
import com.example.q.pocketmusic.util.common.LogUtils;
import com.example.q.pocketmusic.util.common.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.QueryListListener;

/**
 * Created by 81256 on 2017/9/6.
 */

public class GiftPresenter extends BasePresenter<GiftPresenter.IView> {
    private IView activity;
    private GiftModel model;
    private int mPage;

    public GiftPresenter(IView activity) {
        attachView(activity);
        this.activity = getIViewRef();
        model = new GiftModel();
    }


    public void getGiftList(final boolean isRefreshing) {
        if (isRefreshing) {
            mPage = 0;
        }
        model.getGiftList(UserUtil.user, mPage, new ToastQueryListener<Gift>() {
            @Override
            public void onSuccess(List<Gift> list) {
                activity.setGiftList(list, isRefreshing);
            }
        });

    }

    public void setPage(int page) {
        this.mPage = page;
    }

    public void getMoreGiftList() {
        mPage++;
        model.getGiftList(UserUtil.user, mPage, new ToastQueryListener<Gift>() {
            @Override
            public void onSuccess(List<Gift> list) {
                activity.setGiftList(list, false);
            }
        });
    }

    //收礼物，批量更新
    public void receivedAllCoin(List<Gift> allData) {
        activity.showLoading(true);
        int sum = 0;
        List<BmobObject> gifts = new ArrayList<>();
        for (Gift gift : allData) {
            sum += gift.getCoin();
            gift.setGet(true);
            gifts.add(gift);
        }
        final int finalSum = sum;
        final int finalSum1 = sum;
        new BmobBatch().updateBatch(gifts).doBatch(new QueryListListener<BatchResult>() {
            @Override
            public void done(List<BatchResult> list, BmobException e) {
                activity.showLoading(false);
                if (e == null) {
                    UserUtil.increment(finalSum1, new ToastUpdateListener() {
                        @Override
                        public void onSuccess() {
                            ToastUtil.showToast(CommonString.ADD_COIN_BASE + finalSum);
                            activity.onRefresh();
                        }
                    });
                }
            }
        });

    }


    public interface IView extends IBaseView {

        void setGiftList(List<Gift> list, boolean isRefreshing);

        void onRefresh();
    }
}
