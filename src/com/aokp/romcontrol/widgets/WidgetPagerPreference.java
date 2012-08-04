package com.aokp.romcontrol.widgets;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
    private int[] widgetIds;
    private int[] mWidgetHeight;
    private int[] mWidgetResId;
    private ImageView mWidgetView;
    private ImageView mLeftButton;
    private ImageView mRightButton;
    private TextView mTitleView;
    private TextView mSummaryView;
    private String[] mTitle;
    private String[] mSummary;
    private int mCurrentPage = 0;
    private int mPendingWidgetId = -1;
    Context mContext;
    
    BroadcastReceiver mWidgetIdReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            Log.i(TAG, "widget id receiver go!");

            // Need to De-Allocate the ID that this was replacing.
            if (widgetIds[mPendingWidgetId] != -1) {
                Intent delete = new Intent();
                delete.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetIds[mPendingWidgetId]);
                delete.setAction(ACTION_DEALLOCATE_ID);
                mContext.sendBroadcast(delete);
            }
            widgetIds[mPendingWidgetId] = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (mPendingWidgetId == mWidgetIdQty) { // we put a widget in the last spot
                mWidgetIdQty++;
            }
            saveWidgets();
            inflateWidgetPref();
            updatePreviews(mCurrentPage);
        };
    };
    
    BroadcastReceiver mWidgetDataReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "widget data receiver go!");
            int widgetid = intent.getIntExtra("widgetid",-1);
            int target = -1;
            for (int i = 0 ; i < mWidgetIdQty; i++) {
                if (widgetIds[i]==widgetid){
                    target = i;
                    break;
                }
            }
            if (target > -1) { 
                mWidgetResId[target] = intent.getIntExtra("imageid",0);
                mWidgetHeight[target] = intent.getIntExtra("height",0);
                mTitle[target] = intent.getStringExtra("label");
                Log.d(TAG,"Widget:"+widgetid + " label:"+mTitle[target] + " ResId:" + mWidgetResId[target]);
                if (mCurrentPage == target) {
                    updatePreviews(mCurrentPage);
                    Log.d(TAG,"Updated current page");
                }
            }
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
        mContext.registerReceiver(mWidgetDataReceiver, filter);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        // Set our custom views inside the layout
         mTitleView = (TextView) view.findViewById(R.id.title);
         mSummaryView = (TextView) view.findViewById(R.id.summary);
         mWidgetView = (ImageView) view.findViewById(R.id.widget_preview);
         mWidgetView.setOnClickListener(mDoPrefClick);
         mLeftButton = (ImageView) view.findViewById(R.id.left_button);
         mLeftButton.setOnClickListener(mDoNavButtonClick);
         mRightButton = (ImageView) view.findViewById(R.id.right_button);
         mRightButton.setOnClickListener(mDoNavButtonClick);
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
        widgetIds = new int[mWidgetIdQty+1];
        mWidgetHeight = new int[mWidgetIdQty+1];
        mWidgetResId = new int[mWidgetIdQty+1];
        mTitle = new String[mWidgetIdQty+1];
        mSummary = new String[mWidgetIdQty+1];;
        Log.i(TAG, "inflatewidgets: " + settingWidgets);
        if (settingWidgets != null && settingWidgets.length() > 0) {
            String[] split = settingWidgets.split("\\|");
            for (int i = 0; i < split.length; i++) {
                if (split[i].length() > 0)
                    widgetIds[i] = Integer.parseInt(split[i]);
                    mWidgetResId[i] = R.drawable.widget_na;
                    requestWidgetInfo(widgetIds[i]);
                    updateSummary(i);
            }
        }
        // set Widget ID to -1 for 'add button'
        widgetIds[mWidgetIdQty] = -1;
        mWidgetResId[mWidgetIdQty] = R.drawable.widget_add;
        mTitle[mWidgetIdQty] = mContext.getResources().getString(R.string.navbar_widget_title_new);
        updateSummary(mWidgetIdQty);
        updatePreviews(mCurrentPage);
     }

     View.OnClickListener mDoPrefClick = new View.OnClickListener() {
         public void onClick(View v) {
             mPendingWidgetId = mCurrentPage;
             // selectWidget();
             // send intent to pick a new widget
             Intent send = new Intent();
             send.setAction(ACTION_ALLOCATE_ID);
             mContext.sendBroadcast(send);  
         };
     };
     
     View.OnClickListener mDoNavButtonClick = new View.OnClickListener() {
         public void onClick(View v) {
             switch (v.getId()) {
                 case R.id.left_button:
                     if (--mCurrentPage < 0) {
                         mCurrentPage = 0;
                     }
                     break;
                 case  R.id.right_button:
                     if (++mCurrentPage > mWidgetIdQty) {
                         mCurrentPage = mWidgetIdQty;
                     }
                     break;
             }
             updatePreviews(mCurrentPage);
         };
     };
     
     private void saveWidgets() {
         StringBuilder widgetString = new StringBuilder();
         for (int i = 0; i < (mWidgetIdQty); i++) {
             widgetString.append(widgetIds[i]);
             if (i != (mWidgetIdQty - 1))
                 widgetString.append("|");
         }
         Settings.System.putString(mContext.getContentResolver(), Settings.System.NAVIGATION_BAR_WIDGETS,
                 widgetString.toString());
         Log.d(TAG,"Saved:" + widgetString.toString());
     }
     
     public void resetNavBarWidgets() {
         for (int i = 0; i < (mWidgetIdQty); i++) {
             if (widgetIds[i] != -1) {
                 Intent delete = new Intent();
                 delete.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetIds[i]);
                 delete.setAction(ACTION_DEALLOCATE_ID);
                 mContext.sendBroadcast(delete);
             }
         }
         Settings.System.putString(mContext.getContentResolver(), 
                 Settings.System.NAVIGATION_BAR_WIDGETS,"");
         inflateWidgetPref();
     }
     
     private void requestWidgetInfo(int widgetid){
         Intent intent = new Intent(ACTION_GET_WIDGET_DATA);
         intent.putExtra("widgetid", widgetIds[mCurrentPage]);
         mContext.sendBroadcast(intent);
     }
     
     private void updateSummary(int page) {
         if (mWidgetIdQty <= page) {
             mSummary[page] = mContext.getResources().getString(R.string.navbar_widget_summary_add);
         } else {
             mSummary[page] = (String.format(mContext.getResources().getString(R.string.navbar_widget_summary),
                   (mCurrentPage + 1),mWidgetIdQty));
         }
     }
     
     private void updatePreviews(int page) {
         mWidgetView.setImageResource(mWidgetResId[page]);
         mTitleView.setText(mTitle[page]);
         mSummaryView.setText(mSummary[page]);
        
     }
}
