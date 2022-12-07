package com.dunn.instrument.oom;

import android.app.Application;

/**
 * @ClassName: Help
 * @Author: ZhuYiDian
 * @CreateDate: 2022/3/30 1:11
 * @Description:
 */
public class Help {
    /**
     * 需要检查application有没有在其它地方设置过 registerActivityLifecycleCallbacks  防止覆盖
     * 在application中调用
     * @param application
     */
    public static void initMemory(Application application){
        LeakCanary.init(application);
    }
}
