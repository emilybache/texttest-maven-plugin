#!/bin/bash -l

# This script will be replaced with this maven plugin when it is ready.

# directory of this maven project must be passed as first argument
DIR=$1
# name of the texttest app to run is passed as second argument
APP_NAME=$2

TARGET_DIR=$DIR/target

# install these tests in the TEXTTEST_HOME directory.
cd ${TEXTTEST_HOME}
rm ${APP_NAME}
ln -s ${DIR}/src/it/texttest ${APP_NAME}
ls -l
cd -

# get texttest to write test results under the target directory
SANDBOX=${TARGET_DIR}/sandbox
mkdir ${SANDBOX}
export TEXTTEST_TMP=${SANDBOX}

# We're making sure that if we kill this script, that the texttest process is also killed
trap : SIGTERM SIGINT

texttest.py -b all -a ${APP_NAME} -c ${DIR}
exitcode=$?
TEXTTEST_PID=$!
wait $TEXTTEST_PID
if [ $exitcode -gt 128 ]; then
    kill $TEXTTEST_PID
fi

exit $exitcode
