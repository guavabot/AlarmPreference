/*
 * Copyright (c) 2014 Ivan Soriano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guavabot.alarmpreference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TimePicker;

public class AlarmPreference extends DialogPreference {
    private Alarm mAlarm;
    Calendar calendar;
    private CheckBox mEnabled;
    private TextView mTimeView;
    private TimePicker mTimePicker2;
    private CheckBox[] mDayCheckBoxes = new CheckBox[7];
    private int[] checkBoxesIds = new int[] {
            R.id.checkMonday,
            R.id.checkTuesday,
            R.id.checkWednesday,
            R.id.checkThursday,
            R.id.checkFriday,
            R.id.checkSaturday,
            R.id.checkSunday
    };
    ColorStateList mNormalTextColor;
    
    public AlarmPreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
        setDialogLayoutResource(R.layout.alarm_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        mAlarm = new Alarm();
        calendar = Calendar.getInstance();
    }
    
    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        
        mEnabled = (CheckBox) v.findViewById(R.id.enable_alarm);
        boolean alarmOn = mAlarm.isAlarmOn();
        mEnabled.setChecked(alarmOn);
        mEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTimeView.setEnabled(isChecked);
                for (CheckBox dayCheckBox : mDayCheckBoxes) {
                    dayCheckBox.setEnabled(isChecked);
                }
            }
        });
        
        mTimeView = (TextView) v.findViewById(R.id.alarm_time);
        mNormalTextColor =  mTimeView.getTextColors();
        calendar.setTimeInMillis(mAlarm.getTriggerTime());
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        mTimeView.setText(sdf.format(calendar.getTime()));
        mTimeView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(null);
                mTimePicker2 = new TimePicker(getContext());
                mTimePicker2.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
                mTimePicker2.setCurrentMinute(calendar.get(Calendar.MINUTE));
                builder.setView(mTimePicker2);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        calendar.set(Calendar.HOUR_OF_DAY, mTimePicker2.getCurrentHour());
                        calendar.set(Calendar.MINUTE, mTimePicker2.getCurrentMinute());
                        calendar.set(Calendar.SECOND, 0);
                        mTimeView.setText(sdf.format(calendar.getTime()));
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
        
        int weeklyAlarms = mAlarm.getWeeklyAlarms();
        for (int i = 0; i < 7; i++) {
            mDayCheckBoxes[i] = (CheckBox) v.findViewById(checkBoxesIds[i]);
            boolean dayAlarmOn = (weeklyAlarms & (1 << i)) != 0;
            mDayCheckBoxes[i].setChecked(dayAlarmOn);
            if (dayAlarmOn) {
                mDayCheckBoxes[i].setTextColor(mNormalTextColor);
                mDayCheckBoxes[i].setTextAppearance(getContext(), R.style.boldText);
            } else {
                mDayCheckBoxes[i].setTextColor(Color.GRAY);
            }
            
            mDayCheckBoxes[i].setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        buttonView.setTextColor(mNormalTextColor);
                        buttonView.setTextAppearance(getContext(), R.style.boldText);
                    } else {
                        buttonView.setTextColor(Color.GRAY);
                        buttonView.setTextAppearance(getContext(), R.style.normalText);
                    }
                }
            });
        }
        
        if (!alarmOn) {
            mTimeView.setEnabled(false);
            for (CheckBox dayCheckBox : mDayCheckBoxes) {
                dayCheckBox.setEnabled(false);
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            
            mAlarm.setAlarmOn(mEnabled.isChecked());
            mAlarm.setTriggerTime(calendar.getTimeInMillis());
            int weeklyAlarms = mAlarm.getWeeklyAlarms();
            for (int i = 0; i < 7; i++) {
                if (mDayCheckBoxes[i].isChecked()) {
                    weeklyAlarms |= 1 << i;
                } else {
                    weeklyAlarms &= ~(1 << i);
                }
            }
            mAlarm.setWeeklyAlarms(weeklyAlarms);

            setSummary(getSummary());
            if (callChangeListener(mAlarm.toString())) {
                persistString(mAlarm.toString());
                notifyChanged();
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            mAlarm.setValues(getPersistedString(null));
        } else if (defaultValue != null) {
            mAlarm.setValues((String) defaultValue);
        }
        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        if (mAlarm == null) {
            return null;
        }

        if (!mAlarm.isAlarmOn()) {
            return getContext().getResources().getText(R.string.alarm_disabled);
        }
        String[] dayNames = getContext().getResources().getStringArray(R.array.alarm_days_summary);
        StringBuilder builder = new StringBuilder();
        int weeklyAlarms = mAlarm.getWeeklyAlarms();
        for (int i = 0; i < 7; i++) {
            if ((weeklyAlarms & (1 << i)) != 0) builder.append(dayNames[i]);
        }
        builder.append(getContext().getResources().getText(R.string.alarm_at));
        builder.append(DateFormat.getTimeFormat(getContext()).format(new Date(mAlarm.getTriggerTime())));
        return builder.toString();
    }
    
    public void setAlarm(Alarm alarm) {
        mAlarm = alarm;
        calendar.setTimeInMillis(mAlarm.getTriggerTime());
    }
    
    public Alarm getAlarm() {
        return mAlarm;
    }
    
} 
