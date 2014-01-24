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

import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;

import android.os.Parcel;
import android.os.Parcelable;

public class Alarm implements Parcelable {
    public static final int Monday = 0x01;
    public static final int Tuesday = 0x02;
    public static final int Wednesday = 0x04;
    public static final int Thursday = 0x08;
    public static final int Friday = 0x10;
    public static final int Saturday = 0x20;
    public static final int Sunday = 0x40;
    
    private boolean mAlarmOn;
    private long mTriggerTime;
    private int mWeeklyAlarms;

    public Alarm() {
        mAlarmOn = false;
        mTriggerTime = Calendar.getInstance().getTimeInMillis();
        mWeeklyAlarms = 0x7F; //7 days on
    }

    public Alarm(boolean alarmOn, long triggerTime, int weeklyAlarms) {
        mAlarmOn = alarmOn;
        mTriggerTime = triggerTime;
        mWeeklyAlarms = weeklyAlarms;
    }
    
    public Alarm(JSONArray jsonArray) {
        this(
                jsonArray.optBoolean(0),
                jsonArray.optLong(1, Calendar.getInstance().getTimeInMillis()),
                jsonArray.optInt(2, 0x7F)); //default 7 days on
    }

    public Alarm(Parcel in) {
        mAlarmOn = in.readByte() != 0;
        mTriggerTime = in.readLong();
        mWeeklyAlarms = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mAlarmOn ? 1 : 0));
        dest.writeLong(mTriggerTime);
        dest.writeInt(mWeeklyAlarms);
    }
    
    public JSONArray toJSONArray() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(mAlarmOn);
        jsonArray.put(mTriggerTime);
        jsonArray.put(mWeeklyAlarms);
        return jsonArray;
    }
    
    @Override
    public String toString() {
        return toJSONArray().toString();
    }
    
    public void setValues(String alarmData) {
        if (alarmData != null) {
            try {
                JSONArray jsonArray = new JSONArray(alarmData);
                mAlarmOn = jsonArray.optBoolean(0, true);
                mTriggerTime = jsonArray.optLong(1, Calendar.getInstance().getTimeInMillis());
                mWeeklyAlarms = jsonArray.optInt(2, 0x7F); //default 7 days on
            } catch (JSONException e) {
                throw new RuntimeException("Parsing alarm from invalid String alarmData", e);
            }
        }
    }

    public boolean isAlarmOn() {
        if (mWeeklyAlarms == 0) mAlarmOn = false;
        return mAlarmOn;
    }

    public void setAlarmOn(boolean alarmOn) {
        mAlarmOn = alarmOn;
    }

    public long getTriggerTime() {
        return mTriggerTime;
    }

    public void setTriggerTime(long triggerTime) {
        mTriggerTime = triggerTime;
    }

    public int getWeeklyAlarms() {
        return mWeeklyAlarms;
    }
    
    public void setWeeklyAlarms(int weeklyAlarms) {
        mWeeklyAlarms = weeklyAlarms;
    }
    
    public boolean isDayAlarm(int dayField) {
        return (mWeeklyAlarms & dayField) != 0;
    }
    
    public void setDayAlarm(int dayField, boolean onOrOff) {
        if (onOrOff) {
            mWeeklyAlarms |= dayField;
        } else {
            mWeeklyAlarms &= ~dayField;
        }
    }
    
    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        public Alarm createFromParcel(Parcel in) {
            return new Alarm(in);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };
}