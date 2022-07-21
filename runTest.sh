#!/bin/bash

VALID_ARGS=$(getopt -o s:c:a:hn:t:q: --long skills:,contacts_per_skill:,agents:,help,cluster_name:,stream_name:,queue_name: -- "$@")
if [[ $? -ne 0 ]]; then
    exit 1;
fi

eval set -- "$VALID_ARGS"

parameters=""

while [ : ]; do
  case "$1" in
    -s | --skills)
        echo "Processing 'skills' option '$2'"
		parameters+="-Dskills=$2 "
        shift 2
        ;;
    -c | --contacts_per_skill)
        echo "Processing 'contacts_per_skill' option '$2'"
		parameters+="-Dcontacts_per_skill=$2 "
        shift 2
        ;;
    -a | --agents)
        echo "Processing 'agents' option '$2'"
		parameters+="-Dagents=$2 "
        shift 2
        ;;
	-n | --cluster_name)
		echo "Processing 'cluster_name' option '$2'"
		parameters+="-Dcluster_name=$2 "
		shift 2
		;;
	-t | --stream_name)
		echo "Processing 'stream_name' option '$2'"
		parameters+="-Dstream_name=$2 "
		shift 2
		;;
	-q | --queue_name)
		echo "Processing 'queue_name' option '$2'"
		parameters+="-Dqueue_name=$2 "
		shift 2
		;;
    -h | --help) shift; 
        echo "Options: 
	-s, --skills: Number of skills
	-c, --contacts_per_skill: Number of contacts per skill
	-a, --agents: Number of agents
	-n, --cluster_name: Name of the cluster_name
	-t, --stream_name: Name of the matchrequests stream_name
	-q, --queue_name: Name of the results queue" >&2
            exit 1
        ;;
	--) break;
  esac
done

mvn gatling:test $parameters