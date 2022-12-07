package com.dunn.instrument.oom.sample;

import android.app.Application;

import com.dunn.instrument.oom.Help;

/**
 * @ClassName: ApiOom
 * @Author: ZhuYiDian
 * @CreateDate: 2022/3/20 3:05
 * @Description:
 */
public class ApiOom {
    public static void oomInit(Application application){
        Help.initMemory(application);
    }
}
