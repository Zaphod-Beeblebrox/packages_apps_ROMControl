package com.aokp.romcontrol.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.aokp.romcontrol.R;

public class WidgetPagerPreference extends Preference {
    
    private static final String TAG = "Widget";
    public static final String ACTION_ALLOCATE_ID = "com.android.systemui.ACTION_ALLOCATE_ID";
    public static final String ACTION_DEALLOCATE_ID = "com.android.systemui.ACTION_DEALLOCATE_ID";
    public static final String ACTION_SEND_ID = "com.android.systemui.ACTION_SEND_ID";
    public static final String ACTION_GET_WIDGET_DATA = "com.android.systemui.ACTION_GET_WIDGET_DATA";
    public static final String ACTION_SEND_WIDGET_DATA = "com.android.systemui.ACTION_SEND_WIDGET_DATA";
    public int mWidgetIdQty = 0;
    int mWidgetIds[];
    private int mCurrentPage = 0;
    private int mPendingWidgetId = -1;
    private int[] mWidgetHeight;
    private int[] mWidgetWidth;
    private String[] mTitles;
    private ViewPager mViewPager;
    WidgetPagerAdapter mAdapter;
    Context mContext;
    ImageView mWidgetView;
    int[] mWidgetResId;
    String[] mProvider;
    TextView mTitle;
    TextView mSummary;
    AppWidgetManager mAppWidgetManager;
    
    BroadcastReceiver mWidgetIdReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            Log.i(TAG, "widget id receiver go!");

