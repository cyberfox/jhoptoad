
package com.sniflabs.hoptoad;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <strong>HoptoadNotice:</strong><br/>
 * A Java wrapper for hoptoad error reporting application, because 
 * <em>java guys need access to cool error reporting too!!</em><br/>
 * <br/>
 * <strong>requires:</strong> <br/>
 * <a href="http://sourceforge.net/projects/yamlbeans/">yamlbeans</a> from
 * http://sourceforge.net/projects/yamlbeans/ <br/>
 * <br/>
 * 
 * <strong>Sample use:</strong>
 * <code><pre>
 * catch (Exception exp) {
 *   HoptoadNotice notice = 
 *   	new HoptoadNotice("fff9de2222222e156015fb875844de43",exp);
 *   notice.postData(true);
 * }
 * </pre>
 * </code>
 * 
 * @see <a href="http://sourceforge.net/projects/yamlbeans/">yamlbeans</a> 
 * @author Noah Paessel (noah@sniflabs.com)
 * @version 0.1 (may be quite flakey!)
 */
public class HoptoadNotice {
//	public static final String	HOPTOAD_URL="http://fox.vulpine.com:9889";
	public static final String	HOPTOAD_URL="http://hoptoadapp.com/notices/";

	private String 				api_key;
	private String 				error_message;
	private String				error_class;

	private ArrayList<String>		backtrace;
	private Map<String, String>		environment;

	public static void main(String[] args) {
    try {
      zarf();
    } catch(Exception e) {
      HoptoadNotice h = new HoptoadNotice("foo", e);
      h.postData(true);
    }
  }

  private static void zarf() {
    throw new NullPointerException("Testing 2");
  }

  private StringBuffer dumpEnvironment(String prefix) {
    StringBuffer rval = new StringBuffer(prefix + "environment: \n");
    for(Map.Entry<String, String> pair : environment.entrySet()) {
      rval.append(prefix).append("  ").append(pair.getKey()).append(": ").append(pair.getValue()).append('\n');
    }

    return rval;
  }

  private StringBuffer dumpBacktrace(String prefix) {
    StringBuffer rval = new StringBuffer();
    rval.append(prefix).append("backtrace: \n");
    for(String entry : backtrace) {
      rval.append(prefix).append("- ").append(entry).append("\n");
    }

    return rval;
  }

  /**
	 * default constructor. Doesn't help anyone, but is required for bean-ness.
	 * initializes data structures.
	 */
	public HoptoadNotice() {
		super();
		api_key = "invalid api key";
		backtrace = new ArrayList<String>();

		environment = new HashMap<String, String>();
	}

	/**
	 * Create a new HoptoadNotice notice for sending to hoptoadapp.com <br>
	 * <strong>NOTE:</strong> this does not send actually send the notice
	 * @param key Your HoapToadApp API key.
	 * @param exception the exception you want to log
	 * 
	 */
	public HoptoadNotice(String key, Throwable exception) {
		this();
		api_key = key;
		error_message = exception.getMessage();
		if (null == error_message || error_message.length() == 0) {
			error_message = exception.toString();
		}
		error_class   = exception.getClass().getSimpleName();
		environment   = System.getenv();
		
		for(StackTraceElement element : exception.getStackTrace()) {
			backtrace.add(String.format("%s:%d  %s %s", 
					element.getFileName(), 
					element.getLineNumber(), 
					element.getClassName(), 
					element.getMethodName()));
		}
	}
	

	/**
	 * return the YAML string for this notice.<br/>
   *
   * mrs - I generally dislike external requirements, so I faked up this
   * super-simple YAML generator, so I could embed this in my app.
   *
	 * @return the YAML for the HopToadNotice datastructure
	 */
	public String toYaml() {
    StringBuffer sb = new StringBuffer("--- \nnotice: \n");
    sb.append("  api_key: ").append(api_key).append("\n");
    sb.append(dumpBacktrace("  "));
    sb.append(dumpEnvironment("  "));
    sb.append("  error_class: ").append(error_class).append('\n');
    sb.append("  error_message: ").append(error_message).append('\n');
    sb.append("  request: {}\n");
    sb.append("  session: {}\n");

		return sb.toString();
	}

  /**
	 * Send the data to the HopToadApp.
	 * Sets HTTP headers, and then sends the YAML file.
	 * Optionally prints out the YAML text if you pass in true for debug.
	 * @param debug set to true to see the YAML sent, and the XML server results.
	 */
	public void postData(boolean debug) {
    try {
      // establish connection to hoptoad
      URLConnection conn = new URL(HOPTOAD_URL).openConnection();
      // set up the connection to handle posting x-yaml document.
      // and for receiving XML formatted response..
      // HEY HOPTOAD KIDS: Can I send XML instead of YAML?
      // us java guys are just broken that way!
      conn.setRequestProperty("Content-type", "application/x-yaml");
      conn.setRequestProperty("Accept", "text/xml, application/xml");
      conn.setDoOutput(true);

      OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
      if (debug) {
        System.out.println(toYaml());
      }
      wr.write(toYaml());
      wr.flush();
      wr.close();

      // print out debugging messages
      if (debug) {
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
          System.out.println(line);
        }
        rd.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.err.printf("error communicating with HopToadApp: %s\n", e);
    }
	}
}
