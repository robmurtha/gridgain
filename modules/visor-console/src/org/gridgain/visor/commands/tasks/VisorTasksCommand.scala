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

/*
 * ___    _________________________ ________
 * __ |  / /____  _/__  ___/__  __ \___  __ \
 * __ | / /  __  /  _____ \ _  / / /__  /_/ /
 * __ |/ /  __/ /   ____/ / / /_/ / _  _, _/
 * _____/   /___/   /____/  \____/  /_/ |_|
 *
 */

package org.gridgain.visor.commands.tasks

import java.util.UUID
import org.gridgain.grid._
import org.gridgain.grid.events._
import GridEventType._
import events._
import org.gridgain.grid.util.typedef.X
import org.gridgain.grid.lang.GridPredicate
import org.gridgain.grid.kernal.processors.task.GridInternal
import org.gridgain.grid.util.{GridUtils => U}
import org.gridgain.visor.commands.{VisorConsoleCommand, VisorTextTable}
import org.gridgain.visor._
import visor._
import collection.immutable._
import collection.JavaConversions._
import scala.util.control.Breaks._
import org.gridgain.visor.commands.{VisorConsoleUtils => CU}

/**
 * Task execution state.
 *
 * @author @java.author
 * @version @java.version
 */
private object State extends Enumeration {
    /** Type shortcut. */
    type State = Value

    val STARTED = Value("Started")
    val FINISHED = Value("Finished")
    val TIMEDOUT = Value("Timed out")
    val FAILED = Value("Failed")
    val UNDEFINED = Value("<undefined>")
}

import org.gridgain.visor.commands.tasks.State._

/**
 * Task execution data.
 *
 * @author @java.author
 * @version @java.version
 */
private case class Execution(
    id: GridUuid,
    taskName: String,
    var evts: List[GridEvent] = Nil,
    var nodeIds: Set[UUID] = Set.empty[UUID],
    var failedNodeIds: Set[UUID] = Set.empty[UUID],
    var startTs: Long = Long.MaxValue,
    var endTs: Long = Long.MinValue,
    var state: State = UNDEFINED,
    var origNodeId: UUID = null
) {
    assert(id != null)
    assert(taskName != null)

    /**
     * ID8 form of task execution ID.
     */
    lazy val id8: String =
        U.id8(id)

    /**
     * ID8 of the task execution + its associated variable.
     */
    lazy val id8Var: String =
        id8 + "(@" + setVarIfAbsent(id, "e") + ')'

    /**
     * Task host + its associated variable host.
     */
    lazy val taskNameVar: String =
        taskName + "(@" + setVarIfAbsent(taskName, "t") + ')'

    /**
     * Gets number of job rejections in this session.
     */
    lazy val rejections: Int =
        evts.count(_.`type` == EVT_JOB_REJECTED)

    /**
     * Gets number of job cancellations in this session.
     */
    lazy val cancels: Int =
        evts.count(_.`type` == EVT_JOB_CANCELLED)

    /**
     * Gets number of job finished in this session.
     */
    lazy val finished: Int =
        evts.count(_.`type` == EVT_JOB_FINISHED)

    /**
     * Gets number of job started in this session.
     */
    lazy val started: Int =
        evts.count(_.`type` == EVT_JOB_STARTED)

    /**
     * Gets number of job failures in this session.
     */
    lazy val failures: Int =
        evts.count(_.`type` == EVT_JOB_FAILED)

    /**
     * Gets number of job failovers in this session.
     */
    lazy val failovers: Int =
        evts.count(_.`type` == EVT_JOB_FAILED_OVER)

    /**
     * Gets duration of the session.
     */
    lazy val duration: Long =
        endTs - startTs

    override def equals(r: Any) =
        if (this eq r.asInstanceOf[AnyRef])
            true
        else if (r == null || !r.isInstanceOf[Execution])
            false
        else
            r.asInstanceOf[Execution].id == id

    override def hashCode() =
        id.hashCode()
}

/**
 * Task data.
 *
 * @author @java.author
 * @version @java.version
 */
private case class Task(
    taskName: String,
    var execs: Set[Execution] = Set.empty[Execution]
) {
    /**
     * Task host + its associated variable host.
     */
    lazy val taskNameVar: String =
        taskName + "(@" + setVarIfAbsent(taskName, "t") + ')'

    /**
     * Oldest timestamp for this task (oldest event related to this task).
     */
    lazy val oldest: Long =
        execs.min(Ordering.by[Execution, Long](_.startTs)).startTs

    /**
     * Latest timestamp for this task (latest event related to this task).
     */
    lazy val latest: Long =
        execs.max(Ordering.by[Execution, Long](_.endTs)).endTs

    /**
     * Gets timeframe of this task executions.
     */
    lazy val timeframe: Long =
        latest - oldest

    /**
     * Total number of execution for this task.
     */
    lazy val totalExecs: Int =
        execs.size

    /**
     * Gets number of execution with given state.
     *
     * @param state Execution state to filter by.
     */
    def execsFor(state: State): Int =
        execs.count(_.state == state)

    /**
     * Minimal duration of this task execution.
     */
    lazy val minDuration: Long =
        execs.min(Ordering.by[Execution, Long](_.duration)).duration

    /**
     * Maximum duration of this task execution.
     */
    lazy val maxDuration: Long =
        execs.max(Ordering.by[Execution, Long](_.duration)).duration

    /**
     * Average duration of this task execution.
     */
    lazy val avgDuration: Long =
        (0L /: execs)((b, a) => a.duration + b) / execs.size

    /**
     * Minimal number of nodes this task was executed on.
     */
    lazy val minNodes: Int =
        execs.min(Ordering.by[Execution, Int](_.nodeIds.size)).nodeIds.size

    /**
     * Minimal number of nodes this task was executed on.
     */
    lazy val maxNodes: Int =
        execs.max(Ordering.by[Execution, Int](_.nodeIds.size)).nodeIds.size

    /**
     * Average number of nodes this task was executed on.
     */
    lazy val avgNodes: Int =
        (0 /: execs)((b, a) => a.nodeIds.size + b) / execs.size

    override def equals(r: Any) =
        if (this eq r.asInstanceOf[AnyRef])
            true
        else if (r == null || !r.isInstanceOf[Task])
            false
        else
            r.asInstanceOf[Task].taskName == taskName

    override def hashCode() =
        taskName.hashCode()
}

