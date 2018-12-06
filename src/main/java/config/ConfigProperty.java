package config;

import java.util.Properties;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/11/20 18:10
 */
public class ConfigProperty extends Properties {
    public final String db_url;
    public final String db_user;
    public final String db_passwd;
    public final boolean check_add_table;
    public final boolean useCache;
    public final String logs_dir;

    public ConfigProperty(Properties defaults) {
        super(defaults);
        System.out.println("defaults:" + defaults);

        db_url = getString("db_url");
        db_user = getString("db_user");
        db_passwd = getString("db_passwd");
        check_add_table = getBoolean("check_add_table");
        logs_dir = getString("logs_dir");
        useCache = getBoolean("use_cache");
    }

    private String getString(String key) {
        return (String) defaults.get(key);
    }

    private boolean getBoolean(String key) {
        return getString(key).equals("1");
    }
}
