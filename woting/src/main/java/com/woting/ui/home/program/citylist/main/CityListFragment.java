package com.woting.ui.home.program.citylist.main;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.woting.R;
import com.woting.common.application.BSApplication;
import com.woting.common.config.GlobalConfig;
import com.woting.common.constant.BroadcastConstants;
import com.woting.common.constant.StringConstant;
import com.woting.common.util.DialogUtils;
import com.woting.common.volley.VolleyCallback;
import com.woting.common.volley.VolleyRequest;
import com.woting.common.widgetui.TipView;
import com.woting.ui.home.main.HomeActivity;
import com.woting.ui.home.model.Catalog;
import com.woting.ui.home.model.CatalogName;
import com.woting.ui.home.program.citylist.adapter.CityListAdapter;
import com.woting.ui.home.program.diantai.fragment.CityRadioFragment;
import com.woting.ui.interphone.linkman.view.CharacterParser;
import com.woting.ui.interphone.linkman.view.PinyinComparator_d;
import com.woting.ui.interphone.linkman.view.SideBar;
import com.woting.ui.interphone.linkman.view.SideBar.OnTouchingLetterChangedListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 城市列表
 * @author 辛龙
 * 2016年4月7日
 */
public class CityListFragment extends Fragment implements OnClickListener, TipView.WhiteViewClick {
    private Context context;
    private CharacterParser characterParser;
    private PinyinComparator_d pinyinComparator;
    private CityListAdapter adapter;
    private List<CatalogName> srcList;
    private List<CatalogName> userList = new ArrayList<>();

    private View rootView;
    private TipView tipView;// 出错、没有数据、没有网络提示
    private TipView tipSearchNull;// 搜索为空
    private Dialog dialog;
    private SideBar sideBar;
    private TextView dialogs;
    private ListView listView;
    private EditText et_Search_content;
    private ImageView image_clear;

    private boolean isCancelRequest;
    private String tag = "CITY_LIST_REQUEST_CANCEL_TAG";
    private String type;

    @Override
    public void onWhiteViewClick() {
        if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
            dialog = DialogUtils.Dialogph(context, "正在获取信息");
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

        Bundle bundle = getArguments();
        type = bundle.getString("type");
        characterParser = CharacterParser.getInstance();// 实例化汉字转拼音类
        pinyinComparator = new PinyinComparator_d();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.activity_citylists, container, false);
            rootView.setOnClickListener(this);

