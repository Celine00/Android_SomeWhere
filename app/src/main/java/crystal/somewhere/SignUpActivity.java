package crystal.somewhere;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.listener.SaveListener;
import crystal.somewhere.bean.User;

/**
 * Created by Celine on 2018/1/4.
 */

public class SignUpActivity extends AppCompatActivity {
    EditText etUsername,etPassword,etRepeatPassword;
    Button btnSignUp;
    ImageView ivEye1, ivEye2;
    private int eyeTag1 = 0, eyeTag2 = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("注册");
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        // 初始化 Bmob SDK
        Bmob.initialize(this, "9020a320ba34abf2203135dad5de9727");

        // 获取界面中的相关View
        etUsername = (EditText) findViewById(R.id.userNewName);
        etPassword = (EditText) findViewById(R.id.userNewPassword);
        etRepeatPassword= (EditText) findViewById(R.id.userRepeatPassword);
        btnSignUp = (Button) findViewById(R.id.signUpButton);
        ivEye1 = (ImageView) findViewById(R.id.eyeToggle1);
        ivEye2 = (ImageView) findViewById(R.id.eyeToggle2);
        // 设置注册按钮点击事件
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 获取用户输入的用户名和密码
                final String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                // 非空验证
                if(TextUtils.isEmpty(username) || TextUtils.isEmpty(password)){
                    Toast.makeText(SignUpActivity.this, "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 两次输入的密码不相同
                if (!etPassword.getText().toString().equals(etRepeatPassword.getText().toString())) {
                    Toast.makeText(SignUpActivity.this, "两次输入密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 使用BmobSDK提供的注册功能
                User user = new User();
                user.setUsername(username);
                user.setPassword(password);
                user.signUp(SignUpActivity.this, new SaveListener() {
                    @Override
                    public void onSuccess() {
                        Log.e("debug","成功");
                        Toast.makeText(SignUpActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        Log.e("debug","失败");
                        Toast.makeText(SignUpActivity.this, s, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        // 设置密码可见性
        ivEye1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (eyeTag1 == 0) {
                    // 设置密码为可见
                    ivEye1.setImageResource(R.drawable.hide);
                    etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    eyeTag1 = 1;
                } else if (eyeTag1 == 1) {
                    // 设置密码为不可见
                    ivEye1.setImageResource(R.drawable.view);
                    etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    eyeTag1 = 0;
                }
            }
        });
        // 设置密码可见性
        ivEye2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (eyeTag2 == 0) {
                    // 设置密码为可见
                    ivEye2.setImageResource(R.drawable.hide);
                    etRepeatPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    eyeTag2 = 1;
                } else if (eyeTag2 == 1) {
                    // 设置密码为不可见
                    ivEye2.setImageResource(R.drawable.view);
                    etRepeatPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    eyeTag2 = 0;
                }
            }
        });
    }
}
