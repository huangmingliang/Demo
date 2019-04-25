package com.ktc.demo;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
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
import com.ktc.onedrive.R;
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
        googleHelper=GoogleAuthenticationHelper.getInstance(getContext());
        getUsers();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (dm.heightPixels * 0.4));
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawableResource(android.R.color.white);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);
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
    }

    private void getUsers() {
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
        gv_user = view.findViewById(R.id.gv_user);
    }

}
