package crystal.somewhere;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadFileListener;
import crystal.somewhere.bean.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserEditActivity extends AppCompatActivity {
    private static final int CAMERA_CODE = 1;
    private static final int GALLERY_CODE = 2;
    private static final int CROP_CODE = 3;
    private static String picPath;
    private CircleImageView profile;
    private EditText edit_name, edit_phone, edit_email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);
        // 初始化 Bmob SDK
        Bmob.initialize(this, "9020a320ba34abf2203135dad5de9727");

        edit_name = (EditText) findViewById(R.id.edit_name);
        edit_phone = (EditText) findViewById(R.id.edit_phone);
        edit_email = (EditText) findViewById(R.id.edit_email);
        profile = (CircleImageView) findViewById(R.id.profile);
        Button save = (Button) findViewById(R.id.save);
        Button exit = (Button) findViewById(R.id.exit);

        final String username = (String) User.getObjectByKey(UserEditActivity.this, "username");
        String mobilePhone = (String) User.getObjectByKey(UserEditActivity.this, "mobilePhoneNumber");
        String email = (String) User.getObjectByKey(UserEditActivity.this, "email");
        edit_name.setText(username);
        edit_name.setSelection(username.length());
        edit_phone.setText(mobilePhone);
        edit_email.setText(email);

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(UserEditActivity.this);
                builder.setIcon(R.drawable.alert);
                builder.setTitle("选择头像");
                String[] Items = {"拍照", "从相册中选择"};
                builder.setItems(Items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            chooseFromCamera();
                        }
                        else if (i == 1) {
                            chooseFromGallery();
                        }
                    }
                });
                builder.setCancelable(true);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        //下载头像
        User user1 = BmobUser.getCurrentUser(this, User.class);
        BmobFile avatarFile = user1.getProfile();
        if (avatarFile != null) {
            String avatarUrl = avatarFile.getFileUrl(UserEditActivity.this);
            if (!TextUtils.equals(avatarUrl, "")) {
                BmobFile bmobfile = new BmobFile(user1.getPicName() + ".png", "", avatarUrl);
                bmobfile.download(UserEditActivity.this, new DownloadFileListener() {
                    @Override
                    public void onSuccess(String url) {
                        profile.setImageBitmap(BitmapFactory.decodeFile(url));   //根据地址解码并显示图片

                    }

                    @Override
                    public void onFailure(int arg0, String arg1) {
                        Toast.makeText(UserEditActivity.this, "下载失败" + arg1, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                User newUser = new User();
                newUser.setUsername(edit_name.getText().toString());
                newUser.setMobilePhoneNumber(edit_phone.getText().toString());
                newUser.setEmail(edit_email.getText().toString());
                User user = BmobUser.getCurrentUser(UserEditActivity.this, User.class);
                newUser.update(UserEditActivity.this, user.getObjectId(), new UpdateListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "更新成功", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserEditActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                    @Override
                    public void onFailure(int code, String msg) {
                        Toast.makeText(getApplicationContext(), "更新失败"+msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BmobUser.logOut(UserEditActivity.this);   //清除缓存用户对象
                finish();
            }
        });
    }

    private void chooseFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_CODE);
    }

    private void chooseFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //设置选择类型为图片类型
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_CODE) {
            if (data == null) {}
            else {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    //获得拍的照片
                    Bitmap bitmap = extras.getParcelable("data");
                    //将Bitmap转化为uri
                    Uri uri = saveBitmap(bitmap, "temp");
                    //启动图像裁剪
                    startImageZoom(uri);
                }
            }
        }
        else if (requestCode == GALLERY_CODE) {
            if (data == null) {}
            else {
                //用户从图库选择图片后会返回所选图片的Uri
                Uri uri;
                //获取到用户所选图片的Uri
                uri = data.getData();
                //返回的Uri为content类型的Uri,不能进行复制等操作,需要转换为文件Uri
                uri = convertUri(uri);
                startImageZoom(uri);
            }
        }
        else if (requestCode == CROP_CODE) {
            if (data == null){
                return;
            }else{
                Bundle extras = data.getExtras();
                if (extras != null){
                    //获取到裁剪后的图像
                    Bitmap bm = extras.getParcelable("data");
                    profile.setImageBitmap(bm);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 将content类型的Uri转化为文件类型的Uri
     * @param uri
     * @return
     */
    private Uri convertUri(Uri uri){
        InputStream is;
        try {
            //Uri ----> InputStream
            is = getContentResolver().openInputStream(uri);
            //InputStream ----> Bitmap
            Bitmap bm = BitmapFactory.decodeStream(is);
            is.close();
            return saveBitmap(bm, "temp");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将Bitmap写入SD卡中的一个文件中,并返回写入文件的Uri
     * @param bm
     * @param dirPath
     * @return
     */
    private Uri saveBitmap(Bitmap bm, String dirPath) {
        //新建文件夹用于存放裁剪后的图片
        File tmpDir = new File(Environment.getExternalStorageDirectory() + "/" + dirPath);
        if (!tmpDir.exists()){
            tmpDir.mkdir();
        }

        //新建文件存储裁剪后的图片
        final long time = System.currentTimeMillis();
        picPath = tmpDir.getAbsolutePath() + "/" + Long.toString(time) +".png";
        Toast.makeText(getApplicationContext(), picPath, Toast.LENGTH_SHORT).show();
        File img = new File(picPath);
        try {
            //打开文件输出流
            FileOutputStream fos = new FileOutputStream(img);
            //将bitmap压缩后写入输出流(参数依次为图片格式、图片质量和输出流)
            bm.compress(Bitmap.CompressFormat.PNG, 85, fos);
            //刷新输出流
            fos.flush();
            //关闭输出流
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final BmobFile bmobFile = new BmobFile(new File(picPath));
        bmobFile.uploadblock(UserEditActivity.this, new UploadFileListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "上传成功", Toast.LENGTH_SHORT).show();
                bmobFile.getFileUrl(UserEditActivity.this);
                User newUser = new User();
                newUser.setProfile(bmobFile);
                newUser.setPicName(Long.toString(time));
                User user = BmobUser.getCurrentUser(UserEditActivity.this, User.class);
                newUser.update(UserEditActivity.this, user.getObjectId(), new UpdateListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "更新成功", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(int code, String msg) {
                        Toast.makeText(getApplicationContext(), "更新失败"+msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(int i, String s) {
                Toast.makeText(getApplicationContext(), "上传失败"+s, Toast.LENGTH_LONG).show();
            }
        });
        //返回File类型的Uri
        return Uri.fromFile(img);
    }

    /**
     * 通过Uri传递图像信息以供裁剪
     * @param uri
     */
    private void startImageZoom(Uri uri){
        //构建隐式Intent来启动裁剪程序
        Intent intent = new Intent("com.android.camera.action.CROP");
        //设置数据uri和类型为图片类型
        intent.setDataAndType(uri, "image/*");
        //显示View为可裁剪的
        intent.putExtra("crop", true);
        //裁剪的宽高的比例为1:1
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        //输出图片的宽高均为150
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        //裁剪之后的数据是通过Intent返回
        intent.putExtra("return-data", true);
        startActivityForResult(intent, CROP_CODE);
    }

}
