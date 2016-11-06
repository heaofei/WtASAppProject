package com.woting.common.application;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.umeng.socialize.PlatformConfig;
import com.woting.common.config.SocketClientConfig;
import com.woting.common.constant.KeyConstant;
import com.woting.common.helper.CommonHelper;
import com.woting.common.receiver.NetWorkChangeReceiver;
import com.woting.common.service.LocationService;
import com.woting.common.service.SocketService;
import com.woting.common.service.SubclassService;
import com.woting.common.util.PhoneMessage;
import com.woting.ui.download.service.DownloadService;
import com.woting.ui.interphone.commom.service.NotificationService;
import com.woting.ui.interphone.commom.service.VoiceStreamPlayerService;
import com.woting.ui.interphone.commom.service.VoiceStreamRecordService;

import java.util.ArrayList;
import java.util.List;

/**
 * BSApplication
 * 作者：xinlong on 2016/11/6 21:18
 * 邮箱：645700751@qq.com
 */
public class BSApplication extends Application {

    private NetWorkChangeReceiver netWorkChangeReceiver = null;
    private static RequestQueue queues;
    private static Context instance;
    private static Intent Socket, record, voicePlayer, Subclass, download, Location, Notification;
    public static SocketClientConfig scc;
    public static SharedPreferences SharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SharedPreferences = this.getSharedPreferences("wotingfm", Context.MODE_PRIVATE);
        queues = Volley.newRequestQueue(this);
        InitThird();                        //第三方使用的相关方法
        PhoneMessage.getPhoneInfo(instance);//获取手机信息

        List<String> _l = new ArrayList<String>();//其中每个间隔要是0.5秒的倍数
        _l.add("INTE::500");                 //第1次检测到未连接成功，隔0.5秒重连
        _l.add("INTE::500");                 //第2次检测到未连接成功，隔0.5秒重连
        _l.add("INTE::1000");                //第3次检测到未连接成功，隔1秒重连
        _l.add("INTE::1000");                //第4次检测到未连接成功，隔1秒重连
        _l.add("INTE::2000");                //第5次检测到未连接成功，隔2秒重连
        _l.add("INTE::2000");                //第6次检测到未连接成功，隔2秒重连
        _l.add("INTE::5000");                //第7次检测到未连接成功，隔5秒重连
        _l.add("INTE::10000");               //第8次检测到未连接成功，隔10秒重连
        _l.add("INTE::60000");               //第9次检测到未连接成功，隔1分钟重连
        _l.add("GOTO::8");                   //之后，调到第9步处理
        scc = new SocketClientConfig();
        scc.setReConnectWays(_l);
        Socket = new Intent(this, SocketService.class);                //socket服务
        startService(Socket);
        record = new Intent(this, VoiceStreamRecordService.class);     //录音服务
        startService(record);
        voicePlayer = new Intent(this, VoiceStreamPlayerService.class);//播放服务
        startService(voicePlayer);
        Location = new Intent(this, LocationService.class);            //定位服务
        startService(Location);
        Subclass = new Intent(this, SubclassService.class);            //单对单接听控制服务
        startService(Subclass);
        download = new Intent(this, DownloadService.class);
        startService(download);
        Notification = new Intent(this, NotificationService.class);
        startService(Notification);
        CommonHelper.checkNetworkStatus(instance);                     //网络设置获取
        this.registerNetWorkChangeReceiver(new NetWorkChangeReceiver(this));// 注册网络状态及返回键监听
    }

    public static Context getAppContext() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unRegisterNetWorkChangeReceiver(this.netWorkChangeReceiver);
        onStop();
    }

    /***
     * 注册网络监听者
     */
    private void registerNetWorkChangeReceiver(NetWorkChangeReceiver netWorkChangeReceiver) {
        this.netWorkChangeReceiver = netWorkChangeReceiver;
        IntentFilter filter = new IntentFilter();
        filter.addAction(NetWorkChangeReceiver.intentFilter);
        this.registerReceiver(netWorkChangeReceiver, filter);
    }

    /**
     * 取消网络变化监听者
     */
    private void unRegisterNetWorkChangeReceiver(NetWorkChangeReceiver netWorkChangeReceiver) {
        this.unregisterReceiver(netWorkChangeReceiver);
    }

    //第三方使用的相关方法
    private void InitThird() {
        PlatformConfig.setWeixin(KeyConstant.WEIXIN_KEY, KeyConstant.WEIXIN_SECRET);
        PlatformConfig.setQQZone(KeyConstant.QQ_KEY, KeyConstant.QQ_SECRET);
        PlatformConfig.setSinaWeibo(KeyConstant.WEIBO_KEY, KeyConstant.WEIBO_SECRET);
    }

    //volley
    public static RequestQueue getHttpQueues() {
        return queues;
    }

    //app退出时执行该操作
    private void onStop() {
        instance.stopService(Socket);
        instance.stopService(record);
        instance.stopService(voicePlayer);
        instance.stopService(Subclass);
        instance.stopService(download);
        instance.stopService(Location);
        instance.stopService(Notification);
    }
}
