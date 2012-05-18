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

	public final void updateCachedFields() {
		if(status > 0 && seconds_taken > 0) {
			timeEstimate = seconds_taken * status / 100;
		} else {
			timeEstimate = seconds_estimate;
		}
	}

	public final int calculateImportance(Date t) {
		return task_utility.evaluate(t) * task_likelyhood_time.evaluate(t) / timeEstimate;
	}
}