@GridInternal
class VisorContainsFilter(n: Long, s: String) extends GridPredicate[GridEvent] {
    override def apply(e: GridEvent): Boolean = {
        (e.timestamp >= System.currentTimeMillis - n) && (e match {
            case te: GridTaskEvent => !CU.containsInTaskName(te.taskName(), te.taskClassName(), s)
            case je: GridJobEvent => !CU.containsInTaskName(je.taskName(), je.taskClassName(), s)
            case _ => false
        })
    }
}

@GridInternal
class VisorTaskNameFilter(s: String) extends GridPredicate[GridEvent] {
    override def apply(e: GridEvent): Boolean = {
        e match {
            case te: GridTaskEvent => te.taskName() == s
            case je: GridJobEvent => je.taskName() == s
            case _ => false
        }
    }
}

@GridInternal
class VisorSessionIdFilter(u: GridUuid) extends GridPredicate[GridEvent] {
    override def apply(e: GridEvent): Boolean = {
        e match {
            case te: GridTaskEvent => te.taskSessionId() == u
            case je: GridJobEvent => je.taskSessionId() == u
            case _ => false
        }
    }
}

/**
 * ==Overview==
 * Visor 'tasks' command implementation.
 *
 * ==Importing==
 * When using this command from Scala code (not from REPL) you need to make sure to
 * properly import all necessary typed and implicit conversions:
 * <ex>
 * import org.gridgain.visor._
 * import commands.tasks.VisorTasksCommand._
 * </ex>
 * Note that `VisorTasksCommand` object contains necessary implicit conversions so that
 * this command would be available via `visor` keyword.
 *
 * ==Help==
 * {{{
 * +---------------------------------------------------------------------------------------+
 * | tasks | Prints statistics about tasks and executions.                                 |
 * |       |                                                                               |
 * |       | Note that this command depends on GridGain events.                            |
 * |       |                                                                               |
 * |       | GridGain events can be individually enabled and disabled and disabled events  |
 * |       | can affect the results produced by this command. Note also that configuration |
 * |       | of Event Storage SPI that is responsible for temporary storage of generated   |
 * |       | events on each node can also affect the functionality of this command.        |
 * |       |                                                                               |
 * |       | By default - all events are enabled and GridGain stores last 10,000 local     |
 * |       | events on each node. Both of these defaults can be changed in configuration.  |
 * +---------------------------------------------------------------------------------------+
 * }}}
 *
 * ====Specification====
 * {{{
 *     visor tasks
 *     visor tasks "-l {-t=<num>s|m|h|d} {-r}"
 *     visor tasks "-s=<substring> {-t=<num>s|m|h|d} {-r}"
 *     visor tasks "-g {-t=<num>s|m|h|d} {-r}"
 *     visor tasks "-h {-t=<num>s|m|h|d} {-r}"
 *     visor tasks "-n=<task-name> {-r}"
 *     visor tasks "-e=<exec-id>"
 * }}}
 *
 * ====Arguments====
 * {{{
 *     -l
 *         List all tasks and executions for a given time period.
 *         Executions sorted chronologically (see '-r'), and tasks alphabetically
 *         See '-t=<num>s|m|h|d' on how to specify the time limit.
 *         Default time period is one hour, i.e '-t=1h'
 *
 *         This is a default mode when command is called without parameters.
 *     -s=<substring>
 *         List all tasks and executions for a given task name substring.
 *         Executions sorted chronologically (see '-r'), and tasks alphabetically
 *         See '-t=<num>s|m|h|d' on how to specify the time limit.
 *         Default time period is one hour, i.e '-t=1h'
 *     -g
 *         List all tasks grouped by nodes for a given time period.
 *         Tasks sorted alphabetically
 *         See '-t=<num>s|m|h|d' on how to specify the time limit.
 *         Default time period is one hour, i.e '-t=1h'
 *     -h
 *         List all tasks grouped by hosts for a given time period.
 *         Tasks sorted alphabetically
 *         See '-t=<num>s|m|h|d' on how to specify the time limit.
 *         Default time period is one hour, i.e '-t=1h'
 *     -t=<num>s|m|h|d
 *         Defines time frame for '-l' parameter:
 *            =<num>s Last <num> seconds.
 *            =<num>m Last <num> minutes.
 *            =<num>h Last <num> hours.
 *            =<num>d Last <num> days.
 *     -r
 *         Reverse sorting of executions.
 *     -n=<task-name>
 *         Prints aggregated statistic for named task.
 *     -e=<exec-id>
 *         Prints aggregated statistic for given task execution.
 * }}}
 *
 * ====Examples====
 * {{{
 *     visor tasks "-l"
 *         Prints list of all tasks and executions for the last hour (default).
 *     visor tasks
 *         Prints list of all tasks and executions for the last hour (default).
 *     visor tasks "-l -t=5m"
 *         Prints list of tasks and executions that started during last 5 minutes.
 *     visor tasks "-s=Task"
 *         Prints list of all tasks and executions that have 'Task' in task name.
 *     visor tasks "-g"
 *         Prints list of tasks grouped by nodes.
 *     visor tasks "-g -t=5m"
 *         Prints list of tasks that started during last 5 minutes grouped by nodes.
 *     visor tasks "-h"
 *         Prints list of tasks grouped by hosts.
 *     visor tasks "-h -t=5m"
 *         Prints list of tasks that started during last 5 minutes grouped by hosts.
 *     visor tasks "-n=GridTask"
 *         Prints summary for task named 'GridTask'.
 *     visor tasks "-e=7D5CB773-225C-4165-8162-3BB67337894B"
 *         Traces task execution with ID '7D5CB773-225C-4165-8162-3BB67337894B'.
 *     visor tasks "-e=@s1"
 *         Traces task execution with ID taken from 's1' memory variable.
 * }}}
 *
 * @author @java.author
 * @version @java.version
 */
