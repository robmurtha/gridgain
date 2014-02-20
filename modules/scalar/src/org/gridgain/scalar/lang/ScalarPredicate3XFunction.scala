// @scala.file.header

/*
 * ________               ______                    ______   _______
 * __  ___/_____________ ____  /______ _________    __/__ \  __  __ \
 * _____ \ _  ___/_  __ `/__  / _  __ `/__  ___/    ____/ /  _  / / /
 * ____/ / / /__  / /_/ / _  /  / /_/ / _  /        _  __/___/ /_/ /
 * /____/  \___/  \__,_/  /_/   \__,_/  /_/         /____/_(_)____/
 *
 */

package org.gridgain.scalar.lang

import org.gridgain.grid.util.lang.{GridLambdaAdapter, GridPredicate3X}

/**
 * Wrapping Scala function for `GridPredicate3X`.
 *
 * @author @java.author
 * @version @java.version
 */
class ScalarPredicate3XFunction[T1, T2, T3](val inner: GridPredicate3X[T1, T2, T3]) extends GridLambdaAdapter
    with ((T1, T2, T3) => Boolean) {
    assert(inner != null)

    peerDeployLike(inner)

    /**
     * Delegates to passed in grid predicate.
     */
    def apply(t1: T1, t2: T2, t3: T3): Boolean = {
        inner.applyx(t1, t2, t3)
    }
}