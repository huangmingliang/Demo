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
import android.widget.ImageView;

import com.ktc.googledrive.GoogleDriveActivity;
import com.ktc.onedrive.OneDriveActivity;
import com.ktc.share.DensityUtil;

public class PlatformDialogFragment extends DialogFragment implements View.OnClickListener {

    private View rlMicro;
    private View rlGoogle;
    private ImageView ivCancel;

    static public PlatformDialogFragment newInstance() {
        return new PlatformDialogFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            Window window = dialog.getWindow();
            window.setLayout(DensityUtil.dip2px(getContext(),358), DensityUtil.dip2px(getContext(),263));
            window.setGravity(Gravity.CENTER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_platform, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        ivCancel=view.findViewById(R.id.iv_cancel);
        ivCancel.setOnClickListener(this);
        rlMicro = view.findViewById(R.id.rl_micro);
        rlMicro.setOnClickListener(this);
        rlGoogle = view.findViewById(R.id.rl_google);
        rlGoogle.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.rl_micro) {
            OneDriveActivity.navToOneDrive(getContext());
        } else if (i == R.id.rl_google) {
            GoogleDriveActivity.navToGoogleDrive(getContext());
        }
        dismiss();
    }
}