class VisorTasksCommand {
    /** Limit for printing tasks and executions. */
    private val SHOW_LIMIT = 100

    /**
     * Prints error message and advise.
     *
     * @param errMsgs Error messages.
     */
    private def scold(errMsgs: Any*) {
        assert(errMsgs != null)

        warn(errMsgs: _*)
        warn("Type 'help tasks' to see how to use this command.")
    }

    /**
     * ===Command===
     * Prints statistics about executed tasks.
     *
     * ===Examples===
     * <ex>tasks</ex>
     * Prints list of all tasks for the last hour (default).
     *
     * <ex>tasks "-l"</ex>
     * Prints list of all tasks for the last hour (default).
     *
     * <ex>tasks "-l -a"</ex>
     * Prints list of all tasks and executions for the last hour (default).
     *
     * <ex>tasks "-l -t=5m"</ex>
     * Prints list of tasks that started during last 5 minutes.
     *
     * <ex>tasks "-g"</ex>
     * Prints list of tasks grouped by nodes.
     *
     * <ex>tasks "-g -t=5m"</ex>
     * Prints list of tasks that started during last 5 minutes grouped by nodes.
     *
     * <ex>tasks "-h"</ex>
     * Prints list of tasks grouped by hosts.
     *
     * <ex>tasks "-h -t=5m"</ex>
     * Prints list of tasks that started during last 5 minutes grouped by hosts.
     *
     * <ex>tasks "-n=GridTask"</ex>
     * Prints summary for task named 'GridTask'.
     *
     * <ex>tasks "-n=GridTask -a"</ex>
     * Prints summary and executions for task named 'GridTask'.
     *
     * <ex>tasks "-e=7D5CB773-225C-4165-8162-3BB67337894B"</ex>
     * Traces task execution with ID '7D5CB773-225C-4165-8162-3BB67337894B'.
     *
     * <ex>tasks "-e=@s1"</ex>
     * Traces task execution with ID taken from 's1' memory variable.
     *
     * @param args Command arguments.
     */
    def tasks(args: String) {
        if (!isConnected)
            adviseToConnect()
        else {
            val argLst = parseArgs(args)

            if (hasArgFlag("l", argLst)) {
                val p = timePeriod(argValue("t", argLst) getOrElse "1h")

                if (p.isDefined)
                    list(p.get, "visor", hasArgFlag("r", argLst), hasArgFlag("a", argLst))
            }
            else if (hasArgName("s", argLst))  {
                val tf = timePeriod(argValue("t", argLst) getOrElse "1h")

                if (tf.isDefined) {
                    val s = argValue("s", argLst)

                    if (s.isDefined) {
                        list(tf.get, s.get, hasArgFlag("r", argLst), hasArgFlag("a", argLst))
                    }
                    else
                        scold("Invalid arguments: " + args)
                }
            }
            else if (hasArgFlag("g", argLst)) {
                val f = timePeriod(argValue("t", argLst) getOrElse "1h")

                if (f.isDefined)
                    nodes(f.get)
            }
            else if (hasArgFlag("h", argLst)) {
                val f = timePeriod(argValue("t", argLst) getOrElse "1h")

                if (f.isDefined)
                    hosts(f.get)
            }
            else if (hasArgName("n", argLst)) {
                val n = argValue("n", argLst)

                if (!n.isDefined)
                    scold("Invalid arguments: " + args)
                else
                    task(n.get, hasArgFlag("r", argLst), hasArgFlag("a", argLst))
            }
            else if (hasArgName("e", argLst)) {
                val s = argValue("e", argLst)

                if (!s.isDefined)
                    scold("Invalid arguments: " + args)
                else
                    exec(s.get, hasArgFlag("r", argLst))
            }
            else
                scold("Incorrect arguments: " + args)
        }
    }

    /**
     * ===Command===
     * Simply a shortcut for `tasks("-l")`
     *
     * ===Examples===
     * <ex>tasks</ex>
     * Prints list of all tasks and executions for the last hour (default).
     */
    def tasks() {
        tasks("-l")
    }

    /**
     * Creates predicate that filters events by timestamp.
     *
     * @param arg Command argument.
     * @return time period.
     */
    private def timePeriod(arg: String): Option[Long] = {
        assert(arg != null)

        var n = 0

        try
            n = arg.substring(0, arg.length - 1).toInt
        catch {
            case e: NumberFormatException =>
                scold("Time frame size is not numeric in: " + arg)

                return None
        }

        if (n <= 0) {
            scold("Time frame size is not positive in: " + arg)

            None
        }
        else {
            val m = arg.last match {
                case 's' => 1000
                case 'm' => 1000 * 60
                case 'h' => 1000 * 60 * 60
                case 'd' => 1000 * 60 * 60 * 24
                case _ =>
                    scold("Invalid time frame suffix in: " + arg)

                    return None
            }

            Some(n * m)
        }
    }

