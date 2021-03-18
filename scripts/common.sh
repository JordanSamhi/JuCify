#!/bin/bash

end_program () {
    echo "[!] $1"
    echo "End of program."
    exit 1
}

print_info () {
    echo "[*] $1"
}

check_return () {
    if [ $1 -ne 0 ]
    then
        end_program "$2"
    else
        if [ "$3" ]
        then
            print_info "$3"
        fi
    fi
}
