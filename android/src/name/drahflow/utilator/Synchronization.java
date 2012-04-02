package name.drahflow.utilator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Synchronization {
	private Utilator ctx;

	public Synchronization(Utilator ctx) {
		this.ctx = ctx;
	}

	public void perform() {
		try {
			URL url = new URL("https://38020.vs.webtropia.com/utilator/server");

			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			try {
				urlConnection.setDoOutput(true);
				urlConnection.setChunkedStreamingMode(0);

				OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
				serializeTasks(out);

				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				deserializeTasks(in);
			} finally {
				urlConnection.disconnect();
			}
		} catch(MalformedURLException mue) {
			throw new Error("Hard-coded URL was invalid", mue);
		} catch(IOException ioe) {
			Toast toast = Toast.makeText(ctx, "Sync failed: " + ioe.toString(), Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	private void serializeTasks(OutputStream o) throws IOException {
		o.write("Test\n".getBytes());
		o.write("Blub\r\n".getBytes());
		o.write("Foo\n\r".getBytes());
		o.close();
	}

	private void deserializeTasks(InputStream i) throws IOException {
	}
}
