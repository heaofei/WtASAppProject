package com.woting.ui.musicplay.comment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.woting.R;
import com.woting.common.config.GlobalConfig;
import com.woting.common.helper.CommonHelper;
import com.woting.common.util.CommonUtils;
import com.woting.common.util.ToastUtils;
import com.woting.common.volley.VolleyCallback;
import com.woting.common.volley.VolleyRequest;
import com.woting.common.widgetui.MyEditText;
import com.woting.common.widgetui.TipView;
import com.woting.common.widgetui.xlistview.XListView;
import com.woting.ui.base.baseactivity.AppBaseActivity;
import com.woting.ui.musicplay.comment.adapter.ChatLVAdapter;
import com.woting.ui.musicplay.comment.adapter.ContentNoAdapter;
import com.woting.ui.musicplay.comment.adapter.FaceGVAdapter;
import com.woting.ui.musicplay.comment.adapter.FaceVPAdapter;
import com.woting.ui.musicplay.comment.model.opinion;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 评论
 */
public class CommentActivity extends AppBaseActivity implements View.OnClickListener, TipView.WhiteViewClick, View.OnTouchListener {
    private List<View> views = new ArrayList<>();
    private List<String> faceList;
    private List<opinion> newList = new ArrayList<>();
    private Dialog confirmDialog;
    private View chatFaceContainerView;// 表情布局
    private LinearLayout mDotsLayout;
    private XListView commentList;// 评论列表
    private ViewPager mViewPager;
    private MyEditText input;
    private TipView tipView;// 没有网络、没有数据提示

    private int columns = 6;
    private int rows = 4;
    private long time2 = 0;
    private String contentId;
    private String discussId;
    private String mediaType;
    private String tag = "HOME_COMMENT_TAG";
    private boolean isCancelRequest;
    private int page = 1;
    private int refreshType = 1; // refreshType 1 为下拉加载 2 为上拉加载更多
    private ChatLVAdapter adapter;

    @Override
    public void onWhiteViewClick() {
        callInternet();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        initData();
        initView();
        initEvent();
        initListViewListener();
        delDialog();// 初始化删除确认对话框
        initViewPager();
        callInternet();
    }

    // 初始化数据
    private void initData() {
        contentId = getIntent().getStringExtra("contentId");
        mediaType = getIntent().getStringExtra("MediaType");
        // 从Asset中取出的表情list
        if (GlobalConfig.staticFacesList != null && GlobalConfig.staticFacesList.size() > 0) {
            faceList = GlobalConfig.staticFacesList;
        }
    }

    // 初始化视图
    private void initView() {
        commentList = (XListView) findViewById(R.id.lv_comment);
        commentList.setSelector(new ColorDrawable(Color.TRANSPARENT));// 取消默认 selector

        mViewPager = (ViewPager) findViewById(R.id.face_viewpager);
        mViewPager.setOnPageChangeListener(new PageChange());

        chatFaceContainerView = findViewById(R.id.chat_face_container);// 表情布局
        mDotsLayout = (LinearLayout) findViewById(R.id.face_dots_container);// 表情下小圆点
        input = (MyEditText) findViewById(R.id.input_sms);

        tipView = (TipView) findViewById(R.id.tip_view);
        tipView.setWhiteClick(this);
    }

    // 初始化点击事件
    private void initEvent() {
        findViewById(R.id.head_left_btn).setOnClickListener(this);// 返回
        findViewById(R.id.image_face).setOnClickListener(this);// 表情按钮
        findViewById(R.id.send_sms).setOnClickListener(this);// 发送
    }


