package crystal.somewhere;
//reference: http://blog.csdn.net/gaoshouxiaodi/article/details/50519344
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadBatchListener;
import cn.bmob.v3.listener.UploadFileListener;
import co.lujun.androidtagview.TagContainerLayout;
import co.lujun.androidtagview.TagView;
import crystal.somewhere.bean.Place;
import crystal.somewhere.utils.StringUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import com.sendtion.xrichtext.RichTextEditor;
import com.sendtion.xrichtext.RichTextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class EditActivity extends AppCompatActivity {
    private EditText et_new_title, et_new_description, et_tag_input;
    private RichTextEditor et_new_content;
    private TagContainerLayout mTagContainerLayout;
    private LinearLayout tags_edit;
    private Button bt_add_tag;
    private ImageView coverPage;

    private Place mp;
    private DataBaseHelper mdbh;
    private String returnedId;

    private Subscription subsLoading, subsInsert;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        init();
    }

    private void init() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_new);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setNavigationIcon(R.drawable.left_arrow);

        et_new_title = (EditText) findViewById(R.id.et_new_title);
        et_new_description = (EditText) findViewById(R.id.et_new_description);
        et_new_content = (RichTextEditor) findViewById(R.id.et_edit_content);
        mTagContainerLayout = (TagContainerLayout) findViewById(R.id.tagcontainerLayout);
        mTagContainerLayout.setTagBackgroundColor(Color.TRANSPARENT);
        tags_edit = (LinearLayout) findViewById(R.id.tag_edit_layout);
        bt_add_tag = (Button) findViewById(R.id.add_button);
        et_tag_input = (EditText) findViewById(R.id.tag_input);
        coverPage = (ImageView) findViewById(R.id.cover_page);

        et_new_title.setInputType(InputType.TYPE_CLASS_TEXT);
        et_new_description.setInputType(InputType.TYPE_CLASS_TEXT);
        et_new_content.setVisibility(View.VISIBLE);
        RichTextView temp = (RichTextView) findViewById(R.id.tv_edit_content);
        temp.setVisibility(View.GONE);
        tags_edit.setVisibility(View.VISIBLE);

        Bundle bundle = getIntent().getExtras();
        mp = (Place) bundle.getSerializable("mp");
        mTagContainerLayout.setTags(mp.getTags());
        et_new_title.setText(mp.getName());
        et_new_description.setText(mp.getDescription());
        if(mp.getContent() != null) {
            et_new_content.post(new Runnable() {
                @Override
                public void run() {
                    showDataSync(mp.getContent());
                }
            });
        }

        mdbh = new DataBaseHelper(this);

        bt_add_tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = et_tag_input.getText().toString();
                if(!s.equals("")) mTagContainerLayout.addTag(s);
                et_tag_input.setText("");
            }
        });

        if(mp.getCoverPageLocalPath().equals("")) coverPage.setImageResource(R.drawable.sea);
        else {
            Bitmap bitmap = getBitmap(mp.getCoverPageLocalPath());
            coverPage.setImageBitmap(bitmap);
        }
        coverPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //调用系统图库
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");// 相片类型
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 555);
            }
        });

        //mTagContainerLayout.setIsTagViewClickable(true);
        mTagContainerLayout.setOnTagClickListener(new TagView.OnTagClickListener() {
            @Override
            public void onTagClick(int position, String text) {
                // ...
            }
            @Override
            public void onTagLongClick(final int position, String text) {
                mTagContainerLayout.removeTag(position);
                showToast("Tag \""+text+"\" removed!");
            }
            @Override
            public void onTagCrossClick(int position) {
                // ...
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_insert_image:
                //调用系统图库
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");// 相片类型
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 666);
                break;
            case R.id.action_new_save:
                updateLocalDBAndServer();
                Intent intent1 = new Intent();
                Bundle bundle = new Bundle();
                bundle.putSerializable("mp", mp);
                intent1.putExtras(bundle);
                setResult(RESULT_OK, intent1);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateLocalDBAndServer() {
        final String title = et_new_title.getText().toString();
        String description = et_new_description.getText().toString();
        String content = getEditData();
        List<String> taglist = mTagContainerLayout.getTags();
        String tags = StringUtils.listToString(taglist);

        mdbh.update(mp.getLatitude(), mp.getLongitude(), tags, title, description, content, mp.getCoverPageLocalPath());

        //prepare for jump to detail activity
        mp.setName(title);
        mp.setDescription(description);
        mp.setTags(taglist);
        mp.setContent(content);
        //coverPagePath are already set when insert picture completely

        BmobQuery<Place> query = new BmobQuery<Place>();
        query.addWhereEqualTo("latitude", mp.getLatitude());
        query.addWhereEqualTo("longitude", mp.getLongitude());
        query.findObjects(this, new FindListener<Place>() {
            @Override
            public void onSuccess(List<Place> object) {
                returnedId = object.get(0).getObjectId();
            }
            @Override
            public void onError(int i, String s) {
                Log.i("bmob","查询失败："+i+","+s);
            }
        });
        mp.update(this, returnedId, new UpdateListener() {
            @Override
            public void onSuccess() {
                showToast("更新成功");
            }
            @Override
            public void onFailure(int i, String s) {
                Log.i("bmob","更新失败："+i+","+s);
            }
        });

        //文件不能随类一起上传，否则云端缺少URL与类成员关联起来
        uploadCoverPage();
        uploadPictures();
    }

    private String getEditData() {
        List<RichTextEditor.EditData> editList = et_new_content.buildEditData();
        StringBuffer content = new StringBuffer();
        for (RichTextEditor.EditData itemData : editList) {
            if (itemData.inputStr != null) {
                content.append(itemData.inputStr);
            } else if (itemData.imagePath != null) {
                content.append("<img src=\"").append(itemData.imagePath).append("\"/>");
            }
        }
        return content.toString();
    }


    private void showDataSync(final String html){
        subsLoading = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                showEditData(subscriber, html);
            }
        })
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())//生产事件在io
                .observeOn(AndroidSchedulers.mainThread())//消费事件在UI线程
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        showToast("解析错误：图片不存在或已损坏");
                    }

                    @Override
                    public void onNext(String text) {
                        if (text.contains("/storage/")){
                            et_new_content.addImageViewAtIndex(et_new_content.getLastIndex(), text);
                        } else {
                            et_new_content.addEditTextAtIndex(et_new_content.getLastIndex(), text);
                        }
                    }
                });
    }

    protected void showEditData(Subscriber<? super String> subscriber, String html) {
        try{
            List<String> textList = StringUtils.cutStringByImgTag(html);
            for (int i = 0; i < textList.size(); i++) {
                String text = textList.get(i);
                if (text.contains("<img")) {
                    String imagePath = StringUtils.getImgSrc(text);
                    if (new File(imagePath).exists()) {
                        subscriber.onNext(imagePath);
                    } else {
                        showToast("图片"+i+"已丢失，请重新插入！");
                    }
                } else {
                    subscriber.onNext(text);
                }

            }
            subscriber.onCompleted();
        }catch (Exception e){
            e.printStackTrace();
            subscriber.onError(e);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data != null) {
                if (requestCode == 666) {
                    //异步方式插入图片
                    insertImagesSync(data, true);
                }
                if(requestCode == 555) {
                    insertImagesSync(data, false);
                }
            }
        }
    }

    //异步插图
    private void insertImagesSync(final Intent data, final boolean forContent){
        subsInsert = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try{
                    et_new_content.measure(0, 0);
                    Uri selectedImage = data.getData();
                    final String picturePath= handleImageOnKitKat(selectedImage);
                    subscriber.onNext(picturePath);
                    subscriber.onCompleted();
                }catch (Exception e){
                    e.printStackTrace();
                    subscriber.onError(e);
                }
            }
        })
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())//生产事件在io
                .observeOn(AndroidSchedulers.mainThread())//消费事件在UI线程
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        et_new_content.addEditTextAtIndex(et_new_content.getLastIndex(), " ");
                        showToast("图片插入成功");
                    }

                    @Override
                    public void onError(Throwable e) {
                        showToast("图片插入失败:"+e.getMessage());
                    }

                    @Override
                    public void onNext(String imagePath) {
                        if(forContent) et_new_content.insertImage(imagePath, et_new_content.getMeasuredWidth());
                        else {
                            Bitmap bitmap = getBitmap(imagePath);
                            coverPage.setImageBitmap(bitmap);
                            mp.setCoverPageLocalPath(imagePath);
                        }
                    }
                });
    }

    //获取绝对路径，kitkat（API19）及以前需要特殊处理
    private String handleImageOnKitKat(Uri uri) {
        String imagePath = null;
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        }
        return imagePath;
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = this.getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }

            cursor.close();
        }
        return path;
    }

    private void uploadCoverPage() {
        final BmobFile bmobFile = new BmobFile(new File(mp.getCoverPageLocalPath()));
        bmobFile.uploadblock(EditActivity.this, new UploadFileListener() {
            @Override
            public void onSuccess() {
                bmobFile.getFileUrl(EditActivity.this);
                mp.setCoverPage(bmobFile);
                mp.update(EditActivity.this, returnedId, new UpdateListener() {
                    @Override
                    public void onSuccess() {
                    }
                    @Override
                    public void onFailure(int code, String msg) {
                    }
                });
                //Toast.makeText(getApplicationContext(), "上传成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i, String s) {
                //Toast.makeText(getApplicationContext(), "上传失败"+s, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadPictures() {
        List<String> temp = new ArrayList<String>();
        List<RichTextEditor.EditData> editList = et_new_content.buildEditData();
        for (RichTextEditor.EditData itemData : editList) {
            if (itemData.imagePath != null) {
                temp.add(itemData.imagePath);
            }
        }
        //final String[] filePaths = (String[]) temp.toArray();不能这么干，真是的，童话里都是骗人的
        final String[] filePaths = new String[temp.size()];
        for(int i = 0; i < temp.size(); ++i) {
            filePaths[i] = temp.get(i);
        }
        BmobFile.uploadBatch(EditActivity.this, filePaths, new UploadBatchListener() {
            @Override
            public void onSuccess(List<BmobFile> files, List<String> urls) {
                //1、files-上传完成后的BmobFile集合，是为了方便大家对其上传后的数据进行操作，例如你可以将该文件保存到表中
                //2、urls-上传文件的完整url地址
                if(urls.size()==filePaths.length){//如果数量相等，则代表文件全部上传完成
                    for(BmobFile file : files) {
                        file.getFileUrl(EditActivity.this);
                    }
                    mp.setPictures(files);
                    mp.update(EditActivity.this, returnedId, new UpdateListener() {
                        @Override
                        public void onSuccess() {
                        }
                        @Override
                        public void onFailure(int code, String msg) {
                        }
                    });
                }
                showToast("上传图片成功");
            }

            @Override
            public void onError(int statuscode, String errormsg) {
                showToast("错误码"+statuscode +",错误描述："+errormsg);
            }

            @Override
            public void onProgress(int curIndex, int curPercent, int total,int totalPercent) {
                //1、curIndex--表示当前第几个文件正在上传
                //2、curPercent--表示当前上传文件的进度值（百分比）
                //3、total--表示总的上传文件数
                //4、totalPercent--表示总的上传进度（百分比）
            }
        });
    }

    //路径转Bitmap
    private Bitmap getBitmap(String path) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        BitmapFactory.decodeFile(path, opts);
        int width = opts.outWidth;
        int height = opts.outHeight;
        float scaleWidth = 0.f, scaleHeight = 0.f;
        int windowWidth = getWindowManager().getDefaultDisplay().getWidth();
        if (width > windowWidth || height > 250) {
            scaleWidth = ((float) width) / windowWidth;
            scaleHeight = ((float) height) / 350;
        }
        opts.inJustDecodeBounds = false;
        float scale = Math.max(scaleWidth, scaleHeight);
        opts.inSampleSize = (int) scale;
        WeakReference<Bitmap> weak = new WeakReference<Bitmap>(
                BitmapFactory.decodeFile(path, opts));
        return Bitmap.createScaledBitmap(weak.get(), windowWidth, 250, true);
    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}