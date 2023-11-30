package com.wokoworks.web3j;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DedupeQueue<T> {
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final int maxCapacity;
    private final List<Item> items;
    public DedupeQueue(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.items = new ArrayList<>(maxCapacity);
    }

    public void put(long value, T data) throws InterruptedException {
        lock.lock();
        try {
            if (items.size() <= 0) {
                Item item = new Item();
                item.setValue(value);
                item.setData(data);

                items.add(item);
                notEmpty.signal();
                return;
            }
            Item item = items.get(0);
            if (value <= item.getValue()) {
                return;
            }
            if (items.size() >= maxCapacity) {
                notFull.await();
            }

            item = new Item();
            item.setValue(value);
            item.setData(data);
            items.add(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T peek() throws InterruptedException {
        lock.lock();
        try {
            if (items.size() <= 0) {
                notEmpty.await();
            }
            return items.get(0).getData();
        } finally {
            lock.unlock();
        }
    }

    public boolean poll() {
        lock.lock();
        try {
            if (items.size() <= 0) {
                return false;
            }
            items.remove(0);
            notFull.signal();
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Data
    private class Item {
        private long value;
        private T data;
    }
}
