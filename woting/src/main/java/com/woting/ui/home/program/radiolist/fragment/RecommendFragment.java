package com.woting.ui.home.program.radiolist.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.woting.R;
import com.woting.common.config.GlobalConfig;
import com.woting.common.constant.BroadcastConstants;
import com.woting.common.constant.IntegerConstant;
import com.woting.common.constant.StringConstant;
import com.woting.common.util.CommonUtils;
import com.woting.common.util.DialogUtils;
import com.woting.common.util.PicassoBannerLoader;
import com.woting.common.util.ToastUtils;
import com.woting.common.volley.VolleyCallback;
import com.woting.common.volley.VolleyRequest;
import com.woting.common.widgetui.TipView;
import com.woting.common.widgetui.xlistview.XListView;
import com.woting.common.widgetui.xlistview.XListView.IXListViewListener;
import com.woting.ui.home.main.HomeActivity;
import com.woting.ui.home.player.main.dao.SearchPlayerHistoryDao;
import com.woting.ui.home.player.main.model.PlayerHistory;
import com.woting.ui.home.program.album.main.AlbumFragment;
import com.woting.ui.home.program.fmlist.model.RankInfo;
import com.woting.ui.home.program.radiolist.adapter.ForNullAdapter;
import com.woting.ui.home.program.radiolist.adapter.RadioListAdapter;
import com.woting.ui.home.program.radiolist.main.RadioListFragment;
import com.woting.ui.home.program.radiolist.mode.Image;
import com.woting.ui.main.MainActivity;
import com.youth.banner.Banner;
import com.youth.banner.listener.OnBannerListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类推荐列表
 *
 * @author woting11
 */
public class RecommendFragment extends Fragment implements TipView.WhiteViewClick {
    private Context context;
    private SearchPlayerHistoryDao dbDao;// 数据库
    private RadioListAdapter adapter;
    private Banner mLoopViewPager;

    private List<Image> imageList;
    private List<String> ImageStringList = new ArrayList<>();
    private List<RankInfo> newList = new ArrayList<>();

    private View rootView;
    private Dialog dialog;// 加载对话框
    private XListView mListView;// 列表
    private TipView tipView;// 没有网络、没有数据提示

    private boolean isFirst = true;
    private int page = 1;// 页码
    private int refreshType = 1;// refreshType == 1 为下拉加载  == 2 为上拉加载更多

    @Override
    public void onWhiteViewClick() {
        if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
            dialog = DialogUtils.Dialogph(context, "正在获取数据");
            sendRequest();
        } else {
            tipView.setVisibility(View.VISIBLE);
            tipView.setTipView(TipView.TipStatus.NO_NET);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        initDao();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_radio_list_layout, container, false);

