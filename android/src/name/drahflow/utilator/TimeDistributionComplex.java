package name.drahflow.utilator;

import java.util.*;

public final class TimeDistributionComplex extends TimeDistribution {
	private static GregorianCalendar cal = new GregorianCalendar();

	public boolean isConstant(Date s, Date e) { return false; }

	public interface Entry {
		public int apply(int value, Date t);
	}

	public static class EntryConstant implements Entry {
		public int v;
		public EntryConstant(int v) { this.v = v; }
		public int apply(int value, Date t) { return value + v; }
	}

	public static class EntryMulrange implements Entry {
		public Date start;
		public Date end;
		public int multiplier;

		public EntryMulrange(Date s, Date e, int mul) { start = s; end = e; multiplier = mul; }
		public int apply(int value, Date t) {
			if(start.compareTo(t) <= 0 && end.compareTo(t) > 0) {
				return value * multiplier / 1000;
			}

			return value;
		}
	}

	public static class EntryMulhours implements Entry {
		public static class Range {
			int start;
			int end;
		}

		public List<Range> ranges;
		public int multiplier;

		public EntryMulhours(List<Range> r, int mul) { ranges = r; multiplier = mul; }
		public int apply(int value, Date t) {
			cal.setTime(t);
			int minutesSinceDayStart = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

			for(Range r: ranges) {
				if(r.start <= minutesSinceDayStart && minutesSinceDayStart < r.end) return value * multiplier / 1000;
			}

			return value;
		}
	}

	public static class EntryMuldays implements Entry {
		public Set<Date> dates; // normalized to 00:00
		public int multiplier;

		public EntryMuldays(Set<Date> d, int mul) { dates = d; multiplier = mul; }
		public int apply(int value, Date t) {
			cal.setTime(t);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);

			if(dates.contains(cal.getTime())) {
				return value * multiplier / 1000;
			}

			return value;
		}
	}

	public Entry[] entries;

	public TimeDistributionComplex(Entry[] e) { this.entries = e; }

	public int evaluate(Date t) {
		int value = 0;

		for(Entry e: entries) value = e.apply(value, t);

		return value;
	}
}
