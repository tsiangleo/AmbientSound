package com.github.tsiangleo.sr.client.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.github.tsiangleo.sr.client.R;

/**
 * Created by tsiang on 2016/11/26.
 */

public class BaseActivity extends AppCompatActivity implements View.OnTouchListener{
    //正常情况下的按钮颜色
    public final static String BUTTON_COLOR_NORMAL = "#1C86EE";
    //按下按钮之后的按钮颜色
    public final static String BUTTON_COLOR_DOWN = "#1874CD";


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v instanceof Button){
            //按下按钮
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                Drawable shape = getResources().getDrawable(R.drawable.view_yuan_button_dark);
                v.setBackgroundDrawable(shape);
            }
            //抬起手指
            if(event.getAction() == MotionEvent.ACTION_UP){
                Drawable shape = getResources().getDrawable(R.drawable.view_yuan_button);
                v.setBackgroundDrawable(shape);
            }
        }

        // return false保证还可以处理onclick事件
        return false;
    }
    /**
     * 在对话框中展示消息，点击确认后，关闭该对话框。
     * @param msg
     */
    protected void showMsgAndCloseDialog(String msg){
        new AlertDialog.Builder(this)
                .setTitle("消息提示")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
    }

    /**
     * 在对话框中展示消息，点击确认后，执行的指定Activity的finish()方法。
     * @param msg
     * @param activityInstance
     */
    protected void showMsgAndCloseActivity(final String msg, final Activity activityInstance){
        new AlertDialog.Builder(this)
                .setTitle("消息提示")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activityInstance.finish();
                    }
                }).create().show();
    }
}
