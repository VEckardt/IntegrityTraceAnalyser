/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package traceanalyser.api;

import com.ptc.services.common.api.IntegrityAPI;
import java.util.Map;

/**
 *
 * @author veckardt
 */
public class IntegrityCommands extends IntegrityAPI {

    static Map<String, String> env = System.getenv();

    public IntegrityCommands() {
        super(env, "IntegrityTraceDetails");
    }

    public String getEnv(String key) {
        return env.get(key);
    }
}