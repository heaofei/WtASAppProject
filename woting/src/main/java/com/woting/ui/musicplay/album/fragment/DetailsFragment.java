package com.woting.ui.musicplay.album.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.woting.R;
import com.woting.common.config.GlobalConfig;
import com.woting.common.constant.IntegerConstant;
import com.woting.common.constant.StringConstant;
import com.woting.common.gatherdata.GatherData;
import com.woting.common.gatherdata.model.DataModel;
import com.woting.common.gatherdata.model.ReqParam;
import com.woting.common.util.AssembleImageUrlUtils;
import com.woting.common.util.DialogUtils;
import com.woting.common.util.ToastUtils;
import com.woting.common.volley.VolleyCallback;
import com.woting.common.volley.VolleyRequest;
import com.woting.common.widgetui.RoundImageView;
import com.woting.common.widgetui.TipView;
import com.woting.ui.music.main.HomeActivity;
import com.woting.ui.musicplay.more.PlayerMoreOperationActivity;
import com.woting.ui.musicplay.anchor.AnchorDetailsFragment;
import com.woting.ui.musicplay.album.main.AlbumFragment;
import com.woting.ui.musicplay.album.model.ContentCatalogs;
import com.woting.ui.music.model.anchor;
import com.woting.ui.music.search.main.SearchLikeActivity;
import com.woting.ui.mine.main.MineActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.List;

/**
 * 专辑详情页
 * 作者：xinlong on 2016/11/16 17:40
 * 邮箱：645700751@qq.com
 */
public class DetailsFragment extends Fragment implements OnClickListener {
    private Context context;

    private View rootView;
    private Dialog dialog;
    private RoundImageView imageHead;
    private TextView textAnchor, textContent, textLabel, textConcern;
    private ImageView imageConcern;

