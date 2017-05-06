package com.woting.ui.interphone.find.findresult.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.woting.R;
import com.woting.common.config.GlobalConfig;
import com.woting.common.constant.IntegerConstant;
import com.woting.common.util.AssembleImageUrlUtils;
import com.woting.common.util.BitmapUtils;
import com.woting.ui.interphone.model.GroupInfo;

import java.util.List;
/**
 * 搜索结果群组的适配器
 */
public class FindGroupResultAdapter extends BaseAdapter {
    private List<GroupInfo> list;
    private Context context;
    private String url;

    public FindGroupResultAdapter(Context context, List<GroupInfo> list) {
        this.list = list;
        this.context = context;
    }

    public void ChangeData(List<GroupInfo> list) {
        this.list = list;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_contactquery, null);
            holder = new ViewHolder();
            holder.textview_invitename = (TextView) convertView.findViewById(R.id.RankTitle);        // 人名

            holder.textview_invitemessage = (TextView) convertView.findViewById(R.id.RankContent);    // 介绍
            holder.textview_invitemessage.setVisibility(View.GONE);

            holder.tv_b_id = (TextView) convertView.findViewById(R.id.RankId);//id
            holder.imageview_inviteimage = (ImageView) convertView.findViewById(R.id.RankImageUrl);    // 该人头像

            holder.img_zhezhao = (ImageView) convertView.findViewById(R.id.img_zhezhao);
            Bitmap bmp_zhezhao = BitmapUtils.readBitMap(context, R.mipmap.wt_6_b_y_b);
            holder.img_zhezhao.setImageBitmap(bmp_zhezhao);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        GroupInfo Inviter = list.get(position);
        if (Inviter.getGroupName() == null || Inviter.getGroupName().equals("")) {
            holder.textview_invitename.setText("未知");
        } else {
            holder.textview_invitename.setText(Inviter.getGroupName());
        }

        if (Inviter.getGroupNum() == null || Inviter.getGroupNum().equals("")) {
            holder.tv_b_id.setVisibility(View.GONE);
            holder.tv_b_id.setText("群号: " + "未知");//id
        } else {
            holder.tv_b_id.setVisibility(View.VISIBLE);
            holder.tv_b_id.setText("群号: " + Inviter.getGroupNum());//id
        }

        if (Inviter.getGroupImg() == null || Inviter.getGroupImg().equals("")
                || Inviter.getGroupImg().equals("null") || Inviter.getGroupImg().trim().equals("")) {
            Bitmap bmp = BitmapUtils.readBitMap(context, R.mipmap.wt_image_tx_qz);
            holder.imageview_inviteimage.setImageBitmap(bmp);
        } else {
            if (Inviter.getGroupImg().startsWith("http:")) {
                url = Inviter.getGroupImg();
            } else {
                url = GlobalConfig.imageurl + Inviter.getGroupImg();
            }
            String _url = AssembleImageUrlUtils.assembleImageUrl150(url);
            // 加载图片
            AssembleImageUrlUtils.loadImage(url, _url, holder.imageview_inviteimage, IntegerConstant.TYPE_GROUP);
        }
        return convertView;
    }

    class ViewHolder {
        public TextView textview_invitename;
        public TextView textview_invitemessage;
        public ImageView imageview_inviteimage;
        public ImageView img_zhezhao;
        public TextView tv_b_id;
    }
}
