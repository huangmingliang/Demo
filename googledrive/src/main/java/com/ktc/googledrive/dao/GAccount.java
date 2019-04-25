package com.ktc.googledrive.dao;


public class GAccount {
    public GAccount(String sub, String refreshToken, String name, String picture) {
        this.sub = sub;
        this.refreshToken = refreshToken;
        this.name = name;
        this.picture = picture;
    }
    public String sub;
    public String refreshToken;
    public String name;
    public String picture;


}
