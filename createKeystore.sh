#!/bin/sh
keytool -keystore assets/my.bks -storetype BKS -provider org.bouncycastle.jce.provider.BouncyCastleProvider -storepass changeit -importcert -trustcacerts -alias GeoTrust -file GeoTrust_Global_CA.cer
keytool -keystore assets/my.bks -storetype BKS -provider org.bouncycastle.jce.provider.BouncyCastleProvider -storepass changeit -importcert -trustcacerts -alias GeoTrust -file Entrust_net.cer
