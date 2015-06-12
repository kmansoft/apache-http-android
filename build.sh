#!/bin/bash

echo
echo "*** Building core"
echo

( cd httpcomponents-core && gradle clean createAndroidJar )

echo
echo "*** Building client"
echo

( cd httpcomponents-client-android && gradle clean createAndroidJar )

echo
echo "***Build output:"
echo

ls -l httpcomponents-core/build/libs/ httpcomponents-client-android/build/libs/

echo
echo "*** Copying:"
echo

cp httpcomponents-core/build/libs/* ../AquaMail/libs/
cp httpcomponents-client-android/build/libs/* ../AquaMail/libs/

echo
echo "*** Done:"
echo

ls -l ../AquaMail/libs/http*

