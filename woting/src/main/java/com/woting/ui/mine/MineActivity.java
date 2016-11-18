package com.woting.ui.mine;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;
import com.umeng.analytics.MobclickAgent;
import com.woting.R;
import com.woting.common.application.BSApplication;
import com.woting.common.config.GlobalConfig;
import com.woting.common.constant.StringConstant;
import com.woting.common.http.MyHttp;
import com.woting.common.manager.FileManager;
import com.woting.common.util.AssembleImageUrlUtils;
import com.woting.common.util.BitmapUtils;
import com.woting.common.util.CommonUtils;
import com.woting.common.util.DialogUtils;
import com.woting.common.util.ImageUploadReturnUtil;
import com.woting.common.util.PhoneMessage;
import com.woting.common.util.ToastUtils;
import com.woting.common.volley.VolleyCallback;
import com.woting.common.volley.VolleyRequest;
import com.woting.ui.baseactivity.BaseActivity;
import com.woting.ui.common.login.LoginActivity;
import com.woting.ui.common.photocut.PhotoCutActivity;
import com.woting.ui.common.qrcodes.EWMShowActivity;
import com.woting.ui.interphone.find.findresult.model.UserInviteMeInside;
import com.woting.ui.mine.favorite.activity.FavoriteActivity;
import com.woting.ui.mine.hardware.HardwareIntroduceActivity;
import com.woting.ui.mine.model.UserPortaitInside;
import com.woting.ui.mine.person.updatepersonnews.UpdatePersonActivity;
import com.woting.ui.mine.person.updatepersonnews.model.UpdatePerson;
import com.woting.ui.mine.playhistory.activity.PlayHistoryActivity;
import com.woting.ui.mine.set.SetActivity;
import com.woting.ui.mine.shapeapp.ShapeAppActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 个人信息主页
 * 作者：xinlong on 2016/11/6 21:18
 * 邮箱：645700751@qq.com
 */
public class MineActivity extends BaseActivity implements OnClickListener {
    private SharedPreferences sharedPreferences = BSApplication.SharedPreferences;
    private UpdatePerson pModel;

    private final int TO_GALLERY = 1;           // 标识 打开系统图库
    private final int TO_CAMERA = 2;            // 标识 打开系统照相机
    private final int PHOTO_REQUEST_CUT = 7;    // 标识 跳转到图片裁剪界面
    private final int UPDATE_USER = 3;            // 标识 跳转到修改个人信息界面
    private int imageNum;

    private String returnType;
    private String miniUri;
    private String isLogin;                     // 是否登录
    private String userName;                    // 用户名
    private String userId;                      // 用户Id
    private String outputFilePath;
    private String filePath;
    private String url;                         // 完整用户头像地址
    private String photoCutAfterImagePath;
    private String userNum;
    private String userSign;
    private String region;
    private String regionId;

    private Dialog dialog;
    private Dialog imageDialog;
    private View linStatusNoLogin;              // 没有登录时的状态
    private View linStatusLogin;                // 登录时的状态
    private View linLike;                       // 我喜欢的
    private View linAnchor;                     // 我的主播  我关注的主播
    private View linSubscribe;                  // 我的订阅
    private View linAlbum;                      // 我的专辑  我上传的专辑
    private View circleView;
    private View viewLine;

    private TextView textUserAutograph;
    private TextView textUserArea;
    private TextView textUserId;                // 显示用户 ID
    //    private TextView textTime;                  // 定时关闭的时间
    private TextView textUserName;              // 用户名
    private ImageView imageToggle;              // 流量提醒
    private ImageView imageHead;                // 用户头像

