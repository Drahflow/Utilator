package name.drahflow.utilator;

import java.util.*;

public final class FakeCalendar {
	public int year; // 0000 .. 2012
	public int month; // 0 .. 11
	public int day; // 1 .. 31
	public int hour; // 0 .. 23
	public int minute; // 0 .. 59
	public int second; // 0 .. 60

	final static GregorianCalendar cal = new GregorianCalendar();

	public FakeCalendar() { }

	public void setTime(Date d) {
		cal.setTime(d);

		year = cal.get(Calendar.YEAR);
		month = cal.get(Calendar.MONTH);
		day = cal.get(Calendar.DAY_OF_MONTH);
		hour = cal.get(Calendar.HOUR);
		minute = cal.get(Calendar.MINUTE);
		second = cal.get(Calendar.SECOND);
	}

	public void addSeconds(int amount) {
		second += amount;
		minute += second / 60; second = second % 60;
		hour += minute / 60; minute = minute % 60;

		if(hour > 24) {
			hour -= 24;

			if(day < 28) {
				++day;
			} else {
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month);
				cal.set(Calendar.DAY_OF_MONTH, day);

				cal.add(Calendar.DAY_OF_MONTH, 1);

				year = cal.get(Calendar.YEAR);
				month = cal.get(Calendar.MONTH);
				day = cal.get(Calendar.DAY_OF_MONTH);
			}
		}
	}
}
