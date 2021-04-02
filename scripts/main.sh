#!/bin/bash

. ./common.sh --source-only

RAW=false

while getopts f:p:r option
do
    case "${option}"
        in
        f) APK_PATH=${OPTARG};;
        p) PLATFORMS_PATH=${OPTARG};;
        r) RAW=true;;
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

pkg_name=$(androguard axml $APK_PATH|grep "package="|tr ' ' '\n'|grep package|sed 's/package=\(.*\)/\1/g'|tr -d '"')
if [ "$RAW" = false ]
then
    print_info "Processing $pkg_name"
fi

if [ "$RAW" = false ]
then
    print_info "Extracting Java-to-Binary and Binary-to-Java function calls..."
fi
./launch_native_disclosurer.sh -f $APK_PATH

if [ "$RAW" = false ]
then
    print_info "Generating Binary Callgraph per Library..."
fi
if [ "$RAW" = false ]
then
    ./launch_retdec.sh -f $APK_PATH -d $ENTRYPOINTS_DIR
else
    ./launch_retdec.sh -f $APK_PATH -d $ENTRYPOINTS_DIR -r
fi


CALLGRAPHS_PATHS_TO_ENTRYPOINTS=""

if [ "$RAW" = false ]
then
    print_info "Extracting Relevant Callgraph parts..."
fi
if [ $(ls $ENTRYPOINTS_DIR/|grep "*.entrypoints" |wc -l) -gt 0 ]
then
    for efile in $(ls -1 $ENTRYPOINTS_DIR/*.entrypoints)
    do
        bname=$(basename $efile .result.entrypoints)
        for f in $(find $APK_DIRNAME/$APK_BASENAME/lib/ -name "*.dot")
        do
            bnamef=$(basename $f)
            if [[ $bnamef = $bname* ]]
            then
                python3 process_binary_callgraph.py -d $f -e $efile -w $APK_DIRNAME/$APK_BASENAME/$bname.callgraph
                CALLGRAPHS_PATHS+=$APK_DIRNAME/$APK_BASENAME/$bname.callgraph":"$(dirname $efile)/$(basename $efile .entrypoints)"|"
            fi
        done
    done
fi

if [ ! -z "$CALLGRAPHS_PATHS" ]
then
    if [ "$RAW" = false ]
    then
        java -jar ../target/JuCify-0.1-jar-with-dependencies.jar -a $APK_PATH -p $PLATFORMS_PATH -f $CALLGRAPHS_PATHS -ta
    else
        java -jar ../target/JuCify-0.1-jar-with-dependencies.jar -a $APK_PATH -p $PLATFORMS_PATH -f $CALLGRAPHS_PATHS -ta -r
    fi
else
    print_info "Not executing JuCify"
fi
