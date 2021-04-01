package org.xtgo.xcube.base;

import androidx.appcompat.app.AppCompatActivity;


import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    public static String TAG = "Xcube_xposed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        Button b = findViewById(R.id.button);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //   stringFromJNI2();
                printSomeLog();
                FileUtils fileUtils = FileUtils.getInstance(MainActivity.this);
                String ss = MainActivity.this.getDir("cache", MODE_PRIVATE).getAbsolutePath();
                Log.d(TAG, "dst = " + ss);

                fileUtils.setFileOperateCallback(new FileUtils.FileOperateCallback() {
                    @Override
                    public void onSuccess() {
                        ArrayList<String> cmds = new ArrayList<>();
                        cmds.add("rm -rf /data/local/tmp/xcube");
                        cmds.add("cp -r " + ss + " /data/local/tmp/xcube");
                        cmds.add("chmod -R 777 /data/local/tmp/xcube");
                        ShellUtils.CommandResult result = ShellUtils.execCommand(cmds, true, true);
                        if (result.result == 0) {
                            Toast.makeText(MainActivity.this, "初始化成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "失败，联系sven", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailed(String error) {
                        Toast.makeText(MainActivity.this, "失败，联系sven", Toast.LENGTH_SHORT).show();
                    }
                });
                fileUtils.copyAssetsToSD("xcube", ss);
            }
        });
    }

    public static void printSomeLog() {
        Log.d("Xcube", "printSomeLog");
    }

}
