@echo off
echo *** CREATE KEYSTORE ***
keytool -genkey -alias test -keyalg RSA -validity 7 -keystore keystore
rem password=password

rem keytool -list -v -keystore keystore

echo *** EXPORT SELF SIGNED CERTIFICATE ***
keytool -export -alias test -keystore keystore -rfc -file test.cer

echo *** IMPORT CERTIFICATE INTO NEW TRUSTSTORE ***
keytool -import -alias test -file duke.cer -keystore truststore
rem password=password

rem keytool -list -v -keystore truststore
