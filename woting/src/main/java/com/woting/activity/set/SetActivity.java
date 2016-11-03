package com.woting.activity.set;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.woting.R;
import com.woting.activity.baseactivity.BaseActivity;
import com.woting.activity.person.feedback.activity.FeedbackActivity;
import com.woting.activity.set.about.AboutActivity;
import com.woting.activity.set.contactus.activity.ContactUsActivity;
import com.woting.activity.set.downloadposition.activity.DownloadPositionActivity;
import com.woting.activity.set.help.HelpActivity;
import com.woting.activity.set.update.UpdateManager;
import com.woting.common.application.BSApplication;
import com.woting.common.config.GlobalConfig;
import com.woting.common.constant.StringConstant;
import com.woting.common.volley.VolleyCallback;
import com.woting.common.volley.VolleyRequest;
import com.woting.manager.CacheManager;
import com.woting.util.DialogUtils;
import com.woting.util.PhoneMessage;
import com.woting.util.ToastUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;

/**
 * 设置
 * @author 辛龙
 *         2016年2月26日
 */
public class SetActivity extends BaseActivity implements OnClickListener {
    private Dialog updateDialog;        // 版本更新对话框
    private Dialog dialog;              // 加载数据对话框
    private Dialog clearCacheDialog;    // 清除缓存对话框
    private Button logOut;              // 注销
    private TextView textCache;         // 缓存

