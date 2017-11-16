#!/bin/bash

TIMEOUT=30

# Adapted from:
#   https://github.com/Eficode/wait-for

wait_for() {
  for i in `seq $TIMEOUT` ; do
    nc -z "$HOST" "$PORT" > /dev/null 2>&1
    
    result=$?
    if [ $result -eq 0 ] ; then
      if [ $# -gt 0 ] ; then
        exec "$@"
      fi
      exit 0
    fi
    echo "Waiting for Fetch server to come up..." >&2
    sleep 1
  done
  echo "Timed out waiting for Fetch server" >&2
  exit 1
}

argc=$#
argv=($@)

HELP=false
DIRECT=false
for (( j=0; j<argc; j++ )); do
    # Check if the help flag was passed
    if [ ${argv[j]} == "--help" ]; then
        HELP=true
    fi

    # Check if the direct ingest flag was passed
    if [ ${argv[j]} == "--direct" ]; then
        DIRECT=true
    fi

    # Check if there is an '--fh' arg that is not the last arg
    if [ ${argv[j]} == "--fh" ] && [ $((j)) -lt $((argc-1)) ]; then
	# Set HOST to arg after '--fh'
	HOST=${argv[j+1]}
    fi

    # Check if there is an '--fp' arg that is not the last arg
    if [ ${argv[j]} == "--fp" ] && [ $((j)) -lt $((argc-1)) ]; then
	# Set PORT to arg after '--fp'
	PORT=${argv[j+1]}
    fi
done

if $HELP || $DIRECT ; then
    exec "$@"
    exit 0
fi

# Fetch HOST and PORT are needed to ping the service before launching search
if [ "$HOST" == "" -o "$PORT" == "" ]; then
    echo "Error: you must specify a Fetch server using '--fp PORT' and '--fh HOST'" >&2
    exit 2
fi

wait_for "$@"