    private String tag = "PERSON_VOLLEY_REQUEST_CANCEL_TAG";
    private boolean isCancelRequest;
    private boolean isFirst = true;             // 第一次加载界面
    private boolean isUpdate;                   // 个人资料有改动

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);
        // 获取数据存储对象
        imageDialog();
        setView();
        setType();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void setType() {
        String a = android.os.Build.VERSION.RELEASE;
        Log.e("系统版本号", a + "");
        Log.e("系统版本号截取", a.substring(0, a.indexOf(".")) + "");
        boolean v = false;
        if (Integer.parseInt(a.substring(0, a.indexOf("."))) >= 5) {
            v = true;
        }
        if (v) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);        //透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);    //透明导航栏
        }
    }

    // 登陆状态下 用户设置头像对话框
    private void imageDialog() {
        final View dialog = LayoutInflater.from(context).inflate(R.layout.dialog_imageupload, null);
        dialog.findViewById(R.id.tv_gallery).setOnClickListener(this);      // 从手机相册选择
        dialog.findViewById(R.id.tv_camera).setOnClickListener(this);       // 拍照

        imageDialog = new Dialog(context, R.style.MyDialog);
        imageDialog.setContentView(dialog);
        imageDialog.setCanceledOnTouchOutside(true);
        imageDialog.getWindow().setBackgroundDrawableResource(R.color.dialog);
    }

    // 设置 view
    private void setView() {
        Bitmap bmp = BitmapUtils.readBitMap(context, R.mipmap.img_person_background);
//        textTime = (TextView) findViewById(R.id.text_time);                 // 定时关闭的时间显示

        findViewById(R.id.imageView_ewm).setOnClickListener(this);          // 二维码
        findViewById(R.id.lin_xiugai).setOnClickListener(this);             // 修改个人资料
        findViewById(R.id.text_denglu).setOnClickListener(this);            // 点击登录
        findViewById(R.id.image_nodenglu).setOnClickListener(this);         // 没有登录时的头像
        findViewById(R.id.lin_playhistory).setOnClickListener(this);        // 播放历史
//        findViewById(R.id.lin_timer).setOnClickListener(this);              // 定时
        findViewById(R.id.lin_liuliang).setOnClickListener(this);           // 流量提醒
        findViewById(R.id.lin_hardware).setOnClickListener(this);           // 智能硬件
        findViewById(R.id.lin_app).setOnClickListener(this);                // 应用分享
        findViewById(R.id.lin_set).setOnClickListener(this);                // 设置

        linStatusNoLogin = findViewById(R.id.lin_status_nodenglu);          // 未登录时的状态
        ImageView lin_image_0 = (ImageView) findViewById(R.id.lin_image_0); // 未登录时的背景图片
        lin_image_0.setImageBitmap(bmp);

        linStatusLogin = findViewById(R.id.lin_status_denglu);              // 登录时的状态
        imageToggle = (ImageView) findViewById(R.id.wt_img_toggle);         // 流量提醒开关
        textUserName = (TextView) findViewById(R.id.tv_username);           // 用户名
        ImageView lin_image = (ImageView) findViewById(R.id.lin_image);     // 登录时的背景图片
        lin_image.setImageBitmap(bmp);

        imageHead = (ImageView) findViewById(R.id.image_touxiang);          // 登录后的头像
        imageHead.setOnClickListener(this);

        linLike = findViewById(R.id.lin_like);                              // like
        linLike.setOnClickListener(this);

        linAnchor = findViewById(R.id.lin_anchor);                          // 我的主播
        linAnchor.setOnClickListener(this);

        linSubscribe = findViewById(R.id.lin_subscribe);                    // 我的订阅
        linSubscribe.setOnClickListener(this);

        linAlbum = findViewById(R.id.lin_album);                            // 我的专辑
        linAlbum.setOnClickListener(this);

        textUserArea = (TextView) findViewById(R.id.text_user_area);        // 用户信息
        textUserId = (TextView) findViewById(R.id.text_user_id);            // 显示用户 ID
        textUserAutograph = (TextView) findViewById(R.id.text_user_autograph);// 用户签名
        circleView = findViewById(R.id.circle_view);
        viewLine = findViewById(R.id.view_line);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lin_set:              // 设置
                Intent intentSet = new Intent(context, SetActivity.class);
                intentSet.putExtra("LOGIN_STATE", isLogin);
                startActivityForResult(intentSet, 0x222);
                break;
            case R.id.lin_playhistory:      // 播放历史
                startActivity(new Intent(context, PlayHistoryActivity.class));
                break;
            case R.id.text_denglu:          // 登陆
                startActivity(new Intent(context, LoginActivity.class));
                break;
            case R.id.lin_liuliang:         // 流量提示
                String wifiSet = sharedPreferences.getString(StringConstant.WIFISET, "true");
                Editor et = sharedPreferences.edit();
                if (wifiSet.equals("true")) {
                    Bitmap bitmap = BitmapUtils.readBitMap(context, R.mipmap.wt_person_close);
                    imageToggle.setImageBitmap(bitmap);
                    et.putString(StringConstant.WIFISET, "false");
                } else {
                    Bitmap bitmap = BitmapUtils.readBitMap(context, R.mipmap.wt_person_on);
                    imageToggle.setImageBitmap(bitmap);
                    et.putString(StringConstant.WIFISET, "true");
                }
                if (et.commit()) {
                    Log.v("commit", "数据 commit 失败!");
                }
                break;
            case R.id.lin_xiugai:           // 修改个人资料
                startActivityForResult(new Intent(context, UpdatePersonActivity.class), UPDATE_USER);
                break;
            case R.id.imageView_ewm:        // 展示二维码
                UserInviteMeInside news = new UserInviteMeInside();
                news.setPortraitMini(url);
                news.setUserId(userId);
                news.setUserName(userName);
                Intent intent = new Intent(context, EWMShowActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("type", 1);
                bundle.putString("id", userId);
                bundle.putString("image", url);
                bundle.putString("news", userSign);
                bundle.putString("name", userName);
                bundle.putSerializable("person", news);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.lin_like:             // 我喜欢的
                startActivity(new Intent(context, FavoriteActivity.class));
                break;
            case R.id.lin_anchor:           // 我的主播
                ToastUtils.show_allways(context, "我的主播!");
                break;
            case R.id.lin_subscribe:        // 我的订阅
                ToastUtils.show_allways(context, "我的订阅!");
                break;
            case R.id.lin_album:            // 我的专辑
                ToastUtils.show_allways(context, "我的专辑!");
                break;
            case R.id.lin_hardware:         // 智能硬件
                startActivity(new Intent(context, HardwareIntroduceActivity.class));
                break;
            case R.id.lin_app:              // 应用分享
                startActivity(new Intent(context, ShapeAppActivity.class));
                break;
            case R.id.image_nodenglu:       // 没有登录的默认头像
                startActivity(new Intent(context, LoginActivity.class));
                break;
            case R.id.tv_gallery:           // 从图库选择
                doDialogClick(0);
                imageDialog.dismiss();
                break;
            case R.id.tv_camera:            // 拍照
                doDialogClick(1);
                imageDialog.dismiss();
                break;
            case R.id.image_touxiang:       // 修改头像
                imageDialog.show();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initLoginStates();
    }

    // 初始化状态  登陆 OR 未登录
    private void initLoginStates() {
        if (isFirst) {                   // 避免重复加载
            isFirst = false;
        } else if (isLogin.equals(sharedPreferences.getString(StringConstant.ISLOGIN, "false"))) {
            Log.v("Person", "登录状态没有发生变化 -- > > " + isLogin);
            return;
        }
        isLogin = sharedPreferences.getString(StringConstant.ISLOGIN, "false"); // 获取用户的登陆状态

        if (isLogin.equals("true")) {   // 登录状态
            linStatusNoLogin.setVisibility(View.GONE);      // 未登录状态
            linStatusLogin.setVisibility(View.VISIBLE);     // 登录状态
            linLike.setVisibility(View.VISIBLE);            // 我喜欢的
            linAnchor.setVisibility(View.VISIBLE);
            linSubscribe.setVisibility(View.VISIBLE);
            linAlbum.setVisibility(View.VISIBLE);
            viewLine.setVisibility(View.GONE);

            userName = sharedPreferences.getString(StringConstant.USERNAME, "");// 用户名
            userId = sharedPreferences.getString(StringConstant.USERID, "");    // 用户 ID
            url = sharedPreferences.getString(StringConstant.IMAGEURL, "");     // 用户头像
            userNum = sharedPreferences.getString(StringConstant.USER_NUM, "");// 用户号
            userSign = sharedPreferences.getString(StringConstant.USER_SIGN, "");// 签名
            region = sharedPreferences.getString(StringConstant.REGION, "");// 区域
            textUserName.setText(userName);
            textUserAutograph.setText(userSign);

            if (region.equals("")) {
                if (GlobalConfig.CityName != null && !GlobalConfig.CityName.equals("null")
                        && GlobalConfig.District != null && !GlobalConfig.District.equals("null")) {
                    region = GlobalConfig.CityName + GlobalConfig.District;
                } else {
                    region = "您还没有填写地址";
                }
                textUserArea.setText(region);
            } else {
                textUserArea.setText(region);
            }

            if (userNum.equals("")) {
                circleView.setVisibility(View.GONE);
                textUserId.setVisibility(View.GONE);
            } else {
                circleView.setVisibility(View.VISIBLE);
                textUserId.setVisibility(View.VISIBLE);
                textUserId.setText("ID：(" + userNum + ")");
            }
            if (!url.equals("")) {
                if (!url.startsWith("http:")) {
                    url = AssembleImageUrlUtils.assembleImageUrl150(GlobalConfig.imageurl + url);
                } else {
                    url = AssembleImageUrlUtils.assembleImageUrl150(url);
                }
                Picasso.with(context).load(url.replace("\\/", "/")).into(imageHead);
            }
        } else {                        // 未登录
            linStatusNoLogin.setVisibility(View.VISIBLE);
            linStatusLogin.setVisibility(View.GONE);
            linLike.setVisibility(View.GONE);
            linAnchor.setVisibility(View.GONE);
            linSubscribe.setVisibility(View.GONE);
            linAlbum.setVisibility(View.GONE);
            viewLine.setVisibility(View.VISIBLE);

            Bitmap bitmap = BitmapUtils.readBitMap(context, R.mipmap.reg_default_portrait);
            imageHead.setImageBitmap(bitmap);
        }

        // 获取当前的流量提醒按钮状态
        String wifiSet = sharedPreferences.getString(StringConstant.WIFISET, "true");
        if (wifiSet.equals("true")) {
            Bitmap bitmap = BitmapUtils.readBitMap(context, R.mipmap.wt_person_on);
            imageToggle.setImageBitmap(bitmap);
        } else {
            Bitmap bitmap = BitmapUtils.readBitMap(context, R.mipmap.wt_person_close);
            imageToggle.setImageBitmap(bitmap);
        }
    }

    // 拍照调用逻辑  从相册选择 which == 0   拍照 which == 1
    private void doDialogClick(int which) {
        switch (which) {
            case 0:    // 调用图库
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, TO_GALLERY);
                break;
            case 1:    // 调用相机
                String savePath = FileManager.getImageSaveFilePath(context);
                FileManager.createDirectory(savePath);
                String fileName = System.currentTimeMillis() + ".jpg";
                File file = new File(savePath, fileName);
                Uri outputFileUri = Uri.fromFile(file);
                outputFilePath = file.getAbsolutePath();
                Intent intents = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intents.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                startActivityForResult(intents, TO_CAMERA);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TO_GALLERY:                // 照片的原始资源地址
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    Log.e("URI:", uri.toString());
                    int sdkVersion = Integer.valueOf(Build.VERSION.SDK);
                    Log.d("sdkVersion:", String.valueOf(sdkVersion));
                    String path;
                    if (sdkVersion >= 19) { // 或者 android.os.Build.VERSION_CODES.KITKAT这个常量的值是19
                        path = getPath_above19(context, uri);
                    } else {
                        path = getFilePath_below19(uri);
                    }
                    imageNum = 1;
                    startPhotoZoom(Uri.parse(path));
                }
                break;
            case TO_CAMERA:
                if (resultCode == Activity.RESULT_OK) {
                    imageNum = 1;
                    startPhotoZoom(Uri.parse(outputFilePath));
                }
                break;
            case PHOTO_REQUEST_CUT:
                if (resultCode == 1) {
                    imageNum = 1;
                    photoCutAfterImagePath = data.getStringExtra("return");
                    dialog = DialogUtils.Dialogph(context, "头像上传中");
                    dealt();
                }
                break;
            case UPDATE_USER:// 修改个人资料界面返回
                if (resultCode == 1) {
                    Bundle bundle = data.getExtras();
                    pModel = (UpdatePerson) bundle.getSerializable("data");
                    regionId = bundle.getString("regionId");
                    if (GlobalConfig.CURRENT_NETWORK_STATE_TYPE == -1) {
                        ToastUtils.show_allways(context, "网络失败，请检查网络");
                        return;
                    }
                    sendUpdate(pModel);
                }
            case 0x222:// 其它设置界面返回
                if (resultCode == RESULT_OK) {
                    userNum = sharedPreferences.getString(StringConstant.USER_NUM, "");// 用户号
                    if (!userNum.equals("")) {
                        circleView.setVisibility(View.VISIBLE);
                        textUserId.setVisibility(View.VISIBLE);
                        textUserId.setText("ID：(" + userNum + ")");
                    }
                }
                break;
        }
    }

    // 图片裁剪
    private void startPhotoZoom(Uri uri) {
        Intent intent = new Intent(context, PhotoCutActivity.class);
        intent.putExtra("URI", uri.toString());
        intent.putExtra("type", 1);
        startActivityForResult(intent, PHOTO_REQUEST_CUT);
    }

    // 图片处理
    private void dealt() {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    ToastUtils.show_allways(context, "保存成功");
                    Editor et = sharedPreferences.edit();
                    String imageUrl;
                    if (miniUri.startsWith("http:")) {
                        imageUrl = miniUri;
                    } else {
                        imageUrl = GlobalConfig.imageurl + miniUri;
                    }
                    et.putString(StringConstant.IMAGEURL, imageUrl);
                    if (!et.commit()) {
                        Log.v("commit", "数据 commit 失败!");
                    }
                    // 正常切可用代码 已从服务器获得返回值，但是无法正常显示
                    Picasso.with(context).load(imageUrl.replace("\\/", "/")).into(imageHead);
                } else if (msg.what == 0) {
                    ToastUtils.show_allways(context, "头像保存失败，请稍后再试");
                } else if (msg.what == -1) {
                    ToastUtils.show_allways(context, "头像保存异常，图片未上传成功，请重新发布");
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (imageDialog != null) {
                    imageDialog.dismiss();
                }
            }
        };

        new Thread() {
            @Override
            public void run() {
                super.run();
                int m = 0;
                Message msg = new Message();
                try {
                    filePath = photoCutAfterImagePath;
                    String ExtName = filePath.substring(filePath.lastIndexOf("."));
                    String TestURI = GlobalConfig.baseUrl + "wt/common/upload4App.do?FType=UserP&ExtName=";
                    String Response = MyHttp.postFile(new File(filePath), TestURI
                            + ExtName
                            + "&PCDType="
                            + GlobalConfig.PCDType
                            + "&UserId="
                            + CommonUtils.getUserId(getApplicationContext())
                            + "&IMEI=" + PhoneMessage.imei);
                    Log.e("图片上传数据", TestURI
                            + ExtName
                            + "&UserId="
                            + CommonUtils.getUserId(getApplicationContext())
                            + "&IMEI=" + PhoneMessage.imei);
                    Log.e("图片上传结果", Response);
                    Gson gson = new Gson();
                    Response = ImageUploadReturnUtil.getResPonse(Response);
                    UserPortaitInside u = gson.fromJson(Response, new TypeToken<UserPortaitInside>() {
                    }.getType());
                    if (u != null) {
                        try {
                            returnType = u.getReturnType();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        try {
                            miniUri = u.getPortraitMini();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (returnType == null || returnType.equals("")) {
                        msg.what = 0;
                    } else {
                        if (returnType.equals("1001")) {
                            msg.what = 1;
                        } else {
                            msg.what = 0;
                        }
                    }
                    if (m == imageNum) {
                        msg.what = 1;
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null) {
                        msg.obj = "异常" + e.getMessage();
                        Log.e("图片上传返回值异常", "" + e.getMessage());
                    } else {
                        Log.e("图片上传返回值异常", "" + e);
                        msg.obj = "异常";
                    }
                    msg.what = -1;
                }
                handler.sendMessage(msg);
            }
        }.start();
    }

    // API19以下获取图片路径的方法
    private String getFilePath_below19(Uri uri) {
        // 这里开始的第二部分，获取图片的路径：低版本的是没问题的，但是 sdk > 19 会获取不到
        String[] proj = {MediaStore.Images.Media.DATA};

        // 好像是 android 多媒体数据库的封装接口，具体的看 Android 文档
        Cursor cursor = getContentResolver().query(uri, proj, null, null, null);

        // 获得用户选择的图片的索引值
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        System.out.println("***************" + column_index);

        // 将光标移至开头 ，这个很重要，不小心很容易引起越界
        cursor.moveToFirst();

        // 最后根据索引值获取图片路径   结果类似：/mnt/sdcard/DCIM/Camera/IMG_20151124_013332.jpg
        String path = cursor.getString(column_index);
        System.out.println("path:" + path);
        return path;
    }

    /**
     * APIlevel 19以上才有
     * 创建项目时，我们设置了最低版本API Level，比如我的是10，
     * 因此，AS检查我调用的API后，发现版本号不能向低版本兼容，
     * 比如我用的“DocumentsContract.isDocumentUri(context, uri)”是Level 19 以上才有的，
     * 自然超过了10，所以提示错误。
     * 添加    @TargetApi(Build.VERSION_CODES.KITKAT)即可。
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath_above19(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * 与 onBackPress 同理 手机实体返回按键的处理
     */
    long waitTime = 2000;
    long touchTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && KeyEvent.KEYCODE_BACK == keyCode) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - touchTime) >= waitTime) {
                ToastUtils.show_allways(context, "再按一次退出");
                touchTime = currentTime;
            } else {
                MobclickAgent.onKillProcess(this);
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 广播接收  接收来自定时服务的时间更新广播
//    private BroadcastReceiver timerBroadcast = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (action.equals(BroadcastConstants.TIMER_UPDATE)) {
//                String s = intent.getStringExtra("update");
//                if (textTime != null) {
//                    textTime.setVisibility(View.VISIBLE);
//                    textTime.setText(s);
//                }
//            } else if (action.equals(BroadcastConstants.TIMER_STOP)) {
//                if (textTime != null) {
//                    textTime.setVisibility(View.GONE);
//                }
//            }
//        }
//    };

    String nickName;
    String sign;
    String gender;
    String birthday;
    String starSign;
    String email;
    String area;

    // 判断个人资料是否有修改过  有则将数据提交服务器
    private void sendUpdate(UpdatePerson pM) {
        JSONObject jsonObject = VolleyRequest.getJsonObject(context);
        try {
            nickName = pM.getNickName();
            if (!nickName.equals(sharedPreferences.getString(StringConstant.NICK_NAME, ""))) {
                if (nickName.trim().equals("")) {
                    jsonObject.put("NickName", "&null");
                } else {
                    jsonObject.put("NickName", nickName);
                }
                isUpdate = true;
            }

            sign = pM.getUserSign();
            if (!sign.equals(sharedPreferences.getString(StringConstant.USER_SIGN, ""))) {
                if (sign.trim().equals("")) {
                    jsonObject.put("UserSign", "&null");
                } else {
                    jsonObject.put("UserSign", sign);
                }
                isUpdate = true;
            }

            gender = pM.getGender();
            Log.v("gender", "gender -- > > " + gender);
            if (!gender.equals(sharedPreferences.getString(StringConstant.GENDERUSR, "xb001"))) {
                jsonObject.put("SexDictId", gender);
                isUpdate = true;
            }

            birthday = pM.getBirthday();
            if (!birthday.equals(sharedPreferences.getString(StringConstant.BIRTHDAY, ""))) {
                jsonObject.put("Birthday", Long.valueOf(birthday));
                isUpdate = true;
            }

            starSign = pM.getStarSign();
            if (!starSign.equals(sharedPreferences.getString(StringConstant.STAR_SIGN, ""))) {
                jsonObject.put("StarSign", starSign);
                isUpdate = true;
            }

            email = pM.getEmail();
            if (!email.equals(sharedPreferences.getString(StringConstant.EMAIL, ""))) {
                if (!email.trim().equals("")) {
                    if (isEmail(email)) {
                        jsonObject.put("MailAddr", email);
                        isUpdate = true;
                    } else {
                        ToastUtils.show_allways(context, "邮箱格式不正确，请重新修改!");
                    }
                } else {
                    jsonObject.put("MailAddr", "&null");
                    isUpdate = true;
                }
            }

            area = pM.getRegion();
            if (!area.equals(sharedPreferences.getString(StringConstant.REGION, ""))) {
                jsonObject.put("RegionDictId", regionId);
                isUpdate = true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            isUpdate = false;
        }

        // 个人资料没有修改过则不需要将数据提交服务器
        if (!isUpdate) {
            return;
        }
        isUpdate = false;
        Log.v("数据改动", "数据有改动，将数据提交到服务器!" );
        VolleyRequest.RequestPost(GlobalConfig.updateUserUrl, tag, jsonObject, new VolleyCallback() {
            @Override
            protected void requestSuccess(JSONObject result) {
                if (dialog != null) dialog.dismiss();
                if (isCancelRequest) return;
                try {
                    String returnType = result.getString("ReturnType");
                    Log.v("returnType", "returnType -- > > " + returnType);
                    if (returnType != null && returnType.equals("1001") || returnType != null && returnType.equals("T")) {
                        SharedPreferences.Editor et = BSApplication.SharedPreferences.edit();
                        if (!nickName.equals(sharedPreferences.getString(StringConstant.NICK_NAME, ""))) {
                            et.putString(StringConstant.NICK_NAME, nickName);
                        }
                        if (!sign.equals(sharedPreferences.getString(StringConstant.USER_SIGN, ""))) {
                            et.putString(StringConstant.USER_SIGN, sign);
                        }
                        if (!gender.equals(sharedPreferences.getString(StringConstant.GENDERUSR, ""))) {
                            et.putString(StringConstant.GENDERUSR, gender);
                        }
                        if (!birthday.equals(sharedPreferences.getString(StringConstant.BIRTHDAY, ""))) {
                            et.putString(StringConstant.BIRTHDAY, birthday);
                        }
                        if (!starSign.equals(sharedPreferences.getString(StringConstant.STAR_SIGN, ""))) {
                            et.putString(StringConstant.STAR_SIGN, starSign);
                        }
                        if (!email.equals(sharedPreferences.getString(StringConstant.EMAIL, ""))) {
                            if (!email.equals("")) {
                                if (isEmail(email)) {
                                    et.putString(StringConstant.EMAIL, email);
                                }
                            } else {
                                et.putString(StringConstant.EMAIL, "");
                            }
                        }

                        if (!area.equals(sharedPreferences.getString(StringConstant.REGION, ""))) {
                            et.putString(StringConstant.REGION, pModel.getRegion());
                        }

                        if (!et.commit()) {
                            Log.w("commit", " 数据 commit 失败!");
                        }

                        if (!userSign.equals(pModel.getUserSign())) {// 签名
                            userSign = pModel.getUserSign();
                            textUserAutograph.setText(userSign);
                        }

                        if (!region.equals(area)) {// 区域
                            region = area;
                            if (region.equals("")) {
                                if (GlobalConfig.CityName != null && !GlobalConfig.CityName.equals("null")
                                        && GlobalConfig.District != null && !GlobalConfig.District.equals("null")) {

                                    region = GlobalConfig.CityName + GlobalConfig.District;
                                } else {
                                    region = "北京东城";
                                }
                            }
                            textUserArea.setText(region);
                        }
                    } else {
//                        ToastUtils.show_allways(context, "信息修改失败!");
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

    // 验证邮箱的方法
    private boolean isEmail(String str) {
        Pattern pattern = Pattern.compile("^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$"); // 验证邮箱格式
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isCancelRequest = VolleyRequest.cancelRequest(tag);
    }
}
