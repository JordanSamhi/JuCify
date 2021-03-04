#!/bin/bash

. ./common.sh --source-only

while getopts f:p option
do
    case "${option}"
        in
        f) FILE=${OPTARG};;
    esac
done

if [ -z "$FILE" ]
then
    echo
    read -p "APK path: " FILE
fi

if [ ! -f "$FILE" ]
then
    end_program "$FILE does not exist"
fi

python3.7 main.py $FILE
check_return $? "Something went wront with retdec" ""
