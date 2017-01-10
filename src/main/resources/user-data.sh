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
export ENDPOINT=

# Hostname/IP and path of the EMS repository
export UBUNTU_EMS_REPOSITORY_HOSTNAME_OR_IP="get.openbaton.org"
export UBUNTU_EMS_REPOSITORY_PATH="repos/apt/debian/"
export CENTOS_EMS_REPOSITORY_HOSTNAME_OR_IP="get.openbaton.org"
export CENTOS_EMS_REPOSITORY_PATH="repos/rpm/"


################
#### Ubuntu ####
################

install_ems_on_ubuntu () {
    result=$(dpkg -l | grep "ems-${EMS_VERSION}" | wc -l)
    if [ ${result} -eq 0 ]; then
        echo "Downloading EMS from ${UBUNTU_EMS_REPOSITORY_HOSTNAME_OR_IP}"
        echo "deb http://${UBUNTU_EMS_REPOSITORY_HOSTNAME_OR_IP}/${UBUNTU_EMS_REPOSITORY_PATH} ems main" >> /etc/apt/sources.list
        wget -O - http://get.openbaton.org/public.gpg.key | apt-key add -
        apt-get update
        cp /usr/share/zoneinfo/$TIMEZONE /etc/localtime
        apt-get install git -y
        apt-get install -y --force-yes ems-$EMS_VERSION
    else
        echo "EMS is already installed"
    fi
}

install_zabbix_on_ubuntu () {
    echo "Installing zabbix-agent for server at $MONITORING_IP"
    apt-get install -y zabbix-agent
    sed -i -e "s/ServerActive=127.0.0.1/ServerActive=$MONITORING_IP:10051/g" -e "s/Server=127.0.0.1/Server=$MONITORING_IP/g" -e "s/Hostname=Zabbix server/#Hostname=/g" /etc/zabbix/zabbix_agentd.conf
    service zabbix-agent restart
    rm zabbix-release_2.2-1+precise_all.deb
    echo "finished installing zabbix-agent!"
}


################
#### CentOS ####
################

install_ems_on_centos () {
    result=$(yum list installed | grep "ems" | grep "${EMS_VERSION}" | wc -l)
    if [ ${result} -eq 0 ]; then
        echo "Downloading EMS from ${CENTOS_EMS_REPOSITORY_HOSTNAME_OR_IP}"
        echo "[openbaton]" >> /etc/yum.repos.d/OpenBaton.repo
        echo "name=Open Baton Repository" >> /etc/yum.repos.d/OpenBaton.repo
        echo "baseurl=http://${CENTOS_EMS_REPOSITORY_HOSTNAME_OR_IP}/${CENTOS_EMS_REPOSITORY_PATH}" >> /etc/yum.repos.d/OpenBaton.repo
        echo "gpgcheck=0" >> /etc/yum.repos.d/OpenBaton.repo
        echo "enabled=1" >> /etc/yum.repos.d/OpenBaton.repo
        yum install -y ems
        service ems start
    else
        echo "EMS is already installed"
    fi
}


#############
#### EMS ####
#############

export LANG=en_US.UTF-8
export LANGUAGE=en_US.UTF-8
export LC_COLLATE=C
export LC_CTYPE=en_US.UTF-8
source /etc/bashrc

#adduser user
#echo -e "password\\npassword" | (passwd user)

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
        if [ -z "$MONITORING_IP" ]; then
            echo "No MONITORING_IP is defined, I will not download zabbix-agent"
        else
	        install_zabbix_on_ubuntu
        fi
	    ;;
    centos)
	    install_ems_on_centos
	    ;;
    *)
	    echo "OS not recognized"
	    exit 1
	    ;;
esac	


mkdir -p /etc/openbaton/ems
echo [ems] > /etc/openbaton/ems/conf.ini
echo broker_ip=$BROKER_IP >> /etc/openbaton/ems/conf.ini
echo broker_port=$BROKER_PORT >> /etc/openbaton/ems/conf.ini
echo username=$USERNAME >> /etc/openbaton/ems/conf.ini
echo password=$PASSWORD >> /etc/openbaton/ems/conf.ini
echo exchange=$EXCHANGE_NAME >> /etc/openbaton/ems/conf.ini
echo heartbeat=$EMS_HEARTBEAT >> /etc/openbaton/ems/conf.ini
echo autodelete=$EMS_AUTODELETE >> /etc/openbaton/ems/conf.ini
export hn=`hostname`
echo type=$ENDPOINT >> /etc/openbaton/ems/conf.ini
echo hostname=$hn >> /etc/openbaton/ems/conf.ini

service ems restart
