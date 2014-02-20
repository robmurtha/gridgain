// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.continuous;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Continuous routine handler.
 *
 * @author @java.author
 * @version @java.version
 */
public interface GridContinuousHandler extends Externalizable {
    /**
     * Registers listener.
     *
     * @param nodeId ID of the node that started routine.
     * @param routineId Routine ID.
     * @param ctx Kernal context.
     * @return Whether listener was actually registered.
     * @throws GridException In case of error.
     */
    public boolean register(UUID nodeId, UUID routineId, GridKernalContext ctx) throws GridException;

    /**
     * Callback called after listener is registered and acknowledgement is sent.
     *
     * @param routineId Routine ID.
     * @param ctx Kernal context.
     */
    public void onListenerRegistered(UUID routineId, GridKernalContext ctx);

    /**
     * Unregisters listener.
     *
     * @param routineId Routine ID.
     * @param ctx Kernal context.
     */
    public void unregister(UUID routineId, GridKernalContext ctx);

    /**
     * Notifies local callback.
     *
     * @param nodeId ID of the node where notification came from.
     * @param routineId Routine ID.
     * @param objs Notification objects.
     * @param ctx Kernal context.
     */
    public void notifyCallback(UUID nodeId, UUID routineId, Collection<?> objs, GridKernalContext ctx);

    /**
     * Deploys and marshals inner objects (called only if peer deployment is enabled).
     *
     * @param ctx Kernal context.
     * @throws GridException In case of error.
     */
    public void p2pMarshal(GridKernalContext ctx) throws GridException;

    /**
     * Unmarshals inner objects (called only if peer deployment is enabled).
     *
     * @param nodeId Sender node ID.
     * @param ctx Kernal context.
     * @throws GridException In case of error.
     */
    public void p2pUnmarshal(UUID nodeId, GridKernalContext ctx) throws GridException;

    /**
     * @return Topic for ordered notifications. If {@code null}, notifications
     * will be sent in non-ordered messages.
     */
    @Nullable public Object orderedTopic();
}