#!/bin/sh

export MONITORING_IP=
export TIMEZONE=
export BROKER_IP=
export BROKER_PORT=
export USERNAME=
export PASSWORD=
export EXCHANGE_NAME=
export EMS_HEARTBEAT=
export EMS_AUTODELETE=
export EMS_VERSION=
export #Hostname=
export ENDPOINT=

# Hostname/IP and path of the EMS repository
export UBUNTU_EMS_REPOSITORY_HOSTNAME_OR_IP="get.openbaton.org"
export UBUNTU_EMS_REPOSITORY_PATH="repos/openbaton"
export CENTOS_EMS_REPOSITORY_HOSTNAME_OR_IP="get.openbaton.org"
export CENTOS_EMS_REPOSITORY_PATH="repos/rpm/"

export EMS_PROPERTIES_FILE="/etc/openbaton/openbaton-ems.properties"

export OS_DISTRIBUTION_CODENAME=
export OS_DISTRIBUTION_RELEASE_MAJOR=

export LANG=en_US.UTF-8
export LANGUAGE=en_US.UTF-8
export LC_COLLATE=C
export LC_CTYPE=en_US.UTF-8

#################
#### Generic ####
#################

prepare_machine_generic () {
    cp /usr/share/zoneinfo/$TIMEZONE /etc/localtime
    mkdir /opt/openbaton
    #Installation of pip
    wget https://bootstrap.pypa.io/get-pip.py
    python get-pip.py
    pip install --upgrade setuptools
}

install_configure_pip_packages () {
    pip install pika
    pip install gitpython
    #Installation of Generic EMS
    if [ -n "$EMS_VERSION" ]
    then
        echo "Installing EMS $EMS_VERSION"
        pip install openbaton-ems==$EMS_VERSION
    else
        echo "Not defined ems version. Installing latest version..." &&
        pip install openbaton-ems
    fi
    add-upstart-ems
}

################
#### Ubuntu ####
################

prepare_machine_ubuntu () {
    echo "Checking for running apt-get processes"
    while [ ! -z "$(ps -A | grep apt-get | awk '{print $1}')" ];do
    echo "Waiting for running apt-get processes to finish"
    sleep 5s
    done
    echo "Finished waiting for running apt-get processes"
    apt-get update
    apt-get install -y python
    apt-get install -y git
}

install_zabbix_on_ubuntu () {
    result=$(dpkg -l | grep "zabbix-agent" | wc -l)
    if [ ${result} -eq 0 ]; then
        echo "Installing zabbix-agent for server at $MONITORING_IP"
        apt-get install -y zabbix-agent
    else
        echo "Zabbix-agent is already installed"
    fi
}


################
#### CentOS ####
################

prepare_machine_centos () {
    yum install -y wget
    yum install -y git
}

install_zabbix_on_centos () {
    result=$( yum list installed | grep zabbix-agent | wc -l )
    if [ ${result} -eq 0 ]; then
        echo "Adding repository .."
        rpm -Uvh http://repo.zabbix.com/zabbix/3.0/rhel/${OS_DISTRIBUTION_RELEASE_MAJOR}/x86_64/zabbix-release-3.0-1.el${OS_DISTRIBUTION_RELEASE_MAJOR}.noarch.rpm
        echo "Installing zabbix-agent .."
        yum install -y zabbix zabbix-agent
    else
        echo "Zabbix-agent is already installed"
    fi
}


#############
#### EMS ####
#############

configure_ems () {
    mkdir -p /etc/openbaton
    echo [ems] > ${EMS_PROPERTIES_FILE}
    echo broker_ip=$BROKER_IP >> ${EMS_PROPERTIES_FILE}
    echo broker_port=$BROKER_PORT >> ${EMS_PROPERTIES_FILE}
    echo username=$USERNAME >> ${EMS_PROPERTIES_FILE}
    echo password=$PASSWORD >> ${EMS_PROPERTIES_FILE}
    echo exchange=$EXCHANGE_NAME >> ${EMS_PROPERTIES_FILE}
    echo heartbeat=$EMS_HEARTBEAT >> ${EMS_PROPERTIES_FILE}
    echo autodelete=$EMS_AUTODELETE >> ${EMS_PROPERTIES_FILE}
    echo type=$ENDPOINT >> ${EMS_PROPERTIES_FILE}
    echo hostname=$Hostname >> ${EMS_PROPERTIES_FILE}
    service openbaton-ems restart
}


################
#### Zabbix ####
################

configure_zabbix () {
    sed -i -e "s|ServerActive=127.0.0.1|ServerActive=${MONITORING_IP}:10051|g" -e "s|Server=127.0.0.1|Server=${MONITORING_IP}|g" -e "s|Hostname=Zabbix server|#Hostname=|g" /etc/zabbix/zabbix_agentd.conf
    service zabbix-agent restart
}


##############
#### Main ####
##############

if [ $(cat /etc/os-release | grep -i "ubuntu" | wc -l) -gt 0 ]; then
    os=ubuntu
elif [ $(cat /etc/os-release | grep -i "centos" | wc -l) -gt 0 ]; then
    os=centos
else
    os=undefined
    echo "OS is not supported... Thus neither EMS nor Zabbix gets installed/configured. Supported OS: [ubuntu, centos]"
    exit 1
fi

case ${os} in
    ubuntu)
        prepare_machine_ubuntu
        if [ -z "${MONITORING_IP}" ]; then
            echo "No MONITORING_IP is defined, I will not download zabbix-agent"
        else
	        install_zabbix_on_ubuntu
        fi
	    ;;
    centos)
	    prepare_machine_centos
        if [ -z "${MONITORING_IP}" ]; then
            echo "No MONITORING_IP is defined, I will not download zabbix-agent"
        else
            yum install -y redhat-lsb-core
            OS_DISTRIBUTION_RELEASE_MAJOR=$( lsb_release -a | grep "Release:" | awk -F'\t' '{ print $2 }' | awk -F'.' '{ print $1 }' )
            install_zabbix_on_centos
        fi
	    ;;
    *)
	    echo "OS not recognized"
	    exit 1
	    ;;
esac

prepare_machine_generic
install_configure_pip_packages
configure_ems
if [ -n "${MONITORING_IP}" ]; then
    configure_zabbix
fi
