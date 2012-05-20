package name.drahflow.utilator;

import java.util.*;

public class TimeDistributionConstant extends TimeDistribution {
	public int v;

	public boolean isConstant(Date s, Date e) { return true; }

	public TimeDistributionConstant(int v) {
		this.v = v;
	}

	public int evaluate(long t, FakeCalendar cal) {
		return v;
	}
}
