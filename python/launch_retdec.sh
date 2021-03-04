#!/bin/bash

. ./common.sh --source-only

while getopts f:p option
do
    case "${option}"
        in
        f) FILE=${OPTARG};;
        p) PDF=true;;
    esac
done

if [ -z "$FILE" ]
then
    echo
    read -p "Binary path: " FILE
fi

if [ -z "$PDF" ]
then
    PDF=false
fi

if [ "$PDF" = true ]
then
    OPTS="--backend-emit-cg --graph-format pdf"
else
    OPTS="--backend-emit-cg"
fi

if [ ! -f "$FILE" ]
then
    end_program "$FILE does not exist"
fi

print_info "Extracting CallGraph..."
./retdec/bin/retdec-decompiler.py $FILE $OPTS &> /dev/null
check_return $? "Something went wront with retdec" "Retdec successfully executed."

ls -1 $FILE.*|grep -vE ".*\.dot|.*\.pdf"|parallel rm {}
