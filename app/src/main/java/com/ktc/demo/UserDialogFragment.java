package com.ktc.demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;

import com.ktc.googledrive.GoogleAuthenticationHelper;
import com.ktc.googledrive.GoogleDriveActivity;
import com.ktc.googledrive.dao.GAccount;
import com.ktc.onedrive.AuthenticationHelper;
import com.ktc.onedrive.OneDriveActivity;
import com.ktc.share.DensityUtil;
import com.microsoft.identity.client.IAccount;

import java.util.ArrayList;
import java.util.List;

public class UserDialogFragment extends DialogFragment {

    private AuthenticationHelper mAuthHelper = null;
    private GoogleAuthenticationHelper googleHelper=null;
    private List<Account> accounts = new ArrayList<>();
    private UserBackupAdapter adapter;
    private GridView gv_user;

    static public UserDialogFragment newInstance() {
        return new UserDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthHelper = AuthenticationHelper.getInstance(getContext());
        googleHelper= GoogleAuthenticationHelper.getInstance(getContext());
        getMicroUsers();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            Window window = dialog.getWindow();
            window.setLayout(DensityUtil.dip2px(getContext(),668),DensityUtil.dip2px(getContext(),398));
            window.setGravity(Gravity.CENTER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(com.ktc.onedrive.R.layout.fragment_user, container, false);
        initView(view);
        initUsers();
        initView(view);
        return view;
    }


    private void initUsers() {
        adapter = new UserBackupAdapter(getContext(), accounts);
        gv_user.setAdapter(adapter);
        gv_user.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < accounts.size()) {
                    Account account = accounts.get(position);
                    if ("micro".equals(account.type)) {
                        OneDriveActivity.navToOneDrive(getContext(), account.id);
                    } else {
                        GoogleDriveActivity.navToGoogleDrive(getContext(),account.id);
                    }
                } else {
                    PlatformDialogFragment dialogFragment = PlatformDialogFragment.newInstance();
                    dialogFragment.show(getFragmentManager(), "platform");
                }
                dismiss();
            }
        });

        gv_user.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.d("hml","position="+position);
                if (position<accounts.size()){
                    final Account account=accounts.get(position);
                    createDeleteDialog(account, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            boolean result=false;
                            if (account.type.equals("micro")){
                                result=mAuthHelper.removeUser(account.iAccount);
                            }else if (account.type.equals("google")){
                                result=googleHelper.removeUser(account.gAccount);
                            }
                            if (result){
                                accounts.remove(position);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });

                    return true;
                }
                return false;
            }
        });
    }

    private void getMicroUsers() {
        Account account;
        for (IAccount iAccount : mAuthHelper.getAccounts()) {
            account = new Account(iAccount.getAccountIdentifier().getIdentifier(), iAccount.getUsername(), "micro", iAccount);
            accounts.add(account);
        }

        for (GAccount gAccount:googleHelper.getAccounts()){
            account=new Account(gAccount.sub,gAccount.name,"google",gAccount);
            accounts.add(account);
        }
    }


    private void initView(View view) {
        gv_user = view.findViewById(com.ktc.onedrive.R.id.gv_user);

    }

    private void createDeleteDialog(Account account, DialogInterface.OnClickListener listener){
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        if (account.type.equals("micro")){
            builder.setTitle("OneDrive");
        }else {
            builder.setTitle("GoogleDrive");
        }
        builder.setMessage(String.format(getString(com.ktc.onedrive.R.string.delete_account),account.name));
        builder.setPositiveButton(getString(com.ktc.onedrive.R.string.ok),listener);
        builder.setNegativeButton(getString(com.ktc.onedrive.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

}
