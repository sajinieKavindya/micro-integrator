#!/bin/bash
PID=`cat $1`
LOG_FILE="$2"

top -i 1 -pid $PID | awk \
    -v pid="$PID" -v cpuLog="$LOG_FILE" '
    $1+0>0 {printf "%d\n", \
            $3 > cpuLog
            fflush(cpuLog)
            close(cpuLog)}'
