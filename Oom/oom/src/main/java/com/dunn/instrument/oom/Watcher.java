package com.dunn.instrument.oom;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public class Watcher {
    private static Watcher mInstance;
    private final ReferenceQueue queue;
    private Handler backgroundHandler;
    private final Set<String> retainedKeys;

    private Watcher() {
        queue = new ReferenceQueue();
        HandlerThread handlerThread = new HandlerThread("Watcher");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
        retainedKeys = new CopyOnWriteArraySet<>();
    }

    public static Watcher getInstance() {
        if (mInstance == null) {
            synchronized (Watcher.class) {
                if (mInstance == null) {
                    mInstance = new Watcher();
                }
            }
        }

        return mInstance;
    }

    public void addWatch(Object object) {
        // 其实我们需要用 WeakReference 把对象包裹起来
        String key = UUID.randomUUID().toString();
        // 这样写只能检测到当时的情况，5 个 Activity ，当一个一个返回的时候
        final KeyWeakReference weakReference = new KeyWeakReference(key, object, queue);
        retainedKeys.add(key);
        // 把这些对象存到一个集合

        // 用另外一个线程去检测
        backgroundHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkRelease(weakReference);
            }
        }, 5000);
    }

    private void checkRelease(KeyWeakReference weakReference) {
        removeWeaklyReferences();
        Log.e("TAG", "-->checkRelease");
        if (isRelease(weakReference)) {
            return;
        }

        // 调用 gc
        runGC();

        removeWeaklyReferences();

        if (!isRelease(weakReference)) {
            // 会打印 4 次，并不是有四个对象泄漏，用工具看其实只有一个
            Log.e("TAG", "-->有内存泄漏" + weakReference.get());
            // 泄漏的路径我们需要拿到，SecondActivity
            HeapAnalyzer heapAnalyzer = new HeapAnalyzer();
            try {
                heapAnalyzer.analysisLeak(weakReference.key);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeWeaklyReferences() {
        // 不一定准，某些手机这里永远都是空，多以要改造
        KeyWeakReference reference;
        while ((reference = (KeyWeakReference) queue.poll())!=null){
            retainedKeys.remove(reference.key);
        }
    }

    private void runGC() {
        Runtime.getRuntime().gc();
        System.runFinalization();
    }

    private boolean isRelease(KeyWeakReference weakReference) {
        return !retainedKeys.contains(weakReference.key);
    }
}
