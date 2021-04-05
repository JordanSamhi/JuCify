#! /bin/bash

$* &
pid=$!
((lim = 300))
while [[ $lim -gt 0 ]] ; do
    sleep 1
    proc=$(ps -ef | awk -v pid=$pid '$2==pid{print}{}')
    ((lim = lim - 1))
    if [[ -z "$proc" ]] ; then
            ((lim = -9))
    fi
done
if [[ $lim -gt -9 ]] ; then
    kill -9 -$pid
fi
