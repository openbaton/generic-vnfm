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


################
#### Ubuntu ####
################

install_ems_on_ubuntu () {
    result=$(dpkg -l | grep "ems" | grep -i "open baton\|openbaton" | wc -l)
    if [ ${result} -eq 0 ]; then
        echo "Downloading EMS from ${UBUNTU_EMS_REPOSITORY_HOSTNAME_OR_IP}"

        # TODO: use the general approach when the "openbaton-ems" pckg is added to all repositories
        #echo "deb http://${UBUNTU_EMS_REPOSITORY_HOSTNAME_OR_IP}/${UBUNTU_EMS_REPOSITORY_PATH}/${OS_DISTRIBUTION_CODENAME}/release ${OS_DISTRIBUTION_CODENAME} main" >> /etc/apt/sources.list
        echo "deb http://${UBUNTU_EMS_REPOSITORY_HOSTNAME_OR_IP}/repos/openbaton/stretch/release stretch main" >> /etc/apt/sources.list
        wget -O - http://get.openbaton.org/keys/openbaton.public.key | apt-key add -

        echo "Checking for running apt-get processes"
        while [ ! -z "$(ps -A | grep apt-get | awk '{print $1}')" ];do
            echo "Waiting for running apt-get processes to finish"
            sleep 5s
        done
        echo "Finished waiting for running apt-get processes"
        apt-get update
        cp /usr/share/zoneinfo/$TIMEZONE /etc/localtime
        apt-get install -y git
        if [ -z ${EMS_VERSION} ]; then
            apt-get install -y openbaton-ems=${EMS_VERSION}
        else
            apt-get install -y openbaton-ems
        fi
    else
        echo "EMS is already installed"
    fi
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

# TODO: create the new openbaton-ems.rpm and update the references to that EMS
install_ems_on_centos () {
    result=$(yum list installed | grep "ems" | grep -i "open baton\|openbaton" | wc -l)
    if [ ${result} -eq 0 ]; then
        echo "Downloading EMS from ${CENTOS_EMS_REPOSITORY_HOSTNAME_OR_IP}"
        echo "[openbaton]" >> /etc/yum.repos.d/OpenBaton.repo
        echo "name=Open Baton Repository" >> /etc/yum.repos.d/OpenBaton.repo
        echo "baseurl=http://${CENTOS_EMS_REPOSITORY_HOSTNAME_OR_IP}/${CENTOS_EMS_REPOSITORY_PATH}" >> /etc/yum.repos.d/OpenBaton.repo
        echo "gpgcheck=0" >> /etc/yum.repos.d/OpenBaton.repo
        echo "enabled=1" >> /etc/yum.repos.d/OpenBaton.repo
        cp /usr/share/zoneinfo/$TIMEZONE /etc/localtime
        yum install -y git
        yum install -y ems
        systemctl enable ems
        #systemctl start ems
    else
        echo "EMS is already installed"
    fi
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
fi

case ${os} in
    ubuntu) 
        OS_DISTRIBUTION_CODENAME=$( lsb_release -a | grep "Codename" | sed "s/[ \t]*//g" | awk -F':' '{ print $2 }' )
	    install_ems_on_ubuntu
        if [ -z "${MONITORING_IP}" ]; then
            echo "No MONITORING_IP is defined, I will not download zabbix-agent"
        else
	        install_zabbix_on_ubuntu
        fi
	    ;;
    centos)
	    install_ems_on_centos
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

configure_ems
if [ -n "${MONITORING_IP}" ]; then
    configure_zabbix
fi
