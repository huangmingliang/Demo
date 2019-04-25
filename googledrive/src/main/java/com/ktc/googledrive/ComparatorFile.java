package com.ktc.googledrive;


import com.google.api.services.drive.model.File;

import java.util.Comparator;

public class ComparatorFile implements Comparator<File> {

    @Override
    public int compare(File o1, File o2) {
        String mimeType1=o1.getMimeType();
        String mimeType2=o2.getMimeType();
        String folder="application/vnd.google-apps.folder";
        if (mimeType1.equals(folder)&&!mimeType2.equals(folder)){
            return -1;
        }else if (!mimeType1.equals(folder)&&mimeType2.equals(folder)){
            return 1;
        }else {
            return 0;
        }

    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}
