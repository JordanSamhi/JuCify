#!/bin/bash

. ./common.sh --source-only

RAW=false

while getopts d:f:p:r option
do
    case "${option}"
        in
        f) FILE=${OPTARG};;
        d) ENTRYPOINT_DIR=${OPTARG};;
        p) PDF=true;;
        r) RAW=true;;
    esac
done

if [ -z "$FILE" ]
then
    echo
    read -p "APK path: " FILE
fi

if [ -z "$ENTRYPOINT_DIR" ]
then
    echo
    read -p "Entrypoints directory path: " ENTRYPOINT_DIR
fi

if [ -z "$PDF" ]
then
    PDF=false
fi

if [ "$PDF" = true ]
then
    OPTS="--backend-emit-cg --graph-format pdf"
else
    OPTS="--backend-emit-cg --backend-cg-conversion manual"
fi

if [ ! -f "$FILE" ]
then
    end_program "$FILE does not exist"
fi

DST_FLD=$(dirname $FILE)"/"
NEW_FLD=$(basename $FILE .apk)
DST=$DST_FLD$NEW_FLD
mkdir -p $DST
unzip -o $FILE -d $DST > /dev/null 2>&1


if [ "$RAW" = false ]
then
    print_info "Extracting CallGraph..."
fi

for f in $ENTRYPOINT_DIR/*result
do
    LIBNAME_WITH_INFO=$(basename $f .so.result)
    for ff in $(find $DST/lib/ -name "*.so")
    do
        LIBNAME=$(basename $ff .so)
        if [ $LIBNAME_WITH_INFO == $LIBNAME ]
        then
            if [ "$RAW" = false ]
            then
                print_info "Processing $LIBNAME..."
            fi
            ./retdec/bin/retdec-decompiler.py $ff $OPTS > /dev/null 2>&1
            wait
            ls -1 $ff.*|grep -vE ".*\.dot|.*\.pdf"|parallel rm {}
        fi
    done
done

