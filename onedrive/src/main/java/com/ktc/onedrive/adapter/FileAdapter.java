package com.ktc.onedrive.adapter;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.ktc.onedrive.GraphHelper;
import com.ktc.onedrive.R;
import com.ktc.share.DensityUtil;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.extensions.DriveItem;
import com.microsoft.graph.extensions.Thumbnail;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.List;

public class FileAdapter extends BaseAdapter {

    private List<DriveItem> items;
    private Context context;
    private LayoutInflater inflater;
    private GraphHelper graphHelper = GraphHelper.getInstance();

    public FileAdapter(Context context, List<DriveItem> items) {
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
        final DriveItem item = (DriveItem) getItem(position);
        holder.tvFileName.setText(item.name);
        SimpleDateFormat format=new SimpleDateFormat(context.getString(R.string.modified_time)+"yyyy/MM/dd HH:mm");
        holder.tvModifiedTime.setText(format.format(item.lastModifiedDateTime.getTime()));
        holder.ivMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               initPopupWindow(holder.ivMore,position,item.folder!=null);
            }
        });
        holder.ivType.setImageDrawable(null);
        if (item.folder != null) {
            holder.ivType.setImageResource(R.drawable.ic_file_folder);
        } else {
            String mimeType = item.file.mimeType;
            if (mimeType.startsWith("image/")) {
                loadThumbnail(item.id, R.drawable.ic_file_picture, holder.ivThumbnail,holder.ivType,false);
            }else if (mimeType.startsWith("video/")) {
                loadThumbnail(item.id, R.drawable.ic_file_video, holder.ivThumbnail,holder.ivType,true);
            } else if (mimeType.equals("application/pdf")) {
                loadThumbnail(item.id, R.drawable.ic_file_pdf,holder.ivThumbnail,holder.ivType,false);
            } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                holder.ivType.setImageResource(R.drawable.ic_file_doc);
            } else if (mimeType.startsWith("audio/")) {
                holder.ivType.setImageResource(R.drawable.ic_file_audio);
            }  else {
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

    private void loadThumbnail(String itemId, final int resId, final ImageView ivThumbnail, final ImageView ivType, final boolean video) {
        graphHelper.getDriveItemThumbnail(itemId, "large", new ICallback<Thumbnail>() {
            @Override
            public void success(Thumbnail thumbnail) {
                Picasso.get()
                        .load(thumbnail.url)
                        .centerCrop()
                        .resize(DensityUtil.dip2px(context,154),DensityUtil.dip2px(context,140))
                        .tag(context)
                        .into(ivThumbnail);
                if (video){
                    ivType.setImageResource(R.drawable.ic_video);
                }
            }

            @Override
            public void failure(ClientException ex) {
                ivType.setImageResource(resId);
            }
        });
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
