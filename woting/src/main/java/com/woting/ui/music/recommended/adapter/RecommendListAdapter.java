package com.woting.ui.music.recommended.adapter;

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
import com.woting.common.constant.StringConstant;
import com.woting.common.util.AssembleImageUrlUtils;
import com.woting.common.util.BitmapUtils;
import com.woting.ui.model.content;

import java.util.List;

/**
 * 推荐数据展示
 */
public class RecommendListAdapter extends BaseAdapter {
    private List<content> list;
    private Context context;
    private Bitmap bmp;

    public RecommendListAdapter(Context context, List<content> list) {
        this.context = context;
        this.list = list;
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
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_fragment_recommend, null);

            // 六边形封面图片遮罩
            Bitmap bitmapMask = BitmapUtils.readBitMap(context, R.mipmap.wt_6_b_y_b);
            holder.imageMask = (ImageView) convertView.findViewById(R.id.img_zhezhao);
            holder.imageMask.setImageBitmap(bitmapMask);

            holder.imageCover = (ImageView) convertView.findViewById(R.id.RankImageUrl);// 封面图片
            holder.textTitle = (TextView) convertView.findViewById(R.id.RankTitle);// 标题
            holder.textContent = (TextView) convertView.findViewById(R.id.RankContent);// 专辑或主播
            holder.textCount = (TextView) convertView.findViewById(R.id.tv_num);// 收听次数
            holder.imageNumberTime = (ImageView) convertView.findViewById(R.id.image_number_time);// 时间或集数图标
            holder.textTotal = (TextView) convertView.findViewById(R.id.tv_total);// 时间或集数

            bmp = BitmapUtils.readBitMap(context, R.mipmap.wt_image_playertx);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        content lists = list.get(position);
        String mediaType = lists.getMediaType();// 媒体类型

        // 封面图片
        String contentImg = lists.getContentImg();
        if (contentImg == null || contentImg.equals("null") || contentImg.trim().equals("")) {
            holder.imageCover.setImageBitmap(bmp);
        } else {
            if (!contentImg.startsWith("http")) {
                contentImg = GlobalConfig.imageurl + contentImg;
            }
            String _url = AssembleImageUrlUtils.assembleImageUrl180(contentImg);
            // 加载图片
            AssembleImageUrlUtils.loadImage(_url, contentImg, holder.imageCover, IntegerConstant.TYPE_LIST);
        }

        // 标题
        String contentName = lists.getContentName();
        if (contentName == null || contentName.equals("")) {
            contentName = "未知";
        }
        holder.textTitle.setText(contentName);

        // 专辑或主播
        String name;
        if (mediaType != null && mediaType.equals(StringConstant.TYPE_SEQU)) {
            name = lists.getContentName();
            if (name == null || name.trim().equals("")) {
                name = "未知";
            }
        } else {
            try {
                name = lists.getContentPersons().get(0).getPerName();
                if (name == null || name.equals("")) {
                    name = "未知";
                }
            } catch (Exception e) {
                e.printStackTrace();
                name = "未知";
            }
        }
        holder.textContent.setText(name);

        // 收听次数
        String playCount = lists.getPlayCount();
        if (playCount == null || playCount.equals("") || playCount.equals("null")) {
            playCount = "0";
        }
        holder.textCount.setText(playCount);

        // 时间或集数
        if (mediaType != null) {
            switch (mediaType) {
                case StringConstant.TYPE_SEQU:// 专辑  显示集数
                    holder.imageNumberTime.setImageResource(R.mipmap.image_program_number);
                    String contentCount = lists.getContentSubCount();
                    if (contentCount == null || contentCount.equals("") || contentCount.equals("null")) {
                        contentCount = "0";
                    }
                    contentCount = contentCount + "集";
                    holder.textTotal.setText(contentCount);
                    break;
                case StringConstant.TYPE_AUDIO:// 单体节目  显示时长
                case StringConstant.TYPE_RADIO:
                    holder.imageNumberTime.setImageResource(R.mipmap.image_program_time);
                    String contentTime = lists.getContentTimes();
                    if (contentTime == null || contentTime.equals("") || contentTime.equals("null")) {
                        contentTime = context.getString(R.string.play_time);
                    } else {
                        long minute = Long.valueOf(lists.getContentTimes()) / (1000 * 60);
                        long second = (Long.valueOf(lists.getContentTimes()) / 1000) % 60;
                        if (second < 10) {
                            contentTime = minute + "\'" + " " + "0" + second + "\"";
                        } else {
                            contentTime = minute + "\'" + " " + second + "\"";
                        }
                    }
                    holder.textTotal.setText(contentTime);
                    break;
            }
        }

        return convertView;
    }

    static class ViewHolder {
        public ImageView imageMask;// 六边形封面图片遮罩
        public ImageView imageCover;// 封面图片
        public TextView textTitle;// 标题
        public TextView textContent;// 专辑或主播
        public TextView textCount;// 收听次数
        public ImageView imageNumberTime;// 时间或集数图标
        public TextView textTotal;// 时间或集数
    }

}