            setView();
            setListener();
            if (GlobalConfig.CityCatalogList != null && GlobalConfig.CityCatalogList.size() > 0) {
                srcList = GlobalConfig.CityCatalogList;
                handleCityList(srcList);
            } else {
                if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
                    dialog = DialogUtils.Dialogph(context, "正在获取信息");
                    sendRequest();
                } else {
                    tipView.setVisibility(View.VISIBLE);
                    tipView.setTipView(TipView.TipStatus.NO_NET);
                }
            }
        }
        return rootView;
    }

    private void setView() {
        sideBar = (SideBar) rootView.findViewById(R.id.sidrbar);
        dialogs = (TextView) rootView.findViewById(R.id.dialog);
        sideBar.setTextView(dialogs);
        listView = (ListView) rootView.findViewById(R.id.country_lvcountry);
        et_Search_content = (EditText) rootView.findViewById(R.id.et_search);
        image_clear = (ImageView) rootView.findViewById(R.id.image_clear);

        tipView = (TipView) rootView.findViewById(R.id.tip_view);
        tipView.setWhiteClick(this);

        tipSearchNull = (TipView) rootView.findViewById(R.id.tip_search_null);
        tipSearchNull.setTipView(TipView.TipStatus.NO_DATA, "没有找到该城市\n换个城市再试一次吧");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.head_left_btn:// 返回
                HomeActivity.close();
                break;
            case R.id.image_clear:
                image_clear.setVisibility(View.INVISIBLE);
                et_Search_content.setText("");
                break;
        }
    }

    private void handleCityList(List<CatalogName> srcList) {
        if (srcList.size() == 0) {
            tipView.setVisibility(View.VISIBLE);
            tipView.setTipView(TipView.TipStatus.NO_DATA, "数据列表为空!");
        } else {
            tipView.setVisibility(View.GONE);
            userList.clear();
            userList.addAll(srcList);
            filledData(userList);
            Collections.sort(userList, pinyinComparator);
            listView.setAdapter(adapter = new CityListAdapter(context, userList));
            setInterface();
        }
    }

    // 发送网络请求
    private void sendRequest() {
        VolleyRequest.requestPost(GlobalConfig.getCatalogUrl, tag, setParam(), new VolleyCallback() {
            private String ReturnType;

            @Override
            protected void requestSuccess(JSONObject result) {
                if (dialog != null) dialog.dismiss();
                if (isCancelRequest) return;
                try {
                    ReturnType = result.getString("ReturnType");
                    if (ReturnType != null && ReturnType.equals("1001")) {
                        Catalog subListAll = new Gson().fromJson(result.getString("CatalogData"), new TypeToken<Catalog>() {}.getType());
                        srcList = subListAll.getSubCata();
                        GlobalConfig.CityCatalogList = srcList;
                        handleCityList(srcList);
                    } else {
                        tipView.setVisibility(View.VISIBLE);
                        tipView.setTipView(TipView.TipStatus.IS_ERROR);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    tipView.setVisibility(View.VISIBLE);
                    tipView.setTipView(TipView.TipStatus.IS_ERROR);
                }
            }

            @Override
            protected void requestError(VolleyError error) {
                if (dialog != null) dialog.dismiss();
                tipView.setVisibility(View.VISIBLE);
                tipView.setTipView(TipView.TipStatus.IS_ERROR);
            }
        });
    }

    // 设置请求参数
    private JSONObject setParam() {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            jsonObject.put("CatalogType", "2");
            jsonObject.put("ResultType", "1");
            jsonObject.put("RelLevel", "3");
            jsonObject.put("Page", "1");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void filledData(List<CatalogName> person) {
        for (int i = 0; i < person.size(); i++) {
            person.get(i).setName(person.get(i).getCatalogName());
            String pinyin = characterParser.getSelling(person.get(i).getCatalogName());// 汉字转换成拼音
            String sortString = pinyin.substring(0, 1).toUpperCase();
            if (sortString.matches("[A-Z]")) {// 正则表达式，判断首字母是否是英文字母
                person.get(i).setSortLetters(sortString.toUpperCase());
            } else {
                person.get(i).setSortLetters("#");
            }
        }
    }

    private void setInterface() {
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (type != null && !type.trim().equals("") && type.equals("address")) {
                    SharedPreferences sp = BSApplication.SharedPreferences;
                    Editor et = sp.edit();
                    et.putString(StringConstant.CITYTYPE, "true");
                    if (userList.get(position).getCatalogId() != null && !userList.get(position).getCatalogId().equals("")) {
                        et.putString(StringConstant.CITYID, userList.get(position).getCatalogId());
                        GlobalConfig.AdCode = userList.get(position).getCatalogId();
                    }
                    if (userList.get(position).getCatalogName() != null && !userList.get(position).getCatalogName().equals("")) {
                        et.putString(StringConstant.CITYNAME, userList.get(position).getCatalogName());
                        GlobalConfig.CityName = userList.get(position).getCatalogName();
                    }
                    et.commit();
                    context.sendBroadcast(new Intent(BroadcastConstants.CITY_CHANGE));
                    HomeActivity.close();
                } else {
                    CityRadioFragment fragment = new CityRadioFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("fromtype", "city");
                    bundle.putString("name", userList.get(position).getCatalogName());
                    bundle.putString("type", "2");
                    bundle.putString("id", userList.get(position).getCatalogId());
                    fragment.setArguments(bundle);
                    HomeActivity.open(fragment);
                }
            }
        });

        // 设置右侧触摸监听
        sideBar.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(String s) {
                int position = adapter.getPositionForSection(s.charAt(0));// 该字母首次出现的位置
                if (position != -1) {
                    listView.setSelection(position);
                }
            }
        });
    }

    private void setListener() {
        rootView.findViewById(R.id.head_left_btn).setOnClickListener(this);
        image_clear.setOnClickListener(this);

        // 当输入框输入过汉字，且回复0后就要调用使用 userList1 的原表数据
        et_Search_content.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String searchName = s.toString();
                if (searchName.trim().equals("")) {// 关键词为空
                    image_clear.setVisibility(View.INVISIBLE);
                    tipSearchNull.setVisibility(View.GONE);
                    if (srcList == null || srcList.size() == 0) {
                        listView.setVisibility(View.GONE);
                    } else {
                        listView.setVisibility(View.VISIBLE);
                        userList.clear();
                        userList.addAll(srcList);
                        filledData(userList);
                        Collections.sort(userList, pinyinComparator);
                        adapter = new CityListAdapter(context, userList);
                        listView.setAdapter(adapter);
                        setInterface();
                    }
                } else {
                    userList.clear();
                    userList.addAll(srcList);
                    image_clear.setVisibility(View.VISIBLE);
                    search(searchName);
                }
            }
        });
    }

    // 根据输入框中的值来过滤数据并更新ListView
    private void search(String search_name) {
        List<CatalogName> filterDateList = new ArrayList<>();
        if (TextUtils.isEmpty(search_name)) {
            filterDateList = userList;
            tipSearchNull.setVisibility(View.GONE);
        } else {
            filterDateList.clear();
            for (CatalogName sortModel : userList) {
                String name = sortModel.getName();
                if (name.contains(search_name) || characterParser.getSelling(name).startsWith(search_name)) {
                    filterDateList.add(sortModel);
                }
            }
        }

        // 根据a-z进行排序
        Collections.sort(filterDateList, pinyinComparator);
        adapter.ChangeDate(filterDateList);
        userList.clear();
        userList.addAll(filterDateList);
        if (filterDateList.size() == 0) {
            tipSearchNull.setVisibility(View.VISIBLE);
            tipSearchNull.setTipView(TipView.TipStatus.NO_DATA, "没有找到相关城市\n换个城市搜索一下吧");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isCancelRequest = VolleyRequest.cancelRequest(tag);
        srcList = null;
        userList = null;
        adapter = null;
        sideBar = null;
        dialogs = null;
        listView = null;
        et_Search_content = null;
        listView = null;
        image_clear = null;
        pinyinComparator = null;
        context = null;
        characterParser = null;
    }
}