            // Need to De-Allocate the ID that this was replacing.
            if (mWidgetIds[mPendingWidgetId] != -1) {
                Intent delete = new Intent();
                delete.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,mWidgetIds[mPendingWidgetId]);
                delete.setAction(ACTION_DEALLOCATE_ID);
                mContext.sendBroadcast(delete);
            }
            mWidgetIds[mPendingWidgetId] = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (mPendingWidgetId == mWidgetIdQty) { // we put a widget in the last spot
                mWidgetIdQty++;
            }
            saveWidgets();
            inflateWidgetPref();
            mViewPager.setCurrentItem(mPendingWidgetId);
            mAdapter.notifyDataSetChanged();
        };
    };

    public WidgetPagerPreference(Context context) {
        super(context);
        mContext = context;
    }
    public WidgetPagerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.widget_preview_preference);
        mContext = context;
        IntentFilter filter = new IntentFilter(ACTION_SEND_ID);
        mContext.registerReceiver(mWidgetIdReceiver, filter);
        filter = new IntentFilter(ACTION_SEND_WIDGET_DATA);
        mAppWidgetManager = AppWidgetManager.getInstance(mContext);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        // Set our custom views inside the layout
         mViewPager = (ViewPager) view.findViewById(R.id.pager);
         mTitle = (TextView) view.findViewById(R.id.title);
         mSummary = (TextView) view.findViewById(R.id.summary);
         inflateWidgetPref();
    }
    
    public void inflateWidgetPref() {
        // calculate number of Widgets
        String settingWidgets = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_WIDGETS);
        if (settingWidgets != null && settingWidgets.length() > 0) {
            String[] split = settingWidgets.split("\\|");
            mWidgetIdQty = split.length;
        } else {
            mWidgetIdQty = 0;
        }
        mWidgetIds = new int[mWidgetIdQty+1];
        mWidgetResId = new int[mWidgetIdQty+1];
        mWidgetHeight = new int [mWidgetIdQty+1];
        mWidgetWidth = new int [mWidgetIdQty+1];
        mProvider = new String[mWidgetIdQty+1];
        mTitles = new String[mWidgetIdQty+1];
        Log.i(TAG, "inflatewidgets: " + settingWidgets);
        if (settingWidgets != null && settingWidgets.length() > 0) {
            String[] split = settingWidgets.split("\\|");
            for (int i = 0; i < split.length; i++) {
                if (split[i].length() > 0) {
                    mWidgetIds[i] = Integer.parseInt(split[i]);
                    //requestWidgetInfo(mWidgetIds[i]);
                    requestWidgetInfo(i);
                }
            }
        }
        // set Widget ID to -1 for 'add button'
        mWidgetIds[mWidgetIdQty] = -1;
        if (mViewPager != null) {
            if (mAdapter == null) {
                mViewPager.setAdapter(mAdapter = new WidgetPagerAdapter());
                mViewPager.setOnPageChangeListener(mNewPageListener);
            }
            int dp = mAdapter.getHeight(mViewPager.getCurrentItem());
            float px = dp * mContext.getResources().getDisplayMetrics().density;
            mViewPager.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) px));
        }
     }
     
     View.OnClickListener mDoPrefClick = new View.OnClickListener() {
         public void onClick(View v) {
             doWidgetPrefClick();
         };
     };
     
     private void doWidgetPrefClick () {
         mPendingWidgetId = mCurrentPage;
         // selectWidget();
         // send intent to pick a new widget
         Intent send = new Intent();
         send.setAction(ACTION_ALLOCATE_ID);
         mContext.sendBroadcast(send);  
     }
     
     private void saveWidgets() {
         StringBuilder widgetString = new StringBuilder();
         for (int i = 0; i < (mWidgetIdQty); i++) {
             widgetString.append(mWidgetIds[i]);
             if (i != (mWidgetIdQty - 1))
                 widgetString.append("|");
         }
         Settings.System.putString(mContext.getContentResolver(), Settings.System.NAVIGATION_BAR_WIDGETS,
                 widgetString.toString());
         Log.d(TAG,"Saved:" + widgetString.toString());
         inflateWidgetPref();
     }
     
     public void resetNavBarWidgets() {
         for (int i = 0; i < (mWidgetIdQty); i++) {
             if (mWidgetIds[i] != -1) {
                 Intent delete = new Intent();
                 delete.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,mWidgetIds[i]);
                 delete.setAction(ACTION_DEALLOCATE_ID);
                 mContext.sendBroadcast(delete);
             }
         }
         Settings.System.putString(mContext.getContentResolver(), 
                 Settings.System.NAVIGATION_BAR_WIDGETS,"");
         inflateWidgetPref();
         mAdapter.notifyDataSetChanged();
     }
     
     private void requestWidgetInfo(int id){
    	 Log.d(TAG,"Requesting Widget:"+ id + " - ID:"+ mWidgetIds[id]);
    	 AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(mWidgetIds[id]);
    	 Log.d(TAG,"AppWidgetInfo:" + appWidgetInfo);
    	 if (appWidgetInfo != null) {
    		mTitles[id]= appWidgetInfo.label;
     		mWidgetResId[id]= appWidgetInfo.previewImage;
     		mWidgetHeight[id] = appWidgetInfo.minHeight;
     		mWidgetWidth[id] = appWidgetInfo.minWidth;
     		mProvider[id] = appWidgetInfo.provider.flattenToString();
     		PackageManager pm = mContext.getPackageManager();
     		ImageView iv = (ImageView) mViewPager.findViewWithTag("preview_"+id);
     		if (iv != null) {
     			iv.setImageDrawable(pm.getDrawable(appWidgetInfo.provider.getPackageName(), 
     					appWidgetInfo.previewImage, null));
     		mViewPager.invalidate();
     		}
    	 }
    	 
     }
     
     private void updateSummary() {
         if (mCurrentPage < mWidgetIdQty) {
        	 mSummary.setText(String.format(mContext.getResources().getString(R.string.navbar_widget_summary),
                     (mCurrentPage + 1),mWidgetIdQty));
             mTitle.setText(mTitles[mCurrentPage]);
         } else {
        	 mSummary.setText(mContext.getResources().getString(R.string.navbar_widget_summary_add));
             mTitle.setText("");
         }
     }
     
     public SimpleOnPageChangeListener mNewPageListener = new SimpleOnPageChangeListener() {

         @Override
         public void onPageSelected(int page) {
             mCurrentPage = page;
             Log.d(TAG,"Page Selected:" + page);
             int dp = mAdapter.getHeight(page);
             float px = dp * mContext.getResources().getDisplayMetrics().density;
             mViewPager.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) px));
             updateSummary();
             requestWidgetInfo(page);
         }

     };
     
     public class WidgetPagerAdapter extends PagerAdapter {

         View[] widgetViews = new View[1];

         public WidgetPagerAdapter() {
             setWidgetIds();
         }

         public void setWidgetIds () {
             widgetViews = new View[mWidgetIds.length];
         }
         
         @Override
         public int getCount() {
             return widgetViews.length;
         }

         public int getHeight(int pos) {
             if (mWidgetHeight[pos] != 0) {
                 return mWidgetHeight[pos];
             } else {
                 return getSavedHeight(pos);
             }
         }

         private int getSavedHeight(int pos) {
             SharedPreferences prefs = mContext.getSharedPreferences("widget_adapter",
                     Context.MODE_WORLD_WRITEABLE);
             return prefs.getInt("widget_pos_" + pos, 100);
         }

         /**
          * Create the page for the given position. The adapter is responsible for
          * adding the view to the container given here, although it only must ensure
          * this is done by the time it returns from {@link #finishUpdate()}.
          * 
          * @param container The containing View in which the page will be shown.
          * @param position The page position to be instantiated.
          * @return Returns an Object representing the new page. This does not need
          *         to be a View, but can be some other container of the page.
          */
         @Override
         public Object instantiateItem(View collection, int position) {
             int widgetId = mWidgetIds[position];
             LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
             ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.widget_image_preview, null);
             mWidgetView = (ImageView) vg.findViewById(R.id.widget_preview);
             mWidgetView.setTag("preview_" + position);
             if (widgetId == -1) {
                 mWidgetView.setImageResource(R.drawable.widget_add);
             } else {
                 mWidgetView.setImageResource(R.drawable.widget_na);
             }
             mWidgetView.setOnClickListener(mDoPrefClick);
             if (widgetViews != null && position < widgetViews.length){
                 widgetViews[position] = vg;
                 ((ViewPager) collection).addView(widgetViews[position], 0);
                 return widgetViews[position];
             } else {
                 Log.d(TAG,"widgetViews null!!");
                 return null;
             }  
         }

         /**
          * Remove a page for the given position. The adapter is responsible for
          * removing the view from its container, although it only must ensure this
          * is done by the time it returns from {@link #finishUpdate()}.
          * 
          * @param container The containing View from which the page will be removed.
          * @param position The page position to be removed.
          * @param object The same object that was returned by
          *            {@link #instantiateItem(View, int)}.
          */
         @Override
         public void destroyItem(View collection, int position, Object view) {
             ((ViewPager) collection).removeView((ViewGroup) view);
         }

         @Override
         public boolean isViewFromObject(View view, Object object) {
             return view == ((View) object);
         }

         /**
          * Called when the a change in the shown pages has been completed. At this
          * point you must ensure that all of the pages have actually been added or
          * removed from the container as appropriate.
          * 
          * @param container The containing View which is displaying this adapter's
          *            page views.
          */
         @Override
         public void finishUpdate(View arg0) {
         }

         @Override
         public void restoreState(Parcelable arg0, ClassLoader arg1) {
         }

         @Override
         public Parcelable saveState() {
             return null;
         }

         @Override
         public void startUpdate(View arg0) {
         }
     }
}
