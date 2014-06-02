package name.drahflow.utilator;

import java.util.*;

public final class TimeDistributionComplex extends TimeDistribution {
	public boolean isConstant(Date s, Date e) { return false; }

	public interface Entry {
		public int apply(int value, long t, FakeCalendar cal);
	}

	public static class EntryConstant implements Entry {
		public int v;
		public EntryConstant(int v) { this.v = v; }
		public int apply(int value, long t, FakeCalendar cal) { return value + v; }
		public String toString() { return "[Const: " + v + "]"; }
	}

	public static class EntryMulrange implements Entry {
		public long start;
		public long end;
		public int multiplier;

		public EntryMulrange(long s, long e, int mul) { start = s; end = e; multiplier = mul; }
		public int apply(int value, long t, FakeCalendar cal) {
			if(start <= t && t < end) {
				return value * multiplier / 1000;
			}

			return value;
		}
		public String toString() { return "[Mulrange: " + start + "-" + end + ":" + multiplier + "]"; }
	}

	public static class EntryMulhours implements Entry {
		public static class Range {
			int start;
			int end;
		}

		public List<Range> ranges;
		public int multiplier;

		public EntryMulhours(List<Range> r, int mul) { ranges = r; multiplier = mul; }
		public int apply(int value, long t, FakeCalendar cal) {
			int minutesSinceDayStart = cal.hour * 60 + cal.minute;

			for(Range r: ranges) {
				if(r.start <= minutesSinceDayStart && minutesSinceDayStart < r.end) return value * multiplier / 1000;
			}

			return value;
		}
	}

	public static class EntryMuldays implements Entry {
		public Set<Long> dates; // calculation see below
		public int multiplier;

		public EntryMuldays(Set<Long> d, int mul) { dates = d; multiplier = mul; }
		public int apply(int value, long t, FakeCalendar cal) {
			final long date = cal.year * 400 + cal.month * 32 + cal.day;

			if(dates.contains(date)) {
				return value * multiplier / 1000;
			}

			return value;
		}
	}

	public Entry[] entries;

	public TimeDistributionComplex(Entry[] e) { this.entries = e; }

	public int evaluate(long t, FakeCalendar cal) {
		// Ye might be led to believe that a Java5-style loop would be faster.
		// I measured and lo, it was not (and looked at the bytecode and beheld a local
		// variable it merrily writes and reads instead of using entries[i] from the top of the stack)

		int value = 0;
		final Entry[] entries = this.entries;
		final int len = entries.length;

		for(int i = 0; i < len; ++i) value = entries[i].apply(value, t, cal);

		return value;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("[TimeDistributionComplex: ");
		for(Entry e: entries) {
			str.append(e.toString());
		}
		str.append("]");
		return str.toString();
	}
}
