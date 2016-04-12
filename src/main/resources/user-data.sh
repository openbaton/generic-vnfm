#!/bin/bash

export MONITORING_IP=
export TIMEZONE=
export BROKER_IP=
export USERNAME=
export PASSWORD=
export EXCHANGE_NAME=
export EMS_HEARTBEAT=
export EMS_AUTODELETE=
export EMS_VERSION=
export ENDPOINT=

adduser user
echo -e "password\\npassword" | (passwd user)
echo "deb http://get.openbaton.org/repos/apt/debian/ ems main" >> /etc/apt/sources.list
wget -O - http://get.openbaton.org/public.gpg.key | apt-key add -
apt-get update
cp /usr/share/zoneinfo/$TIMEZONE /etc/localtime
apt-get install git -y
echo "Installing zabbix-agent for server at $MONITORING_IP"
sudo apt-get install -y zabbix-agent
sudo sed -i -e 's/ServerActive=127.0.0.1/ServerActive=$MONITORING_IP:10051/g' -e 's/Server=127.0.0.1/Server=$MONITORING_IP/g' -e 's/Hostname=Zabbix server/#Hostname=/g' /etc/zabbix/zabbix_agentd.conf
sudo service zabbix-agent restart
sudo rm zabbix-release_2.2-1+precise_all.deb
echo "finished installing zabbix-agent!"

apt-get install -y ems-$EMS_VERSION
mkdir -p /etc/openbaton/ems
echo [ems] > /etc/openbaton/ems/conf.ini
echo orch_ip=$BROKER_IP >> /etc/openbaton/ems/conf.ini
echo username=$USERNAME >> /etc/openbaton/ems/conf.ini
echo password=$PASSWORD >> /etc/openbaton/ems/conf.ini
echo exchange=$EXCHANGE_NAME >> /etc/openbaton/ems/conf.ini
echo heartbeat=$EMS_HEARTBEAT >> /etc/openbaton/ems/conf.ini
echo autodelete=$EMS_AUTODELETE >> /etc/openbaton/ems/conf.ini
export hn=`hostname`
echo type=$ENDPOINT >> /etc/openbaton/ems/conf.ini
echo hostname=$hn >> /etc/openbaton/ems/conf.ini
echo orch_port=61613 >> /etc/openbaton/ems/conf.ini
service ems restart
