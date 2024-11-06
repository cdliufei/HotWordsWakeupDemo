package com.example.hotwordswakeupdemo

import com.anysou.aslogger.ASLogApplication
import com.anysou.aslogger.ASLogIConfig

class WakeupApplication : ASLogApplication() {
    override fun onCreate() {
        super.onCreate()

        ASLogIConfig.getInstance()
            .setShowLog(true)     //是否在logcat中显示log,默认不显示。
            .setWriteLog(true)    //是否在文件中记录，默认不记录。
            .setFileSize(100000)  //日志文件的大小，默认0.1M,以bytes为单位。
            .setSQLLen(100)       //设置SQLite数据库的数据最大条数。默认100条。
            .setSaveSQL(true)     //设置为file文本存储记录方式，true=SQLite数据库。
            .setAutoUpdate(true)  //设置为逐步替换更新模式。（file文本存储记录方式才有效）
            .setTag("myTag");     //logcat日志过滤tag。
    }
}