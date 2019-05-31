package com.ktc.googledrive;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.File;
import com.ktc.share.DensityUtil;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FileAdapter extends BaseAdapter {

    private List<File> items;
    private Context context;
    private LayoutInflater inflater;

    public FileAdapter(Context context, List<File> items) {
        this.items = items;
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_file, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final File item = (File) getItem(position);
        holder.tvFileName.setText(item.getName());
        DateTime dateTime=item.getModifiedTime();
        if (dateTime!=null){
            Date date=new Date(dateTime.getValue());
            SimpleDateFormat format=new SimpleDateFormat(context.getString(R.string.modified_time)+"yyyy/MM/dd HH:mm");
            holder.tvModifiedTime.setText(format.format(date));
        }
        holder.ivMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isFolder = item.getMimeType().equals("application/vnd.google-apps.folder");
                initPopupWindow(holder.ivMore,position,isFolder);
            }
        });
        String mimeType = item.getMimeType();
        holder.ivType.setImageDrawable(null);
        loadThumbnail(item.getThumbnailLink(),holder.ivThumbnail);
        if (mimeType.equals("application/vnd.google-apps.folder")) {
            holder.ivType.setImageResource(R.drawable.ic_file_folder);
        } else {
            if (mimeType.startsWith("image/")) {
                //loadThumbnail(item.getThumbnailLink(),holder.ivThumbnail);
            }else if (mimeType.startsWith("video/")) {

                holder.ivType.setImageResource(R.drawable.ic_video);
            } else if (mimeType.equals("application/pdf")) {
                //loadThumbnail(item.getThumbnailLink(),holder.ivThumbnail);
            } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                holder.ivType.setImageResource(R.drawable.ic_file_doc);
            }  else if (mimeType.startsWith("audio/")) {
                holder.ivType.setImageResource(R.drawable.ic_file_audio);
            } else {
                holder.ivType.setImageResource(R.drawable.ic_file_other);
            }

        }
        return convertView;
    }

    private void initPopupWindow(View view, final int position,boolean folder){
        final PopupWindow popupWindow=new PopupWindow();
        popupWindow.setWidth(DensityUtil.dip2px(context,59));
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        //popupWindow.setBackgroundDrawable(context.getDrawable(R.drawable.bg_file_window));
        popupWindow.setFocusable(true);
        View content=LayoutInflater.from(context).inflate(R.layout.window_file,null);
        TextView tvDownload=content.findViewById(R.id.tv_save);
        tvDownload.setVisibility(folder?View.GONE:View.VISIBLE);
        tvDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                if (listener!=null){
                    listener.onDownload(position);
                }
            }
        });
        TextView tvDelete=content.findViewById(R.id.tv_delete);
        tvDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                if (listener!=null){
                    listener.onDelete(position);
                }
            }
        });
        popupWindow.setContentView(content);
        popupWindow.showAsDropDown(view,-46,0, Gravity.NO_GRAVITY);

    }

    private void loadThumbnail(String thumbnail, final ImageView imageView) {
        Picasso.get()
                .load(thumbnail)
                .centerCrop()
                .resize(DensityUtil.dip2px(context,154),DensityUtil.dip2px(context,140))
                .tag(context)
                .into(imageView);


    }

    private OnFileListener listener;
    public void setOnFileListener(OnFileListener listener){
        this.listener=listener;
    }
    public interface OnFileListener{
        void onDelete(int position);
        void onDownload(int position);
    }

    public static class ViewHolder {
        public View rootView;
        public ImageView ivThumbnail;
        public ImageView ivType;
        public ImageView ivMore;
        public TextView tvFileName;
        public TextView tvModifiedTime;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.ivThumbnail = rootView.findViewById(R.id.iv_thumbnail);
            this.ivType = rootView.findViewById(R.id.iv_type);
            this.ivMore = rootView.findViewById(R.id.iv_more);
            this.tvFileName = rootView.findViewById(R.id.tv_file_name);
            this.tvModifiedTime = rootView.findViewById(R.id.tv_modified_time);
        }

    }

}