    private int updateType = 1;         // 版本更新类型
    private String updateNews;          // 版本更新内容
    private String cache;               // 缓存
    private String cachePath;           // 缓存路径
    private String tag = "SET_REQUEST_CANCEL_TAG";
    private boolean isCancelRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        cachePath = Environment.getExternalStorageDirectory() + "/woting/image";// 缓存路径
        initDialog();
        initViews();
    }

    // 初始化控件
    private void initViews() {
        findViewById(R.id.head_left_btn).setOnClickListener(this);          // 返回
        findViewById(R.id.lin_clear).setOnClickListener(this);              // 清除缓存
        findViewById(R.id.lin_update).setOnClickListener(this);             // 检查更新
        findViewById(R.id.lin_help).setOnClickListener(this);               // 我听帮助
        findViewById(R.id.lin_about).setOnClickListener(this);              // 关于
        findViewById(R.id.lin_feedback).setOnClickListener(this);           // 意见反馈
        findViewById(R.id.lin_downloadposition).setOnClickListener(this);   // 下载位置
        findViewById(R.id.lin_contactus).setOnClickListener(this);          // 联系我们

        logOut = (Button) findViewById(R.id.lin_zhuxiao);                   // 注销
        logOut.setOnClickListener(this);
        if(getIntent() != null) {
            if (!getIntent().getStringExtra("LOGIN_STATE").equals("true")) {
                logOut.setVisibility(View.GONE);
            }
        }

        textCache = (TextView) findViewById(R.id.text_cache);               // 缓存
        initCache();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.head_left_btn:        // 返回
                finish();
                break;
            case R.id.lin_zhuxiao:          // 注销登录
                if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
                    dialog = DialogUtils.Dialogph(context, "正在获取数据", dialog);
                    sendRequestLogout();    // 清空数据
                } else {
                    ToastUtils.show_short(context, "网络失败，请检查网络");
                }
                break;
            case R.id.lin_clear:            // 清空缓存
                clearCacheDialog.show();
                break;
            case R.id.lin_about:            // 关于
                startActivity(new Intent(context, AboutActivity.class));
                break;
            case R.id.lin_feedback:         // 意见反馈
                startActivity(new Intent(context, FeedbackActivity.class));
                break;
            case R.id.lin_update:           // 检查更新
                if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE != -1) {
                    dialog = DialogUtils.Dialogph(context, "通讯中", dialog);
                    sendRequestUpdate();
                } else {
                    ToastUtils.show_short(context, "网络失败，请检查网络");
                }
                break;
            case R.id.lin_help:             // 使用帮助
                startActivity(new Intent(context, HelpActivity.class));
                break;
            case R.id.lin_downloadposition: // 下载位置
                startActivity(new Intent(context, DownloadPositionActivity.class));
                break;
            case R.id.lin_contactus:        // 联系我们
                startActivity(new Intent(context, ContactUsActivity.class));
                break;
            case R.id.tv_update:            // 更新
                okUpdate();
                updateDialog.dismiss();
                break;
            case R.id.tv_qx:                // 取消更新
                if (updateType == 1) {
                    updateDialog.dismiss();
                } else {
                    ToastUtils.show_short(context, "本次需要更新");
                }
                break;
            case R.id.tv_confirm:           // 确定清除
                clearCacheDialog.dismiss();
                new ClearCacheTask().execute();
                break;
            case R.id.tv_cancle:            // 取消清除
                clearCacheDialog.dismiss();
                break;
        }
    }

    // 初始化对话框
    private void initDialog() {
        // 清除缓存对话框
        View dialog1 = LayoutInflater.from(context).inflate(R.layout.dialog_exit_confirm, null);
        dialog1.findViewById(R.id.tv_confirm).setOnClickListener(this); // 清空
        dialog1.findViewById(R.id.tv_cancle).setOnClickListener(this);  // 取消
        TextView textTitle = (TextView) dialog1.findViewById(R.id.tv_title);
        textTitle.setText("是否删除本地存储缓存");

        clearCacheDialog = new Dialog(context, R.style.MyDialog);
        clearCacheDialog.setContentView(dialog1);
        clearCacheDialog.setCanceledOnTouchOutside(false);
        clearCacheDialog.getWindow().setBackgroundDrawableResource(R.color.dialog);

        // 更新弹出框
        View dialog2 = LayoutInflater.from(this).inflate(R.layout.dialog_update, null);
        dialog2.findViewById(R.id.tv_update).setOnClickListener(this);  // 开始更新
        dialog2.findViewById(R.id.tv_qx).setOnClickListener(this);      // 取消
        TextView textContent = (TextView) dialog2.findViewById(R.id.text_contnt);
        textContent.setText(Html.fromHtml("<font size='26'>" + updateNews + "</font>"));

        updateDialog = new Dialog(context, R.style.MyDialog);
        updateDialog.setContentView(dialog2);
        updateDialog.setCanceledOnTouchOutside(false);
        updateDialog.getWindow().setBackgroundDrawableResource(R.color.dialog);
    }

    // 注销数据交互
    private void sendRequestLogout() {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        VolleyRequest.RequestPost(GlobalConfig.logoutUrl, tag, jsonObject, new VolleyCallback() {

            @Override
            protected void requestSuccess(JSONObject result) {
                if (dialog != null) dialog.dismiss();
                // 如果网络请求已经执行取消操作 就表示就算请求成功也不需要数据返回了 所以方法就此结束
                if (isCancelRequest) return;
                try {
                    String returnType = result.getString("ReturnType");
                    String message = result.getString("Message");

                    Log.v("returnType", "returnType -- > > " + returnType + " -- message -- > > " + message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Editor et = BSApplication.SharedPreferences.edit();
                et.putString(StringConstant.ISLOGIN, "false");
                et.putString(StringConstant.USERID, "");
                et.putString(StringConstant.IMAGEURL, "");
                if (!et.commit()) {
                    Log.v("commit", "数据 commit 失败!");
                }
                logOut.setVisibility(View.INVISIBLE);
                sendBroadcast(new Intent("push_down_completed"));// 发送广播 更新已下载和未下载界面
                Toast.makeText(context, "注销成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected void requestError(VolleyError error) {
                if (dialog != null) dialog.dismiss();
                ToastUtils.showVolleyError(context);
            }
        });
    }

    // 调用更新功能
    protected void okUpdate() {
        UpdateManager updateManager = new UpdateManager(this);
        updateManager.checkUpdateInfo1();
    }

    // 更新数据交互
    private void sendRequestUpdate() {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            jsonObject.put("Version", PhoneMessage.appVersonName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        VolleyRequest.RequestPost(GlobalConfig.VersionUrl, tag, jsonObject, new VolleyCallback() {

            @Override
            protected void requestSuccess(JSONObject result) {
                if (dialog != null) dialog.dismiss();
                if (isCancelRequest) return;
                try {
                    String returnType = result.getString("ReturnType");
                    if (returnType != null && returnType.equals("1001")) {
                        GlobalConfig.apkUrl = result.getString("DownLoadUrl");
                        String MastUpdate = result.getString("MastUpdate");
                        String ResultList = result.getString("CurVersion");
                        if (ResultList != null && MastUpdate != null) {
                            dealVersion(ResultList, MastUpdate);
                        } else {
                            Log.e("检查更新返回值", "返回值为1001，但是返回的数值有误");
                        }
                    } else {
                        ToastUtils.show_allways(context, "当前已是最新版本");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void requestError(VolleyError error) {
                if (dialog != null) dialog.dismiss();
                ToastUtils.showVolleyError(context);
            }
        });
    }

    // 检查版本更新
    protected void dealVersion(String ResultList, String mastUpdate) {
        String Version = "0.1.0.X.0";
        String Descn = null;
        try {
            JSONTokener jsonParser = new JSONTokener(ResultList);
            JSONObject arg1 = (JSONObject) jsonParser.nextValue();
            Version = arg1.getString("Version");
            Descn = arg1.getString("Descn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 版本更新比较
        String verson = Version;
        String[] strArray = verson.split("\\.");
        String verson_build;
        try {
            verson_build = strArray[4];
            int verson_old = PhoneMessage.versionCode;
            int verson_new = Integer.parseInt(verson_build);
            if (verson_new > verson_old) {
                if (mastUpdate != null && mastUpdate.equals("1")) {        // 强制升级
                    if (Descn != null && !Descn.trim().equals("")) {
                        updateNews = Descn;
                    } else {
                        updateNews = "本次版本升级较大，需要更新";
                    }
                    updateType = 2;
                    updateDialog.show();
                } else {            // 普通升级
                    if (Descn != null && !Descn.trim().equals("")) {
                        updateNews = Descn;
                    } else {
                        updateNews = "有新的版本需要升级喽";
                    }
                    updateType = 1;// 不需要强制升级
                    updateDialog.show();
                }
            } else if (verson_new == verson_old) {
                ToastUtils.show_allways(context, "已经是最新版本");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("版本处理异常", e.toString() + "");
        }
    }

    // 启动统计缓存的线程
    private void initCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(cachePath);
                try {
                    cache = CacheManager.getCacheSize(file);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textCache.setText(cache);
                        }
                    });
                } catch (Exception e) {
                    Log.e("获取本地缓存文件大小", cache);
                }
            }
        }).start();
    }

    // 清除缓存异步任务
    private class ClearCacheTask extends AsyncTask<Void, Void, Void> {
        private boolean clearResult;

        @Override
        protected void onPreExecute() {
            dialog = DialogUtils.Dialogph(context, "正在清除缓存", dialog);
        }

        @Override
        protected Void doInBackground(Void... params) {
            clearResult = CacheManager.delAllFile(cachePath);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            clearCacheDialog.dismiss();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            if (clearResult) {
                ToastUtils.show_allways(context, "缓存已清除");
                textCache.setText("0MB");
            } else {
                Log.e("缓存异常", "缓存清理异常");
                initCache();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isCancelRequest = VolleyRequest.cancelRequest(tag);
        updateDialog = null;
        dialog = null;
        clearCacheDialog = null;
        updateNews = null;
        cache = null;
        cachePath = null;
        textCache = null;
        setContentView(R.layout.activity_null);
    }
}
