package com.example.yellow.gpssensor;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.w3c.dom.Text;

public class login extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private boolean isFirstLaunch;
    private MYSQL sql;
    private int SignMode=1;//默认1-登录模式，2-注册模式

    //登录微信的应用ID
    private static final String APP_ID="wxd8e1494ccbebbd1f";
    //和微信通信的openapi接口
    private IWXAPI api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sql=new MYSQL(this);
        checkShouldRegister();
        init_login();
    }
    private void init_login()
    {
        final Button unlogin = (Button)findViewById(R.id.login_no_register_button);
        unlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                        + getResources().getResourcePackageName(R.drawable.qq_zone) + "/"
                        + getResources().getResourceTypeName(R.drawable.qq_zone) + "/"
                        + getResources().getResourceEntryName(R.drawable.qq_zone));
                sql.new_user("游客",null,uri.toString());
                Cursor c=sql.select_user_by_name("游客");
                c.moveToNext();
                DataShare ds=((DataShare)getApplicationContext());
                ds.setUserid(c.getString(0));
                ds.setUsername("游客");
                goToHome();
            }
        });
    }
    public void checkShouldRegister(){
        sharedPreferences=getApplicationContext().getSharedPreferences("MyPreference",MODE_PRIVATE);
        isFirstLaunch=sharedPreferences.getBoolean("isFirstLaunch",true);
        if(!isFirstLaunch){
            DataShare ds=((DataShare)getApplicationContext());
            ds.setUsername(sharedPreferences.getString("Username","游客"));
            Cursor c=sql.select_user_by_name(ds.getUsername());
            c.moveToNext();
            ds.setUserid(c.getString(0));
            goToHome();
        }
        else{
            //Guide The User To Register In
        }
    }
    public void goToHome(){
        Intent intent = new Intent(login.this,home_page.class);
        DataShare ds=((DataShare)getApplicationContext());
        intent.putExtra("user",ds.getUserid());
        startActivity(intent);
        login.this.finish();
    }

    public void registerVisibility(int mode){//1-exist user login,  2-new user register
        ConstraintLayout CL1=(ConstraintLayout)findViewById(R.id.login_alternative_container);
        TextView tv=(TextView)findViewById(R.id.confirm_password_text);
        EditText et=(EditText)findViewById(R.id.confirm_password_edit);
        EditText etname=(EditText)findViewById(R.id.login_name_edit);
        Button btn=(Button)findViewById(R.id.login_btn);
        if(mode==1){
            CL1.setVisibility(View.VISIBLE);
            tv.setVisibility(View.GONE);
            et.setVisibility(View.GONE);
            //默认输入上次登录的用户名
            String str=sharedPreferences.getString("Username","游客");
            if(!str.equals("游客")) etname.setText(str);

            btn.setText("登录");
        }
        else if(mode==2){
            CL1.setVisibility(View.GONE);
            tv.setVisibility(View.VISIBLE);
            et.setVisibility(View.VISIBLE);
            btn.setText("完成");
        }
    }
    public void goToRegister(View view){
        SignMode=2;
        registerVisibility(2);
    }
    public void RegisterIn(View view){
        EditText et_confirm=(EditText)findViewById(R.id.confirm_password_edit);
        EditText et_new=(EditText)findViewById(R.id.login_password_edit);
        EditText et_name=(EditText)findViewById(R.id.login_name_edit);
        if(SignMode==2){
            if(et_confirm.getText().toString().equals("") || et_new.getText().toString().equals("")){
                Toast.makeText(this,"Password Incomplete",Toast.LENGTH_SHORT).show();
            }
            //保存用户名和密码
            else if(et_confirm.getText().toString().equals(et_new.getText().toString())){
                SharedPreferences.Editor editor=sharedPreferences.edit();
                editor.putBoolean("isFirstLaunch",false);
                editor.putString("Password",et_confirm.getText().toString());
                editor.putString("Username",et_name.getText().toString());
                editor.apply();

                Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                        + getResources().getResourcePackageName(R.drawable.qq_zone) + "/"
                        + getResources().getResourceTypeName(R.drawable.qq_zone) + "/"
                        + getResources().getResourceEntryName(R.drawable.qq_zone));
                sql.new_user(et_name.getText().toString(),et_confirm.getText().toString(),uri.toString());

                Intent intent=new Intent(this,label.class);
                Cursor c=sql.select_user_by_name(et_name.getText().toString());
                c.moveToNext();
                intent.putExtra("user",c.getString(0));

                startActivity(intent);
                DataShare ds=((DataShare)getApplicationContext());
                ds.setUsername(sharedPreferences.getString("Username","游客"));
                ds.setUserid(c.getString(1));
                Toast.makeText(this,sharedPreferences.getString("Username","游客"),Toast.LENGTH_SHORT).show();
                this.finish();
            }
            else{
                Toast.makeText(this,"Inconsistent Password!",Toast.LENGTH_SHORT).show();
            }
        }
        else if(SignMode==1){
            Cursor c=sql.select_user_by_name(et_name.getText().toString());
            if(c.moveToNext()){
                if(c.getString(2).equals(et_new.getText().toString())){
                    SharedPreferences.Editor editor=sharedPreferences.edit();
                    editor.putBoolean("isFirstLaunch",false);
                    editor.putString("Password",et_new.getText().toString());
                    editor.putString("Username",et_name.getText().toString());
                    editor.apply();
                    goToHome();
                }
                else{
                    Toast.makeText(this,"密码错误",Toast.LENGTH_SHORT).show();
                }
            }
            else{
                Toast.makeText(this,"用户名不存在",Toast.LENGTH_SHORT).show();
            }
        }

    }
    public void RegisterByWeiXin(View view){
        Toast.makeText(this,"--还没申请到登录权限(ㄒoㄒ)--",Toast.LENGTH_SHORT).show();

/*        //获取微信实例
        api= WXAPIFactory.createWXAPI(this,APP_ID,true);
        api.registerApp(APP_ID);//注册到微信

        String text="登录";
        WXTextObject textObject=new WXTextObject();
        textObject.text=text;

        WXMediaMessage msg=new WXMediaMessage();
        msg.mediaObject=textObject;
        msg.description=text;

        //构造Req,微信处理完返回应用
        SendMessageToWX.Req req=new SendMessageToWX.Req();
        //transaction用于唯一标识一个字段
        req.transaction=String.valueOf(System.currentTimeMillis());
        req.message=msg;

        //调用api发送消息到微信
        api.sendReq(req);

        final SendAuth.Req SAreq=new SendAuth.Req();
        SAreq.scope="snsapi_userinfo";
        SAreq.state="wechat_state_login";*/

    }


}