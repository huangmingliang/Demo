package com.ktc.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserBackupAdapter extends BaseAdapter {

    private List<Account> accounts;
    private Context context;
    private LayoutInflater inflater;

    public UserBackupAdapter(Context context, List<Account> accounts) {
        this.context = context;
        this.accounts = accounts;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return accounts.size()+1;
    }

    @Override
    public Object getItem(int position) {
        return accounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.adapter_backup_user, parent, false);
            holder=new ViewHolder(convertView);
            convertView.setTag(holder);
        }else {
            holder= (ViewHolder) convertView.getTag();
        }
        Account account=null;
        if (position<accounts.size()){
            account= (Account) getItem(position);
        }
        if (position==accounts.size()){
            holder.profileImage.setImageResource(R.drawable.ic_add_user);
            holder.tvUserName.setVisibility(View.GONE);
        }else {
            holder.profileImage.setImageResource(R.drawable.ic_cloud_user_default);
            holder.tvUserName.setVisibility(View.VISIBLE);
            holder.tvUserName.setText(account.name);
        }
        return convertView;
    }

    public static class ViewHolder {
        public View rootView;
        public CircleImageView profileImage;
        public TextView tvUserName;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.profileImage = rootView.findViewById(R.id.profile_image);
            this.tvUserName = rootView.findViewById(R.id.tv_userName);
        }

    }
}
