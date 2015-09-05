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
