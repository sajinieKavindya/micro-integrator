/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.integrator.ntask.core;

import org.apache.synapse.task.TaskManagerObserver;
import org.wso2.micro.integrator.ntask.common.TaskException;
import org.wso2.micro.integrator.ntask.core.impl.LocalTaskActionListener;

import java.util.List;

/**
 * This interface represents the task manager functionalities.
 */
public interface TaskManager {

    /**
     * Initialize the startup tasks.
     *
     * @throws TaskException Exception
     */
    void initStartupTasks() throws TaskException;

    /**
     * Reschedules a task with the given name, only the trigger information will be updated in the
     * reschedule.
     *
     * @param taskName The task to be rescheduled
     * @throws TaskException Exception
     */
    void rescheduleTask(String taskName) throws TaskException;

    /**
     * Stops and deletes a task with the given name.
     *
     * @param taskName The name of the task
     * @return true if the task was found and deleted
     * @throws TaskException Exception
     */
    boolean deleteTask(String taskName) throws TaskException;

    /**
     * Handles the task with given name. Schedule if its not coordinated else update the task DB.
     *
     * @param taskName The name of the task
     * @throws TaskException Exception
     */
    void handleTask(String taskName) throws TaskException;

    void handleTask(String taskName, boolean scheduledInPausedMode) throws TaskException;

    /**
     * Get all the coordinated tasks ( the tasks which need db interaction ) deployed in this node.
     *
     * @return List of deployed coordinated tasks.
     */
    List<String> getAllCoordinatedTasksDeployed();

    /**
     * Get all the locally running coordinated tasks.
     *
     * @return List of all locally running coordinated tasks.
     */
    List<String> getLocallyRunningCoordinatedTasks();

    void removeTaskFromLocallyRunningTaskList(String taskName);

    /**
     * Handles the pause operation for the task with the given name.
     *
     * @param taskName The name of the task
     * @throws TaskException Exception
     */
    void handleTaskPause(String taskName) throws TaskException;

    /**
     * Resumes a paused task with the given name.
     *
     * @param taskName The name of the task
     * @throws TaskException Exception
     */
    void handleTaskResume(String taskName) throws TaskException;

    /**
     * Registers a new task or updates if one already exists.
     *
     * @param taskInfo The task information
     * @throws TaskException Exception
     */
    void registerTask(TaskInfo taskInfo) throws TaskException;

    /**
     * Gets tasks state information
     *
     * @param taskName The name of the task
     * @return State of the task
     * @throws TaskException Exception
     */
    TaskState getTaskState(String taskName) throws TaskException;

    boolean isDeactivated(String taskName) throws TaskException;

    boolean isTaskRunning(String taskName) throws TaskException;

    /**
     * Get task information.
     *
     * @param taskName The name of the task
     * @return The task information
     * @throws TaskException if the task cannot be found
     */
    TaskInfo getTask(String taskName) throws TaskException;

    /**
     * Get all task information.
     *
     * @return Task information list
     * @throws TaskException Exception
     */
    List<TaskInfo> getAllTasks() throws TaskException;

    /**
     * Registers a listener to be notified when an action is performed on a task.
     *
     * @param listener the listener to be notified
     * @param taskName the name of the task for which the listener should be registered
     */
    void registerLocalTaskActionListener(LocalTaskActionListener listener, String taskName);

    /**
     * Task states.
     */
    enum TaskState {
        NORMAL,
        PAUSED,
        ERROR,
        FINISHED,
        NONE,
        BLOCKED,
        UNKNOWN
    }

}
