package com.woting.ui.home.program.album.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.woting.common.util.CommonUtils;
import com.woting.common.util.DialogUtils;
import com.woting.common.util.ToastUtils;
import com.woting.common.volley.VolleyCallback;
import com.woting.common.volley.VolleyRequest;
import com.woting.common.widgetui.xlistview.XListView;
import com.woting.ui.download.dao.FileInfoDao;
import com.woting.ui.download.model.FileInfo;
import com.woting.ui.download.service.DownloadService;
import com.woting.ui.home.main.HomeActivity;
import com.woting.ui.home.player.main.dao.SearchPlayerHistoryDao;
import com.woting.ui.home.player.main.fragment.PlayerFragment;
import com.woting.ui.home.player.main.model.PlayerHistory;
import com.woting.ui.home.program.album.activity.AlbumActivity;
import com.woting.ui.home.program.album.adapter.AlbumAdapter;
import com.woting.ui.home.program.album.adapter.AlbumMainAdapter;
import com.woting.ui.home.program.album.model.ContentInfo;
import com.woting.ui.main.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 专辑列表页
 * 作者：xinlong on 2016/11/16 17:40
 * 邮箱：645700751@qq.com
 */
public class ProgramFragment extends Fragment implements OnClickListener {
	private View rootView;
	private Context context;
	private FileInfoDao FID;
	private SearchPlayerHistoryDao dbDao;
	private Dialog dialog;
	private XListView lv_album;         // 节目列表
	private ListView  lv_download; 		// 下载列表
	private ImageView img_download, img_quanxuan; 	// 下载 全选
	private TextView tv_quxiao, tv_download, tv_sum, textTotal;
	private LinearLayout lin_quanxuan, lin_status2;
	private ImageView imageSort;					// 排序
	private ImageView imageSortDown;
	private AlbumMainAdapter mainAdapter;
	private AlbumAdapter adapter;
	private List<ContentInfo> SubListAll = new ArrayList<>();
	private List<ContentInfo> urlList = new ArrayList<>();
	private List<ContentInfo> SubList; 				// 请求返回的网络数据值
	private List<FileInfo> fList;
	private boolean flag = false; 					// 标记全选的按钮
	private int sum = 0; 							// 计数项
	private String userId;
	private boolean isCancelRequest;
	private String tag = "PROGRAM_VOLLEY_REQUEST_CANCEL_TAG";
	protected String sequId;
	protected String sequName;
	protected String sequImg;
	protected String sequDesc;
	private int page=1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
		userId = CommonUtils.getUserId(context);
		initDao();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (rootView == null) {
			rootView = inflater.inflate(R.layout.fragment_album_program, container, false);
			findView(rootView);
			if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
				dialog = DialogUtils.Dialogph(context, "正在获取数据");
				send();
			} else {
				ToastUtils.show_short(context, "网络失败，请检查网络");
			}
		}
		return rootView;
	}

	/**
	 * 初始化控件
	 *
	 * @param view
	 */
	private void findView(View view) {
		lv_album = (XListView) view.findViewById(R.id.lv_album); 			// 专辑显示界面
		img_download = (ImageView) view.findViewById(R.id.img_download);
		img_download.setOnClickListener(this);
		tv_quxiao = (TextView) view.findViewById(R.id.tv_quxiao); 			// 取消动画
		tv_quxiao.setOnClickListener(this);
		img_quanxuan = (ImageView) view.findViewById(R.id.img_quanxuan); 	// img_quanxuan
		lin_quanxuan = (LinearLayout) view.findViewById(R.id.lin_quanxuan); // lin_quanxuan
		lin_quanxuan.setOnClickListener(this);
		lv_download = (ListView) view.findViewById(R.id.lv_download); 		// lv_download
		tv_download = (TextView) view.findViewById(R.id.tv_download); 		// 开始下载
		tv_download.setOnClickListener(this);
		tv_sum = (TextView) view.findViewById(R.id.tv_sum); 				// 计数项
		lin_status2 = (LinearLayout) view.findViewById(R.id.lin_status2); 	// 第二种状态
		textTotal = (TextView) view.findViewById(R.id.text_total); 			// 下载列表的总计

		imageSort = (ImageView) view.findViewById(R.id.img_sort);			// 排序
		imageSort.setOnClickListener(this);

		imageSortDown = (ImageView) view.findViewById(R.id.img_sort_down);
		imageSortDown.setOnClickListener(this);

		lv_album.setPullLoadEnable(true);
		lv_album.setPullRefreshEnable(false);
		lv_album.setXListViewListener(new XListView.IXListViewListener() {
			@Override
			public void onRefresh() {

			}
			@Override
			public void onLoadMore() {
				if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
					dialog = DialogUtils.Dialogph(context, "正在获取数据");
					send();
				} else {
					ToastUtils.show_short(context, "网络失败，请检查网络");
				}
			}
		});

	}

	/**
	 * ListView 的 Item 的监听事件
	 */
	private void setListener() {
		lv_album.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				/*ToastUtils.show_always(context,SubListAll.get(position-1).getContentName());*/
				if (SubListAll != null && SubListAll.get(position-1) != null && SubListAll.get(position-1).getMediaType() != null) {
					String MediaType = SubListAll.get(position-1).getMediaType();
					if (MediaType.equals("RADIO") || MediaType.equals("AUDIO")) {
						String playerName = SubListAll.get(position-1).getContentName();
						String playerImage = SubListAll.get(position-1).getContentImg();
						String playUrl = SubListAll.get(position-1).getContentPlay();
						String playUrI = SubListAll.get(position-1).getContentURI();
						String playMediaType = SubListAll.get(position-1).getMediaType();
						String playContentShareUrl = SubListAll.get(position-1).getContentShareURL();
						String ContentId = SubListAll.get(position-1).getContentId();
						String playAllTime = SubListAll.get(position-1).getContentTimes();
						String playInTime = "0";
						String playContentDesc = SubListAll.get(position-1).getContentDescn();
						String playNum = SubListAll.get(position-1).getPlayCount();
						String playZanType = "0";
						String playFrom = SubListAll.get(position-1).getContentPub();
						String playFromId = "";
						String playFromUrl = "";
						String playAddTime = Long.toString(System.currentTimeMillis());
						String bjUserId = CommonUtils.getUserId(context);
						String ContentFavorite =SubListAll.get(position-1).getContentFavorite();
						String localUrl=SubListAll.get(position-1).getLocalurl();
						//name id desc img
						String sequName1=sequName;
						String sequId1=sequId;
						String sequDesc1=sequDesc;
						String sequImg1=sequImg;
						String ContentPlayType=SubListAll.get(position-1).getContentPlayType();

						PlayerHistory history = new PlayerHistory(
								playerName,  playerImage, playUrl, playUrI,playMediaType,
								playAllTime, playInTime, playContentDesc, playNum,
								playZanType, playFrom , playFromId,playFromUrl,playAddTime,bjUserId,playContentShareUrl,
								ContentFavorite,ContentId,localUrl,sequName1,sequId1,sequDesc1,sequImg1,ContentPlayType);
						dbDao.deleteHistory(playUrl);
						dbDao.addHistory(history);
						if(PlayerFragment.context!=null){
							MainActivity.change();
							HomeActivity.UpdateViewPager();
							PlayerFragment.TextPage=1;
							Intent push=new Intent(BroadcastConstants.PLAY_TEXT_VOICE_SEARCH);
							Bundle bundle1=new Bundle();
							bundle1.putString("text", SubListAll.get(position-1).getContentName());
							push.putExtras(bundle1);
							context.sendBroadcast(push);
						}else{
							SharedPreferences.Editor et = BSApplication.SharedPreferences.edit();
							et.putString(StringConstant.PLAYHISTORYENTER, "true");
							et.putString(StringConstant.PLAYHISTORYENTERNEWS,SubListAll.get(position-1).getContentName());
							et.commit();
							MainActivity.change();
							HomeActivity.UpdateViewPager();
						}
						getActivity().setResult(1);
						getActivity().finish();
					} else {
						ToastUtils.show_always(context, "暂不支持播放");
					}
				}
			}
		});
	}

	/**
	 * 向服务器发送请求
	 */
	public void send() {
		JSONObject jsonObject = VolleyRequest.getJsonObject(context);
		try {
			jsonObject.put("MediaType", "SEQU");
			jsonObject.put("ContentId", AlbumActivity.id);
			jsonObject.put("Page",String.valueOf(page));
			jsonObject.put("PageSize","20");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		VolleyRequest.RequestPost(GlobalConfig.getContentById, tag, jsonObject, new VolleyCallback() {

			@Override
			protected void requestSuccess(JSONObject result) {
				if (dialog != null) {
					dialog.dismiss();
				}
				if(isCancelRequest){
					return ;
				}
				try {
					String ReturnType = result.getString("ReturnType");
					if (ReturnType != null) { // 根据返回值来对程序进行解析
						if (ReturnType.equals("1001")) {
							page++;
							try {
								String	ResultList = result.getString("ResultInfo"); // 获取列表
								JSONTokener jsonParser = new JSONTokener(ResultList);
								JSONObject arg1 = (JSONObject) jsonParser.nextValue();
								// 此处后期需要用typeToken将字符串StringSubList 转化成为一个list集合
								try{
									String total =arg1.getString("ContentSubCount");
									textTotal.setText("共" + total + "集");
								}catch(Exception e){
									e.printStackTrace();
								}

								try {
									String StringSubList = arg1.getString("SubList");

									Gson gson = new Gson();
									SubList = gson.fromJson(StringSubList, new TypeToken<List<ContentInfo>>() {}.getType());
									if (SubList != null && SubList.size() > 0) {
										//SubListAll.clear();
										SubListAll.addAll(SubList);
										mainAdapter = new AlbumMainAdapter(context, SubListAll);
										lv_album.setAdapter(mainAdapter);
										setListener();
										getData();
										adapter = new AlbumAdapter(context, SubListAll);
										lv_download.setAdapter(adapter);
										setInterface();

										if(SubList.size()!=20){
											lv_album.setPullLoadEnable(false);
											ToastUtils.show_always(context,"本专辑已经没有更多节目了");
										}
									/*	int showpage=page-1;
										ToastUtils.show_always(context,"请求了第"+showpage+"页的信息,此时list的长度为"+SubListAll.size());*/
									}
								}catch (Exception e){

								}
								try {
									sequId=arg1.getString("ContentId");
								}catch (Exception e){

								}
								try {
									sequImg=arg1.getString("ContentImg");
								}catch (Exception e){

								}
								try {
									sequName=arg1.getString("ContentName");
								}catch (Exception e){

								}
								try {
									sequDesc=arg1.getString("ContentDescn");
								}catch (Exception e){

								}
								lv_album.stopLoadMore();
							} catch (Exception e) {
								e.printStackTrace();
								lv_album.stopLoadMore();
							}
						}else{
							if (ReturnType.equals("0000")) {
//							ToastUtils.show_always(context, "无法获取相关的参数");
								ToastUtils.show_always(context, "数据出错了，请稍后再试！");
							} else if (ReturnType.equals("1002")) {
//							ToastUtils.show_always(context, "无此分类信息");
								ToastUtils.show_always(context, "数据出错了，请稍后再试！");
							} else if (ReturnType.equals("1003")) {
//							ToastUtils.show_always(context, "无法获得列表");
								ToastUtils.show_always(context, "数据出错了，请稍后再试！");
							} else if (ReturnType.equals("1011")) {
//							ToastUtils.show_always(context, "列表为空（列表为空[size==0]");
								ToastUtils.show_always(context, "数据出错了，请稍后再试！");
							} else if (ReturnType.equals("T")) {
//							ToastUtils.show_always(context, "获取列表异常");
								ToastUtils.show_always(context, "数据出错了，请稍后再试！");
							}
							lv_album.stopLoadMore();
							lv_album.setPullLoadEnable(false);

						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			protected void requestError(VolleyError error) {
				if (dialog != null) {
					dialog.dismiss();
				}
				lv_album.stopLoadMore();
			}
		});
	}



	/**
	 * 实现接口的方法
	 */
	private void setInterface() {
		lv_download.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (SubListAll != null && SubListAll.get(position) != null) {
					if (SubListAll.get(position).getCheckType() == 3) {
						ToastUtils.show_always(context, "已经下载过");
					} else {
						if (SubListAll.get(position).getCheckType() == 1) {
							SubListAll.get(position).setCheckType(2);
						} else {
							SubListAll.get(position).setCheckType(1);
						}
						int downLoadSum = 0;
						sum = 0;
						for (int i = 0; i < SubListAll.size(); i++) {
							if (SubListAll.get(i).getCheckType() == 2) {
								sum++;
							}
							if(SubListAll.get(i).getCheckType() == 3) {
								downLoadSum++;
							}
							setSum();
							adapter.notifyDataSetChanged();
						}

						//更新全选图标
						if(sum == (SubListAll.size() - downLoadSum)){
							flag = true;
							img_quanxuan.setImageResource(R.mipmap.wt_group_checked);
						}else{
							flag = false;
							img_quanxuan.setImageResource(R.mipmap.wt_group_nochecked);
						}
					}
				}
			/*	ToastUtils.show_always(context,SubListAll.get(position).getContentName());*/
			}
		});
	}

	/**
	 * 获取数据
	 */
	private void getData() {
		fList = FID.queryFileInfoAll(userId);
		Log.e("fList", fList.size() + "");
		ArrayList<FileInfo> seqList = new ArrayList<>();
		if (fList != null && fList.size() > 0) {
			for (int i = 0; i < fList.size(); i++) {
				if (fList.get(i).getSequimgurl() != null
						&& fList.get(i).getSequimgurl().equals(AlbumActivity.ContentImg)) {

					seqList.add(fList.get(i));
				}
			}
		}
		Log.e("seqList", seqList.size() + "");
		if (seqList != null && seqList.size() > 0) {
			for (int i = 0; i < seqList.size(); i++) {
				String temp= seqList.get(i).getUrl();
				if (temp != null && !temp.trim().equals("")) {
					for (int j = 0; j < SubListAll.size(); j++) {
						if (SubListAll.get(j).getContentPlay() != null
								&& SubListAll.get(j).getContentPlay().equals(temp)) {
							SubListAll.get(j).setCheckType(3);
						}
					}
				}
			}
		}
	}

	/**
	 * 点击事件
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.img_download: 	// 显示下载列表
				if(SubListAll.size() == 0){
					return ;
				}
		/*		SubListAll.clear();
				SubListAll.addAll(SubList);*/
		/*		getData();*/
				if (adapter != null) {
					adapter.notifyDataSetChanged();
				} else {
					adapter = new AlbumAdapter(context, SubListAll);
					lv_download.setAdapter(adapter);
				}
				lv_download.setSelection(0);
				lin_status2.setVisibility(View.VISIBLE);
				break;
			case R.id.tv_quxiao: 		// 取消
				lin_status2.setVisibility(View.GONE);

				for(int i=0; i<SubListAll.size(); i++){
					if(SubListAll.get(i).getCheckType() != 3){
						img_quanxuan.setImageResource(R.mipmap.image_not_all_check);
						SubListAll.get(i).setCheckType(1);
					}
				}
				sum = 0;
				setSum();
				flag = false;
				break;
			case R.id.lin_quanxuan: 	// 全选
				if (flag == false) { 	// 默认为未选中状态
					sum = 0;
					for (int i = 0; i < SubListAll.size(); i++) {
						if (SubListAll.get(i).getCheckType() != 3) {
							SubListAll.get(i).setCheckType(2);
							sum++;
						}
					}
					flag = true;
					img_quanxuan.setImageResource(R.mipmap.wt_group_checked);
					setSum();
				} else {
					for (int i = 0; i < SubListAll.size(); i++) {
						if (SubListAll.get(i).getCheckType() != 3) {
							SubListAll.get(i).setCheckType(1);
						}
					}
					flag = false;
					img_quanxuan.setImageResource(R.mipmap.wt_group_nochecked);
					sum = 0;
					setSum();
				}
				adapter.notifyDataSetChanged();
				break;
			case R.id.tv_download: 		// 下载
				urlList.clear();
				for (int i = 0; i < SubListAll.size(); i++) {
					if(SubListAll.get(i).getCheckType()==2){
						ContentInfo mContent = SubListAll.get(i);
						mContent.setSequdesc(AlbumActivity.ContentDesc);
						mContent.setSequname(AlbumActivity.ContentName);
						mContent.setSequimgurl(AlbumActivity.ContentImg);
						mContent.setSequid(AlbumActivity.id);
						//判断userId是否为空
						mContent.setUserid(userId);
						mContent.setDownloadtype("0");
						FID.updataDownloadStatus(mContent.getContentPlay(), "0");//将所有数据设置
						urlList.add(mContent);
					}
				}
				if (urlList.size() > 0) {
					FID.insertFileInfo(urlList);
					List<FileInfo> tempList = FID.queryFileInfo("false",userId);//查询表中未完成的任务
					//未下载列表
					for(int kk=0;kk<tempList.size();kk++){
						if(tempList.get(kk).getDownloadtype()==1){
							DownloadService.workStop(tempList.get(kk));
							FID.updataDownloadStatus(tempList.get(kk).getUrl(), "2");
							Log.e("测试下载问题"," 暂停下载的单体"+(tempList.get(kk).getFileName()));
						}
					}
					tempList.get(0).setDownloadtype(1);
					FID.updataDownloadStatus(tempList.get(0).getUrl(), "1");
					Log.e("数据库内数据", tempList.toString());
					DownloadService.workStart(tempList.get(0));
					//发送更新界面数据广播
					Intent p_intent=new Intent("push_down_uncompleted");
					context.sendBroadcast(p_intent);
					lin_status2.setVisibility(View.GONE);
				} else {
					ToastUtils.show_always(context, "请重新选择数据");
					return;
				}
				break;
			case R.id.img_sort:
				if(SubListAll.size() != 0 && mainAdapter != null){
					Collections.reverse(SubListAll);			// 倒序
					mainAdapter.notifyDataSetChanged();
					imageSortDown.setVisibility(View.VISIBLE);
					imageSort.setVisibility(View.GONE);
				}
				break;
			case R.id.img_sort_down:
				if(SubListAll.size() != 0 && mainAdapter != null){
					Collections.reverse(SubListAll);			// 倒序
					mainAdapter.notifyDataSetChanged();
					imageSortDown.setVisibility(View.GONE);
					imageSort.setVisibility(View.VISIBLE);
				}
				break;
		}
	}

	protected void setSum() {
		tv_sum.setText(sum + "");
	}

	/**
	 * 初始化数据库命令执行对象
	 */
	private void initDao() {
		dbDao = new SearchPlayerHistoryDao(context);
		FID = new FileInfoDao(context);
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
		rootView = null;
		context = null;
		FID = null;
		dbDao = null;
		dialog = null;
		lv_album = null;
		lv_download = null;
		img_download = null;
		img_quanxuan = null;
		tv_quxiao = null;
		tv_download = null;
		tv_sum = null;
		textTotal = null;
		lin_quanxuan = null;
		lin_status2 = null;
		imageSort = null;
		imageSortDown = null;
		mainAdapter = null;
		adapter = null;
		SubListAll = null;
		urlList = null;
		SubList = null;
		fList = null;
		userId = null;
	}
}
