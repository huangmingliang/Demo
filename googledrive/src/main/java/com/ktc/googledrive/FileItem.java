package com.ktc.googledrive;

import java.util.List;

public class FileItem {
    String kind;
    String id;
    String name;
    String mimeType;
    boolean directory;
    private List<String> parents;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }


    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public List<String> getParents() {
        return parents;
    }

    public void setParents(List<String> parents) {
        this.parents = parents;
    }

    @Override
    public String toString() {
        return "FileItem{" +
                "kind='" + kind + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", directory=" + directory +
                ", parents=" + parents +
                '}';
    }
}
