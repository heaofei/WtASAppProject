package com.woting.ui.mine.myupload.upload;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.woting.R;
import com.woting.common.util.ToastUtils;
import com.woting.ui.baseactivity.AppBaseActivity;
import com.woting.ui.mine.myupload.adapter.SelectFileListAdapter;
import com.woting.ui.mine.myupload.model.MediaStoreInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 选择本地音频文件
 */
public class SelectLocalFileActivity extends AppBaseActivity implements
        View.OnClickListener, AdapterView.OnItemClickListener, SelectFileListAdapter.ImagePlayListener {

    private SelectFileListAdapter adapter;
    private  List<MediaStoreInfo> list;

    private int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_local_file);

        initView();
    }

    // 初始化视图
    private void initView() {
        findViewById(R.id.image_left_back).setOnClickListener(this);// 返回
        findViewById(R.id.text_recording).setOnClickListener(this);// 录音

        Button btnNext = (Button) findViewById(R.id.btn_next);// 下一步
        btnNext.setOnClickListener(this);

        TextView textTip = (TextView) findViewById(R.id.text_tip);
        ListView listView = (ListView) findViewById(R.id.list_view);// 文件列表

        list = getLocalAudioFile();
        if(list == null || list.size() <= 0) {
            textTip.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.GONE);
        } else {
//            textTip.setVisibility(View.GONE);
//            btnNext.setVisibility(View.VISIBLE);
            listView.setSelector(new ColorDrawable(Color.TRANSPARENT));
            listView.setAdapter(adapter = new SelectFileListAdapter(context, list));
            listView.setOnItemClickListener(this);
            adapter.setImagePlayListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_left_back:// 返回
                finish();
                break;
            case R.id.text_recording:// 录音
                ToastUtils.show_allways(context, "录音");
                break;
            case R.id.btn_next:// 下一步
                Intent intent = new Intent(context, UploadActivity.class);
                intent.putExtra("MEDIA__FILE_PATH", list.get(index).getData());
                startActivityForResult(intent, 0xeee);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(index != position) {
            index = position;
            adapter.setIndex(index);
        }
//        MediaStoreInfo data = list.get(position);// 获取路径
//        ToastUtils.show_allways(context, data.getData());
    }

    @Override
    public void playClick() {
        startActivity(getAudioFileIntent(list.get(index).getData()));
    }

    private Intent getAudioFileIntent(String audioFilePath) {
        Intent mIntent = new Intent();
        mIntent.setAction(android.content.Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(new File(audioFilePath));
        mIntent.setDataAndType(uri , "audio/*");
        return mIntent;
    }

    // 获取本地音频文件
    private List<MediaStoreInfo> getLocalAudioFile() {
        List<MediaStoreInfo> list = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (cursor == null) {
            return null;
        }
        cursor.moveToFirst();
        int fileNum = cursor.getCount();
        Log.i("MainActivity", "--------- AUDIO START ---------");
        for (int counter = 0; counter < fileNum; counter++) {
            MediaStoreInfo mediaStoreInfo = new MediaStoreInfo();
            String data1 = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String type = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
//            long addTime = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR));
            long addTime = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED));
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            mediaStoreInfo.setData(data1);
            mediaStoreInfo.setTitle(title);
            mediaStoreInfo.setType(type);
            mediaStoreInfo.setId(id);
            mediaStoreInfo.setSize(size);
            mediaStoreInfo.setAddTime(addTime);
            mediaStoreInfo.setDuration(duration);
            list.add(mediaStoreInfo);
            Log.v("MainActivity", "position=" + counter);
            Log.i("MainActivity", "data1=" + data1);
            Log.i("MainActivity", "title=" + title);
            Log.i("MainActivity", "type=" + type);
            Log.i("MainActivity", "id=" + id);
            Log.i("MainActivity", "size=" + size);
            Log.i("MainActivity", "addTime=" + addTime);
            Log.i("MainActivity", "duration=" + duration);
            cursor.moveToNext();
        }
        cursor.close();
        Log.i("MainActivity", "--------- AUDIO END ---------");
        return list;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0xeee) {
            if(resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            }
        }
    }
}
