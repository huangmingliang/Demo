package com.ktc.demo;

import com.ktc.googledrive.dao.GAccount;
import com.microsoft.identity.client.IAccount;

public class Account {

    public Account(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Account(String id, String name, String type,IAccount iAccount) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.iAccount=iAccount;
    }

    public Account(String id, String name, String type,GAccount gAccount) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.gAccount=gAccount;
    }


    public IAccount iAccount;
    public GAccount gAccount;
    public String id;
    public String name;
    public String type;
}