    /**
     * Gets collections of tasks and executions for given event filter.
     *
     * @param f Event filter.
     */
    private def mkData(f: GridPredicate[GridEvent]): (List[Task], List[Execution]) = {
        assert(f != null)

        var evts = grid.events().localQuery(f).toList

        val remote = grid.forRemotes()

        if (remote.nodes().size() > 0)
            evts ++= remote.events().remoteQuery(f, 0).get.toList

        var sMap = Map.empty[GridUuid, Execution] // Execution map.
        var tMap = Map.empty[String, Task] // Task map.

        def getSession(id: GridUuid, taskName: String): Execution = {
            assert(id != null)
            assert(taskName != null)

            sMap.get(id) getOrElse {
                val s = Execution(id, taskName)

                sMap = sMap + (id -> s)

                s
            }
        }

        def getTask(taskName: String): Task = {
            assert(taskName != null)

            tMap.get(taskName) getOrElse {
                val t = Task(taskName)

                tMap = tMap + (taskName -> t)

                t
            }
        }

        evts foreach {
            case te: GridTaskEvent =>
                val s = getSession(te.taskSessionId(), te.taskName())
                val t = getTask(te.taskName())

                t.execs = t.execs + s

                s.evts = s.evts :+ te
                s.nodeIds = s.nodeIds + te.nodeId()
                s.startTs = math.min(s.startTs, te.timestamp())
                s.endTs = math.max(s.endTs, te.timestamp())

                te.`type` match {
                    case EVT_TASK_STARTED =>
                        if (s.state == UNDEFINED) s.state = STARTED

                        s.origNodeId = te.nodeId()

                    case EVT_TASK_FINISHED =>
                        if (s.state == UNDEFINED || s.state == STARTED) s.state = FINISHED

                        s.origNodeId = te.nodeId()

                    case EVT_TASK_FAILED => if (s.state == UNDEFINED || s.state == STARTED) s.state = FAILED
                    case EVT_TASK_TIMEDOUT => if (s.state == UNDEFINED || s.state == STARTED) s.state = TIMEDOUT
                    case _ =>
                }

            case je: GridJobEvent =>
                val s = getSession(je.taskSessionId(), je.taskName())
                val t = getTask(je.taskName())

                t.execs = t.execs + s

                // Collect node IDs where jobs didn't finish ok.
                je.`type` match {
                    case EVT_JOB_CANCELLED | EVT_JOB_FAILED | EVT_JOB_REJECTED | EVT_JOB_TIMEDOUT =>
                        s.failedNodeIds = s.failedNodeIds + je.nodeId
                    case _ =>
                }

                s.evts = s.evts :+ je
                s.nodeIds = s.nodeIds + je.nodeId()
                s.startTs = math.min(s.startTs, je.timestamp())
                s.endTs = math.max(s.endTs, je.timestamp())
        }

        tMap.values.toList -> sMap.values.toList
    }