            mListView = (XListView) rootView.findViewById(R.id.listview_fm);
            tipView = (TipView) rootView.findViewById(R.id.tip_view);
            tipView.setWhiteClick(this);
            View headView = LayoutInflater.from(context).inflate(R.layout.headview_acitivity_radiolist, null);
            // 轮播图
            mLoopViewPager = (Banner) headView.findViewById(R.id.slideshowView);
            mListView.addHeaderView(headView);
            setListener();
        }
        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser && adapter == null && getActivity() != null) {
            if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
                if (!isFirst) dialog = DialogUtils.Dialogph(context, "正在获取数据");
                sendRequest();
                getImage();
                isFirst = false;
            } else {
                tipView.setVisibility(View.VISIBLE);
                tipView.setTipView(TipView.TipStatus.NO_NET);
            }
        }
        // 如果轮播图没有的话重新加载轮播图
        if (imageList == null) getImage();
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUserVisibleHint(getUserVisibleHint());
    }

    // 请求网络数据
    public void sendRequest() {
        if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE == -1) {
            if (dialog != null) dialog.dismiss();
            RadioListFragment.closeDialog();
            if (refreshType == 1) {
                tipView.setVisibility(View.VISIBLE);
                tipView.setTipView(TipView.TipStatus.NO_NET);
                mListView.stopRefresh();
            } else {
                mListView.stopLoadMore();
            }
            return;
        }
        VolleyRequest.requestPost(GlobalConfig.getContentUrl, RadioListFragment.tag, setParam(), new VolleyCallback() {
            private String ReturnType;

            @Override
            protected void requestSuccess(JSONObject result) {
                RadioListFragment.closeDialog();
                if (dialog != null) dialog.dismiss();
                if (RadioListFragment.isCancel()) return;
                try {
                    ReturnType = result.getString("ReturnType");
                    if (ReturnType != null && ReturnType.equals("1001")) {
                        JSONObject arg1 = (JSONObject) new JSONTokener(result.getString("ResultList")).nextValue();
                        List<RankInfo> subList = new Gson().fromJson(arg1.getString("List"), new TypeToken<List<RankInfo>>() {}.getType());
                        if (subList != null && subList.size() >= 9) {
                            page++;
                            mListView.setPullLoadEnable(true);
                        } else {
                            mListView.setPullLoadEnable(false);
                        }
                        if (refreshType == 1) newList.clear();
                        newList.addAll(subList);
                        if (adapter == null) {
                            mListView.setAdapter(adapter = new RadioListAdapter(context, newList));
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        setOnItem();
                        tipView.setVisibility(View.GONE);
                    } else {
                        mListView.setAdapter(new ForNullAdapter(context));
                        if (refreshType == 1) {
                            tipView.setVisibility(View.VISIBLE);
                            tipView.setTipView(TipView.TipStatus.NO_DATA, "数据君不翼而飞了\n点击界面会重新获取数据哟");
                        } else {
                            ToastUtils.show_always(context, "没有更多的数据了");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mListView.setAdapter(new ForNullAdapter(context));
                    if (refreshType == 1) {
                        tipView.setVisibility(View.VISIBLE);
                        tipView.setTipView(TipView.TipStatus.IS_ERROR);
                    } else {
                        ToastUtils.show_always(context, "数据加载错误");
                    }
                }

                if (refreshType == 1) {
                    mListView.stopRefresh();
                } else {
                    mListView.stopLoadMore();
                }
            }

            @Override
            protected void requestError(VolleyError error) {
                if (dialog != null) dialog.dismiss();
                RadioListFragment.closeDialog();
                if (refreshType == 1) {
                    tipView.setVisibility(View.VISIBLE);
                    tipView.setTipView(TipView.TipStatus.IS_ERROR);
                } else {
                    ToastUtils.showVolleyError(context);
                }
            }
        });
    }

    private JSONObject setParam() {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            jsonObject.put("MediaType", "");
            jsonObject.put("CatalogType", RadioListFragment.catalogType);
            jsonObject.put("CatalogId", RadioListFragment.id);
            jsonObject.put("Page", String.valueOf(page));
            jsonObject.put("PerSize", "3");
            jsonObject.put("ResultType", "2");
            jsonObject.put("PageSize", "10");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void setOnItem() {
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (newList != null && position >= 2) {
                    if (newList.get(position - 2) != null && newList.get(position - 2).getMediaType() != null) {
                        String MediaType = newList.get(position - 2).getMediaType();
                        if (MediaType.equals(StringConstant.TYPE_RADIO) || MediaType.equals(StringConstant.TYPE_AUDIO)) {
                            String playerName = newList.get(position - 2).getContentName();
                            String playerImage = newList.get(position - 2).getContentImg();
                            String playUrl = newList.get(position - 2).getContentPlay();
                            String playUrI = newList.get(position - 2).getContentURI();
                            String playContentShareUrl = newList.get(position - 2).getContentShareURL();
                            String playMediaType = newList.get(position - 2).getMediaType();
                            String playAllTime = newList.get(position - 2).getContentTimes();
                            String playInTime = "0";
                            String playContentDesc = newList.get(position - 2).getContentDescn();
                            String playNum = newList.get(position - 2).getPlayCount();
                            String playZanType = "0";
                            String playFrom = newList.get(position - 2).getContentPub();
                            String playFromId = "";
                            String playFromUrl = "";
                            String playAddTime = Long.toString(System.currentTimeMillis());
                            String bjUserId = CommonUtils.getUserId(context);
                            String ContentFavorite = newList.get(position - 2).getContentFavorite();
                            String ContentId = newList.get(position - 2).getContentId();
                            String localUrl = newList.get(position - 2).getLocalurl();

                            String sequName = newList.get(position - 2).getSequName();
                            String sequId = newList.get(position - 2).getSequId();
                            String sequDesc = newList.get(position - 2).getSequDesc();
                            String sequImg = newList.get(position - 2).getSequImg();

                            String ContentPlayType = newList.get(position - 2).getContentPlayType();
                            String IsPlaying = newList.get(position - 2).getIsPlaying();

                            // 如果该数据已经存在数据库则删除原有数据，然后添加最新数据
                            PlayerHistory history = new PlayerHistory(
                                    playerName, playerImage, playUrl, playUrI, playMediaType,
                                    playAllTime, playInTime, playContentDesc, playNum,
                                    playZanType, playFrom, playFromId, playFromUrl, playAddTime, bjUserId, playContentShareUrl,
                                    ContentFavorite, ContentId, localUrl, sequName, sequId, sequDesc, sequImg, ContentPlayType, IsPlaying);
                            dbDao.deleteHistory(playUrl);
                            dbDao.addHistory(history);

                            Intent push = new Intent(BroadcastConstants.PLAY_TEXT_VOICE_SEARCH);
                            Bundle bundle1 = new Bundle();
                            bundle1.putString(StringConstant.TEXT_CONTENT, newList.get(position - 2).getContentName());
                            push.putExtras(bundle1);
                            context.sendBroadcast(push);
                            MainActivity.change();
                        } else if (MediaType.equals(StringConstant.TYPE_SEQU)) {
                            AlbumFragment fragment = new AlbumFragment();
                            Bundle bundle = new Bundle();
                            bundle.putInt(StringConstant.FROM_TYPE, IntegerConstant.TAG_HOME);
                            bundle.putString("type", "recommend");
                            bundle.putSerializable("list", newList.get(position - 2));
                            fragment.setArguments(bundle);
                            HomeActivity.open(fragment);
                        } else {
                            ToastUtils.show_short(context, "暂不支持的Type类型");
                        }
                    }
                }
            }
        });
    }

    // 设置刷新、加载更多参数
    private void setListener() {
        mListView.setPullLoadEnable(true);
        mListView.setPullRefreshEnable(true);
        mListView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mListView.setXListViewListener(new IXListViewListener() {
            @Override
            public void onRefresh() {
                if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
                    refreshType = 1;
                    page = 1;
                    sendRequest();
                } else {
                    tipView.setVisibility(View.VISIBLE);
                    tipView.setTipView(TipView.TipStatus.NO_NET);
                }
            }

            @Override
            public void onLoadMore() {
                if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
                    refreshType = 2;
                    sendRequest();
                } else {
                    ToastUtils.show_always(context, "网络失败，请检查网络");
                }
            }
        });
    }

    // 初始化数据库命令执行对象
    private void initDao() {
        dbDao = new SearchPlayerHistoryDao(context);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != rootView) {
            ((ViewGroup) rootView.getParent()).removeView(rootView);
        }
    }

    // 请求网络获取分类信息
    private void getImage() {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            jsonObject.put("CatalogType", RadioListFragment.catalogType);
            jsonObject.put("CatalogId", RadioListFragment.id);
            jsonObject.put("Size", "4");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        VolleyRequest.requestPost(GlobalConfig.getImage, RadioListFragment.tag, jsonObject, new VolleyCallback() {
            private String ReturnType;

            @Override
            protected void requestSuccess(JSONObject result) {
                if (dialog != null) dialog.dismiss();
                if (RadioListFragment.isCancel()) return;
                try {
                    ReturnType = result.getString("ReturnType");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (ReturnType != null && ReturnType.equals("1001")) {
                    try {
                        imageList = new Gson().fromJson(result.getString("LoopImgs"), new TypeToken<List<Image>>() {
                        }.getType());
                        //  mLoopViewPager.setAdapter(new LoopAdapter(mLoopViewPager, context, imageList));
                        //  mLoopViewPager.setHintView(new IconHintView(context, R.mipmap.indicators_now, R.mipmap.indicators_default));
                        mLoopViewPager.setImageLoader(new PicassoBannerLoader());

                        for (int i = 0; i < imageList.size(); i++) {
                            ImageStringList.add(imageList.get(i).getLoopImg());
                        }
                        mLoopViewPager.setImages(ImageStringList);

                        mLoopViewPager.setOnBannerListener(new OnBannerListener() {
                            @Override
                            public void OnBannerClick(int position) {
                                ToastUtils.show_always(context, ImageStringList.get(position - 1));
                            }
                        });
                        mLoopViewPager.start();


                        tipView.setVisibility(View.GONE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void requestError(VolleyError error) {
            }
        });
    }

  /*  public class PicassoImageLoader extends ImageLoader {
        @Override
        public void displayImage(Context context, Object path, ImageView imageView) {
            *//**
     注意：
     1.图片加载器由自己选择，这里不限制，只是提供几种使用方法
     2.返回的图片路径为Object类型，由于不能确定你到底使用的那种图片加载器，
     传输的到的是什么格式，那么这种就使用Object接收和返回，你只需要强转成你传输的类型就行，
     切记不要胡乱强转！
     *//*
            String contentImg=path.toString();
            if (!contentImg.startsWith("http")) {
                contentImg = GlobalConfig.imageurl + contentImg;
            }
            contentImg = AssembleImageUrlUtils.assembleImageUrl150(contentImg);
            Picasso.with(context).load(contentImg.replace("\\/", "/")).resize(50,50).centerCrop().into(imageView);
        }


    }*/

}
