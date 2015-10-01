#!/bin/bash

source gradle.properties

_version=${version}

_openbaton_base="/opt/openbaton/"
_message_queue_base="apache-activemq-5.11.1"
_openbaton_config_file=/etc/openbaton/openbaton.properties

function start_activemq_linux {
    sudo ${_openbaton_base}/${_message_queue_base}/bin/activemq start
}

function start_activemq_osx {
    sudo ${_openbaton_base}/${_message_queue_base}/bin/macosx/activemq start
}

function check_activemq {
    if [[ "$OSTYPE" == "linux-gnu" ]]; then
	ps -a | grep -v grep | grep activemq > /dev/null
    	result=$?
        if [ "${result}" -eq "0" ]; then
         	echo "activemq service running, everything is fine"
        else
          	echo "activemq is not running, starting it:"
            	start_activemq_linux
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
	ps aux | grep -v grep | grep activemq > /dev/null
        result=$?
         if [ "${result}" -eq "0" ]; then
          	echo "activemq service running, everything is fine"
         else
           	echo "activemq is not running, starting it:"
            	start_activemq_osx
         fi
    fi
}

function check_already_running {
        result=$(screen -ls | grep generic-vnfm | wc -l);
        if [ "${result}" -ne "0" ]; then
                echo "generic-vnfm is already running.."
		exit;
        fi
}

function start {

    if [ ! -d build/  ]
        then
            compile
    fi

    check_activemq
    check_already_running
    if [ 0 -eq $? ]
        then
	    screen -X eval "chdir $PWD"
	    screen -S openbaton -p 0 -X screen -t generic-vnfm java -jar "build/libs/generic-vnfm-$_version.jar" 
	    screen -c .screenrc -r -p 0
    fi
}

function stop {
    if screen -list | grep "openbaton"; then
	    screen -S openbaton -p 0 -X stuff "exit$(printf \\r)"
    fi
}

function restart {
    kill
    start
}


function kill {
    if screen -list | grep "openbaton"; then
	    screen -ls | grep openbaton | cut -d. -f1 | awk '{print $1}' | xargs kill
    fi
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

