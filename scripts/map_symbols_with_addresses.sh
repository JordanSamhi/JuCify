#!/bin/bash
apk=$1
bname=$(basename $apk .apk)
fname=$(dirname $apk)
mkdir /tmp/$bname &>/dev/null
unzip -o $apk -d /tmp/$bname &>/dev/null
for f in $(find /tmp/$bname -name "*.so")
do
    retdec/bin/retdec-decompiler.py $f &>/dev/null
    newname=$(echo $f.map|sed 's/\/tmp\///g'|sed 's/\//___/g')
    python3 map_symbols_with_addresses.py $f.config.json > $fname/$newname
done
