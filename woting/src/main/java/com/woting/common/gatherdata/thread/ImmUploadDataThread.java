package com.woting.common.gatherdata.thread;

import android.util.Log;

import com.woting.common.gatherdata.GatherData;
import com.woting.common.gatherdata.model.DataModel;
import com.woting.common.util.JsonEncloseUtils;
import com.woting.common.volley.VolleyRequest;

/**
 * 立即上传数据线程
 * Created by Administrator on 2017/4/11.
 */
public class ImmUploadDataThread extends Thread {

    @Override
    public void run() {
        while (GatherData.isRun) {
            try {
                DataModel data = GatherData.immQueue.take();
                if (data != null) {
                    String jsonStr = JsonEncloseUtils.btToString(data);
                    Log.v("TAG", "IMM jsonStr -- > > " + jsonStr);

                    // 上传数据
                    VolleyRequest.updateData(jsonStr);
                }

                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
