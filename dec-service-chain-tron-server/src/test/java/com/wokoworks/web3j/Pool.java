package com.wokoworks.web3j;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Pool {

    public static ThreadPoolExecutor create(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(3);
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, queue, (r, executor) -> {
            try {
                workQueue.put(r);
            } catch (InterruptedException e) {
                throw new RejectedExecutionException();
            }
        }) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final Runnable task = workQueue.poll();
                if (task != null) {
                    final boolean add = queue.offer(task);
                    if (!add) {
                        System.out.println("=======================异常");
                    }
                } else {
                    System.out.println("=======================");
                }
            }
        };
    }

    public static void main(String[] args) throws InterruptedException {
//        final ThreadPoolExecutor threadPoolExecutor = Pool.create(1, 3, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5));
//
//        for (int i = 0; i < 15; i++) {
//            final int num = i;
//            threadPoolExecutor.execute(() -> {
//                System.out.println(Thread.currentThread().getName() + " num is:" + num);
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            });
//            System.out.println(i);
//        }
//
//        TimeUnit.SECONDS.sleep(200000000);
        Lock lockProducer = new ReentrantLock();
        Condition producerCondition = lockProducer.newCondition();
        lockProducer.lock();
        try {
            producerCondition.signal();
        } finally {
            lockProducer.unlock();
        }
        System.out.println("ddd");
    }
}
