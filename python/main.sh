#!/bin/bash

. ./common.sh --source-only

while getopts f: option
do
    case "${option}"
        in
        f) APK_PATH=${OPTARG};;
    esac
done

if [ -z "$APK_PATH" ]
then
    echo
    read -p "APK path: " APK_PATH
fi

APK_BASENAME=$(basename $APK_PATH .apk)
APK_DIRNAME=$(dirname $APK_PATH)
ENTRYPOINTS_DIR=$APK_DIRNAME/$APK_BASENAME"_result/"

print_info "Extracting Java-to-Binary and Binary-to-Java function calls..."
./launch_native_disclosurer.sh -f $APK_PATH

print_info "Generating Binary Callgraph per Library..."
./launch_retdec.sh -f $APK_PATH -d $ENTRYPOINTS_DIR


CALLGRAPHS_PATHS_TO_ENTRYPOINTS=""

print_info "Extracting Relevant Callgraph parts..."
for efile in $(ls -1 $ENTRYPOINTS_DIR/*.entrypoints)
do
    bname=$(basename $efile .result.entrypoints)
    for f in $(find $APK_DIRNAME/$APK_BASENAME/lib/armeabi-v7a/ -name "*.dot")
    do
        bnamef=$(basename $f)
        if [[ $bnamef = $bname* ]]
        then
            python3 process_binary_callgraph.py -d $f -e $efile -w $APK_DIRNAME/$APK_BASENAME/$bname.callgraph
            echo $efile
            CALLGRAPHS_PATHS+=$(pwd)/$APK_DIRNAME/$APK_BASENAME/$bname.callgraph":"$(pwd)/$(dirname $efile)/$(basename $efile .entrypoints)"|"
        fi
    done
done

echo $CALLGRAPHS_PATHS
