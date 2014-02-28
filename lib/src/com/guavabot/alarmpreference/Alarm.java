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

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class Alarm implements Parcelable {
    public static final int Monday = 1<<0;
    public static final int Tuesday = 1<<1;
    public static final int Wednesday = 1<<2;
    public static final int Thursday = 1<<3;
    public static final int Friday = 1<<4;
    public static final int Saturday = 1<<5;
    public static final int Sunday = 1<<6;

    private static final String KEY_ID = "id";
    private static final String KEY_WEEKLY_ALARMS = "weeklyAlarms";
    private static final String KEY_TIME = "time";
    private static final String KEY_ALARM_ON = "alarmOn";
    public static final DateTimeFormatter TIME_FMT = DateTimeFormat.forPattern("HH:mm:ss.SSS");
    
    private boolean mAlarmOn;
    private LocalTime mTime;
    /** Stores bitwise if the alarm is enabled for each day of the week.
     * Monday is bit 0 */
    private int mWeeklyAlarms;
    /** optional id field */
    private long mId;

    /**
     * Default alarm is off, with time set to current time and all week days activated
     */
    public Alarm() {
        this(false, new LocalTime(), 0x7F, -1);
    }
    
    public Alarm(boolean alarmOn, LocalTime time, int weeklyAlarms) {
        this (alarmOn, time, weeklyAlarms, -1);
    }

    public Alarm(boolean alarmOn, LocalTime time, int weeklyAlarms, long id) {
        mAlarmOn = alarmOn;
        mTime = time;
        mWeeklyAlarms = weeklyAlarms;
        mId = id;
    }
    
    public Alarm(JSONObject json) {
        setValues(json);
    }

    public Alarm(Parcel in) {
        mAlarmOn = in.readByte() != 0;
        String time = in.readString();
        mTime = TIME_FMT.parseLocalTime(time);
        mWeeklyAlarms = in.readInt();
        mId = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mAlarmOn ? 1 : 0));
        dest.writeString(TIME_FMT.print(mTime));
        dest.writeInt(mWeeklyAlarms);
        dest.writeLong(mId);
    }

    @Override
    public int describeContents() {
        return 0;
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(KEY_ALARM_ON, mAlarmOn);
            json.put(KEY_TIME, TIME_FMT.print(mTime));
            json.put(KEY_WEEKLY_ALARMS, mWeeklyAlarms);
            json.put(KEY_ID, mId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
    
    @Override
    public String toString() {
        return toJSON().toString();
    }
    
    public void setValues(String alarmData) {
        if (alarmData != null) {
            try {
                JSONObject json = new JSONObject(alarmData);
                setValues(json);
            } catch (JSONException e) {
                throw new RuntimeException("Parsing alarm from invalid String alarmData", e);
            }
        }
    }

    public void setValues(JSONObject json) {
        mAlarmOn = json.optBoolean(KEY_ALARM_ON, true);
        String time = json.optString(KEY_TIME, null);
        mTime = time != null ? TIME_FMT.parseLocalTime(time) : new LocalTime();
        mWeeklyAlarms = json.optInt(KEY_WEEKLY_ALARMS, 0x7F); //default 7 days on
        mId = json.optInt(KEY_ID, -1);
    }

    public boolean isAlarmOn() {
        if (mWeeklyAlarms == 0) mAlarmOn = false;
        return mAlarmOn;
    }

    public void setAlarmOn(boolean alarmOn) {
        mAlarmOn = alarmOn;
    }

    public LocalTime getTime() {
        return mTime;
    }

    public void setTime(LocalTime time) {
        mTime = time;
    }
    
    public void setTime(int hourOfDay, int minuteOfHour) {
        mTime = mTime
                .withHourOfDay(hourOfDay)
                .withMinuteOfHour(minuteOfHour)
                .withSecondOfMinute(0);
    }

    public int getWeeklyAlarms() {
        return mWeeklyAlarms;
    }
    
    public void setWeeklyAlarms(int weeklyAlarms) {
        mWeeklyAlarms = weeklyAlarms;
    }
    
    public boolean isDayActivated(int dayField) {
        return (mWeeklyAlarms & dayField) != 0;
    }
    
    public void setDayActivated(int dayField, boolean onOrOff) {
        if (onOrOff) {
            mWeeklyAlarms |= dayField;
        } else {
            mWeeklyAlarms &= ~dayField;
        }
    }
    
    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }
    
    public long getNextTrigger() {
        if (mAlarmOn) {
            DateTime alarmDt = mTime.toDateTimeToday();
            for (int i = 0; i < 7; i++) {
                if ((mWeeklyAlarms & (1 << (alarmDt.getDayOfWeek() - 1))) != 0) {
                    if (alarmDt.isAfterNow()) {
                        return alarmDt.getMillis();
                    }
                } 
                alarmDt = alarmDt.plusDays(1);
            }
        }
        return Long.MAX_VALUE;
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