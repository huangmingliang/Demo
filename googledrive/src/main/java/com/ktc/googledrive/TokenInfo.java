package com.ktc.googledrive;

import java.io.Serializable;

public class TokenInfo implements Serializable{

    public String access_token;
    public int expires_in;
    public String refresh_token;
    public String scope;
    public String token_type;
    public String id_token;

}
