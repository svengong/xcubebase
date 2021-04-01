### 说明

这是一个用于驱动frida脚本的xposed插件



### 使用方法

由于libfrida-gumjs.a库100多M，github无法上传，所以请解压xcubebase\app\src\main\cpp\libs目录下的两个zip到当前目录。或者直接去frida官网下载放到libs下
![image](https://user-images.githubusercontent.com/8065935/113249532-6d181300-92f1-11eb-90d8-9774e857be3c.png)


1. 安装插件，打开插件app点击初始化
2. 初始化过程中用到root权限，请允许su授权
3. 初始化成功后会将配置文件xcube.yaml推送到/data/local/tmp/xcube.yaml
4. 修改配置文件，设置要hook的app及其使用的js脚本路径
5. 启动目标app即可在adblog看到js脚本的输出内容

### feature

1. frida hook java时稳定性不好，尤其是锁屏再回来。这个问题使用frida 14.2.7也会出现。
2. 目前这个框架中，frida脚本目前还不能动态更新，只能重启app脚本才能生效
