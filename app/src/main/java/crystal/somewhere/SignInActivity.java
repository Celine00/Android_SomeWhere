package crystal.somewhere;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.SaveListener;
import crystal.somewhere.bean.User;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Celine on 2018/1/4.
 */

public class SignInActivity  extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnSignIn;
    private TextView tvSignUp;
    private ImageView ivEye;
    private CircleImageView profile;
    private int eyeTag = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);
        // 初始化 Bmob SDK
        Bmob.initialize(this, "9020a320ba34abf2203135dad5de9727");

        BmobUser bmobUser = BmobUser.getCurrentUser(SignInActivity.this);
        if (bmobUser != null) {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            //缓存用户对象为空时， 可打开用户登录界面…
            // 获取界面中的相关View
            etUsername = (EditText) findViewById(R.id.userName);
            etPassword = (EditText) findViewById(R.id.userPassword);
            tvSignUp = (TextView) findViewById(R.id.signUpView);
            ivEye = (ImageView) findViewById(R.id.eyeToggle);
            btnSignIn = (Button) findViewById(R.id.signInButton);
            profile = (CircleImageView) findViewById(R.id.userPortrait);
            // 设置登录按钮点击事件
            btnSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 获取用户输入的用户名和密码
                    final String username = etUsername.getText().toString();
                    String password = etPassword.getText().toString();

                    // 非空验证
                    if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                        Toast.makeText(SignInActivity.this, "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 使用BmobSDK提供的登录功能
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(password);
                    user.login(SignInActivity.this, new SaveListener() {
                        @Override
                        public void onSuccess() {
                            Log.e("debug", "成功");
                            Toast.makeText(SignInActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                            //下载头像
                            User user1 = BmobUser.getCurrentUser(SignInActivity.this, User.class);
                            BmobFile avatarFile = user1.getProfile();
                            if (avatarFile != null) {
                                String avatarUrl = avatarFile.getFileUrl(SignInActivity.this);
                                if (!TextUtils.equals(avatarUrl, "")) {
                                    BmobFile bmobfile =new BmobFile(user1.getPicName()+".png","",avatarUrl);
                                    bmobfile.download(SignInActivity.this,new DownloadFileListener() {
                                        @Override
                                        public void onSuccess(String url) {
                                            profile.setImageBitmap(BitmapFactory.decodeFile(url));   //根据地址解码并显示图片

                                        }
                                        @Override
                                        public void onFailure(int arg0, String arg1) {
                                            Toast.makeText(SignInActivity.this, "下载失败"+arg1, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                            }


                            final Intent intent = new Intent(SignInActivity.this, MainActivity.class); //你要转向的Activity
                            intent.putExtra("name", username);
                            Timer timer = new Timer();
                            TimerTask task = new TimerTask() {
                                @Override
                                public void run() {
                                    startActivity(intent); //执行
                                    finish();
                                }
                            };
                            timer.schedule(task, 1000 * 2);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                            Log.e("debug", "失败");
                            Toast.makeText(SignInActivity.this, s, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            // 跳转到注册界面
            tvSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setClass(SignInActivity.this, SignUpActivity.class);
                    startActivity(intent);
                }
            });
            // 设置密码可见性
            ivEye.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (eyeTag == 0) {
                        ivEye.setImageResource(R.drawable.hide);
                        // 密码设置为可见
                        etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        eyeTag = 1;
                    } else if (eyeTag == 1) {
                        ivEye.setImageResource(R.drawable.view);
                        // 设置密码不可见
                        etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        eyeTag = 0;
                    }
                }
            });
        }
    }
}
