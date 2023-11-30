package com.wokoworks.web3j;

import javax.sound.midi.Soundbank;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ThreadPool {
    private static List<String> list = new CopyOnWriteArrayList<>();
    private static ThreadPoolExecutor pool;
    public static void main( String[] args ) throws InterruptedException {

        SynchronousQueue queue = new SynchronousQueue();
        queue.take();

        if (list != null) {
            return;
        }


        //实现自定义接口
        pool = new ThreadPoolExecutor(2, 4, 3000, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
            public Thread newThread(Runnable r) {
                //System.out.println("线程"+r.hashCode()+"创建");
                //线程命名
                Thread th = new Thread(r,"threadPool"+r.hashCode());
                return th;
            }
        }, new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                System.out.println("拒绝"+((ThreadTask)r).getTaskName());
            }
        }) {

//            protected void beforeExecute(Thread t,Runnable r) {
//                System.out.println("准备执行："+ ((ThreadTask)r).getTaskName());
//            }

            protected void afterExecute(Runnable r,Throwable t) {
                System.out.println("执行完毕："+((ThreadTask)r).getTaskName());
                //list.add(((ThreadTask)r).getTaskName());
                //pool.execute(new ThreadTask("luobing"+((ThreadTask)r).getTaskName()));
            }

            protected void terminated() {
                System.out.println("线程池退出");
            }
        };
          
        for(int i=0;i<50;i++) {
            pool.execute(new ThreadTask("Task"+i));
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(pool.getActiveCount()+"\t"+pool.getQueue().size()+"\t"+pool.getPoolSize());
//                    if (list.size()>0) {
//                        //System.out.println("list.get(0)="+list.get(0));
////                        try {
////                            Thread.sleep(5000);
////                        } catch (InterruptedException e) {
////                            e.printStackTrace();
////                        }
//                        pool.execute(new ThreadTask("luobing"+list.get(0)));
//                        list.remove(0);
//                    }
                }
            }
        }).start();


//        Thread.sleep(3000);
//        pool.execute(new ThreadTask("luobing"));

        //pool.shutdown();



        Thread.sleep(9999999);
    }

    public static class ThreadTask implements Runnable{
        private String taskName;
        public String getTaskName() {
            return taskName;
        }
        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }
        public ThreadTask(String name) {
            this.setTaskName(name);
        }
        public void run() {
            //输出执行线程的名称
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("TaskName"+this.getTaskName()+"---ThreadName:"+Thread.currentThread().getName());
        }
    }
}

