#!/bin/bash

. ./common.sh --source-only

RAW=false
TAINT_ANALYSIS=false
EXPORT_CG_DST=false
CLEAN=false

while getopts f:p:rcte option
do
    case "${option}"
        in
        f) APK_PATH=${OPTARG};;
        p) PLATFORMS_PATH=${OPTARG};;
        r) RAW=true;;
        e) EXPORT_CG_DST=true;;
        t) TAINT_ANALYSIS=true;;
        c) CLEAN=true;;
    esac
done

if [ -z "$APK_PATH" ]
then
    echo
    read -p "APK path: " APK_PATH
fi

if [ -z "$PLATFORMS_PATH" ]
then
    echo
    read -p "Platforms path: " PLATFORMS_PATH
fi

APK_BASENAME=$(basename $APK_PATH .apk)
APK_DIRNAME=$(dirname $APK_PATH)
ENTRYPOINTS_DIR=$APK_DIRNAME/$APK_BASENAME"_result/"

pkg_name=$(androguard axml $APK_PATH|grep "package="|tr ' ' '\n'|grep package|sed 's/package=\(.*\)/\1/g'|tr -d '"'|tr -d ">")
if [ "$RAW" = false ]
then
    print_info "Processing $pkg_name"
fi

if [ "$RAW" = false ]
then
    print_info "Extracting Java-to-Binary and Binary-to-Java function calls..."
fi
./execute_with_limit_time.sh ./launch_native_disclosurer.sh -f $APK_PATH >/dev/null 2>/dev/null
wait

DST=$APK_DIRNAME"/"$APK_BASENAME
mkdir -p $DST
unzip -o $APK_PATH -d $DST > /dev/null 2>&1

DOTFILE=$(ls -1 $ENTRYPOINTS_DIR|grep dot)
DOTFILE_BNAME=$(basename $DOTFILE .dot)
ENTRYPOINTFILE=$(ls -1 $ENTRYPOINTS_DIR|grep entrypoints)
python3 process_binary_callgraph.py -d $ENTRYPOINTS_DIR/$DOTFILE -e $ENTRYPOINTS_DIR/$ENTRYPOINTFILE -w $APK_DIRNAME/$APK_BASENAME/$DOTFILE_BNAME.callgraph -m $ENTRYPOINTS_DIR/$DOTFILE_BNAME".map"
CALLGRAPHS_PATHS+=$APK_DIRNAME/$APK_BASENAME/$DOTFILE_BNAME.callgraph":"$(dirname $ENTRYPOINTS_DIR/$ENTRYPOINTFILE)/$(basename $ENTRYPOINTS_DIR/$ENTRYPOINTFILE .entrypoints)"|"


OPTS=""

if [ "$RAW" = true ]
then
    OPTS+="-r "
fi

if [ "$TAINT_ANALYSIS" = true ]
then
    OPTS+="-ta "
fi

if [ "$EXPORT_CG_DST" = true ]
then
    OPTS+="-c $APK_DIRNAME/"$APK_BASENAME"_cg.txt"
fi

if [ ! -z "$CALLGRAPHS_PATHS" ]
then
    java -jar ../target/JuCify-0.1-jar-with-dependencies.jar -a $APK_PATH -p $PLATFORMS_PATH -f $CALLGRAPHS_PATHS $OPTS
else
    print_info "Not executing JuCify"
fi

if [ "$CLEAN" = true ]
then
    rm -rf $APK_DIRNAME/$APK_BASENAME $APK_DIRNAME/$ENTRYPOINTS_DIR
fi
