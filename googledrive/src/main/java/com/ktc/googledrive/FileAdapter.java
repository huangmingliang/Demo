package com.ktc.googledrive;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.services.drive.model.File;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FileAdapter extends BaseAdapter {

    private List<File> items;
    private Context context;
    private LayoutInflater inflater;
    private FragmentManager fragmentManager;

    public FileAdapter(Context context, List<File> items, FragmentManager fragmentManager) {
        this.items = items;
        this.context = context;
        this.fragmentManager = fragmentManager;
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
    public View getView(int position, View convertView, ViewGroup parent) {
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
        holder.ivMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isFile=item.getMimeType().equals("application/vnd.google-apps.folder");
               BottomDialogFragment dialogFragment=BottomDialogFragment.newInstance(item.getId(),isFile,item.getName());
                dialogFragment.show(fragmentManager,"bottom");
            }
        });
        String mimeType = item.getMimeType();
        if (mimeType.equals("application/vnd.google-apps.folder")) {
            holder.ivFile.setImageResource(R.mipmap.folder);
        } else {
            if (mimeType.startsWith("image/")) {
                //Log.e("hml","thumbnail="+item.getThumbnailLink());
                loadThumbnail(item.getThumbnailLink(), R.mipmap.photo, holder.ivFile);
            } else if (mimeType.startsWith("audio/")) {
                holder.ivFile.setImageResource(R.mipmap.audio);
            } else if (mimeType.startsWith("video/")) {
                loadThumbnail(item.getThumbnailLink(), R.mipmap.video, holder.ivFile);
            } else if (mimeType.equals("application/pdf")) {
                loadThumbnail(item.getThumbnailLink(), R.mipmap.pdf, holder.ivFile);
            } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                holder.ivFile.setImageResource(R.mipmap.docx);
            } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                holder.ivFile.setImageResource(R.mipmap.xlsx);
            } else {
                loadThumbnail(item.getThumbnailLink(), R.mipmap.default_thumbnail, holder.ivFile);
            }

        }
        return convertView;
    }

    private void loadThumbnail(String thumbnail, final int resId, final ImageView imageView) {
        Picasso.get()
                .load(thumbnail)
                .placeholder(resId)
                .error(resId)
                .centerInside()
                .tag(context);

    }


    public static class ViewHolder {
        public View rootView;
        public ImageView ivFile;
        public TextView tvFileName;
        public ImageView ivMore;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.ivFile = rootView.findViewById(R.id.iv_file);
            this.tvFileName = rootView.findViewById(R.id.tv_fileName);
            this.ivMore = rootView.findViewById(R.id.iv_more);
        }

    }

}
