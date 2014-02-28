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

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TimePicker;

public class AlarmPreference extends DialogPreference {
    private Alarm mAlarm;
    private CheckBox mEnabledChBox;
    private TimePicker mTimePicker;
    private CheckBox[] mDayCheckBoxes = new CheckBox[7];
    private static final int[] mCheckBoxesIds = new int[] {
            R.id.checkMonday,
            R.id.checkTuesday,
            R.id.checkWednesday,
            R.id.checkThursday,
            R.id.checkFriday,
            R.id.checkSaturday,
            R.id.checkSunday
    };
    private ColorStateList mNormalTextColor;
    
    public AlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.alarm_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        mAlarm = new Alarm();
    }
    
    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        boolean alarmOn = mAlarm.isAlarmOn();
        mEnabledChBox = (CheckBox) v.findViewById(R.id.enable_alarm);
        mEnabledChBox.setChecked(alarmOn);
        mEnabledChBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTimePicker.setEnabled(isChecked);
                for (CheckBox dayCheckBox : mDayCheckBoxes) {
                    dayCheckBox.setEnabled(isChecked);
                }
            }
        });
        mNormalTextColor =  mEnabledChBox.getTextColors();

        mTimePicker = (TimePicker) v.findViewById(R.id.alarm_time);
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        mTimePicker.setDescendantFocusability(TimePicker.FOCUS_BLOCK_DESCENDANTS);
        mTimePicker.setCurrentHour(mAlarm.getTime().getHourOfDay());
        mTimePicker.setCurrentMinute(mAlarm.getTime().getMinuteOfHour());
        
        int weeklyAlarms = mAlarm.getWeeklyAlarms();
        for (int i = 0; i < 7; i++) {
            mDayCheckBoxes[i] = (CheckBox) v.findViewById(mCheckBoxesIds[i]);
            boolean dayAlarmOn = (weeklyAlarms & (1 << i)) != 0;
            mDayCheckBoxes[i].setChecked(dayAlarmOn);
            formatDayCheckBox(mDayCheckBoxes[i], dayAlarmOn);
            mDayCheckBoxes[i].setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    formatDayCheckBox(buttonView, isChecked);
                }
            });
        }
        
        if (!alarmOn) {
            mTimePicker.setEnabled(false);
            for (CheckBox dayCheckBox : mDayCheckBoxes) {
                dayCheckBox.setEnabled(false);
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        if (positiveResult) {
            mAlarm.setAlarmOn(mEnabledChBox.isChecked());
            mAlarm.setTime(mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
            int weeklyAlarms = mAlarm.getWeeklyAlarms();
            for (int i = 0; i < 7; i++) {
                if (mDayCheckBoxes[i].isChecked()) {
                    weeklyAlarms |= 1 << i;
                } else {
                    weeklyAlarms &= ~(1 << i);
                }
            }
            mAlarm.setWeeklyAlarms(weeklyAlarms);

            if (callChangeListener(mAlarm.toString())) {
                setSummary(getSummary());
                persistString(mAlarm.toString());
                notifyChanged();
            }
        }
    }
    
    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        //Fix for time picker not displaying hour after rotation on pre-Jelly Bean devices
        mTimePicker.setCurrentHour(mAlarm.getTime().getHourOfDay());
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
        
        DateTimeFormatter timeFmt = DateTimeFormat.shortTime();
        builder.append(getContext().getString(R.string.alarm_at, timeFmt.print(mAlarm.getTime())));
        return builder.toString();
    }
    
    public void setAlarm(Alarm alarm) {
        mAlarm = alarm;
    }
    
    public Alarm getAlarm() {
        return mAlarm;
    }

    private void formatDayCheckBox(CompoundButton dayCheckBox, boolean isChecked) {
        if (isChecked) {
            dayCheckBox.setTextColor(mNormalTextColor);
            dayCheckBox.setTextAppearance(getContext(), R.style.boldText);
        } else {
            dayCheckBox.setTextColor(Color.GRAY);
            dayCheckBox.setTextAppearance(getContext(), R.style.normalText);
        }
    }
    
} 
