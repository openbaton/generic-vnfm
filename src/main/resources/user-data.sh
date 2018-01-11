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
export OFFLINE_EMS=
# Hostname/IP and path of the EMS repository
export UBUNTU_EMS_REPOSITORY_HOSTNAME_OR_IP="get.openbaton.org"
export UBUNTU_EMS_REPOSITORY_PATH="repos/apt/debian/"
export CENTOS_EMS_REPOSITORY_HOSTNAME_OR_IP="get.openbaton.org"
export CENTOS_EMS_REPOSITORY_PATH="repos/rpm/"

export OS_DISTRIBUTION_RELEASE_MAJOR=

export LANG=en_US.UTF-8
export LANGUAGE=en_US.UTF-8
export LC_COLLATE=C
export LC_CTYPE=en_US.UTF-8
source /etc/bashrc



################
#### Ubuntu ####
################

install_ems_on_ubuntu () {
    if [ ${OFFLINE_EMS} -eq 1 ]; then
        mkdir -p /opt/openbaton/
        python -c "import urllib2;response = urllib2.urlopen('http://${BROKER_IP}:9999/api/v1/download/ems-package.tar.gz');ems = response.read();file = open('ems-package.tar.gz', 'w');file.write(ems);file.close();"
        tar -xf ems-package.tar.gz --directory /opt/openbaton/
        bash /opt/openbaton/ems-package/install-ems-with-dependencies.sh
        add-upstart-ems
    else
        echo "Checking for running apt-get processes"
        while [ ! -z "$(ps -A | grep apt-get | awk '{print $1}')" ];do
        echo "Waiting for running apt-get processes to finish"
        sleep 5s
        done
        echo "Finished waiting for running apt-get processes"
        apt-get update
        apt-get install -y python
        wget https://bootstrap.pypa.io/get-pip.py
        python get-pip.py
        apt-get install -y git
        pip install pika
        pip install gitpython
        cp /usr/share/zoneinfo/$TIMEZONE /etc/localtime
        mkdir /opt/openbaton
        pip install openbaton-ems
        add-upstart-ems

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

install_ems_on_centos () {
    if [ ${OFFLINE_EMS} -eq 1 ]; then
        mkdir -p /opt/openbaton/
        python -c "import urllib2;response = urllib2.urlopen('http://${BROKER_IP}:9999/api/v1/download/ems-package.tar.gz');ems = response.read();file = open('ems-package.tar.gz', 'w');file.write(ems);file.close();"
        tar -xf ems-package.tar.gz --directory /opt/openbaton/
        bash /opt/openbaton/ems-package/install-ems-with-dependencies.sh
        add-upstart-ems
    else
       yum --enablerepo=extras install -y epel-release
       yum install -y wget
       wget https://bootstrap.pypa.io/get-pip.py
       python get-pip.py
       yum install -y git
       pip install pika
       pip install gitpython
       cp /usr/share/zoneinfo/$TIMEZONE /etc/localtime
       mkdir /opt/openbaton
       pip install openbaton-ems
       add-upstart-ems
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
    mkdir -p /etc/openbaton/ems
    echo [ems] > /etc/openbaton/ems/conf.ini
    echo broker_ip=$BROKER_IP >> /etc/openbaton/ems/conf.ini
    echo broker_port=$BROKER_PORT >> /etc/openbaton/ems/conf.ini
    echo username=$USERNAME >> /etc/openbaton/ems/conf.ini
    echo password=$PASSWORD >> /etc/openbaton/ems/conf.ini
    echo exchange=$EXCHANGE_NAME >> /etc/openbaton/ems/conf.ini
    echo heartbeat=$EMS_HEARTBEAT >> /etc/openbaton/ems/conf.ini
    echo autodelete=$EMS_AUTODELETE >> /etc/openbaton/ems/conf.ini
    echo type=$ENDPOINT >> /etc/openbaton/ems/conf.ini
    echo hostname=$Hostname >> /etc/openbaton/ems/conf.ini

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
