/*
 * Copyright (c) 2015 Max Gaukler <development@maxgaukler.de>
 */
package com.t_oster.visicam;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * cache a time-intensive computation
 *
 * @param <CacheType> datatype for internal cache (result of computation)
 * @param <ResultType> return datatype for getFreshResultBlocking() and
 * getCachedResult()
 */
public class CachingAsynchronousHandler<CacheType, ResultType> {
    final Object lockThread;
    final Object lockCache;
    CacheType cache;
    Thread thread;
    Computation<CacheType, ResultType> computation;

    /**
     * @param computeCacheEntry computation object (wrapper around functions that do the work)
     */
    public CachingAsynchronousHandler(Computation<CacheType, ResultType> computeCacheEntry) {
        this.lockThread = new Object();
        this.lockCache = new Object();
        this.cache = null;
        this.thread = null;
        this.computation = computeCacheEntry;
    }
    
    /**
     * Interface for a function that returns a value.
     * Similar to Runnable, but it can return a value
     * and may be called multiple times.
     *
     * @param <CacheType> type for cache entry
     * @param <ResultType> type for final result (often same as cache entry)
     */
    public interface Computation<CacheType, ResultType> {

        /**
         * compute cache entry (usually time-intensive)
         *
         * @return result
         */
        abstract public CacheType computeCacheEntry();
        
        /**
        * create a result object from the cache (usually fast).
        * You can just 'return cache;' here,
        * but in some cases you might want to call copy constructors or something
        * similar so that the cache entry is not modified by users of the returned object.
        *
        * @param cache cached result of computeCacheEntry()
        * @return result object
        */
       abstract public ResultType getCachedResult(CacheType cache);
    }
    /**
     * start a new computation it is not currently running
     *
     * @return thread object of the running (or newly started) thread
     */
    private Thread startOrGetRunningThread() {
        Thread currentThread;
        synchronized (lockThread) {
            if (thread == null || !thread.isAlive()) {
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // fetch response, overwrite cache
                        // overwriting reference variables is atomic, so no need for synchronisation here
                        // this variable is only written by this thread, which has at most one running instance
                        // it is read concurrently. If someone reads the old cache (which should not happen), nothing bad will occur.
                        cache = computation.computeCacheEntry();
                    }
                });
                thread.start();
            }
            currentThread = thread;
        }
        return currentThread;
    }

    /**
     * start a new computation it is not currently running
     */
    public void renew() {
        startOrGetRunningThread();
    }
    
    /**
     * start a new computation it is not currently running, wait until a new result is finished
     */
    public void renewBlocking() {
        try {
            startOrGetRunningThread().join();
        } catch (InterruptedException ex) {
            Logger.getLogger(CachingAsynchronousHandler.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    /**
     * start a new computation, if it is not already running, wait for the result and return it.
     * This method is blocking.
     * @return cached result that is up-to-date (computation has just finished)
     */
    public ResultType getFreshResultBlocking() {
        try {
            // wait until fetching is finished
            startOrGetRunningThread().join();
        } catch (InterruptedException ex) {
            // this must not happen
            Logger.getLogger(VisiCamServer.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        return getCachedResult();
    }
    
    /**
     * return cached result immediately. No new computation will be started.
     * @return cached result that may be extremely outdated or even null, 
     */
    public ResultType getCachedResult() {
        if (cache == null) {
            return null;
        }
        return computation.getCachedResult(cache);
    }
}
