package com.ktc.googledrive;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

public class BottomDialogFragment extends DialogFragment implements View.OnClickListener {

    private DriveHelper driveHelper = DriveHelper.getInstance();
    private GoogleDriveActivity activity;
    private LinearLayout llDownload;
    private LinearLayout llDelete;
    private String itemId;
    private boolean isFile;
    private String fileName;
    private int REQUEST_WRITE_EXTERNAL_STORAGE = 100;
    private final String ONE_DRIVE_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/GoogleDrive";

    public static BottomDialogFragment newInstance(String itemId, boolean isFile, String fileName) {
        Bundle args = new Bundle();
        args.putString("itemId", itemId);
        args.putBoolean("isFile", isFile);
        args.putString("fileName", fileName);
        BottomDialogFragment fragment = new BottomDialogFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        activity = (GoogleDriveActivity) getActivity();
        if (bundle != null) {
            itemId = bundle.getString("itemId");
            isFile = bundle.getBoolean("isFile");
            fileName = bundle.getString("fileName");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            Window window = dialog.getWindow();
            window.setWindowAnimations(R.style.BottomDialogAnimation);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawableResource(android.R.color.white);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_dialog, container, false);

        initView(view);
        setViewVisible();
        return view;
    }

    private void initView(View view) {
        llDownload = view.findViewById(R.id.ll_download);
        llDelete = view.findViewById(R.id.ll_delete);
        llDownload.setOnClickListener(this);
        llDelete.setOnClickListener(this);
    }

    private void setViewVisible() {
        llDownload.setVisibility(isFile ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.ll_download) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission
                        .WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(getContext(), "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
                }
                //申请权限
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                Toast.makeText(getContext(), fileName + "准备下载", Toast.LENGTH_SHORT).show();
                driveHelper.downloadFile(itemId, getFileOutputStream(fileName), new CustomProgressDownloadListener());
            }

        } else if (i == R.id.ll_delete) {
            driveHelper.deleteFile(itemId).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.e("hml","onSuccess");
                    activity.refresh();
                    dismiss();
                }
            });
        }
        getDialog().hide();
    }


    private ProgressDialog dialog;

    private class CustomProgressDownloadListener implements MediaHttpDownloaderProgressListener {
        public CustomProgressDownloadListener() {
            dialog = createProgressBar();
            dialog.show();
        }

        @Override
        public void progressChanged(MediaHttpDownloader downloader) {
            switch (downloader.getDownloadState()) {
                case MEDIA_IN_PROGRESS:
                    dialog.setProgress((int) downloader.getProgress());
                    break;
                case MEDIA_COMPLETE:
                    dialog.setProgress(100);
                    dialog.setTitle("下载完成");
                    break;
            }
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), fileName + "准备下载", Toast.LENGTH_SHORT).show();
                driveHelper.downloadFile(itemId, getFileOutputStream(fileName), new CustomProgressDownloadListener());
            }
        }
    }

    private FileOutputStream getFileOutputStream(String fileName) {
        FileOutputStream fos = null;
        File dir = new File(ONE_DRIVE_SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = new File(dir, fileName);
        if (target.exists()) {
            target.delete();
        }
        try {
            target.createNewFile();
            fos = new FileOutputStream(target);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            return fos;
        }
    }

    private ProgressDialog createProgressBar() {
        ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setTitle("正在下载");
        dialog.setMax(100);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        return dialog;
    }

}