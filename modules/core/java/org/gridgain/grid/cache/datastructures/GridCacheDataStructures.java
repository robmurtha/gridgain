// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.cache.datastructures;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.jetbrains.annotations.*;

/**
 * Convenient facade for cache data structures.
 *
 * @author @java.author
 * @version @java.version
 */
public interface GridCacheDataStructures {
    /**
     * Will get an atomic sequence from cache and create one if it has not been created yet and {@code create} flag
     * is {@code true}.
     *
     * @param name Sequence name.
     * @param initVal Initial value for sequence. If sequence already cached, {@code initVal} will be ignored.
     * @param create Boolean flag indicating whether data structure should be created if does not exist.
     * @return Sequence for the given name.
     * @throws GridException If sequence could not be fetched or created.
     */
    @Nullable public GridCacheAtomicSequence atomicSequence(String name, long initVal, boolean create)
        throws GridException;

    /**
     * Remove sequence from cache.
     *
     * @param name Sequence name.
     * @return {@code True} if sequence has been removed, {@code false} otherwise.
     * @throws GridException If remove failed.
     */
    public boolean removeAtomicSequence(String name) throws GridException;

    /**
     * Will get a atomic long from cache and create one if it has not been created yet and {@code create} flag
     * is {@code true}.
     *
     * @param name Name of atomic long.
     * @param initVal Initial value for atomic long. If atomic long already cached, {@code initVal}
     *        will be ignored.
     * @param create Boolean flag indicating whether data structure should be created if does not exist.
     * @return Atomic long.
     * @throws GridException If atomic long could not be fetched or created.
     */
    @Nullable public GridCacheAtomicLong atomicLong(String name, long initVal, boolean create) throws GridException;

    /**
     * Remove atomic long from cache.
     *
     * @param name Name of atomic long.
     * @return {@code True} if atomic long has been removed, {@code false} otherwise.
     * @throws GridException If removing failed.
     */
    public boolean removeAtomicLong(String name) throws GridException;

    /**
     * Will get a named queue from cache and create one if it has not been created yet and {@code create} flag
     * is {@code true}.
     * If queue is present in cache already, queue properties will not be changed. Use
     * collocation for {@link GridCacheMode#PARTITIONED} caches if you have lots of relatively
     * small queues as it will make fetching, querying, and iteration a lot faster. If you have
     * few very large queues, then you should consider turning off collocation as they simply
     * may not fit in a single node's memory. However note that in this case
     * to get a single element off the queue all nodes may have to be queried.
     *
     * @param name Name of queue.
     * @param type Type of queue.
     * @param cap Capacity of queue, {@code 0} for unbounded queue.
     * @param collocated If {@code true} then all items within the same queue will be collocated on the same node.
     *      Otherwise elements of the same queue maybe be cached on different nodes. If you have lots of relatively
     *      small queues, then you should use collocation. If you have few large queues, then you should turn off
     *      collocation. This parameter works only for {@link GridCacheMode#PARTITIONED} cache.
     * @param create Boolean flag indicating whether data structure should be created if does not exist.
     * @return Queue with given properties.
     * @throws GridException If remove failed.
     */
    @Nullable public <T> GridCacheQueue<T> queue(String name, GridCacheQueueType type, int cap, boolean collocated,
        boolean create) throws GridException;

    /**
     * Remove queue from cache. Internally one transaction will be created for all elements
     * in the queue. If you anticipate that queue may be large, then it's better to use
     * {@link #removeQueue(String, int)} which allows to specify batch size. In that case
     * transaction will be split into multiple transactions which will have upto {@code batchSize}
     * elements in it.
     *
     * @param name Name queue.
     * @return {@code True} if queue has been removed and false if it's not cached.
     * @throws GridException If remove failed.
     */
    public boolean removeQueue(String name) throws GridException;

    /**
     * Remove queue from cache. Internally multiple transactions will be created
     * with no more than {@code batchSize} elements in them. For larger queues, this
     * method is preferrable over {@link #removeQueue(String)} which will create only
     * one transaction for the whole operation.
     *
     * @param name Name queue.
     * @param batchSize Batch size.
     * @return {@code True} if queue has been removed and false if it's not cached.
     * @throws GridException If remove failed.
     */
    public boolean removeQueue(String name, int batchSize) throws GridException;

    /**
     * Will get a atomic reference from cache and create one if it has not been created yet and {@code create} flag
     * is {@code true}.
     *
     * @param name Atomic reference name.
     * @param initVal Initial value for atomic reference. If atomic reference already cached,
     *      {@code initVal} will be ignored.
     * @param create Boolean flag indicating whether data structure should be created if does not exist.
     * @return Atomic reference for the given name.
     * @throws GridException If atomic reference could not be fetched or created.
     */
    @Nullable public <T> GridCacheAtomicReference<T> atomicReference(String name, @Nullable T initVal, boolean create)
        throws GridException;

    /**
     * Remove atomic reference from cache.
     *
     * @param name Atomic reference name.
     * @return {@code True} if atomic reference has been removed, {@code false} otherwise.
     * @throws GridException If remove failed.
     */
    public boolean removeAtomicReference(String name) throws GridException;

    /**
     * Will get a atomic stamped from cache and create one if it has not been created yet and {@code create} flag
     * is {@code true}.
     *
     * @param name Atomic stamped name.
     * @param initVal Initial value for atomic stamped. If atomic stamped already cached,
     *      {@code initVal} will be ignored.
     * @param initStamp Initial stamp for atomic stamped. If atomic stamped already cached,
     *      {@code initStamp} will be ignored.
     * @param create Boolean flag indicating whether data structure should be created if does not exist.
     * @return Atomic stamped for the given name.
     * @throws GridException If atomic stamped could not be fetched or created.
     */
    @Nullable public <T, S> GridCacheAtomicStamped<T, S> atomicStamped(String name, @Nullable T initVal,
        @Nullable S initStamp, boolean create) throws GridException;

    /**
     * Remove atomic stamped from cache.
     *
     * @param name Atomic stamped name.
     * @return {@code True} if atomic stamped has been removed, {@code false} otherwise.
     * @throws GridException If remove failed.
     */
    public boolean removeAtomicStamped(String name) throws GridException;

    /**
     * Gets or creates count down latch. If count down latch is not found in cache and {@code create} flag
     * is {@code true}, it is created using provided name and count parameter.
     *
     * @param name Name of the latch.
     * @param cnt Count for new latch creation.
     * @param autoDel {@code True} to automatically delete latch from cache
     *      when its count reaches zero.
     * @param create Boolean flag indicating whether data structure should be created if does not exist.
     * @return Count down latch for the given name.
     * @throws GridException If operation failed.
     */
    @Nullable public GridCacheCountDownLatch countDownLatch(String name, int cnt, boolean autoDel, boolean create)
        throws GridException;

    /**
     * Removes count down latch from cache.
     *
     * @param name Name of the latch.
     * @return Count down latch for the given name.
     * @throws GridException If operation failed.
     */
    public boolean removeCountDownLatch(String name) throws GridException;
}