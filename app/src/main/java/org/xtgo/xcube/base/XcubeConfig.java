package org.xtgo.xcube.base;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XcubeConfig {
    public boolean active = false;
    public HashMap<String,String> packageConfigList = new HashMap<>();


    public XcubeConfig(String configPath) {
        Yaml yaml = new Yaml();
        InputStream in = null;
        try {
            in = new FileInputStream(configPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in == null) {
            return;
        }
        Map<String, Object> map = yaml.load(in);
        if (map.containsKey("active") && (boolean) map.get("active")) {
            active = true;
        } else {
            return;
        }
        if (map.containsKey("packageConfigList")){
            packageConfigList = (HashMap<String,String>) map.get("packageConfigList");

        }
    }

    public boolean contains(String packageName) {
        for (Map.Entry<String, String> entry : packageConfigList.entrySet()) {
            if (entry.getKey().equals(packageName)) {
                return true;
            }
        }
        return false;
    }
    public String getScriptPath(String packageName){
        return packageConfigList.get(packageName);
    }


    public static class PackageConfig {
        protected String packageName;
        String scriptPath;

        public PackageConfig(String packageName, String scriptPath) {
            this.packageName = packageName;
            this.scriptPath = scriptPath;
        }

        @Override
        public String toString() {
            return "PackageConfig{" +
                    "packageName='" + packageName + '\'' +
                    ", scriptPath='" + scriptPath + '\'' +
                    '}';
        }
    }


    @Override
    public String toString() {
        return "XcubeConfig{" +
                "active=" + active +
                ", packageConfigList=" + packageConfigList +
                '}';
    }


}