    /**
     * Prints list of tasks and executions.
     *
     * @param p Event period.
     * @param reverse Reverse session chronological sorting?
     * @param all Whether to show full information.
     */
    private def list(p: Long, s: String, reverse: Boolean, all: Boolean) {
        breakable {
            try {
                val (tLst, eLst) = mkData(new VisorContainsFilter(p, "visor"))

                if (tLst.isEmpty) {
                    scold("No tasks or executions found.")

                    break()
                }

                if (all) {
                    val execsT = VisorTextTable()

                    execsT.maxCellWidth = 35

                    execsT #=(
                        (
                            "ID8(@ID), Start/End,",
                            "State & Duration"
                            ),
                        "Task Name(@)",
                        "Nodes IP, ID8(@)",
                        "Jobs"
                        )

                    var sortedExecs = if (!reverse) eLst.sortBy(_.startTs).reverse else eLst.sortBy(_.startTs)

                    if (sortedExecs.size > SHOW_LIMIT) {
                        warnLimit(sortedExecs.size, "execution")

                        sortedExecs = sortedExecs.slice(0, SHOW_LIMIT)
                    }

                    sortedExecs foreach ((e: Execution) => {
                        execsT +=(
                            (
                                e.id8Var,
                                " ",
                                "Start: " + formatDateTime(e.startTs),
                                "End  : " + formatDateTime(e.endTs),
                                " ",
                                e.state + ": " + X.timeSpan2HMSM(e.duration)
                                ),
                            e.taskNameVar,
                            (e.nodeIds.map((u: UUID) => {
                                var s = ""

                                if (e.origNodeId != null && e.origNodeId == u)
                                    s = s + "> "
                                else if (e.failedNodeIds.contains(u))
                                    s = s + "! "
                                else
                                    s = s + "  "

                                s + nodeId8Addr(u)
                            }).toList :+ ("Nodes: " + e.nodeIds.size)).reverse
                            ,
                            if (e.started > 0)
                                (
                                    "St: " + e.started,
                                    "Fi: " + e.finished + " (" + formatInt(100 * e.finished / e.started) + "%)",
                                    " ",
                                    "Ca: " + e.cancels + " (" + formatInt(100 * e.cancels / e.started) + "%)",
                                    "Re: " + e.rejections + " (" + formatInt(100 * e.rejections / e.started) + "%)",
                                    "Fo: " + e.failovers + " (" + formatInt(100 * e.failovers / e.started) + "%)",
                                    "Fa: " + e.failures + " (" + formatInt(100 * e.failures / e.started) + "%)"
                                    )
                            else
                                (
                                    "St: " + e.started,
                                    "Fi: " + e.finished,
                                    " ",
                                    "Ca: " + e.cancels,
                                    "Re: " + e.rejections,
                                    "Fo: " + e.failovers,
                                    "Fa: " + e.failures
                                    )
                            )
                    })

                    println("Executions: " + eLst.size)

                    execsT.render()

                    nl()

                    execFootnote()

                    nl()
                }

                val tasksT = VisorTextTable()

                tasksT.maxCellWidth = 55

                tasksT #=(
                    "Task Name(@ID), Oldest/Latest & Rate",
                    "Duration",
                    "Nodes",
                    "Executions"
                    )

                var sortedTasks = tLst.sortBy(_.taskName)

                if (sortedTasks.size > SHOW_LIMIT) {
                    warnLimit(sortedTasks.size, "task")

                    sortedTasks = sortedTasks.slice(0, SHOW_LIMIT)
                }

                sortedTasks foreach ((t: Task) => {
                    val sE = t.execsFor(STARTED)
                    val fE = t.execsFor(FINISHED)
                    val eE = t.execsFor(FAILED)
                    val uE = t.execsFor(UNDEFINED)
                    val tE = t.execsFor(TIMEDOUT)

                    val n = t.execs.size

                    tasksT +=(
                        (
                            t.taskNameVar,
                            " ",
                            "Oldest: " + formatDateTime(t.oldest),
                            "Latest: " + formatDateTime(t.latest),
                            " ",
                            "Exec. Rate: " + n + " in " + X.timeSpan2HMSM(t.timeframe)
                            ),
                        (
                            "min: " + X.timeSpan2HMSM(t.minDuration),
                            "avg: " + X.timeSpan2HMSM(t.avgDuration),
                            "max: " + X.timeSpan2HMSM(t.maxDuration)
                            ),
                        (
                            "min: " + t.minNodes,
                            "avg: " + t.avgNodes,
                            "max: " + t.maxNodes
                            ),
                        (
                            "Total: " + n,
                            " ",
                            "St: " + sE + " (" + formatInt(100 * sE / n) + "%)",
                            "Fi: " + fE + " (" + formatInt(100 * fE / n) + "%)",
                            "Fa: " + eE + " (" + formatInt(100 * eE / n) + "%)",
                            "Un: " + uE + " (" + formatInt(100 * uE / n) + "%)",
                            "Ti: " + tE + " (" + formatInt(100 * tE / n) + "%)"
                            )
                        )
                })

                println("Tasks: " + tLst.size)

                tasksT.render()

                nl()

                taskFootnote()
            }
            catch {
                case e: GridException =>
                    scold(e.getMessage)

                    break()
            }
        }
    }

    /**
     * Prints task footnote.
     */
    private def taskFootnote() {
        println("'St' - Started tasks.")
        println("'Fi' - Finished tasks.")
        println("'Fa' - Failed tasks.")
        println("'Un' - Undefined tasks (originating node left topology).")
        println("'Ti' - Timed out tasks.")
    }

    /**
     * Prints execution footnote.
     */
    private def execFootnote() {
        println("'St' - Started jobs during the task execution.")
        println("'Fi' - Finished jobs during the task execution.")
        println("'Ca' - Cancelled jobs during the task execution.")
        println("'Re' - Rejected jobs during the task execution.")
        println("'Fo' - Failed over jobs during the task execution.")
        println("'Fa' - Failed jobs during the task execution.")
        println("'>' - Originating node (if still in topology).")
        println("'!' - Node on which job didn't complete successfully.")
        println("\n<undefined> - Originating node left topology, i.e. execution state is unknown.")
    }

    /**
     * Prints task summary.
     *
     * @param taskName Task host.
     * @param reverse Reverse session chronological sorting?
     * @param all Whether to show full information.
     */
    private def task(taskName: String, reverse: Boolean, all: Boolean) {
        breakable {
            assert(taskName != null)

            try {
                val (tLst, eLst) = mkData(new VisorTaskNameFilter(taskName))

                if (tLst.isEmpty) {
                    scold("Task not found: " + taskName)

                    break()
                }

                val tasksT = VisorTextTable()

                tasksT.maxCellWidth = 55

                tasksT #= ("Oldest/Latest & Rate", "Duration", "Nodes", "Executions")

                assert(tLst.size == 1)

                val t = tLst.head

                val sE = t.execsFor(STARTED)
                val fE = t.execsFor(FINISHED)
                val eE = t.execsFor(FAILED)
                val uE = t.execsFor(UNDEFINED)
                val tE = t.execsFor(TIMEDOUT)

                val n = t.execs.size

                tasksT += (
                    (
                        "Oldest: " + formatDateTime(t.oldest),
                        "Latest: " + formatDateTime(t.latest),
                        " ",
                        "Exec. Rate: " + n + " in " + X.timeSpan2HMSM(t.timeframe)
                        ),
                    (
                        "min: " + X.timeSpan2HMSM(t.minDuration),
                        "avg: " + X.timeSpan2HMSM(t.avgDuration),
                        "max: " + X.timeSpan2HMSM(t.maxDuration)
                        ),
                    (
                        "min: " + t.minNodes,
                        "avg: " + t.avgNodes,
                        "max: " + t.maxNodes
                        ),
                    (
                        "Total: " + n,
                        " ",
                        "St: " + sE + " (" + formatInt(100 * sE / n) + "%)",
                        "Fi: " + fE + " (" + formatInt(100 * fE / n) + "%)",
                        "Fa: " + eE + " (" + formatInt(100 * eE / n) + "%)",
                        "Un: " + uE + " (" + formatInt(100 * uE / n) + "%)",
                        "Ti: " + tE + " (" + formatInt(100 * tE / n) + "%)"
                        )
                    )

                println("Task: " + t.taskNameVar)

                tasksT.render()

                nl()

                taskFootnote()

                if (all) {
                    val execsT = VisorTextTable()

                    execsT.maxCellWidth = 35

                    execsT #= (
                        (
                            "ID8(@ID), Start/End,",
                            "State & Duration"
                            ),
                        "Task Name(@)",
                        "Nodes IP, ID8(@)",
                        "Jobs"
                        )

                    var sorted = if (!reverse) eLst.sortBy(_.startTs).reverse else eLst.sortBy(_.startTs)

                    if (sorted.size > SHOW_LIMIT) {
                        warnLimit(sorted.size, "execution")

                        sorted = sorted.slice(0, SHOW_LIMIT)
                    }

                    sorted foreach ((e: Execution) => {
                        execsT += (
                            (
                                e.id8Var,
                                " ",
                                "Start: " + formatDateTime(e.startTs),
                                "End  : " + formatDateTime(e.endTs),
                                " ",
                                e.state + ": " + X.timeSpan2HMSM(e.duration)
                            ),
                            e.taskNameVar,
                            (e.nodeIds.map((u: UUID) => {
                                var s = ""

                                if (e.origNodeId != null && e.origNodeId == u)
                                    s = s + "> "
                                else if (e.failedNodeIds.contains(u))
                                    s = s + "! "
                                else
                                    s = s + "  "

                                s + nodeId8Addr(u)
                            }).toList :+ ("Nodes: " + e.nodeIds.size)).reverse
                            ,
                            if (e.started > 0)
                                (
                                    "St: " + e.started,
                                    "Fi: " + e.finished + " (" + formatInt(100 * e.finished / e.started) + "%)",
                                    " ",
                                    "Ca: " + e.cancels + " (" + formatInt(100 * e.cancels / e.started) + "%)",
                                    "Re: " + e.rejections + " (" + formatInt(100 * e.rejections / e.started) + "%)",
                                    "Fo: " + e.failovers + " (" + formatInt(100 * e.failovers / e.started) + "%)",
                                    "Fa: " + e.failures + " (" + formatInt(100 * e.failures / e.started) + "%)"
                                )
                            else
                                (
                                    "St: " + e.started,
                                    "Fi: " + e.finished,
                                    " ",
                                    "Ca: " + e.cancels,
                                    "Re: " + e.rejections,
                                    "Fo: " + e.failovers,
                                    "Fa: " + e.failures
                                )
                        )
                    })

                    println("\nExecutions: " + eLst.size)

                    execsT.render()

                    nl()

                    execFootnote()
                }
            }
            catch {
                case e: GridException =>
                    scold(e.getMessage)

                    break()
            }
        }
    }

    /**
     * Prints task execution.
     *
     * @param sesId String representation of session ID.
     * @param reverse Reverse session chronological sorting?
     */
    private def exec(sesId: String, reverse: Boolean) {
        breakable {
            assert(sesId != null)

            var uuid: GridUuid = null

            try
                uuid = GridUuid.fromString(sesId)
            catch {
                case e: Exception =>
                    scold("Invalid execution ID: " + sesId)

                    break()
            }

            try {
                val (tLst, eLst) = mkData(new VisorSessionIdFilter(uuid))

                if (tLst.isEmpty) {
                    scold("Task execution not found: " + sesId)

                    break()
                }

                assert(eLst.size == 1)
                assert(tLst.size == 1)

                val execT = VisorTextTable()

                execT.maxCellWidth = 35

                execT #= (
                    (
                        "ID8(@ID), Start/End,",
                        "State & Duration"
                        ),
                    "Task Name(@)",
                    "Nodes IP, ID8(@)",
                    "Jobs"
                    )

                val e = eLst.head

                execT += (
                    (
                        e.id8Var,
                        " ",
                        "Start: " + formatDateTime(e.startTs),
                        "End  : " + formatDateTime(e.endTs),
                        " ",
                        e.state + ": " + X.timeSpan2HMSM(e.duration)
                        ),
                    e.taskNameVar,
                    (e.nodeIds.map((u: UUID) => {
                        var s = ""

                        if (e.origNodeId != null && e.origNodeId == u)
                            s = s + "> "
                        else if (e.failedNodeIds.contains(u))
                            s = s + "! "
                        else
                            s = s + "  "

                        s + nodeId8Addr(u)
                    }).toList :+ ("Nodes: " + e.nodeIds.size)).reverse
                    ,
                    if (e.started > 0)
                        (
                            "St: " + e.started,
                            "Fi: " + e.finished + " (" + formatInt(100 * e.finished / e.started) + "%)",
                            " ",
                            "Ca: " + e.cancels + " (" + formatInt(100 * e.cancels / e.started) + "%)",
                            "Re: " + e.rejections + " (" + formatInt(100 * e.rejections / e.started) + "%)",
                            "Fo: " + e.failovers + " (" + formatInt(100 * e.failovers / e.started) + "%)",
                            "Fa: " + e.failures + " (" + formatInt(100 * e.failures / e.started) + "%)"
                        )
                    else
                        (
                            "St: " + e.started,
                            "Fi: " + e.finished,
                            " ",
                            "Ca: " + e.cancels,
                            "Re: " + e.rejections,
                            "Fo: " + e.failovers,
                            "Fa: " + e.failures
                        )
                )

                println("Execution: " + e.id8Var)

                execT.render()

                nl()

                execFootnote()

                val evtsT = VisorTextTable()

                evtsT #= ("Timestamp", "Node ID8(@)", "Event")

                val se = if (!reverse) e.evts.sortBy(_.timestamp()) else e.evts.sortBy(_.timestamp()).reverse

                se.foreach((e: GridEvent) => evtsT += (
                    formatDateTime(e.timestamp()),
                    nodeId8Addr(e.nodeId()),
                    e.name
                    ))

                println("\nTrace:")

                evtsT.render()
            }
            catch {
                case e: GridException =>
                    scold(e.getMessage)

                    break()
            }
        }
    }

    /**
     * Prints list of tasks grouped by nodes.
     *
     * @param f Event filter.
     */
    private def nodes(f: Long) {
        breakable {
            try {
                val eLst = mkData(new VisorContainsFilter(f, "visor"))._2

                if (eLst.isEmpty) {
                    scold("No executions found.")

                    break()
                }

                var nMap = Map.empty[UUID, Set[Execution]]

                eLst.foreach(e => {
                    e.nodeIds.foreach(id => {
                        var eSet = nMap.getOrElse(id, Set.empty[Execution])

                        eSet += e

                        nMap += (id -> eSet)
                    })
                })

                nMap.foreach(e => {
                    val id = e._1
                    val execs = e._2

                    val execsMap = execs.groupBy(_.taskName)

                    val tasksT = VisorTextTable()

                    tasksT.maxCellWidth = 55

                    tasksT #=(
                        "Task Name(@), Oldest/Latest & Rate",
                        "Duration",
                        "Executions"
                        )

                    println("Tasks executed on node " + nodeId8Addr(id) + ":")

                    var sortedNames = execsMap.keys.toList.sorted

                    if (sortedNames.size > SHOW_LIMIT) {
                        warnLimit(sortedNames.size, "task")

                        sortedNames = sortedNames.slice(0, SHOW_LIMIT)
                    }

                    sortedNames.foreach(taskName => {
                        val t = Task(taskName, execsMap.get(taskName).get)

                        val sE = t.execsFor(STARTED)
                        val fE = t.execsFor(FINISHED)
                        val eE = t.execsFor(FAILED)
                        val uE = t.execsFor(UNDEFINED)
                        val tE = t.execsFor(TIMEDOUT)

                        val n = t.execs.size

                        tasksT +=(
                            (
                                t.taskNameVar,
                                " ",
                                "Oldest: " + formatDateTime(t.oldest),
                                "Latest: " + formatDateTime(t.latest),
                                " ",
                                "Exec. Rate: " + n + " in " + X.timeSpan2HMSM(t.timeframe)
                                ),
                            (
                                "min: " + X.timeSpan2HMSM(t.minDuration),
                                "avg: " + X.timeSpan2HMSM(t.avgDuration),
                                "max: " + X.timeSpan2HMSM(t.maxDuration)
                                ),
                            (
                                "Total: " + n,
                                " ",
                                "St: " + sE + " (" + formatInt(100 * sE / n) + "%)",
                                "Fi: " + fE + " (" + formatInt(100 * fE / n) + "%)",
                                "Fa: " + eE + " (" + formatInt(100 * eE / n) + "%)",
                                "Un: " + uE + " (" + formatInt(100 * uE / n) + "%)",
                                "Ti: " + tE + " (" + formatInt(100 * tE / n) + "%)"
                                )
                            )
                    })

                    tasksT.render()

                    nl()
                })

                taskFootnote()
            }
            catch {
                case e: GridException =>
                    scold(e.getMessage)

                    break()
            }
        }
    }

    /**
     * Prints list of tasks grouped by hosts.
     *
     * @param f Event filter.
     */
    private def hosts(f: Long) {
        breakable {
            try {
                val eLst = mkData(new VisorContainsFilter(f, "visor"))._2

                if (eLst.isEmpty) {
                    scold("No executions found.")

                    break()
                }

                var hMap = Map.empty[String, Set[Execution]]

                eLst.foreach(e => {
                    e.nodeIds.foreach(id => {
                        val host = grid.node(id).addresses.headOption

                        if (host.isDefined) {
                            var eSet = hMap.getOrElse(host.get, Set.empty[Execution])

                            eSet += e

                            hMap += (host.get -> eSet)
                        }
                    })
                })

                hMap.foreach(e => {
                    val host = e._1
                    val execs = e._2

                    val execsMap = execs.groupBy(_.taskName)

                    val tasksT = VisorTextTable()

                    tasksT.maxCellWidth = 55

                    tasksT #=(
                        "Task Name(@), Oldest/Latest & Rate",
                        "Duration",
                        "Executions"
                        )

                    println("Tasks executed on host " + host + ":")

                    var sortedNames = execsMap.keys.toList.sorted

                    if (sortedNames.size > SHOW_LIMIT) {
                        warnLimit(sortedNames.size, "task")

                        sortedNames = sortedNames.slice(0, SHOW_LIMIT)
                    }

                    sortedNames.foreach(taskName => {
                        val t = Task(taskName, execsMap.get(taskName).get)

                        val sE = t.execsFor(STARTED)
                        val fE = t.execsFor(FINISHED)
                        val eE = t.execsFor(FAILED)
                        val uE = t.execsFor(UNDEFINED)
                        val tE = t.execsFor(TIMEDOUT)

                        val n = t.execs.size

                        tasksT +=(
                            (
                                t.taskNameVar,
                                " ",
                                "Oldest: " + formatDateTime(t.oldest),
                                "Latest: " + formatDateTime(t.latest),
                                " ",
                                "Exec. Rate: " + n + " in " + X.timeSpan2HMSM(t.timeframe)
                                ),
                            (
                                "min: " + X.timeSpan2HMSM(t.minDuration),
                                "avg: " + X.timeSpan2HMSM(t.avgDuration),
                                "max: " + X.timeSpan2HMSM(t.maxDuration)
                                ),
                            (
                                "Total: " + n,
                                " ",
                                "St: " + sE + " (" + formatInt(100 * sE / n) + "%)",
                                "Fi: " + fE + " (" + formatInt(100 * fE / n) + "%)",
                                "Fa: " + eE + " (" + formatInt(100 * eE / n) + "%)",
                                "Un: " + uE + " (" + formatInt(100 * uE / n) + "%)",
                                "Ti: " + tE + " (" + formatInt(100 * tE / n) + "%)"
                                )
                            )
                    })

                    tasksT.render()

                    nl()
                })

                taskFootnote()
            }
            catch {
                case e: GridException =>
                    scold(e.getMessage)

                    break()
            }
        }
    }

    /**
     * Warns about the limit reached.
     *
     * @param n Actual number of elements
     */
    private def warnLimit(n: Int, name: String) {
        nl()

        warn(n + name + "s found.", "Only first " + SHOW_LIMIT + " are shown.")

        nl()
    }
}

