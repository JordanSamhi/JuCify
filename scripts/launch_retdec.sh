#!/bin/bash

. ./common.sh --source-only

while getopts d:f:p option
do
    case "${option}"
        in
        f) FILE=${OPTARG};;
        d) ENTRYPOINT_DIR=${OPTARG};;
        p) PDF=true;;
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
    OPTS="--backend-emit-cg"
fi

if [ ! -f "$FILE" ]
then
    end_program "$FILE does not exist"
fi

DST_FLD=$(dirname $FILE)"/"
NEW_FLD=$(basename $FILE .apk)
DST=$DST_FLD$NEW_FLD
mkdir -p $DST
unzip -o $FILE -d $DST &> /dev/null

print_info "Extracting CallGraph..."

for f in $ENTRYPOINT_DIR/*result
do
    LIBNAME_WITH_INFO=$(basename $f .so.result)
    for ff in $(find $DST/lib/armeabi-v7a/ -name "*.so")
    do
        LIBNAME=$(basename $ff .so)
        if [ $LIBNAME_WITH_INFO == $LIBNAME ]
        then
            print_info "Processing $LIBNAME..."
            ./retdec/bin/retdec-decompiler.py $ff $OPTS &> /dev/null
            ls -1 $ff.*|grep -vE ".*\.dot|.*\.pdf"|parallel rm {}
        fi
    done
done

