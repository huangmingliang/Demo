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
import android.widget.TextView;

import com.ktc.googledrive.GoogleDriveActivity;
import com.ktc.onedrive.OneDriveActivity;
import com.ktc.onedrive.R;

public class PlatformDialogFragment extends DialogFragment implements View.OnClickListener {

    private TextView tvOneDrive;
    private TextView tvGoogleDrive;

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
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (dm.heightPixels * 0.15));
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawableResource(android.R.color.white);
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
        tvOneDrive = view.findViewById(R.id.tv_one_drive);
        tvGoogleDrive = view.findViewById(R.id.tv_google_drive);

        tvOneDrive.setOnClickListener(this);
        tvGoogleDrive.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.tv_one_drive) {
            OneDriveActivity.navToOneDrive(getContext());
        } else if (i == R.id.tv_google_drive) {
            GoogleDriveActivity.navToGoogleDrive(getContext());
        }
        dismiss();
    }
}
