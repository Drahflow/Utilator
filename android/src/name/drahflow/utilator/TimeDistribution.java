package name.drahflow.utilator;

import java.util.*;

public abstract class TimeDistribution {
	private static GregorianCalendar cal = new GregorianCalendar();

	abstract public int evaluate(Date t, GregorianCalendar cal);
	abstract public boolean isConstant(Date s, Date e);

	public static TimeDistribution compile(int def, List<String> distribution) {
		return compile(def, distribution, new TreeMap<String, Date>());
	}

	public static TimeDistribution compile(int def, List<String> distribution, Map<String, Date> cache) {
		if(distribution == null || distribution.isEmpty()) {
			return new TimeDistributionConstant(def);
		}

		Collections.sort(distribution);

		List<TimeDistributionComplex.Entry> entries = new ArrayList<TimeDistributionComplex.Entry>();
		Integer value = 0;

		for(String entry: distribution) {
			int colon = entry.indexOf(':');
			String[] data = new String[] { entry.substring(1, colon), entry.substring(colon + 1) };

			if(data[0].equals("constant")) {
				if(value != null) {
					value += Integer.parseInt(data[1]);
				} else {
					entries.add(new TimeDistributionComplex.EntryConstant(Integer.parseInt(data[1])));
				}
			} else if(data[0].equals("mulrange")) {
				if(value != null) {
					entries.add(new TimeDistributionComplex.EntryConstant(value));
					value = null;
				}

				int semicolon1 = data[1].indexOf(';');
				int semicolon2 = data[1].indexOf(';', semicolon1 + 1);
				String[] parts  = new String[] {
					data[1].substring(0, semicolon1),
					data[1].substring(semicolon1 + 1, semicolon2),
					data[1].substring(semicolon2 + 1)
				};

				Date start = Util.parseDate(parts[0], cache);
				Date end = Util.parseDate(parts[1], cache);
				int multiplier = Integer.parseInt(parts[2]);
				entries.add(new TimeDistributionComplex.EntryMulrange(start, end, multiplier));
			} else if(data[0].equals("mulhours")) {
				if(value != null) {
					entries.add(new TimeDistributionComplex.EntryConstant(value));
					value = null;
				}

				String[] parts = data[1].split(";");

				List<TimeDistributionComplex.EntryMulhours.Range> ranges = new ArrayList<TimeDistributionComplex.EntryMulhours.Range>();
				for(String hour: parts[0].split(",")) {
					TimeDistributionComplex.EntryMulhours.Range range = new TimeDistributionComplex.EntryMulhours.Range();

					String[] parts2 = hour.split("\\+");
					String[] parts3 = parts2[0].split(":");
					range.start = Integer.parseInt(parts3[0]) * 60 + Integer.parseInt(parts3[1]);
					range.end = range.start + Integer.parseInt(parts2[1]);
					ranges.add(range);
				}

				int multiplier = Integer.parseInt(parts[1]);
				entries.add(new TimeDistributionComplex.EntryMulhours(ranges, multiplier));
			} else if(data[0].equals("muldays")) {
				if(value != null) {
					entries.add(new TimeDistributionComplex.EntryConstant(value));
					value = null;
				}

				String[] parts = data[1].split(";");

				Set<Long> dates = new HashSet<Long>();
				for(String day: parts[0].split(",")) {
					cal.setTime(Util.parseDate(day, cache));
					final long date = cal.get(Calendar.YEAR) * 400 + cal.get(Calendar.MONTH) * 32 + cal.get(Calendar.DAY_OF_MONTH);
					dates.add(date);
				}

				int multiplier = Integer.parseInt(parts[1]);
				entries.add(new TimeDistributionComplex.EntryMuldays(dates, multiplier));
			} else {
				throw new IllegalArgumentException("Unknown time distribution spec: " + entry);
			}
		}

		if(value != null) {
			return new TimeDistributionConstant(value);
		} else {
			// optimize overlapping mulranges
			for(int i = 2; i < entries.size(); ++i) {
				if(entries.get(i - 1) instanceof TimeDistributionComplex.EntryMulrange && entries.get(i) instanceof TimeDistributionComplex.EntryMulrange) {
					final TimeDistributionComplex.EntryMulrange earlier = (TimeDistributionComplex.EntryMulrange)entries.get(i - 1);
					final TimeDistributionComplex.EntryMulrange later = (TimeDistributionComplex.EntryMulrange)entries.get(i);

					if(earlier.multiplier == 0 && later.multiplier == 0) {
						earlier.start = earlier.start.compareTo(later.start) < 0? earlier.start: later.start;
						earlier.end = earlier.end.compareTo(later.end) > 0? earlier.end: later.end;

						entries.remove(i);
						--i;
					}
				}
			}

			return new TimeDistributionComplex(entries.toArray(new TimeDistributionComplex.Entry[entries.size()]));
		}
	}
}
