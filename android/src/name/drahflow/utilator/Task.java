package name.drahflow.utilator;

import java.util.*;

public final class Task {
	String gid;
	String title;
	String description;
	String author;
	int seconds_estimate;
	int seconds_taken;
	int status;
	String closed_at;
	int publication;
	String last_edit;

	TimeDistribution task_utility;
	TimeDistribution task_likelyhood_time;

	// cached stuff
	int timeEstimate;

	// stuff used by algorithms
	int secondsUsed;

	public final void updateCachedFields() {
		if(status > 0 && seconds_taken > 0) {
			timeEstimate = seconds_taken * status / 100;
		} else {
			timeEstimate = seconds_estimate;
		}
	}

	public final boolean hasConstantImportance(Date s, Date e) {
		return task_utility.isConstant(s, e) && task_likelyhood_time.isConstant(s, e);
	}

	public final int calculateImportance(long t, FakeCalendar cal) {
		return (int)(1000l * task_utility.evaluate(t, cal) * task_likelyhood_time.evaluate(t, cal) / timeEstimate);
	}
}
