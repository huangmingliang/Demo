package com.ktc.onedrive.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ktc.onedrive.Fragment.BottomDialogFragment;
import com.ktc.onedrive.GraphHelper;
import com.ktc.onedrive.R;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.extensions.DriveItem;
import com.microsoft.graph.extensions.Thumbnail;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FileAdapter extends BaseAdapter {

    private List<DriveItem> items;
    private Context context;
    private LayoutInflater inflater;
    private FragmentManager fragmentManager;
    private GraphHelper graphHelper=GraphHelper.getInstance();

    public FileAdapter(Context context, List<DriveItem> items,FragmentManager fragmentManager) {
        this.items = items;
        this.context = context;
        this.fragmentManager=fragmentManager;
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
        final DriveItem item = (DriveItem) getItem(position);
        holder.tvFileName.setText(item.name);
        holder.ivMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomDialogFragment dialogFragment=BottomDialogFragment.newInstance(item.id,item.file!=null,item.name,item.size);
                dialogFragment.show(fragmentManager,"bottom");
            }
        });
        if (item.folder != null) {
            holder.ivFile.setImageResource(R.mipmap.folder);
        } else {
            String mimeType = item.file.mimeType;
            if (mimeType.startsWith("image/")) {
                loadThumbnail(item.id,R.mipmap.photo,holder.ivFile);
            } else if (mimeType.startsWith("audio/")) {
                holder.ivFile.setImageResource(R.mipmap.audio);
            } else if (mimeType.startsWith("video/")) {
                loadThumbnail(item.id,R.mipmap.video,holder.ivFile);
            } else if (mimeType.equals("application/pdf")) {
                loadThumbnail(item.id,R.mipmap.pdf,holder.ivFile);
            } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                holder.ivFile.setImageResource(R.mipmap.docx);
            }else if (mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){
                holder.ivFile.setImageResource(R.mipmap.xlsx);
            }else {
                loadThumbnail(item.id,R.mipmap.default_thumbnail,holder.ivFile);
            }

        }
        return convertView;
    }

    private void loadThumbnail(String itemId, final int resId, final ImageView imageView){
        graphHelper.getDriveItemThumbnail(itemId, "medium", new ICallback<Thumbnail>() {
            @Override
            public void success(Thumbnail thumbnail) {
                Picasso.get()
                        .load(thumbnail.url)
                        .placeholder(resId)
                        .error(resId)
                        .resize(thumbnail.width, thumbnail.height)
                        .centerInside()
                        .tag(context)
                        .into(imageView);
            }
            @Override
            public void failure(ClientException ex) {
                imageView.setImageResource(resId);
            }
        });
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
