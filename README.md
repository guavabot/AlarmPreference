AlarmPreference
===============

This is a custom preference for Android.

AlarmPreferences saves to SharedPreferences a JSONArray as a String with:
* a boolean that describes if the alarm is enabled 
* a trigger time in milliseconds (Unix timestamp) 
* an int which stores bitwise if the alarm is on for every day of the week.

Monday is bit 0, Tuesday bit 1, and so on, so an alarm set for every day would
have the value 0x7F or 127 in decimal.

##Usage

Add an AlarmPreference to your preference xml like this:

	<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android" >
		<com.guavabot.alarmpreference.AlarmPreference
				android:key="Alarm_1"
				android:title="Alarm 1" />
	</PreferenceScreen>

By default, an alarm is off, but you can provide a default value in this format:

	android:defaultValue="[true,1389528000000,127]"
	
For the UNIX timestamp, only the time of day should matter to you.

You can also retrieve an alarm as an Alarm object like this:

	Alarm myAlarm = new Alarm();
	myAlarm.setValues(sharedPreferences.getString("Alarm_1", null));

You can check if an alarm is activated certain day like this:

	if (myAlarm.isAlarmOn() && myAlarm.isDayAlarm(Alarm.Monday)) {
		//do something
	}
