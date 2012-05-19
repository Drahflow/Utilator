package name.drahflow.utilator;

import java.util.*;

public final class TimeDistributionComplex extends TimeDistribution {
	public boolean isConstant(Date s, Date e) { return false; }

	public interface Entry {
		public int apply(int value, Date t, GregorianCalendar cal);
	}

	public static class EntryConstant implements Entry {
		public int v;
		public EntryConstant(int v) { this.v = v; }
		public int apply(int value, Date t, GregorianCalendar cal) { return value + v; }
	}

	public static class EntryMulrange implements Entry {
		public Date start;
		public Date end;
		public int multiplier;

		public EntryMulrange(Date s, Date e, int mul) { start = s; end = e; multiplier = mul; }
		public int apply(int value, Date t, GregorianCalendar cal) {
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
		public int apply(int value, Date t, GregorianCalendar cal) {
			int minutesSinceDayStart = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

			for(Range r: ranges) {
				if(r.start <= minutesSinceDayStart && minutesSinceDayStart < r.end) return value * multiplier / 1000;
			}

			return 0;
		}
	}

	public static class EntryMuldays implements Entry {
		public Set<Long> dates; // calculation see below
		public int multiplier;

		public EntryMuldays(Set<Long> d, int mul) { dates = d; multiplier = mul; }
		public int apply(int value, Date t, GregorianCalendar cal) {
			final long date = cal.get(Calendar.YEAR) * 400 + cal.get(Calendar.MONTH) * 32 + cal.get(Calendar.DAY_OF_MONTH);

			if(dates.contains(date)) {
				return value * multiplier / 1000;
			}

			return 0;
		}
	}

	public Entry[] entries;

	public TimeDistributionComplex(Entry[] e) { this.entries = e; }

	public int evaluate(Date t, GregorianCalendar cal) {
		int value = 0;

		for(Entry e: entries) value = e.apply(value, t, cal);

		return value;
	}
}
