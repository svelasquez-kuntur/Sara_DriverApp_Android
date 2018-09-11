package com.sara.driver;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.general.files.GeneralFunctions;
import com.general.files.StartActProcess;
import com.utils.Utils;
import com.view.MTextView;
import com.view.calendarview.CalendarListener;
import com.view.calendarview.CustomCalendarView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RideHistoryActivity extends AppCompatActivity {

    MTextView titleTxt;
    ImageView backImgView;

    GeneralFunctions generalFunc;
    CustomCalendarView calendar_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_history);

        generalFunc = new GeneralFunctions(getActContext());

        titleTxt = (MTextView) findViewById(R.id.titleTxt);
        backImgView = (ImageView) findViewById(R.id.backImgView);
        calendar_view = (CustomCalendarView) findViewById(R.id.calendar_view);

        backImgView.setOnClickListener(new setOnClickList());

        setLabels();
        calendar_view.setCalendarListener(new CalendarListener() {
            @Override
            public void onDateSelected(Date date) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
                String date_formatted = date_format.format(cal.getTime());
                Bundle bn = new Bundle();
                bn.putString("SELECTED_DATE", date_formatted);
                new StartActProcess(getActContext()).startActWithData(SelectedDayHistoryActivity.class, bn);
            }
            @Override
            public void onMonthChanged(Date time) {

            }
        });
    }

    public void setLabels() {
        titleTxt.setText(generalFunc.retrieveLangLBl("", "LBL_RIDE_HISTORY"));
    }

    public Context getActContext() {
        return RideHistoryActivity.this;
    }

    public class setOnClickList implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Utils.hideKeyboard(RideHistoryActivity.this);
            int i = view.getId();
            if (i == R.id.backImgView) {
                RideHistoryActivity.super.onBackPressed();
            }
        }
    }
}
