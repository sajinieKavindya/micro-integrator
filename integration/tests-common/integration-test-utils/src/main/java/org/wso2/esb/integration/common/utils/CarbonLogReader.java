/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.esb.integration.common.utils;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * The class which facilitates tailing the logs from wso2carbon.log file.
 */
public class CarbonLogReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonLogReader.class);
    private CarbonLogTailer carbonLogTailer;
    private Tailer tailer;
    private File carbonLogFile;
    private boolean startReadingFromEndOfFile = true;

    public CarbonLogReader() {

        init(true);
    }

    /**
     * Class initializer.
     *
     * @param startReadingFromEndOfFile - specify whether you want to tail from the end of file or not.
     */
    public CarbonLogReader(boolean startReadingFromEndOfFile) {

        init(startReadingFromEndOfFile);
    }

    private void init(boolean startReadingFromEndOfFile) {
        carbonLogTailer = new CarbonLogTailer();
        carbonLogFile = new File(
                System.getProperty("carbon.home") + File.separator + "repository" + File.separator + "logs"
                        + File.separator + "wso2carbon.log");
        this.startReadingFromEndOfFile = startReadingFromEndOfFile;
    }

    /**
     * Start tailer thread.
     */
    public void start() {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting to tail carbon logs from : " + carbonLogFile.getPath());
        }
        tailer = new Tailer(carbonLogFile, carbonLogTailer, 1, startReadingFromEndOfFile);
        Thread thread = new Thread(tailer);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Get the tailed logs from the start until now.
     *
     * @return - tailed logs as string.
     */
    public String getLogs() {
        return carbonLogTailer.getCarbonLogs();
    }

    /**
     * Clears the tail log container.
     */
    public void clearLogs() {
        carbonLogTailer.clearLogs();
    }

    /**
     * Stops the thread which started tailing the logs.
     */
    public void stop() {
        LOGGER.debug("Stopped tailing carbon logs.");
        tailer.stop();
    }

    /**
     * Check for the existence of the given log message.
     *
     * @param expected expected log string
     * @return true if the log is found, false otherwise
     */
    public boolean assertIfLogExists(String expected) {
        return this.getLogs().contains(expected);
    }

    /**
     * Check for the existence of occurrences of a given log message.
     *
     * @param expected            expected log string
     * @param numberofOccurrences numberofOccurrences expected number of log occurrences
     * @return true if the expected number of log occurrences are found, false otherwise
     */
    public boolean assertIfLogExists(String expected, int numberofOccurrences) {
        return numberofOccurrences == StringUtils.countMatches(this.getLogs(), expected);
    }

    /**
     * Returns the substring between the given start and end substrings.
     *
     * @param startWith starting substring
     * @param endWith   ending substring
     * @param timeout   max time to poll in seconds
     * @return the substring between the given string, empty string otherwise
     * @throws InterruptedException if interrupted while sleeping
     */
    public String getSubstringBetweenStrings(String startWith, String endWith, int timeout)
            throws InterruptedException {
        for (int i = 0; i < timeout; i++) {
            TimeUnit.SECONDS.sleep(1);
            if (assertIfLogExists(startWith) && assertIfLogExists(endWith) && !StringUtils
                    .substringBetween(startWith, endWith).isEmpty()) {
                return StringUtils.substringBetween(startWith, endWith);
            }
        }
        return "";
    }

    /**
     * Check for the existence of the given log message. The polling will happen in one second intervals.
     *
     * @param expected expected log string
     * @param timeout  max time to do polling in seconds
     * @return true if the log is found with given timeout, false otherwise
     * @throws InterruptedException if interrupted while sleeping
     */
    public boolean checkForLog(String expected, int timeout) throws InterruptedException {
        boolean logExists = false;
        for (int i = 0; i < timeout; i++) {
            TimeUnit.SECONDS.sleep(1);
            if (assertIfLogExists(expected)) {
                logExists = true;
                break;
            }
        }
        return logExists;
    }

    /**
     * Check for the existence of the given log message occurrences. The polling will happen in one second intervals.
     *
     * @param expected            expected log message
     * @param timeout             max time to poll in seconds
     * @param numberofOccurrences expected number of log occurrences
     * @return true if the expected number of log occurrences are found within the given timeout, false otherwise
     * @throws InterruptedException if interrupted while sleeping
     */
    public boolean checkForLog(String expected, int timeout, int numberofOccurrences) throws InterruptedException {
        for (int i = 0; i < timeout; i++) {
            TimeUnit.SECONDS.sleep(1);
            if (assertIfLogExists(expected, numberofOccurrences)) {
                return true;
            }
        }
        return false;
    }
}
