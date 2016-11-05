package com.woting.activity.person.favorite.fragment;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.woting.R;
import com.woting.activity.home.main.HomeActivity;
import com.woting.activity.home.player.main.dao.SearchPlayerHistoryDao;
import com.woting.activity.home.player.main.fragment.PlayerFragment;
import com.woting.activity.home.player.main.model.PlayerHistory;
import com.woting.activity.home.program.fmlist.model.RankInfo;
import com.woting.activity.main.MainActivity;
import com.woting.activity.person.favorite.activity.FavoriteActivity;
import com.woting.activity.person.favorite.adapter.FavorListAdapter;
import com.woting.activity.person.favorite.adapter.FavorListAdapter.favorCheck;
import com.woting.common.config.GlobalConfig;
import com.woting.common.constant.StringConstant;
import com.woting.common.volley.VolleyCallback;
import com.woting.common.volley.VolleyRequest;
import com.woting.util.CommonUtils;
import com.woting.util.DialogUtils;
import com.woting.util.ToastUtils;
import com.woting.widgetui.xlistview.XListView;
import com.woting.widgetui.xlistview.XListView.IXListViewListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

/**
 * 我喜欢的 电台界面
 */
public class RadioFragment extends Fragment {
	private FragmentActivity context;
	private Dialog dialog;
	private List<RankInfo> SubList;
	private XListView mlistView;
	private int page = 1;
	private int RefreshType;		// refreshtype 1为下拉加载 2为上拉加载更多
	private ArrayList<RankInfo> newlist = new ArrayList<RankInfo>();
	private boolean flag;
	private int pagesizenum = -1;	// 前端自己算 //先求余 如果等于0 最后结果不加1 如果不等于0 结果加一
	private View rootView;
	private FavorListAdapter adapter;
	private List<String> dellist;
	private String ReturnType;
	private Intent mintent;
	private SearchPlayerHistoryDao dbdao;
//	protected Integer pagesize;
	private View linearNull;
	private String tag = "RADIO_VOLLEY_REQUEST_CANCEL_TAG";
	private boolean isCancelRequest;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this.getActivity();
		flag = true;// 设置等待提示是否展示
		// 数据变化后广播
		mintent = new Intent();
		mintent.setAction(FavoriteActivity.VIEW_UPDATE);
		initDao();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (rootView == null) {
			rootView = inflater.inflate(R.layout.fragment_favorite_sound, container, false);
			linearNull = rootView.findViewById(R.id.linear_null);
			mlistView = (XListView) rootView.findViewById(R.id.listView);
			mlistView.setSelector(new ColorDrawable(Color.TRANSPARENT));
			IntentFilter myfileter = new IntentFilter();
			myfileter.addAction(FavoriteActivity.VIEW_UPDATE);
			myfileter.addAction(FavoriteActivity.SET_NOT_LOAD_REFRESH);
			myfileter.addAction(FavoriteActivity.SET_LOAD_REFRESH);
			context.registerReceiver(mBroadcastReceiver, myfileter);
			if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
				if (flag) {
					flag = false;
				}
				RefreshType = 1;
				page = 1;
				send();			// 发送网络请求
			} else {
				ToastUtils.show_short(context, "网络失败，请检查网络");
			}
		}
		return rootView;
	}
	
	/**
	 * 设置 View 隐藏
	 */
	public void setViewHint(){
		linearNull.setVisibility(View.GONE);
	}
	
	/**
	 * 设置 View 可见  解决全选 Dialog 挡住 ListView 最底下一条 Item 问题
	 */
	public void setViewVisibility(){
		linearNull.setVisibility(View.VISIBLE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setView();
	}

	/**
	 * 初始化数据库
	 */
	private void initDao() {
		dbdao = new SearchPlayerHistoryDao(context);
	}

	private void setListener() {
		adapter.setOnListener(new favorCheck() {
			@Override
			public void checkPosition(int position) {
				if (newlist.get(position).getChecktype() == 0) {
					newlist.get(position).setChecktype(1);
				} else {
					newlist.get(position).setChecktype(0);
				}
				ifAll();
				adapter.notifyDataSetChanged();
			}
		});

		mlistView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(FavoriteActivity.isEdit){
					if (newlist.get(position - 1).getChecktype() == 0) {
						newlist.get(position - 1).setChecktype(1);
					} else {
						newlist.get(position - 1).setChecktype(0);
					}
					ifAll();
					adapter.notifyDataSetChanged();
				}else{
					if (newlist != null && newlist.get(position - 1) != null && newlist.get(position - 1).getMediaType() != null) {
						String MediaType = newlist.get(position - 1).getMediaType();
						if (MediaType.equals("RADIO") || MediaType.equals("AUDIO")) {
							String playername = newlist.get(position - 1).getContentName();
							String playerimage = newlist.get(position - 1).getContentImg();
							String playerurl = newlist.get(position - 1).getContentPlay();
							String playerurI = newlist.get(position - 1).getContentURI();
							String playermediatype = newlist.get(position - 1).getMediaType();
							String playcontentshareurl = newlist.get(position - 1).getContentShareURL();
							String plaplayeralltime = "0";
							String playerintime = "0";
							String playercontentdesc = newlist.get(position - 1).getCurrentContent();
							String playernum = newlist.get(position - 1).getWatchPlayerNum();
							String playerzantype = "0";
							String playerfrom = "";
							String playerfromid = "";
							String playerfromurl = "";
							String playeraddtime = Long.toString(System.currentTimeMillis());
							String bjuserid = CommonUtils.getUserId(context);
							String ContentFavorite = newlist.get(position - 1).getContentFavorite();
							String ContentId = newlist.get(position - 1).getContentId();
							String localurl = newlist.get(position - 1).getLocalurl();

							String sequName= newlist.get(position - 1).getSequName();
							String sequId= newlist.get(position - 1).getSequId();
							String sequDesc= newlist.get(position - 1).getSequDesc();
							String sequImg= newlist.get(position - 1).getSequImg();

							//如果该数据已经存在数据库则删除原有数据，然后添加最新数据
							PlayerHistory history = new PlayerHistory(
									playername,  playerimage, playerurl, playerurI,playermediatype,
									plaplayeralltime, playerintime, playercontentdesc, playernum,
									playerzantype,  playerfrom, playerfromid,playerfromurl, playeraddtime,bjuserid,playcontentshareurl,
									ContentFavorite,ContentId,localurl,sequName,sequId,sequDesc,sequImg);
							dbdao.deleteHistory(playerurl);
							dbdao.addHistory(history);

							if (PlayerFragment.context != null) {
								MainActivity.change();
								HomeActivity.UpdateViewPager();
								PlayerFragment.SendTextRequest(newlist.get(position - 1).getContentName(), context);
								getActivity().finish();
							} else {
								SharedPreferences sp = context.getSharedPreferences("wotingfm", Context.MODE_PRIVATE);
								Editor et = sp.edit();
								et.putString(StringConstant.PLAYHISTORYENTER, "true");
								et.putString(StringConstant.PLAYHISTORYENTERNEWS, newlist.get(position - 1).getContentName());
								et.commit();
								MainActivity.change();
								HomeActivity.UpdateViewPager();
								getActivity().finish();
							}
						} else {
							ToastUtils.show_short(context, "暂不支持的Type类型");
						}
					}
				}
			}
		});
	}

	private void setView() {
		mlistView.setPullRefreshEnable(true);
		mlistView.setPullLoadEnable(true);
		mlistView.setXListViewListener(new IXListViewListener() {
			@Override
			public void onRefresh() {
				if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
					RefreshType = 1;
					page = 1;
					send();
				} else {
					mlistView.stopRefresh();
					ToastUtils.show_short(context, "网络失败，请检查网络");
				}
			}
			
			@Override
			public void onLoadMore() {
				if (page <= pagesizenum) {
					if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
						RefreshType = 2;
						send();
						ToastUtils.show_short(context, "正在请求" + page + "页信息");
					} else {
						ToastUtils.show_short(context, "网络失败，请检查网络");
					}
				} else {
					mlistView.stopLoadMore();
					mlistView.setPullLoadEnable(false);
					ToastUtils.show_allways(context, "已经是最后一页了");
				}
			}
		});
	}

	/**
	 * 发送网络请求
	 */
	private void send() {
		JSONObject jsonObject = VolleyRequest.getJsonObject(context);
		try {
			jsonObject.put("MediaType", "RADIO");
			jsonObject.put("Page", String.valueOf(page));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		VolleyRequest.RequestPost(GlobalConfig.getFavoriteListUrl, tag, jsonObject, new VolleyCallback() {
			private String ReturnType;

			@Override
			protected void requestSuccess(JSONObject result) {
				if (dialog != null) {
					dialog.dismiss();
				}
				if(isCancelRequest){
					return ;
				}
				page++;
				try {
					ReturnType = result.getString("ReturnType");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (ReturnType != null && ReturnType.equals("1001")) {
                    if(isDel){
                        ToastUtils.show_allways(context, "已删除");
                        isDel = false;
                    }
                    try {
                        JSONObject arg1 = (JSONObject) new JSONTokener(result.getString("ResultList")).nextValue();
                        String allCountString = arg1.getString("AllCount");
                        String pageSizeString = arg1.getString("PageSize");
                        if (allCountString != null && !allCountString.equals("") && pageSizeString != null && !pageSizeString.equals("")) {
                            int allCountInt = Integer.valueOf(allCountString);
                            int pageSizeInt = Integer.valueOf(pageSizeString);
                            if(pageSizeInt < 10 || allCountInt < 10){
                                mlistView.stopLoadMore();
                                mlistView.setPullLoadEnable(false);
                            }else{
                                mlistView.setPullLoadEnable(true);
                                if (allCountInt % pageSizeInt == 0) {
                                    pagesizenum = allCountInt / pageSizeInt;
                                } else {
                                    pagesizenum = allCountInt / pageSizeInt + 1;
                                }
                            }
                        }
                        SubList = new Gson().fromJson(arg1.getString("FavoriteList"), new TypeToken<List<RankInfo>>() {}.getType());
                        if (RefreshType == 1) {
                            newlist.clear();
                        }
                        newlist.addAll(SubList);
                        if (adapter == null) {
                            mlistView.setAdapter(adapter = new FavorListAdapter(context, newlist));
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        setListener();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
				} else if (ReturnType != null && ReturnType.equals("0000")) {
                    ToastUtils.show_short(context, "无法获取相关的参数");
                } else if (ReturnType != null && ReturnType.equals("1002")) {
                    ToastUtils.show_short(context, "无此分类信息");
                } else if (ReturnType != null && ReturnType.equals("1003")) {
                    ToastUtils.show_short(context, "无法获得列表");
                } else if (ReturnType != null && ReturnType.equals("1011")) {
                    ToastUtils.show_short(context, "无数据");
                    mlistView.setVisibility(View.GONE);
                }
                // 无论何种返回值，都需要终止掉上拉刷新及下拉加载的滚动状态
                if (RefreshType == 1) {
                    mlistView.stopRefresh();
                } else {
                    mlistView.stopLoadMore();
                }
			}
			
			@Override
			protected void requestError(VolleyError error) {
				if (dialog != null) {
					dialog.dismiss();
				}
			}
		});
	}

	/**
	 * 广播接收器
	 */
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(FavoriteActivity.VIEW_UPDATE)) {
				if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
					page = 1;
					send();
				} else {
					ToastUtils.show_allways(context, "网络失败，请检查网络");
				}
			}else if(action.equals(FavoriteActivity.SET_NOT_LOAD_REFRESH)){
				if(isVisible()){
					mlistView.setPullRefreshEnable(false);
					mlistView.setPullLoadEnable(false);
				}
			}else if(action.equals(FavoriteActivity.SET_LOAD_REFRESH)){
				if(isVisible()){
					mlistView.setPullRefreshEnable(true);
					if(newlist.size() >= 10){
						mlistView.setPullLoadEnable(true);
					}
				}
			}
		}
	};

	/**
	 * 更改界面的view布局 让每个item都可以显示点选框
	 * 
	 * @param type
	 */
	public boolean changeviewtype(int type) {
		if (newlist != null & newlist.size() != 0) {
			for (int i = 0; i < newlist.size(); i++) {
				newlist.get(i).setViewtype(type);
			}
			if (type == 0) {
				for (int i = 0; i < newlist.size(); i++) {
					newlist.get(i).setChecktype(0);
				}
			}
			adapter.notifyDataSetChanged();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 点击全选时的方法
	 * 
	 * @param type
	 */
	public void changechecktype(int type) {
		if (adapter != null) {
			for (int i = 0; i < newlist.size(); i++) {
				newlist.get(i).setChecktype(type);
			}
			adapter.notifyDataSetChanged();
		}
	}

	/**
	 * 获取当前页面选中的为选中的数目
	 * 
	 * @return
	 */
	public int getdelitemsum() {
		int sum = 0;
		for (int i = 0; i < newlist.size(); i++) {
			if (newlist.get(i).getChecktype() == 1) {
				sum++;
			}
		}
		return sum;
	}
	
	/**
	 * 判断是否全部选择
	 */
	public void ifAll(){
		if(getdelitemsum() == newlist.size()){
			Intent intentAll = new Intent();
			intentAll.setAction(FavoriteActivity.SET_ALL_IMAGE);
			context.sendBroadcast(intentAll);
		}else{
			Intent intentNotAll = new Intent();
			intentNotAll.setAction(FavoriteActivity.SET_NOT_ALL_IMAGE);
			context.sendBroadcast(intentNotAll);
		}
	}

	/**
	 * 删除
	 */
	public void delitem() {
		if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
			dialog = DialogUtils.Dialogph(context, "正在删除", dialog);
			for (int i = 0; i < newlist.size(); i++) {
				if (newlist.get(i).getChecktype() == 1) {
					if (dellist == null) {
						dellist = new ArrayList<String>();
						String type = newlist.get(i).getMediaType();
						String contentid = newlist.get(i).getContentId();
						dellist.add(type + "::" + contentid);
					} else {
						String type = newlist.get(i).getMediaType();
						String contentid = newlist.get(i).getContentId();
						dellist.add(type + "::" + contentid);
					}
				}
			}
			RefreshType = 1;
			sendrequest();
		} else {
			ToastUtils.show_allways(context, "网络失败，请检查网络");
		}
	}
	
	private boolean isDel;

	/**
	 * 执行删除单条喜欢的方法
	 */
	protected void sendrequest() {
		JSONObject jsonObject = VolleyRequest.getJsonObject(context);
		try {
			// 对s进行处理 去掉"[]"符号
			String s = dellist.toString();
			jsonObject.put("DelInfos", s.substring(1, s.length() - 1).replaceAll(" ", ""));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		VolleyRequest.RequestPost(GlobalConfig.delFavoriteListUrl, tag, jsonObject, new VolleyCallback() {
			private String Message;
//			private String SessionId;

			@Override
			protected void requestSuccess(JSONObject result) {
				isDel = true;
				dellist.clear();
				if(isCancelRequest){
					return ;
				}
				try {
					ReturnType = result.getString("ReturnType");
//					SessionId = result.getString("SessionId");
					Message = result.getString("Message");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (ReturnType != null && ReturnType.equals("1001")) {
					context.sendBroadcast(mintent);
//					if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
//						page = 1;
//						send();// 将页面改动的消息发送出去	如果删除成功默认去获取第一页数据
//						context.sendBroadcast(mintent);
//					} else {
//						ToastUtils.show_allways(context, "网络失败，请检查网络");
//					}
				}
				if (ReturnType != null && ReturnType.equals("1002")) {
					ToastUtils.show_allways(context, "无法获取用户Id");
				} else if (ReturnType != null && ReturnType.equals("T")) {
					ToastUtils.show_allways(context, "异常返回值");
				} else if (ReturnType != null && ReturnType.equals("200")) {
					ToastUtils.show_allways(context, "尚未登录");
				} else if (ReturnType != null && ReturnType.equals("1003")) {
					ToastUtils.show_allways(context, "异常返回值");
				} else {
					if (Message != null && !Message.trim().equals("")) {
						ToastUtils.show_allways(context, Message + "");
					}
				}
			}
			
			@Override
			protected void requestError(VolleyError error) {
				if (dialog != null) {
					dialog.dismiss();
				}
				dellist.clear();
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
		context.unregisterReceiver(mBroadcastReceiver);
		mlistView = null;
		context = null;
		dialog = null;
		SubList = null;
		newlist = null;
		rootView = null;
		adapter = null;
		dellist = null;
		ReturnType = null;
		mintent = null;
		linearNull = null;
		tag = null;
		if(dbdao != null){
			dbdao.closedb();
			dbdao = null;
		}
	}
}
