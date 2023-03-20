# douwan

都玩投屏发送端SDK

1. 请将aar导入到工程中的libs中， 

然后在module的build.gradle中添加以下配置

在android节点中添加以下配置
repositories {
    flatDir {
        dirs 'libs'
    }
}
 
2. 在dependencies中添加以下配置
compile(name: 'douwan-usbsource-release', ext: 'aar')

3.在AndroidManifest.xml 添加以下配置

  <service
    android:name="com.xindawn.droidusbsource.PhoneSourceService">
  </service>
  
4.当需要编译打包混淆的时候，投屏SDK已混淆过，无需在对投屏SDK及其依赖的第三方jar包进行混淆，请添加以下都玩投屏配置：

-keep class com.xindawn.droidusbsource.PhoneSourceService {*;}
-keep @com.xindawn.droidusbsource.NotProguard class * {*;}
-keepclassmembers class * {
    @com.xindawn.droidusbsource.NotProguard <methods>;
}
-keep class com.xindawn.droidusbsource.MediaControlBrocastFactory {*;}
-keep class com.xindawn.droidusbsource.Logger {*;}
