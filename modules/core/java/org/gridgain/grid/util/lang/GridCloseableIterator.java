/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.util.lang;

import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;

/**
 * Defines "rich" closeable iterator interface that is also acts like lambda function and iterable.
 * <p>
 * Any instance of this interface should be closed when it's no longer needed.
 * <p>
 * Here is the common use of closable iterator.
 * <pre>
 * GridCloseableIterator<T> iter = getIterator();
 *
 * try {
 *     while(iter.hasNext()) {
 *         T next = iter.next();
 *         ...
 *         ...
 *     }
 * }
 * finally {
 *     iter.close();
 * }
 * </pre>
 *
 * @author @java.author
 * @version @java.version
 */
public interface GridCloseableIterator<T> extends GridIterator<T>, GridSpiCloseableIterator<T>, AutoCloseable {
    /**
     * Closes the iterator and frees all the resources held by the iterator.
     * Iterator can not be used any more after calling this method.
     * <p>
     * The method is invoked automatically on objects managed by the
     * {@code try-with-resources} statement.
     *
     * @throws GridException In case of error.
     */
    @Override public void close() throws GridException;

    /**
     * Checks if iterator has been closed.
     *
     * @return {@code True} if iterator has been closed.
     */
    public boolean isClosed();
}
