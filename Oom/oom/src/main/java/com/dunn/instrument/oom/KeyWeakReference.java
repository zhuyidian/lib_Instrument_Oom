package com.dunn.instrument.oom;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class KeyWeakReference extends WeakReference<Object> {
    public String key;
    // 线下做监控上报，误报率肯定要降低，这种方案可能某些机型会有问题
    public KeyWeakReference(String key, Object referent, ReferenceQueue<? super Object> queue) {
        // queue 的作用就是当触发 gc 回收这个对象的时候会把该对象 push 到 queue
        super(referent, queue);
        this.key = key;
    }
}
