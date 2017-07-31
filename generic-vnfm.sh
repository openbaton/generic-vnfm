#!/bin/bash

source gradle.properties

_openbaton_base="/opt/openbaton"
_generic_base="${_openbaton_base}/generic-vnfm"
_openbaton_config_file="/etc/openbaton/generic-vnfm.properties"
_version=${version}
_screen_name="generic-vnfm"


function checkBinary {
  echo -n " * Checking for '$1'..."
  if command -v $1 >/dev/null 2>&1; then
     echo "OK"
     return 0
   else
     echo >&2 "FAILED."
     return 1
   fi
}


_ex='sh -c'
if [ "$_user" != 'root' ]; then
    if checkBinary sudo; then
        _ex='sudo -E sh -c'
    elif checkBinary su; then
        _ex='su -c'
    fi
fi


function check_rabbitmq {
    if [[ "$OSTYPE" == "linux-gnu" ]]; then
	ps -aux | grep -v grep | grep rabbitmq > /dev/null
        if [ $? -ne 0 ]; then
          	echo "rabbit is not running, let's try to start it..."
            	start_rabbitmq
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
	ps aux | grep -v grep | grep rabbitmq > /dev/null
        if [ $? -ne 0 ]; then
          	echo "rabbitmq is not running, let's try to start it..."
            	start_rabbitmq
        fi
    fi
}

function start_rabbitmq {
    $_ex 'rabbitmq-server -detached'
    if [ $? -ne 0 ]; then
        echo "ERROR: rabbitmq is not running properly (check the problem in /var/log/rabbitmq.log) "
        exit 1
    fi
}

function check_already_running {
    pgrep -f generic-vnfm-${_version}.jar
    if [ "$?" -eq "0" ]; then
        echo "generic-vnfm is already running.."
        exit;
    fi
}

function start {
    echo "Starting the Generic-VNFM"
    # if not compiled, compile
    if [ ! -d ${_generic_base}/build/  ]
        then
            compile
    fi
    check_rabbitmq
    check_already_running
    screen_exists=$(screen -ls | grep openbaton | wc -l);
    if [ "${screen_exists}" -eq "0" ]; then

	    if [ -f ${_openbaton_config_file} ]; then
            if [ $1 == "true" ]; then
                java -jar "${_generic_base}/build/libs/generic-vnfm-${_version}.jar" --spring.config.location=file:${_openbaton_config_file}
            else
                echo "Starting the Generic-VNFM in a new screen session (attach to the screen with screen -x openbaton)"
                screen -c screenrc -d -m -S openbaton -t ${_screen_name} java -jar "${_generic_base}/build/libs/generic-vnfm-${_version}.jar" --spring.config.location=file:${_openbaton_config_file}
            fi
        else
	        if [ $1 == "true" ]; then
		        java -jar "${_generic_base}/build/libs/generic-vnfm-${_version}.jar"
	        else
		        screen -c screenrc -d -m -S openbaton -t ${_screen_name} java -jar "${_generic_base}/build/libs/generic-vnfm-${_version}.jar"
	        fi
        fi
    else
        echo "Starting the Generic-VNFM in the existing screen session (attach to the screen with screen -x openbaton)"
        if [ -f ${_openbaton_config_file} ]; then
            screen -S openbaton -X screen -t ${_screen_name} java -jar "${_generic_base}/build/libs/generic-vnfm-${_version}.jar" --spring.config.location=file:${_openbaton_config_file}
        else
            screen -S openbaton -X screen -t ${_screen_name} java -jar "${_generic_base}/build/libs/generic-vnfm-${_version}.jar"
        fi
    fi
}

function stop {
    if screen -list | grep "openbaton" > /dev/null ; then
	    screen -S openbaton -p ${_screen_name} -X stuff '\003'
    fi
}

function restart {
    kill
    start
}


function kill {
    pkill -f generic-vnfm-${_version}.jar
}


function compile {
    ./gradlew build -x test 
}

function tests {
    ./gradlew test
}

function clean {
    ./gradlew clean
}

function end {
    exit
}
function usage {
    echo -e "Open-Baton generic-vnfm\n"
    echo -e "Usage:\n\t ./generic-vnfm.sh [compile|start|stop|test|kill|clean]"
}

##
#   MAIN
##

if [ $# -eq 0 ]
   then
        usage
        exit 1
fi

declare -a cmds=($@)
for (( i = 0; i <  ${#cmds[*]}; ++ i ))
do
    case ${cmds[$i]} in
        "clean" )
            clean ;;
        "sc" )
            clean
            compile
            start ;;
        "start_fg" )
            start "true" ;;
        "start" )
            start ;;
        "stop" )
            stop ;;
        "restart" )
            restart ;;
        "compile" )
            compile ;;
        "kill" )
            kill ;;
        "test" )
            tests ;;
        * )
            usage
            end ;;
    esac
    if [[ $? -ne 0 ]]; 
    then
	    exit 1
    fi
done

