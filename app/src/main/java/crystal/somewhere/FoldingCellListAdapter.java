package crystal.somewhere;

/**
 * Created by Celine on 2018/1/7.
 */

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ramotion.foldingcell.FoldingCell;
import com.zhy.view.flowlayout.FlowLayout;
import com.zhy.view.flowlayout.TagAdapter;
import com.zhy.view.flowlayout.TagFlowLayout;

import java.util.HashSet;
import java.util.List;

import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.DownloadFileListener;
import crystal.somewhere.bean.Place;

/**
 * Simple example of ListAdapter for using with Folding Cell
 * Adapter holds indexes of unfolded elements for correct work with default reusable views behavior
 */
public class FoldingCellListAdapter extends ArrayAdapter<Place> {

    private HashSet<Integer> unfoldedIndexes = new HashSet<>();
    private View.OnClickListener defaultRequestBtnClickListener;


    public FoldingCellListAdapter(Context context, List<Place> objects) {
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // get item for selected view
        Place item = getItem(position);
        // if cell is exists - reuse it, if not - create the new one from resource
        FoldingCell cell = (FoldingCell) convertView;
        final ViewHolder viewHolder;
        if (cell == null) {
            viewHolder = new ViewHolder();
            LayoutInflater vi = LayoutInflater.from(getContext());
            cell = (FoldingCell) vi.inflate(R.layout.cell, parent, false);
            // binding view parts to view holder
            viewHolder.title_placeName = (TextView) cell.findViewById(R.id.title_placeName);
            viewHolder.title_typeName = (TextView) cell.findViewById(R.id.title_typeName);
            viewHolder.title_flowlayout = (TagFlowLayout) cell.findViewById(R.id.title_flowlayout);
            viewHolder.title_coverPage = (ImageView) cell.findViewById(R.id.title_coverPage);

            viewHolder.content_placeName = (TextView) cell.findViewById(R.id.content_placeName);
            viewHolder.content_typeName = (TextView) cell.findViewById(R.id.content_typeName);
            viewHolder.content_flowlayout = (TagFlowLayout) cell.findViewById(R.id.content_flowlayout);
            viewHolder.content_description = (TextView) cell.findViewById(R.id.content_description);
            viewHolder.content_coverPage = (ImageView) cell.findViewById(R.id.content_coverPage);
            viewHolder.content_typeImg = (ImageView) cell.findViewById(R.id.content_typeImg);

            cell.setTag(viewHolder);
        } else {
            // for existing cell set valid valid state(without animation)
            if (unfoldedIndexes.contains(position)) {
                cell.unfold(true);
                Toast.makeText(getContext(),"长按可查看详情~", Toast.LENGTH_SHORT).show();
            } else {
                cell.fold(true);
            }
            viewHolder = (ViewHolder) cell.getTag();
        }

        // bind data from selected element to view through view holder
        viewHolder.title_placeName.setText(item.getName());
        viewHolder.content_placeName.setText(item.getName());
        viewHolder.content_typeName.setText(item.getType());
        viewHolder.content_description.setText(item.getDescription());
        viewHolder.title_typeName.setText(item.getType());
        Log.e("Debug",item.getType());
        // typeImg
        switch (item.getType()) {
            case "other":
                viewHolder.content_typeImg.setImageResource(R.drawable.other);
                break;
            case "food":
                viewHolder.content_typeImg.setImageResource(R.drawable.food);
                break;
            case "trip":
                viewHolder.content_typeImg.setImageResource(R.drawable.trip);
                break;
            case "mall":
                viewHolder.content_typeImg.setImageResource(R.drawable.mall);
                break;
        }
        // coverPage
        BmobFile icon= item.getCoverPage();
        if (icon != null) {
            icon.download(getContext(),new DownloadFileListener() {
                @Override
                public void onSuccess(String url) {
                    viewHolder.content_coverPage.setImageBitmap(BitmapFactory.decodeFile(url));   //根据地址解码并显示图片
                    viewHolder.title_coverPage.setImageBitmap(BitmapFactory.decodeFile(url));   //根据地址解码并显示图片
                }
                @Override
                public void onFailure(int arg0, String arg1) {
                    Toast.makeText(getContext(),"下载失败"+arg1,Toast.LENGTH_SHORT).show();
                }
            });
        }
        // tag
        if (item.getTags() == null) return cell;

        String[] mVals = item.getTags().toArray(new String[item.getTags().size()]);
        //mFlowLayout.setMaxSelectCount(3);
        final LayoutInflater mInflater = LayoutInflater.from(getContext());
        viewHolder.title_flowlayout.setAdapter(new TagAdapter<String>(mVals) {
            @Override
            public View getView(FlowLayout parent, int position, String s) {
                TextView tv = (TextView) mInflater.inflate(R.layout.tv1,
                        viewHolder.title_flowlayout, false);
                tv.setText(s);
                return tv;
            }
        });
        viewHolder.content_flowlayout.setAdapter(new TagAdapter<String>(mVals) {
            @Override
            public View getView(FlowLayout parent, int position, String s) {
                TextView tv = (TextView) mInflater.inflate(R.layout.tv1,
                        viewHolder.content_flowlayout, false);
                tv.setText(s);
                return tv;
            }
        });

        return cell;
    }

    // simple methods for register cell state changes
    public void registerToggle(int position) {
        if (unfoldedIndexes.contains(position))
            registerFold(position);
        else
            registerUnfold(position);
    }

    public void registerFold(int position) {
        unfoldedIndexes.remove(position);
    }

    public void registerUnfold(int position) {
        unfoldedIndexes.add(position);
    }

    public View.OnClickListener getDefaultRequestBtnClickListener() {
        return defaultRequestBtnClickListener;
    }

    public void setDefaultRequestBtnClickListener(View.OnClickListener defaultRequestBtnClickListener) {
        this.defaultRequestBtnClickListener = defaultRequestBtnClickListener;
    }

    // View lookup cache
    private static class ViewHolder {
        TextView title_placeName;
        TextView title_typeName;
        ImageView title_coverPage;
        TagFlowLayout title_flowlayout;

        TextView content_placeName;
        TextView content_typeName;
        ImageView content_typeImg;
        TextView content_description;
        ImageView content_coverPage;
        TagFlowLayout content_flowlayout;
    }
}