    private String tag = "DETAILS_VOLLEY_REQUEST_CANCEL_TAG";
    private boolean isCancelRequest;
    private boolean isConcern;
    private String PersonId;
    private String ContentPub;
    private String ObjType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_album_details, container, false);
            initView(rootView);
            getData();         // 获取数据
            initGatherData();  // 数据上传
        }
        return rootView;
    }

    private void getData() {
        if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
            dialog = DialogUtils.Dialog(context);
            send();
        } else {
            AlbumFragment.setTip(TipView.TipStatus.NO_NET);
        }
    }

    private void initGatherData() {
        if (!TextUtils.isEmpty(AlbumFragment.id)) {
            try {
                String beginTime = String.valueOf(System.currentTimeMillis());
                String apiType = StringConstant.APINAME_OPEN;
                ObjType = StringConstant.OBJTYPE_SEQU;
                ReqParam mReqParam = new ReqParam();
                String objId = AlbumFragment.id;
                DataModel _m = new DataModel(beginTime, apiType, ObjType, mReqParam, objId);
                if (_m != null) {
                    GatherData.collectData(IntegerConstant.DATA_UPLOAD_TYPE_GIVEN, _m);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 初始化控件
    private void initView(View view) {
        imageHead = (RoundImageView) view.findViewById(R.id.round_image_head);// 圆形头像
        imageHead.setOnClickListener(this);

        textAnchor = (TextView) view.findViewById(R.id.text_anchor_name);// 节目名
        textAnchor.setOnClickListener(this);

        textContent = (TextView) view.findViewById(R.id.text_content);// 内容介绍
        textLabel = (TextView) view.findViewById(R.id.text_label);// 标签
        imageConcern = (ImageView) view.findViewById(R.id.image_concern);// 关注
        textConcern = (TextView) view.findViewById(R.id.text_concern);
        LinearLayout linearConcern = (LinearLayout) view.findViewById(R.id.linear_concern);
        linearConcern.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.linear_concern:// 关注
                if (!isConcern) {
                    imageConcern.setImageDrawable(context.getResources().getDrawable(R.mipmap.focus_concern));
                    textConcern.setText("已关注");
                    ToastUtils.show_always(context, "测试---关注成功");
                } else {
                    imageConcern.setImageDrawable(context.getResources().getDrawable(R.mipmap.focus));
                    textConcern.setText("关注");
                    ToastUtils.show_always(context, "测试---取消关注");
                }
                isConcern = !isConcern;
                break;
            case R.id.round_image_head:// 主播详情
            case R.id.text_anchor_name:
                if (!TextUtils.isEmpty(PersonId)) {
                    AnchorDetailsFragment fragment = new AnchorDetailsFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(StringConstant.FROM_TYPE, AlbumFragment.fromType);
                    bundle.putString("PersonId", PersonId);
                    bundle.putString("ContentPub", ContentPub);
                    fragment.setArguments(bundle);
                    if (AlbumFragment.fromType == IntegerConstant.TAG_HOME) {
                        HomeActivity.open(fragment);
                    } else if (AlbumFragment.fromType == IntegerConstant.TAG_MINE) {
                        MineActivity.open(fragment);
                    } else if (AlbumFragment.fromType == IntegerConstant.TAG_SEARCH) {
                        SearchLikeActivity.open(fragment);
                    } else if (AlbumFragment.fromType == IntegerConstant.TAG_MORE) {
                        PlayerMoreOperationActivity.open(fragment);
                    }
                } else {
                    ToastUtils.show_always(context, "此专辑还没有主播哦");
                }
                break;
        }
    }

    // 向服务器发送请求
    public void send() {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            jsonObject.put("MediaType", StringConstant.TYPE_SEQU);
            jsonObject.put("ContentId", AlbumFragment.id);
            jsonObject.put("Page", "1");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        VolleyRequest.requestPost(GlobalConfig.getContentById, tag, jsonObject, new VolleyCallback() {

            @Override
            protected void requestSuccess(JSONObject result) {
                if (dialog != null) dialog.dismiss();
                if (isCancelRequest) return;
                try {
                    String ReturnType = result.getString("ReturnType");
                    if (ReturnType != null && ReturnType.equals("1001")) {
                        String ResultList = result.getString("ResultInfo");
                        JSONObject arg1 = (JSONObject) new JSONTokener(ResultList).nextValue();

                        try {
                            AlbumFragment.id = arg1.getString("ContentId");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            AlbumFragment.ContentImg = arg1.getString("ContentImg");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            AlbumFragment.ContentName = arg1.getString("ContentName");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            AlbumFragment.ContentShareURL = arg1.getString("ContentShareURL");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            AlbumFragment.ContentFavorite = arg1.getString("ContentFavorite");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            ContentPub = arg1.getString("ContentPub");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                        // 以下是主页的数据适配
                        try {
                            String contentSubscribe = arg1.getString("ContentSubscribe");// 专辑是否已经订阅 == "1" 订阅  == "0" 还没订阅
                            AlbumFragment.setFlag(contentSubscribe);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (AlbumFragment.ContentName != null && !AlbumFragment.ContentName.equals("")) {
                            AlbumFragment.tv_album_name.setText(AlbumFragment.ContentName);
                        }

                        AlbumFragment.returnResult = 1;
                        if (AlbumFragment.ContentFavorite != null && !AlbumFragment.ContentFavorite.equals("")) {
                            if (AlbumFragment.ContentFavorite.equals("0")) {
                                AlbumFragment.tv_favorite.setText("喜欢");
                                AlbumFragment.imageFavorite.setImageDrawable(context.getResources().getDrawable(R.mipmap.wt_img_like));
                            } else {
                                AlbumFragment.tv_favorite.setText("已喜欢");
                                AlbumFragment.imageFavorite.setImageDrawable(context.getResources().getDrawable(R.mipmap.wt_img_liked));
                            }
                        }

                      // 内容部分的数据
                        if (AlbumFragment.ContentImg == null || AlbumFragment.ContentImg.equals("")) {
                            AlbumFragment.img_album.setImageResource(R.mipmap.wt_image_playertx);
                        } else {
                            String url;
                            if (AlbumFragment.ContentImg.startsWith("http")) {
                                url = AlbumFragment.ContentImg;
                            } else {
                                url = GlobalConfig.imageurl + AlbumFragment.ContentImg;
                            }
                            String _url1 = AssembleImageUrlUtils.assembleImageUrl300(url);
                            String _url2 = AssembleImageUrlUtils.assembleImageUrl180(url);

                            AssembleImageUrlUtils.loadImage(_url1, url, AlbumFragment.img_album, IntegerConstant.TYPE_MINE);
                            AssembleImageUrlUtils.loadImage(_url2, url, imageHead, IntegerConstant.TYPE_MINE);
                        }

                        // 主播名称
                        String PersonName = "";
                        try {
                            String ContentPersons = arg1.getString("ContentPersons");
                            List<anchor> mPersonInfoList = new Gson().fromJson(ContentPersons, new TypeToken<List<anchor>>() {
                            }.getType());
                            if (mPersonInfoList != null && mPersonInfoList.size() > 0) {
                                if (mPersonInfoList.get(0).getPerId() != null) {
                                    PersonId = mPersonInfoList.get(0).getPerId();
                                    PersonName = mPersonInfoList.get(0).getPerName();
                                } else {
                                    PersonId = "";
                                }
                            } else {
                                PersonId = "";
                            }
                            textAnchor.setText(PersonName);
                        } catch (Exception e) {
                            e.printStackTrace();
                            textAnchor.setText("主播");
                        }
                        String contentDesc;
                        // 节目描述
                        try {
                             contentDesc = arg1.getString("ContentDescn");
                            if (contentDesc != null && !contentDesc.equals("") && !contentDesc.equals("null")) {
                                // 设置界面
                                textContent.setText(Html.fromHtml("<font size='28'>" + contentDesc + "</font>"));
                            } else {
                                // 设置界面
                                textContent.setText("暂无描述");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // 设置界面
                            textContent.setText("暂无描述");
                        }

                        // 节目标签
                        try {
                            String s = arg1.getString("ContentCatalogs");
                            List<ContentCatalogs> contentCatalogsList = new Gson().fromJson(s, new TypeToken<List<ContentCatalogs>>() {
                            }.getType());
                            if (contentCatalogsList != null && contentCatalogsList.size() > 0) {
                                StringBuilder builder = new StringBuilder();
                                for (int i = 0; i < contentCatalogsList.size(); i++) {
                                    String str = contentCatalogsList.get(i).getCataTitle();
                                    builder.append(str);
                                    if (i != contentCatalogsList.size() - 1) builder.append("  ");
                                }
                                // 设置界面
                                textLabel.setText(builder.toString());
                            }else{
                                // 设置界面
                                textLabel.setText("无标签");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // 设置界面
                            textLabel.setText("无标签");
                        }

                        AlbumFragment.hideTip();
                    } else {
                        AlbumFragment.setTip(TipView.TipStatus.IS_ERROR);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    AlbumFragment.setTip(TipView.TipStatus.IS_ERROR);
                }
            }

            @Override
            protected void requestError(VolleyError error) {
                if (dialog != null) dialog.dismiss();
                AlbumFragment.setTip(TipView.TipStatus.IS_ERROR);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != rootView) {
            ((ViewGroup) rootView.getParent()).removeView(rootView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isCancelRequest = VolleyRequest.cancelRequest(tag);
        context = null;
        rootView = null;
        imageHead = null;
        textAnchor = null;
        textContent = null;
        textLabel = null;
        imageConcern = null;
        dialog = null;
        textConcern = null;
    }
}
