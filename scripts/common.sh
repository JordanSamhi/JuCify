#!/bin/bash

BLUE="\033[0;36m"
END="\033[0;0m"

end_program () {
    echo "[!] $1"
    echo "End of program."
    exit 1
}

print_info () {
    echo -e "${BLUE}[*] $1${END}"
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
