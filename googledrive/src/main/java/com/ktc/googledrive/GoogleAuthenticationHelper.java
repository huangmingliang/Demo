package com.ktc.googledrive;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.ktc.googledrive.dao.AccountDbHelper;
import com.ktc.googledrive.dao.AccountEntry;
import com.ktc.googledrive.dao.GAccount;

import java.util.ArrayList;
import java.util.List;


public class GoogleAuthenticationHelper {
    private static GoogleAuthenticationHelper INSTANCE;
    private AccountDbHelper dbHelper;
    private SQLiteDatabase db;

    public GoogleAuthenticationHelper(Context context) {
        if (context!=null){
           dbHelper=new AccountDbHelper(context);
           db=dbHelper.getWritableDatabase();
        }
    }

    public static synchronized GoogleAuthenticationHelper getInstance(Context context){
        if (INSTANCE==null){
            INSTANCE=new GoogleAuthenticationHelper(context);
        }
        return INSTANCE;
    }

    public List<GAccount> getAccounts(){
        List<GAccount> accounts=new ArrayList<>();
        String[] columns={AccountEntry.SUB,AccountEntry.NAME,AccountEntry.PICTURE,AccountEntry.REFRESH_TOKEN};
        Cursor cursor=db.query(AccountEntry.TABLE_NAME,columns,null,null,null,null,null);
        while (cursor.moveToNext()){
            String sub=cursor.getString(cursor.getColumnIndex(AccountEntry.SUB));
            String name=cursor.getString(cursor.getColumnIndex(AccountEntry.NAME));
            String picture=cursor.getString(cursor.getColumnIndex(AccountEntry.PICTURE));
            String refreshToken=cursor.getString(cursor.getColumnIndex(AccountEntry.REFRESH_TOKEN));
            GAccount account=new GAccount(sub,refreshToken,name,picture);
            accounts.add(account);
        }
        return accounts;
    }

    public long saveAccount(GAccount account){
        if (account!=null&&!TextUtils.isEmpty(account.sub)){
            ContentValues values=new ContentValues();
            values.put(AccountEntry.SUB,account.sub);
            values.put(AccountEntry.NAME,account.name);
            values.put(AccountEntry.PICTURE,account.picture);
            values.put(AccountEntry.REFRESH_TOKEN,account.refreshToken);
            return db.insert(AccountEntry.TABLE_NAME,null,values);
        }else {
            return -1;
        }
    }

    public GAccount getAccountById(String id){
        GAccount account=null;
        String[] columns={AccountEntry.SUB,AccountEntry.NAME,AccountEntry.PICTURE,AccountEntry.REFRESH_TOKEN};
        Cursor cursor=db.query(AccountEntry.TABLE_NAME,columns,AccountEntry.SUB+"=?",new String[]{id},null,null,null);
        while (cursor.moveToNext()){
            String sub=cursor.getString(cursor.getColumnIndex(AccountEntry.SUB));
            String name=cursor.getString(cursor.getColumnIndex(AccountEntry.NAME));
            String picture=cursor.getString(cursor.getColumnIndex(AccountEntry.PICTURE));
            String refreshToken=cursor.getString(cursor.getColumnIndex(AccountEntry.REFRESH_TOKEN));
            account=new GAccount(sub,refreshToken,name,picture);
            break;
        }
        cursor.close();
        return account;
    }

    public boolean removeUser(GAccount gAccount){
        if (gAccount==null){
            Log.e("hml","param is null");
            return false;
        }
        int result=db.delete(AccountEntry.TABLE_NAME,AccountEntry.SUB+"=?",new String[]{gAccount.sub});
        if (result!=0){
            return true;
        }else {
            return false;
        }
    }

}
