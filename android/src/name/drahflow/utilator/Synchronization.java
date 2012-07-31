package name.drahflow.utilator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

import static name.drahflow.utilator.Util.*;

public class Synchronization {
	private Utilator ctx;

	public Synchronization(Utilator ctx) {
		this.ctx = ctx;
	}

	public void perform() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			serializeTasks(out);

			URL url = new URL("https://38020.vs.webtropia.com/utilator/server");

			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			try {
				urlConnection.setDoOutput(true);
				urlConnection.setChunkedStreamingMode(0);

				urlConnection.getOutputStream().write(out.toByteArray());

				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				deserializeTasks(in);
			} finally {
				urlConnection.disconnect();
			}

			ctx.db.updateLastSync();
		} catch(MalformedURLException mue) {
			throw new Error("Hard-coded URL was invalid", mue);
		} catch(JSONException jsone) {
			Toast toast = Toast.makeText(ctx, "Sync failed: " + jsone.toString(), Toast.LENGTH_LONG);
			toast.show();
			jsone.printStackTrace();
		} catch(IOException ioe) {
			Toast toast = Toast.makeText(ctx, "Sync failed: " + ioe.toString(), Toast.LENGTH_LONG);
			toast.show();
			ioe.printStackTrace();
		}
	}

	private void serializeTasks(OutputStream o) throws IOException, JSONException {
		OutputStreamWriter writer = new OutputStreamWriter(o);

		JSONObject resRoot = new JSONObject();
		resRoot.put("version", 1);
		resRoot.put("secret", Secrets.SYNC_SECRET);
		resRoot.put("start_date", ctx.db.getLastSync());

		JSONArray resTasks = new JSONArray();
		resRoot.put("task", resTasks);

		String where = "WHERE last_edit IS NULL OR last_edit > (SELECT last FROM last_sync)";

		List<Task> tasks = ctx.db.loadAllTasks(where);
		Map<String, List<String>> taskUtilities = ctx.db.loadManyTaskUtilities(where);
		Map<String, List<String>> taskLikelyhoodTime = ctx.db.loadManyTaskLikelyhoodTime(where);
		Map<String, List<String>> taskExternal = ctx.db.loadManyTaskExternal(where);

		for(Task task: tasks) {
			// FIXME: filter publication state

			JSONObject resTask = new JSONObject();

			resTask.put("gid", task.gid);
			resTask.put("title", task.title);
			resTask.put("description", task.description);
			resTask.put("author", task.author);
			resTask.put("seconds_estimate", task.seconds_estimate);
			resTask.put("seconds_taken", task.seconds_taken);
			resTask.put("status", task.status);
			resTask.put("closed_at", task.closed_at);
			resTask.put("publication", task.publication);
			resTask.put("last_edit", task.last_edit);

			final String gid = task.gid;

			resTask.put("utility",
					taskUtilities.get(gid) == null? new JSONArray(): new JSONArray(taskUtilities.get(gid)));

			resTask.put("likelyhood_time",
					taskLikelyhoodTime.get(gid) == null? new JSONArray(): new JSONArray(taskLikelyhoodTime.get(gid)));

			resTask.put("external",
					taskExternal.get(gid) == null? new JSONArray(): new JSONArray(taskExternal.get(gid)));

			resTasks.put(resTask);
		}

		writer.write(resRoot.toString());
		writer.close();
	}

	private void deserializeTasks(InputStream input) throws IOException, JSONException {
		InputStreamReader reader = new InputStreamReader(input);
		StringBuilder json = new StringBuilder();
		char[] buf = new char[4096];
		int len;

		while((len = reader.read(buf)) > 0) {
			json.append(buf, 0, len);
		}

		reader.close();

		if(json.length() == 0) {
			Toast toast = Toast.makeText(ctx, "Sync failed: empty result", Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		//Log.i("Utilator", "JSON received: " + json);

		JSONObject inRoot = (JSONObject)new JSONTokener(json.toString()).nextValue();
		String error = (String)inRoot.opt("error");
		if(error != null) {
			Toast toast = Toast.makeText(ctx, "Sync failed: " + error, Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		JSONArray inTasks = inRoot.getJSONArray("task");

		for(int i = 0; i < inTasks.length(); ++i) {
			JSONObject inTask = inTasks.getJSONObject(i);
			Map<String, Object> task = ctx.db.loadTask(inTask.getString("gid"));

			if(task == null) {
				ctx.db.createEmptyTask(inTask.getString("gid"));
				writeTask(inTask);
			} else if(task.get("last_edit") == null) {
				writeTask(inTask);
			} else if(loadString(task, "last_edit").compareTo(inTask.getString("last_edit")) < 0) {
				writeTask(inTask);
			}
		}

		Toast toast = Toast.makeText(ctx, "Sync succeeded: " + inTasks.length() + " tasks received", Toast.LENGTH_LONG);
		toast.show();
	}

	private void writeTask(JSONObject inTask) throws JSONException {
		Map<String, Object> task = new HashMap<String, Object>();
		for(String key: Arrays.asList("gid", "title", "description", "author", "seconds_estimate", "seconds_taken", "status", "closed_at", "publication", "last_edit")) {
			task.put(key, inTask.getString(key));
		}

		ctx.db.updateTask(task);
		final String gid = loadString(task, "gid");

		ctx.db.deleteUtilities(gid);
		if(inTask.has("utility")) {
			JSONArray inUtilities = inTask.getJSONArray("utility");
			for(int i = 0; i < inUtilities.length(); ++i) {
				ctx.db.addUtility(gid, inUtilities.getString(i));
			}
		}

		ctx.db.deleteLikelyhoodsTime(gid);
		if(inTask.has("likelyhood_time")) {
			JSONArray inLikelyhoodTime = inTask.getJSONArray("likelyhood_time");
			for(int i = 0; i < inLikelyhoodTime.length(); ++i) {
				ctx.db.addLikelyhoodTime(gid, inLikelyhoodTime.getString(i));
			}
		}

		ctx.db.deleteExternal(gid);
		if(inTask.has("external")) {
			JSONArray inExternal = inTask.getJSONArray("external");
			for(int i = 0; i < inExternal.length(); ++i) {
				ctx.db.addExternal(gid, inExternal.getString(i));
			}
		}
	}
}
