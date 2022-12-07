package com.dunn.instrument.oom;

import android.os.Debug;
import android.os.Environment;
import android.util.Log;

import com.android.tools.perflib.captures.DataBuffer;
import com.android.tools.perflib.captures.MemoryMappedFileBuffer;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.RootObj;
import com.squareup.haha.perflib.Snapshot;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import gnu.trove.THashMap;

public class HeapAnalyzer {
    private LinkedHashSet<Instance> toVisitSet;

    public void analysisLeak(String key) throws IOException {
        // 怎么样找到泄漏链
        // 1. dump 生成一个 hpro 文件
        File heapDumpFile = new File(Environment.getExternalStorageDirectory(), "test.hprof");
        // 这个文件一般会很大，可能 200M 左右，一般会涉及到裁剪
        // 怎么裁剪？需要知道协议和内容
        // 方案：1. 获取到文件后再进行裁剪
        // 方案：2. ptl hook 住文件的 write open 直接进行过滤 （推荐，难度）
        Debug.dumpHprofData(heapDumpFile.getAbsolutePath());
        // 2. 分析 hpro 文件（自动）
        // 2.1. 解析快照：协议：head + body
        // ---------- head -----------
        // 1. 版本号：读字符串 \0 结尾
        // 2. idSize : 4 字节
        // 3. timestamp：8 字节 long 类型
        // ---------- body -----------
        // 1. tag：类型 1
        // 2. timestamp：高位时间 4
        // 3. length：长度 4
        // 4. body：内容
        /*
        读取文件：按照大文件的方式去读
        // ---------- body -----------
        int tag = 读取 1 字节
        读取 4 字节
        读取 length 的长度
        switch(tag){
            case 0x01：
                读取字符串（length）
            break;
            case 0x02：
            break;
            ...
            case 0x0C:
            case 0x1C:
                读取堆信息
            break;
        }
        */
        DataBuffer buffer = new MemoryMappedFileBuffer(heapDumpFile);
        Snapshot snapshot = Snapshot.createSnapshot(buffer);
        // 2.2. 收集 gc root 节点，gc root 总共有多少个？ 很多对象都可以被当作 gc root
        // ClassLoader 可以
        // 142135
        Log.e("TAG","--> "+snapshot.getGCRoots().size());
        deduplicateGcRoots(snapshot);
        Log.e("TAG","--> "+snapshot.getGCRoots().size());
        // 根据 key 去找到泄漏的节点
        Instance leakInstance = findLeakingReference(snapshot, key);
        if(leakInstance != null){
            Log.e("TAG","找到了泄漏的 Instance --> "+leakInstance);
        }
        // 2.3. 从 gc root 节点开始做一个广度遍历（算法）
        // 我先把伪代码
        // top 100 大小链路输出，判断图片有没有重复？
        findLeakTrace(snapshot, leakInstance);
    }

    private void findLeakTrace(Snapshot snapshot, Instance leakInstance) {
        // 2.3 广度遍历要用到什么？递归，leetcode easy
        // 栈，堆，队列？
        // 2.3.1 所有的 gc 入
        for (RootObj gcRoot : snapshot.getGCRoots()) {
            enqueue(null, gcRoot, null);
        }

        // 2.3.2 遍历所有的 gc root 作业
        // top 100 大小链路输出，判断图片有没有重复？
        // 看一看 leakcannary 的源码
    }

    private void enqueue(Object o, RootObj root, Object o1) {
        if(toVisitSet.contains(root)){
            return;
        }
        toVisitSet.add(root);
    }

    private Instance findLeakingReference(Snapshot snapshot, String key) {
        // 需要熟悉 api
        ClassObj refClass = snapshot.findClass(KeyWeakReference.class.getName());
        if(refClass == null){
            return null;
        }
        for (Instance instance : refClass.getInstancesList()) {
            // 获取 instance 所有的 FieldValue
            List<ClassInstance.FieldValue> values = HaHaHelper.classInstanceValues(instance);
            Object keyFiledValue = HaHaHelper.filedValue(values, "key");

            String keyCandidate = HaHaHelper.asString(keyFiledValue);
            if(keyCandidate.equals(key)){
                return instance;
            }
        }

        return null;
    }

    private void deduplicateGcRoots(Snapshot snapshot) {
        THashMap<String, RootObj> uniqueRootMap = new THashMap<>();
        Collection<RootObj> gcRoots = snapshot.getGCRoots();
        for (RootObj gcRoot : gcRoots) {
            String key = generateRootKey(gcRoot);
            if(!uniqueRootMap.containsKey(key)){
                uniqueRootMap.put(key, gcRoot);
            }
        }

        gcRoots.clear();
//        uniqueRootMap.forEach(new TObjectProcedure<String>() {
//            @Override
//            public boolean execute(String s) {
//                return gcRoots.add(uniqueRootMap.get(s));
//            }
//        });
        Iterator<Map.Entry<String, RootObj>> it = uniqueRootMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, RootObj> entry = it.next();
            System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
            gcRoots.add(entry.getValue());
        }

//        uniqueRootMap.forEach(key -> gcRoots.add(uniqueRootMap.get(key)));
    }

    private String generateRootKey(RootObj root) {
        return String.format("%s@0x%08x", root.getRootType().getName(),root.getId());
    }
}
