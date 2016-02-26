#!/bin/bash
	

	# Helper functions
	echoProgress()
	{
	    setColor 6
	    printf "%-70s" "$1..."
	    resetColor
	    return 0
	}
	

	echoError()
	{
	    setColor 6
	    printf "ERROR"
	    if [ ! -z "$1"  ]
	    then
	        resetColor
	        printf " [$1]"
	    fi
	    printf "\n"
	    resetColor
	    return 0
	}
	

	echoOK()
	{
	    setColor 2
	    printf "OK"
	    if [ ! -z "$1"  ]
	    then
	        resetColor
	        printf " [$1]"
	    fi
	    printf "\n"
	    resetColor
	    return 0
	

	}
	

	setColor()
	{
	    tput setaf $1 2>/dev/null
	}
	

	resetColor()
	{
	    tput sgr0 2>/dev/null
	}
	

	if [ $EUID -ne 0  ]; then
	    echo "This script must be run as root" 1>&2
	    exit 1
	fi
	

	################
	job=$1
	name=$2
	buildNumber=$3
	

	jenkins="http://buildprod2.lxc2.prod.amz.fullfacing.com:8080/job"
	################
	

	if [ -f /tmp/${name}.json ]; then
	    rm /tmp/${name}.json
	fi
	

	curl --header 'Cache-Control: no-cache' -s ${jenkins}/${job}/${buildNumber}/api/json >> /tmp/${name}.json
	version="$(jshon -e number < /tmp/${name}.json)"
	jar="$(jshon -e artifacts -a -e fileName -u  < /tmp/${name}.json | grep jar)"
	sysd="$(jshon -e artifacts -a -e fileName -u  < /tmp/${name}.json | grep "\.service")"
	url="$(jshon -e url -u < /tmp/${name}.json)"
	echo $url
	

	echoProgress "Checking latest version"
	file="/opt/fullfacing/${name}/archives/${jar}"
	if [ -f $file ]; then
	    echoOK "Already the latest version installed"
	    exit 0
	else
	    echoOK "Updating to build ${jar}"
	fi
	

	mkdir -p /opt/fullfacing/$name/archives
	cd /opt/fullfacing/$name/archives
	echoProgress "Downloading ${jar}"
	curl -O -s $url/artifact/target/scala-2.11/${jar}
	cd ..
	if [ -f ${name}.jar ]; then
	    rm ${name}.jar
	fi
	ln -s archives/${jar} ${name}.jar
	curl -O -s $url/artifact/etc/systemd/start_service.sh
	chmod +x start_service.sh
	echoOK "download complete"
	

	echoOK "updating sysd script"
	cd /etc/systemd/system
	curl -O -s $url/artifact/etc/systemd/${sysd}
	

	echoProgress "Updating configs"
	

	mkdir -p /etc/fullfacing/${name}
	cd /etc/fullfacing/${name}
	

	curl -O -s $url/artifact/etc/fullfacing/prod/application.conf
	curl -O -s $url/artifact/etc/fullfacing/prod/logback.xml
	curl -O -s $url/artifact/etc/fullfacing/prod/queue.properties
	curl -O -s $url/artifact/etc/fullfacing/prod/mongo.properties

	
	echoProgress "Finishing update/install"
	echoOK "update/install complete"
	systemctl stop $sysd
	systemctl enable $sysd
	systemctl start $sysd