    // 删除评论确认对话框
    private void delDialog() {
        final View dialog = LayoutInflater.from(context).inflate(R.layout.dialog_exit_confirm, null);
        TextView tv_title = (TextView) dialog.findViewById(R.id.tv_title);
        tv_title.setText("删除该条评论？");
        confirmDialog = new Dialog(context, R.style.MyDialog);
        confirmDialog.setContentView(dialog);
        confirmDialog.setCanceledOnTouchOutside(true);
        confirmDialog.getWindow().setBackgroundDrawableResource(R.color.dialog);
        dialog.findViewById(R.id.tv_cancle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.dismiss();
            }
        });
        dialog.findViewById(R.id.tv_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonHelper.checkNetwork(context)) {
                    sendDelComment(discussId);
                    confirmDialog.dismiss();
                }
            }
        });
    }

    // 初始化展示列表控件
    private void initListViewListener() {
        commentList.setPullRefreshEnable(true);
        commentList.setPullLoadEnable(true);
        commentList.setXListViewListener(new XListView.IXListViewListener() {
            @Override
            public void onRefresh() {
                refreshType = 1;
                page = 1;
                send();
            }

            @Override
            public void onLoadMore() {
                refreshType = 2;
                send();
            }
        });
    }

    private void setListView() {
        commentList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (CommonUtils.getUserId(context) != null) {
                    if (newList != null && newList.size() > 0) {
                        discussId = newList.get(position-1).getId();
                        if (newList.get(position-1).getUserId().equals(CommonUtils.getUserId(context))) {
                            if (discussId != null && !discussId.trim().equals("")) {
                                confirmDialog.show();
                            } else {
                                ToastUtils.show_always(context, "服务器返回数据异常,请稍后重试");
                            }
                        } else {
                            ToastUtils.show_always(context, "这条评论不是您提交的，您无权删除");
                        }
                    }
                } else {
                    ToastUtils.show_always(context, "删除评论需要您先登录");
                }
                return false;
            }
        });
    }

    // 获取评论列表
    private void callInternet() {
        if (contentId != null && !contentId.equals("")) {
            if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
                send();
            } else {
                tipView.setVisibility(View.VISIBLE);
                tipView.setTipView(TipView.TipStatus.NO_NET);
            }
        } else {
            tipView.setVisibility(View.VISIBLE);
            tipView.setTipView(TipView.TipStatus.IS_ERROR);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.input_sms:// 输入框
                if (chatFaceContainerView.getVisibility() == View.VISIBLE) {
                    chatFaceContainerView.setVisibility(View.GONE);
                }
                break;
            case R.id.image_face:// 表情
                hideSoftInputView();// 隐藏软键盘
                if (chatFaceContainerView.getVisibility() == View.GONE) {
                    chatFaceContainerView.setVisibility(View.VISIBLE);
                } else {
                    chatFaceContainerView.setVisibility(View.GONE);
                }
                break;
            case R.id.send_sms:// 发送
                String s = input.getText().toString().trim();
                if (!s.equals("")) {
                    if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
                        long time1 = System.currentTimeMillis();
                        if (time1 - time2 > 5000) {
                            sendComment(s);
                        } else {
                            ToastUtils.show_always(context, "您发言太快了，请稍候");
                        }
                    } else {
                        ToastUtils.show_short(context, "网络失败，请检查网络");
                    }
                } else {
                    ToastUtils.show_always(context, "请输入您要输入的评论");
                }
                break;
            case R.id.head_left_btn:// 返回
                finish();
                break;
        }
    }

    // 获取评论
    private void send() {
        tipView.setVisibility(View.GONE);
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            jsonObject.put("MediaType", mediaType);
            jsonObject.put("ContentId", contentId);
            jsonObject.put("IsPub", "1");
            jsonObject.put("Page", String.valueOf(page));
            jsonObject.put("PageSize", "10");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        VolleyRequest.requestPost(GlobalConfig.getMyCommentListUrl, tag, jsonObject, new VolleyCallback() {
            @Override
            protected void requestSuccess(JSONObject result) {
                if (isCancelRequest) return;
                try {
                    String ReturnType = result.getString("ReturnType");
                    if (ReturnType != null && ReturnType.equals("1001")) {
                        page++;
                        List<opinion> OM = new Gson().fromJson(result.getString("DiscussList"), new TypeToken<List<opinion>>() {
                        }.getType());

                        if (refreshType == 1) newList.clear();

                        // 对服务器返回的事件进行 sd 处理
                        for (int i = 0; i < OM.size(); i++) {
                            if (OM.get(i).getTime() != null) {
                                long time = Long.valueOf(OM.get(i).getTime());
                                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
                                OM.get(i).setTime(sd.format(new Date(time)));
                            }
                        }

                        newList.addAll(OM);
                        if (newList == null || newList.size() == 0) {
                            commentList.setAdapter(new ContentNoAdapter(context));
                            return;
                        }

                        if(adapter==null){
                            adapter=new ChatLVAdapter(context, newList);
                            commentList.setAdapter(adapter);
                        }else{
                            adapter.notifyDataSetChanged();
                        }

                    } else {
                        commentList.setAdapter(new ContentNoAdapter(context));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    commentList.setAdapter(new ContentNoAdapter(context));
                }
                setListView();
                // 无论何种返回值，都需要终止掉下拉刷新及上拉加载的滚动状态
                if (newList != null && newList.size() > 10) {
                    commentList.setPullLoadEnable(true);
                } else {
                    commentList.setPullLoadEnable(false);
                }
                if (refreshType == 1) {
                    commentList.stopRefresh();
                } else {
                    commentList.stopLoadMore();
                }
            }

            @Override
            protected void requestError(VolleyError error) {
                ToastUtils.showVolleyError(context);
                commentList.setAdapter(new ContentNoAdapter(context));
            }
        });
    }

    // 删除评论
    private void sendDelComment(final String id) {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            jsonObject.put("ContentId", contentId);
            jsonObject.put("DiscussId", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        VolleyRequest.requestPost(GlobalConfig.delCommentUrl, tag, jsonObject, new VolleyCallback() {
            @Override
            protected void requestSuccess(JSONObject result) {
                if (isCancelRequest) return;
                try {
                    String ReturnType = result.getString("ReturnType");
                    if (ReturnType != null && ReturnType.equals("1001")) {
                        setDelData(id);
                    } else {
                        ToastUtils.show_always(context, "网络失败，请检查网络");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void requestError(VolleyError error) {
                ToastUtils.showVolleyError(context);
            }
        });
    }

    // 组装展示删除数据
    private void setDelData(String _id) {
        if (newList != null && newList.size() > 0) {
            for (int i = 0; i < newList.size(); i++) {
                try {
                    String id = newList.get(i).getId();
                    if (id != null && !id.trim().equals("") && id.trim().equals(_id)) {
                        newList.remove(i);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (newList == null || newList.size() == 0) {
            commentList.setAdapter(new ContentNoAdapter(context));
            return;
        }
        if(adapter==null){
            adapter=new ChatLVAdapter(context, newList);
            commentList.setAdapter(adapter);
        }else{
            adapter.notifyDataSetChanged();
        }
    }

    // 发表评论
    private void sendComment(String opinion) {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            jsonObject.put("MediaType", mediaType);
            jsonObject.put("ContentId", contentId);
            jsonObject.put("Discuss", opinion);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VolleyRequest.requestPost(GlobalConfig.pushCommentUrl, tag, jsonObject, new VolleyCallback() {
            @Override
            protected void requestSuccess(JSONObject result) {
                if (isCancelRequest) return;
                try {
                    String ReturnType = result.getString("ReturnType");
                    if (ReturnType != null && ReturnType.equals("1001")) {
                        // 请求成功 取消 editText 焦点 重新执行获取列表的操作
                        time2 = System.currentTimeMillis();
                        input.setText("");
                        getFirstData();
                    } else {
                        ToastUtils.show_always(context, "发表评论失败，请稍后重试!");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void requestError(VolleyError error) {
                ToastUtils.showVolleyError(context);
            }
        });
    }

    // 获取第一页的数据，为了组装新的展示数据
    private void getFirstData() {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            jsonObject.put("MediaType", mediaType);
            jsonObject.put("ContentId", contentId);
            jsonObject.put("IsPub", "1");
            jsonObject.put("Page", "1");
            jsonObject.put("PageSize", "1");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        VolleyRequest.requestPost(GlobalConfig.getMyCommentListUrl, tag, jsonObject, new VolleyCallback() {
            @Override
            protected void requestSuccess(JSONObject result) {
                if (isCancelRequest) return;
                try {
                    String ReturnType = result.getString("ReturnType");
                    if (ReturnType != null && ReturnType.equals("1001")) {
                        List<opinion> OM = new Gson().fromJson(result.getString("DiscussList"), new TypeToken<List<opinion>>() {
                        }.getType());

                        for (int i = 0; i < OM.size(); i++) {
                            if (OM.get(i).getTime() != null) {
                                long time = Long.valueOf(OM.get(i).getTime());
                                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
                                OM.get(i).setTime(sd.format(new Date(time)));
                                newList.add(i, OM.get(i));
                            }
                        }

                        if (newList == null || newList.size() == 0) {
                            commentList.setAdapter(new ContentNoAdapter(context));
                            return;
                        }
                        if(adapter==null){
                            adapter=new ChatLVAdapter(context, newList);
                            commentList.setAdapter(adapter);
                        }else{
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void requestError(VolleyError error) {
            }
        });
    }

    private void initViewPager() {
        for (int i = 0; i < getPagerCount(); i++) {
            views.add(viewPagerItem(i));
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(16, 16);
            mDotsLayout.addView(dotsItem(i), params);
        }
        FaceVPAdapter mVpAdapter = new FaceVPAdapter(views);
        mViewPager.setAdapter(mVpAdapter);
        mDotsLayout.getChildAt(0).setSelected(true);
    }

    private int getPagerCount() {
        int count = faceList.size();
        return count % (columns * rows - 1) == 0 ? count / (columns * rows - 1) : count / (columns * rows - 1) + 1;
    }

    private ImageView dotsItem(int position) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dot_image, null);
        ImageView iv = (ImageView) layout.findViewById(R.id.face_dot);
        iv.setId(position);
        return iv;
    }

    private View viewPagerItem(int position) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.face_gridview, null);//表情布局
        GridView gridview = (GridView) layout.findViewById(R.id.chart_face_gv);
        // 注：因为每一页末尾都有一个删除图标，所以每一页的实际表情columns *　rows　－　1; 空出最后一个位置给删除图标
        List<String> subList = new ArrayList<>();
        subList.addAll(faceList.subList(position * (columns * rows - 1),
                (columns * rows - 1) * (position + 1) > faceList.size() ? faceList.size() : (columns * rows - 1) * (position + 1)));

        // 末尾添加删除图标
        subList.add("emotion_del_normal.png");
        FaceGVAdapter mGvAdapter = new FaceGVAdapter(subList, context);
        gridview.setAdapter(mGvAdapter);
        gridview.setNumColumns(columns);
        // 单击表情执行的操作
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    String png = ((TextView) ((LinearLayout) view).getChildAt(1)).getText().toString();
                    if (!png.contains("emotion_del_normal")) {// 如果不是删除图标
                        insert(getFace(png));
                    } else {
                        delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return gridview;
    }

    //删除图标执行事件
    // 注：如果删除的是表情，在删除时实际删除的是tempText即图片占位的字符串，所以必需一次性删除掉tempText，才能将图片删除
    private void delete() {
        if (input.getText().length() != 0) {
            int iCursorEnd = Selection.getSelectionEnd(input.getText());
            int iCursorStart = Selection.getSelectionStart(input.getText());
            if (iCursorEnd > 0) {
                if (iCursorEnd == iCursorStart) {
                    if (isDeletePng(iCursorEnd)) {
                        String st = "#[face/png/f_static_000.png]#";
                        (input.getText()).delete(iCursorEnd - st.length(), iCursorEnd);
                    } else {
                        (input.getText()).delete(iCursorEnd - 1, iCursorEnd);
                    }
                } else {
                    (input.getText()).delete(iCursorStart, iCursorEnd);
                }
            }
        }
    }

    // 判断即将删除的字符串是否是图片占位字符串 tempText 如果是：则讲删除整个 tempText
    private boolean isDeletePng(int cursor) {
        String st = "#[face/png/f_static_000.png]#";
        String content = input.getText().toString().substring(0, cursor);
        if (content.length() >= st.length()) {
            String checkStr = content.substring(content.length() - st.length(), content.length());
            String regex = "(\\#\\[face/png/f_static_)\\d{3}(.png\\]\\#)";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(checkStr);
            return m.matches();
        }
        return false;
    }

    // 向输入框里添加表情
    private void insert(CharSequence text) {
        int iCursorStart = Selection.getSelectionStart((input.getText()));
        int iCursorEnd = Selection.getSelectionEnd((input.getText()));
        if (iCursorStart != iCursorEnd) {
            (input.getText()).replace(iCursorStart, iCursorEnd, "");
        }
        int iCursor = Selection.getSelectionEnd((input.getText()));
        (input.getText()).insert(iCursor, text);
    }

    private SpannableStringBuilder getFace(String png) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        try {
            /**
             * 经过测试，虽然这里tempText被替换为png显示，但是但我单击发送按钮时，获取到輸入框的内容是tempText的值而不是png
             * 所以这里对这个tempText值做特殊处理
             * 格式：#[face/png/f_static_000.png]#，以方便判斷當前圖片是哪一個
             * */
            String tempText = "#[" + png + "]#";
            stringBuilder.append(tempText);
            stringBuilder.setSpan(new ImageSpan(context, BitmapFactory.decodeStream(getAssets().open(png))),
                    stringBuilder.length() - tempText.length(), stringBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // 点击空白处隐藏键盘
        commentList.setFocusable(true);
        commentList.setFocusableInTouchMode(true);
        commentList.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        // 隐藏键盘
        imm.hideSoftInputFromWindow(commentList.getWindowToken(), 0);
        return true;
    }

    // 表情 viewPage 的监听 ==== 表情页改变时，dots 效果也要跟着改变
    class PageChange implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int arg0) {
            for (int i = 0; i < mDotsLayout.getChildCount(); i++) {
                mDotsLayout.getChildAt(i).setSelected(false);
            }
            mDotsLayout.getChildAt(arg0).setSelected(true);
        }
    }

    // 隐藏软键盘
    public void hideSoftInputView() {
        InputMethodManager manager = ((InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE));
        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getCurrentFocus() != null) {
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isCancelRequest = VolleyRequest.cancelRequest(tag);
    }
}