/**
 * Companion object that does initialization of the command.
 *
 * @author @java.author
 * @version @java.version
 */
object VisorTasksCommand {
    addHelp(
        name = "tasks",
        shortInfo = "Prints tasks execution statistics.",
        longInfo = List(
            "Prints statistics about tasks and executions.",
            " ",
            "Note that this command depends on GridGain events.",
            " ",
            "GridGain events can be individually enabled and disabled and disabled events",
            "can affect the results produced by this command. Note also that configuration",
            "of Event Storage SPI that is responsible for temporary storage of generated",
            "events on each node can also affect the functionality of this command.",
            " ",
            "By default - all events are enabled and GridGain stores last 10,000 local",
            "events on each node. Both of these defaults can be changed in configuration."
        ),
        spec = List(
            "tasks",
            "tasks -l {-t=<num>s|m|h|d} {-r} {-a}",
            "tasks -s=<substring> {-t=<num>s|m|h|d} {-r}",
            "tasks -g {-t=<num>s|m|h|d} {-r}",
            "tasks -h {-t=<num>s|m|h|d} {-r}",
            "tasks -n=<task-name> {-r} {-a}",
            "tasks -e=<exec-id>"
        ),
        args = List(
            "-l" -> List(
                "List all tasks and executions for a given time period.",
                "Executions sorted chronologically (see '-r'), and tasks alphabetically",
                "See '-t=<num>s|m|h|d' on how to specify the time limit.",
                "Default time period is one hour, i.e '-t=1h'",
                " ",
                "Execution list will be shown only if '-a' flag is provided.",
                " ",
                "This is a default mode when command is called without parameters."
            ),
            "-s=<substring>" -> List(
                "List all tasks and executions for a given task name substring.",
                "Executions sorted chronologically (see '-r'), and tasks alphabetically",
                "See '-t=<num>s|m|h|d' on how to specify the time limit.",
                "Default time period is one hour, i.e '-t=1h'",
                " ",
                "Execution list will be shown only if '-a' flag is provided."
            ),
            "-g" -> List(
                "List all tasks grouped by nodes for a given time period.",
                "Tasks sorted alphabetically",
                "See '-t=<num>s|m|h|d' on how to specify the time limit.",
                "Default time period is one hour, i.e '-t=1h'"
            ),
            "-h" -> List(
                "List all tasks grouped by hosts for a given time period.",
                "Tasks sorted alphabetically",
                "See '-t=<num>s|m|h|d' on how to specify the time limit.",
                "Default time period is one hour, i.e '-t=1h'"
            ),
            "-t=<num>s|m|h|d" -> List(
                "Defines time frame for '-l' parameter:",
                "   =<num>s Last <num> seconds.",
                "   =<num>m Last <num> minutes.",
                "   =<num>h Last <num> hours.",
                "   =<num>d Last <num> days."
            ),
            "-r" -> List(
                "Reverse sorting of executions."
            ),
            "-n=<task-name>" -> List(
                "Prints aggregated statistic for named task.",
                " ",
                "Execution list will be shown only if '-a' flag is provided."
            ),
            "-e=<exec-id>" ->
                "Prints aggregated statistic for given task execution.",
            "-a" -> List(
                "Defines whether to show list of executions.",
                "Can be used with '-l', '-s' or '-n'."
            )
        ),
        examples = List(
            "tasks" ->
                "Prints list of all tasks for the last hour (default).",
            "tasks -l" ->
                "Prints list of all tasks for the last hour (default).",
            "tasks -l -a" ->
                "Prints list of all tasks and executions for the last hour (default).",
            "tasks -l -t=5m" ->
                "Prints list of tasks that started during last 5 minutes.",
            "tasks -s=Task" ->
                "Prints list of all tasks that have 'Task' in task name.",
            "tasks -g" ->
                "Prints list of tasks grouped by nodes.",
            "tasks -g -t=5m" ->
                "Prints list of tasks that started during last 5 minutes grouped by nodes.",
            "tasks -h" ->
                "Prints list of tasks grouped by hosts.",
            "tasks -h -t=5m" ->
                "Prints list of tasks that started during last 5 minutes grouped by hosts.",
            "tasks -n=GridTask" ->
                "Prints summary for task named 'GridTask'.",
            "tasks -n=GridTask -a" ->
                "Prints summary and executions for task named 'GridTask'.",
            "tasks -e=7D5CB773-225C-4165-8162-3BB67337894B" ->
                "Traces task execution with ID '7D5CB773-225C-4165-8162-3BB67337894B'."
        ),
        ref = VisorConsoleCommand(cmd.tasks, cmd.tasks)
    )

    /** Singleton command. */
    private val cmd = new VisorTasksCommand

    /**
     * Singleton.
     */
    def apply() = cmd

    /**
     * Implicit converter from visor to commands "pimp".
     *
     * @param vs Visor tagging trait.
     */
    implicit def fromTrace2Visor(vs: VisorTag) = cmd
}
